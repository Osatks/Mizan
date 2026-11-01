#!/usr/bin/env python3
"""
صور المتجر الأنيقة من لقطات المحاكي: لكل لقطة خلفية متدرجة بألوان التطبيق ونجمة ثمانية خافتة، وعنوان وسطر شرح،
واللقطة في إطار بزوايا مستديرة وظل. الألوان وموضع العنوان تتبدل من صورة إلى أخرى فلا تتكرر.

    python3 iosApp/fastlane/frame/frame.py iosApp/fastlane/screenshots-raw iosApp/fastlane/screenshots

المقاس نفسه مقاس اللقطة (iPhone 6.9 بوصة 1320×2868، iPad 13 بوصة 2064×2752)، فيقبلها deliver كما هي.
يحتاج Pillow مع libraqm (لتشكيل الحروف العربية). الخط IBM Plex Sans Arabic برخصة SIL OFL (OFL.txt).
"""
import math
import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageFont, features

HERE = Path(__file__).resolve().parent
BOLD = str(HERE / "IBMPlexSansArabic-Bold.ttf")
REGULAR = str(HERE / "IBMPlexSansArabic-Regular.ttf")

# العنوان وسطر الشرح لكل شاشة (بلا بادئة ipad_ ورقمها)
CAPTIONS = {
    "home": {
        "ar": ("كل ما يحتاجه المسلم", "مواقيت الصلاة والقرآن والأذكار والقبلة في تطبيق واحد"),
        "en": ("Everything a Muslim needs", "Prayer times, Quran, adhkar and qibla in one app"),
    },
    "quran_page": {
        "ar": ("المصحف بالرسم العثماني", "مع التفسير والترجمة والتلاوة الصوتية"),
        "en": ("The Mushaf, beautifully", "Uthmani script with tafsir, translations and recitation"),
    },
    "prayer": {
        "ar": ("مواقيت دقيقة لبلدك", "حسب الجهة الرسمية، مع الأذان والإقامة"),
        "en": ("Accurate prayer times", "Following your country's official authority, with adhan"),
    },
    "adhkar": {
        "ar": ("أذكار الصباح والمساء", "حصن المسلم مع أدلّتها، في كل وقت"),
        "en": ("Morning & evening adhkar", "Hisn al-Muslim with its sources, for every moment"),
    },
    "quran_index": {
        "ar": ("١١٤ سورة بين يديك", "بحث سريع، وتابع القراءة من حيث توقفت"),
        "en": ("All 114 surahs", "Quick search, and continue where you stopped"),
    },
    "qibla": {
        "ar": ("القبلة من أي مكان", "بوصلة دقيقة بحساسات جهازك"),
        "en": ("Find the qibla anywhere", "A precise compass using your phone's sensors"),
    },
    "zakat": {
        "ar": ("احسب زكاتك بسهولة", "المال والذهب والأسهم مع النصاب"),
        "en": ("Zakat made simple", "Cash, gold and shares, with the nisab"),
    },
    "tasbih": {
        "ar": ("سبحة في جيبك", "عدّاد للتسبيح مع أذكار جاهزة"),
        "en": ("A tasbih in your pocket", "A dhikr counter with ready-made adhkar"),
    },
}

# خلفيات متناوبة: (أعلى، أسفل، لون العنوان، لون الشرح، لون الزخرفة)
PALETTES = [
    ((10, 74, 60), (6, 40, 33), (255, 255, 255), (230, 201, 128), (230, 201, 128)),     # زمردي عميق
    ((246, 240, 226), (233, 222, 196), (12, 76, 61), (120, 96, 52), (176, 141, 66)),    # عاجي وذهبي
    ((14, 46, 58), (9, 24, 34), (255, 255, 255), (152, 210, 196), (110, 190, 170)),     # ليلي فيروزي
    ((20, 98, 78), (11, 61, 49), (250, 241, 214), (255, 255, 255), (250, 230, 170)),    # أخضر مشرق
]


def font(path, size):
    return ImageFont.truetype(path, size, layout_engine=ImageFont.Layout.RAQM)


def gradient(w, h, top, bottom):
    g = Image.new("RGB", (1, 256))
    for y in range(256):
        t = y / 255
        g.putpixel((0, y), tuple(round(a + (b - a) * t) for a, b in zip(top, bottom)))
    return g.resize((w, h), Image.BICUBIC)


def star(draw, cx, cy, r, color, width):
    """نجمة ثمانية (مربعان متراكبان) كما في زخرفة التطبيق"""
    for rot in (0, 45):
        pts = [(cx + r * math.cos(math.radians(rot + 45 + 90 * i)), cy + r * math.sin(math.radians(rot + 45 + 90 * i))) for i in range(4)]
        draw.polygon(pts, outline=color, width=width)
    draw.ellipse((cx - r * 0.42, cy - r * 0.42, cx + r * 0.42, cy + r * 0.42), outline=color, width=width)


def fit_text(text, path, size, max_w, lang):
    while size > 20:
        f = font(path, size)
        if f.getbbox(text, direction=("rtl" if lang == "ar" else "ltr"))[2] <= max_w:
            return f
        size -= 4
    return font(path, size)


def compose(raw: Path, out: Path, key: str, lang: str, index: int):
    shot = Image.open(raw).convert("RGB")
    W, H = shot.size
    ipad = W > 1500
    top, bottom, title_c, sub_c, orn_c = PALETTES[index % len(PALETTES)]
    canvas = gradient(W, H, top, bottom).convert("RGBA")

    # زخرفة: نجمتان خافتتان في زاويتين متبدلتين
    orn = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    od = ImageDraw.Draw(orn)
    a = 34
    big = W * 0.42
    side = index % 2
    star(od, W * (0.92 if side else 0.08), H * 0.06, big, orn_c + (a,), max(3, W // 300))
    star(od, W * (0.06 if side else 0.94), H * 0.97, big * 0.75, orn_c + (a,), max(3, W // 300))
    canvas = Image.alpha_composite(canvas, orn)

    # العنوان أعلى الصورة في الصور الزوجية وأسفلها في الفردية
    caption_top = index % 2 == 0
    margin = round(W * 0.07)
    title, sub = CAPTIONS[key][lang]
    direction = "rtl" if lang == "ar" else "ltr"
    tf = fit_text(title, BOLD, round(W * (0.075 if ipad else 0.088)), W - 2 * margin, lang)
    sf = fit_text(sub, REGULAR, round(W * (0.032 if ipad else 0.04)), W - 2 * margin, lang)
    th = tf.getbbox(title, direction=direction)[3]
    sh = sf.getbbox(sub, direction=direction)[3]
    gap = round(W * 0.02)
    block = th + gap + sh
    caption_h = round(H * 0.2)

    # اللقطة: مصغّرة بزوايا مستديرة وإطار رفيع وظل
    scale = 0.74 if ipad else 0.78
    sw, shh = round(W * scale), round(H * scale)
    small = shot.resize((sw, shh), Image.LANCZOS)
    radius = round(sw * (0.045 if ipad else 0.11))
    border = max(10, round(sw * 0.018))
    fw, fh = sw + 2 * border, shh + 2 * border
    frame = Image.new("RGBA", (fw, fh), (0, 0, 0, 0))
    ImageDraw.Draw(frame).rounded_rectangle((0, 0, fw - 1, fh - 1), radius + border, fill=(18, 20, 22, 255))
    mask = Image.new("L", (sw, shh), 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, sw - 1, shh - 1), radius, fill=255)
    frame.paste(small, (border, border), mask)

    fx = (W - fw) // 2
    fy = (caption_h + round(H * 0.035)) if caption_top else round(H * 0.045)
    shadow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    ImageDraw.Draw(shadow).rounded_rectangle((fx + 10, fy + 30, fx + fw + 10, fy + fh + 30), radius + border, fill=(0, 0, 0, 110))
    shadow = shadow.filter(ImageFilter.GaussianBlur(W // 40))
    canvas = Image.alpha_composite(canvas, shadow)
    canvas.alpha_composite(frame, (fx, fy))

    # العنوان وسطر الشرح في المساحة الباقية
    d = ImageDraw.Draw(canvas)
    region_top = 0 if caption_top else fy + fh
    region_h = (fy if caption_top else H - (fy + fh))
    y = region_top + (region_h - block) // 2
    d.text((W // 2, y), title, font=tf, fill=title_c, anchor="mt", direction=direction)
    d.text((W // 2, y + th + gap), sub, font=sf, fill=sub_c, anchor="mt", direction=direction)

    out.parent.mkdir(parents=True, exist_ok=True)
    canvas.convert("RGB").save(out, "PNG", optimize=True)


def key_of(name: str) -> str:
    stem = name.removesuffix(".png")
    if stem.startswith("ipad_"):
        stem = stem[len("ipad_"):]
    return stem.split("_", 1)[1]


def main(src: str, dst: str):
    if not features.check("raqm"):
        sys.exit("Pillow needs libraqm for Arabic shaping")
    src_p, dst_p = Path(src), Path(dst)
    count = 0
    for loc_dir in sorted(p for p in src_p.iterdir() if p.is_dir()):
        lang = "ar" if loc_dir.name.startswith("ar") else "en"
        for kind in ("", "ipad_"):
            files = sorted(f for f in loc_dir.glob(f"{kind}[0-9]*.png") if (kind == "") == (not f.name.startswith("ipad_")))
            for i, f in enumerate(files):
                key = key_of(f.name)
                if key not in CAPTIONS:
                    print("skip (no caption):", f)
                    continue
                compose(f, dst_p / loc_dir.name / f.name, key, lang, i)
                count += 1
    print(f"{count} framed screenshots in {dst_p}")


if __name__ == "__main__":
    main(*sys.argv[1:3])
