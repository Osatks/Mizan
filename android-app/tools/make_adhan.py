#!/usr/bin/env python3
"""Builds the adhan recordings used by the app from their original files.

    python3 tools/make_adhan.py <folder with the original downloads>

For each recording in RECORDINGS (sources and licences: shared/src/commonMain/assets/licenses/adhan-recordings.txt):
  1. cut to the adhan itself (no announcements or other speech before/after), mono, 80 Hz high-pass,
     gain to -16 LUFS (EBU R128 measurement) with a -1.5 dB peak limiter, short fade in/out;
  2. <id>.m4a — the full adhan, AAC-LC mono 64 kbps (Android alarm + in-app playback on both systems);
  3. adhan_<id>.caf — the iOS notification sound: the opening 28 s with a 3 s fade-out, IMA4 22.05 kHz mono
     (iOS ignores notification sounds of 30 s or more and plays the default sound instead).
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
    # لم يُنزَّل بعد (حدّ طلبات ويكيميديا): المسجد النبوي، File:33937_ejaz215_call-to-prayer-from-the-prophet-s-mo.ogg (CC BY 3.0)
]

NOTIFY_SECONDS = 28.0
TARGET_LUFS = -16.0


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


def encode(wav: Path, m4a: Path, caf: Path):
    run("ffmpeg", "-y", "-v", "error", "-i", str(wav), "-c:a", "aac", "-b:a", "64k", "-ac", "1", "-movflags", "+faststart", str(m4a))
    fade = f"afade=t=out:st={NOTIFY_SECONDS - 3:.1f}:d=3"
    run("ffmpeg", "-y", "-v", "error", "-i", str(wav), "-t", str(NOTIFY_SECONDS), "-af", f"{fade},aresample=22050",
        "-ac", "1", "-c:a", "adpcm_ima_qt", "-f", "caf", str(caf))


def main():
    raw = Path(sys.argv[1])
    tmp = Path(sys.argv[2]) if len(sys.argv) > 2 else raw / "_work"
    for d in (ASSETS, IOS, SITE, tmp):
        d.mkdir(parents=True, exist_ok=True)
    run("ffmpeg", "-y", "-v", "error", "-f", "lavfi", "-i", "anullsrc=r=22050:cl=mono", "-t", "1",
        "-c:a", "adpcm_ima_qt", "-f", "caf", str(IOS / "mizan_vibrate.caf"))
    for rid, name, start, end, bundled in RECORDINGS:
        src = raw / name
        if not src.exists() or src.stat().st_size < 10_000:
            print(f"skip {rid}: {src} missing")
            continue
        wav = tmp / f"{rid}.wav"
        master(src, start, end, wav)
        m4a = (ASSETS if bundled else SITE) / f"{rid}.m4a"
        caf = (IOS if bundled else SITE) / f"adhan_{rid}.caf"
        encode(wav, m4a, caf)
        secs = float(run("ffprobe", "-v", "error", "-show_entries", "format=duration", "-of", "csv=p=0", str(m4a)).stdout)
        print(f"{rid}: {secs:.0f} s, m4a {m4a.stat().st_size // 1000} KB, caf {caf.stat().st_size // 1000} KB")


if __name__ == "__main__":
    main()
