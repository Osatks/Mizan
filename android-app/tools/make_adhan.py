#!/usr/bin/env python3
"""Builds the adhan recordings used by the app from their original files.

    python3 tools/make_adhan.py <folder with the original downloads> [work folder] [--only=id,id]
    python3 tools/make_adhan.py --notify-only [--only=id,id]   # only the notification sounds, from the m4a files

For each recording in RECORDINGS (sources and licences: shared/src/commonMain/assets/licenses/adhan-recordings.txt):
  1. cut to the adhan itself (no announcements or other speech before/after), mono, 80 Hz high-pass,
     gain to -16 LUFS (EBU R128 measurement) with a -1.5 dB peak limiter, short fade in/out;
  2. <id>.m4a — the full adhan, AAC-LC mono 64 kbps (Android alarm + in-app playback on both systems);
  3. adhan_<id>.caf — the iOS notification sound: THE FIRST TWO TAKBIRS ONLY («اللهُ أكبر، اللهُ أكبر»), then a
     2.5 s fade-out to silence, IMA4 22.05 kHz mono (iOS ignores notification sounds of 30 s or more).
     The end of the first two takbirs (takbir_end) is the start of the first clear pause in the adhan: the
     loudness envelope (50 ms RMS, 0.25 s smoothing) is "voiced" above p10 + 35 % of (p90 − p10) of the whole
     recording; voiced runs closer than 0.6 s merge, runs under 0.8 s are dropped, and the takbirs end with the
     first runs that hold 3 % of the adhan's length (slow and fast muezzins alike: most say both takbirs in one
     breath, a few pause between them). TAKBIR overrides it for a recording when listening shows otherwise.
     The value is printed as `takbir = …` for AdhanCatalog.kt: on Android the alarm fades out there too, and
     opening the app soon after continues the adhan from that point (AdhanResume), so it sounds unbroken.
     Every new recording follows the same rule; AdhanTest checks each catalogue entry against its .caf.
Bundled recordings go to shared/src/commonMain/assets/adhan (m4a) and iosApp/Sounds (caf); the others to the
website's src/adhan, published at https://mizan-al-muslem.com/adhan/ and downloaded by the app on demand.
Also writes iosApp/Sounds/mizan_vibrate.caf: one second of silence, so an iOS "vibrate only" notification
gets the system vibration without an audible sound.
"""
import json
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent          # android-app
REPO = ROOT.parent
ASSETS = ROOT / "shared/src/commonMain/assets/adhan"
IOS = REPO / "iosApp/Sounds"
SITE = REPO / "src/adhan"

# id, source file (as downloaded from the source URL in the licences file), start s, end s, bundled
RECORDINGS = [
    # archive.org …asim-javed/ICR_Friday_Prayer_1st_Adhan_17th_May_2024_9th_Dhul_Qadah_1445_by_Asim_Javed.flac
    ("asim", "ia_icr_asim.flac", 0.0, 195.5, True),
    # archive.org aporee_58326_66906/915cABKALKANMuezzincallat545220922674.mp3
    ("kalkan_fajr", "ap_kalkan_915c.mp3", 25.0, 304.0, True),
    # archive.org AzanZhurMasjidKlcc/CallToPrayer.mp3
    ("klcc", "ia_klcc.mp3", 11.0, 181.5, False),
    # archive.org IshaAzanByMoazzambegMirza07092013/Isha Azan by Moazzambeg Mirza (07-09-2013).mp3
    ("mirza", "ia_mirza.mp3", 11.0, 260.0, False),
    # commons.wikimedia.org File:Beautiful_adhan.ogg
    ("adam", "c_beautiful.ogg", 0.0, 152.0, False),
    # commons.wikimedia.org File:33937_ejaz215_call-to-prayer-from-the-prophet-s-mo.ogg (from freesound.org/s/33937)
    ("madinah", "c_madinah_ejaz.ogg", 0.0, None, False),
    # commons.wikimedia.org File:Azan.ogg (the du'a after the adhan is left out)
    ("andrewler", "c_azan.ogg", 0.0, 158.5, False),
    # commons.wikimedia.org File:The_Adhan_-_Muslim_Call_to_Prayer_-_Aaqib_Azeez.mp3
    ("aaqib", "c_aaqib.mp3", 0.0, None, False),
    # archive.org AdanAlfajrFouadAdan/Mp32.mp3
    ("fouad", "ia_fouad2.mp3", 0.0, None, True),
    # archive.org AdanAlfajrFouadAdan/adan alfajr  fouad adan.mp3
    ("fouad_fajr", "ia_fouad_fajr.mp3", 0.0, None, True),
    # archive.org aporee_63024_72492/MorningadhaninKa.mp3
    ("kas_fajr", "ap_kas.mp3", 11.5, 323.0, False),
    # commons.wikimedia.org File:Eid_al-Fitr_Fajr_azan_at_Malmö_Mosque_-_19_August_2012.webm (its 240p transcode;
    # the salawat after the adhan is left out)
    ("malmo_fajr", "c_malmo_fajr.webm", 3.5, 237.0, False),
    # commons.wikimedia.org File:Adhan_in_Shalqar_mosque.webm (its 240p transcode)
    ("shalqar", "c_shalqar.webm", 2.5, 134.5, False),
]

TARGET_LUFS = -16.0
# أول تكبيرتين ثم يتلاشى الصوت في هذه المدة (والأذان يكمل من نهايتهما في التطبيق)
FADE = 2.5
# نهاية التكبيرتين بالثواني إن خالف السماعُ ما يحسبه takbir_end (لا شيء الآن: كل التسجيلات راجعناها)
TAKBIR: dict = {}


def run(*args):
    return subprocess.run(args, check=True, capture_output=True, text=True)


def master(src: Path, start: float, end, out: Path):
    """Cut, mono, high-pass, measured gain to -16 LUFS with a -1.5 dB peak limiter → 44.1 kHz WAV.

    (loudnorm's linear mode leaves these dynamic recordings 3–6 dB short of the target, and its dynamic mode
    pumps the reverb between phrases, so a plain gain plus a limiter is used.)"""
    cut = ["-ss", str(start)] + (["-to", str(end)] if end else [])
    pre = "aformat=channel_layouts=mono,highpass=f=80,aresample=44100"
    r = subprocess.run(["ffmpeg", "-hide_banner", "-nostats", *cut, "-i", str(src), "-ac", "1",
                        "-af", f"{pre},loudnorm=I=-16:TP=-1.5:LRA=11:print_format=json", "-f", "null", "-"],
                       check=True, capture_output=True, text=True)
    m = json.loads(r.stderr[r.stderr.rindex("{"):r.stderr.rindex("}") + 1])
    dur = float(run("ffprobe", "-v", "error", "-show_entries", "format=duration", "-of", "csv=p=0", str(src)).stdout or 0)
    length = (end or dur) - start
    gain = TARGET_LUFS - float(m["input_i"])
    norm = f"volume={gain:.2f}dB,alimiter=limit=0.84:attack=5:release=80:level=false"
    fades = f"afade=t=in:d=0.4,afade=t=out:st={max(0.0, length - 1.5):.2f}:d=1.5"
    run("ffmpeg", "-y", "-v", "error", *cut, "-i", str(src), "-ac", "1", "-af", f"{pre},{norm},{fades},aresample=44100",
        "-c:a", "pcm_s16le", str(out))


def encode(wav: Path, m4a: Path):
    run("ffmpeg", "-y", "-v", "error", "-i", str(wav), "-c:a", "aac", "-b:a", "64k", "-ac", "1", "-movflags", "+faststart", str(m4a))


FRAME = 0.05


def takbir_end(audio: Path) -> float:
    """End of the first two takbirs (seconds): the start of the first clear pause (rule in the module doc)."""
    import numpy as np
    raw = subprocess.run(["ffmpeg", "-v", "quiet", "-i", str(audio), "-ac", "1", "-ar", "8000", "-f", "s16le", "-"],
                         check=True, capture_output=True).stdout
    x = np.frombuffer(raw, np.int16).astype(np.float32) / 32768
    n = int(8000 * FRAME)
    k = len(x) // n
    db = 20 * np.log10(np.sqrt((x[:k * n].reshape(k, n) ** 2).mean(1)) + 1e-6)
    db = np.convolve(db, np.ones(5) / 5, mode="same")
    lo, hi = np.percentile(db, 10), np.percentile(db, 90)
    voiced = db > lo + 0.35 * (hi - lo)
    runs, i = [], 0
    while i < k:
        if voiced[i]:
            j = i
            while j < k and voiced[j]:
                j += 1
            if runs and i * FRAME - runs[-1][1] < 0.6:
                runs[-1][1] = j * FRAME
            else:
                runs.append([i * FRAME, j * FRAME])
            i = j
        else:
            i += 1
    total, held = len(x) / 8000, 0.0
    for a, b in (r for r in runs if r[1] - r[0] >= 0.8):
        held += b - a
        if held >= 0.03 * total:
            return round(b, 2)
    raise SystemExit(f"{audio}: no takbir found")


def notify(rid: str, m4a: Path, caf: Path) -> float:
    """adhan_<id>.caf: the first two takbirs, then a fade to silence (under 30 s for iOS)."""
    t = TAKBIR.get(rid) or takbir_end(m4a)
    assert 3 <= t <= 26, f"{rid}: takbirs end at {t} s?"
    run("ffmpeg", "-y", "-v", "error", "-i", str(m4a), "-t", f"{t + FADE:.2f}",
        "-af", f"afade=t=out:st={t:.2f}:d={FADE},aresample=22050",
        "-ac", "1", "-c:a", "adpcm_ima_qt", "-f", "caf", str(caf))
    return t


def main():
    only = {i for a in sys.argv[1:] if a.startswith("--only=") for i in a[7:].split(",")}
    if "--notify-only" in sys.argv:
        for rid, _, _, _, bundled in RECORDINGS:
            if only and rid not in only:
                continue
            m4a = (ASSETS if bundled else SITE) / f"{rid}.m4a"
            t = notify(rid, m4a, (IOS if bundled else SITE) / f"adhan_{rid}.caf")
            print(f'{rid}: takbir = {t}')
        return
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    raw = Path(args[0])
    tmp = Path(args[1]) if len(args) > 1 else raw / "_work"
    for d in (ASSETS, IOS, SITE, tmp):
        d.mkdir(parents=True, exist_ok=True)
    run("ffmpeg", "-y", "-v", "error", "-f", "lavfi", "-i", "anullsrc=r=22050:cl=mono", "-t", "1",
        "-c:a", "adpcm_ima_qt", "-f", "caf", str(IOS / "mizan_vibrate.caf"))
    for rid, name, start, end, bundled in RECORDINGS:
        if only and rid not in only:
            continue
        src = raw / name
        if not src.exists() or src.stat().st_size < 10_000:
            print(f"skip {rid}: {src} missing")
            continue
        wav = tmp / f"{rid}.wav"
        master(src, start, end, wav)
        m4a = (ASSETS if bundled else SITE) / f"{rid}.m4a"
        caf = (IOS if bundled else SITE) / f"adhan_{rid}.caf"
        encode(wav, m4a)
        t = notify(rid, m4a, caf)
        secs = float(run("ffprobe", "-v", "error", "-show_entries", "format=duration", "-of", "csv=p=0", str(m4a)).stdout)
        print(f"{rid}: {secs:.0f} s, m4a {m4a.stat().st_size // 1000} KB, caf {caf.stat().st_size // 1000} KB, takbir = {t}")


if __name__ == "__main__":
    main()
