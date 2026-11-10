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
        "ar": ("مصحف المدينة النبوية", "طبعة مجمع الملك فهد صفحة بصفحة كالمطبوع"),
        "en": ("The Madinah Mushaf", "The King Fahd Complex print, page by page"),
    },
    "quran_spread": {
        "ar": ("مصحف المدينة النبوية", "صفحتان متقابلتان كالمصحف المطبوع"),
        "en": ("The Madinah Mushaf", "Two facing pages, just like the printed Mushaf"),
    },
    "hadith": {
        "ar": ("حديث اليوم", "حديث صحيح كل يوم بشرحه وفوائده"),
        "en": ("Hadith of the day", "An authentic hadith daily, with its explanation"),
    },
    "tools": {
        "ar": ("أدواتك في مكان واحد", "الزكاة والمواريث والتاريخ الهجري والمسبحة"),
        "en": ("All your tools in one place", "Zakat, inheritance, Hijri dates and tasbih"),
    },
    "share": {
        "ar": ("شاركها قصةً", "الآية والحديث والذكر في صورة أنيقة"),
        "en": ("Share it as a story", "Ayahs, hadith and dhikr as elegant images"),
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
    "tafsir": {
        "ar": ("تفسير كل آية", "تفاسير موثوقة بلمسة على الآية"),
        "en": ("Tafsir for every ayah", "Trusted commentaries, one tap away"),
    },
    "tasbih": {
        "ar": ("سبحة في جيبك", "عدّاد للتسبيح مع أذكار جاهزة"),
        "en": ("A tasbih in your pocket", "A dhikr counter with ready-made adhkar"),
    },
}

# خلفيات متناوبة: (أعلى، أسفل، لون العنوان، لون الشرح، لون الزخرفة، لون التوهج خلف الجهاز)
PALETTES = [
    ((12, 84, 67), (5, 38, 31), (255, 255, 255), (232, 205, 140), (232, 205, 140), (64, 170, 132)),     # زمردي عميق
    ((250, 245, 233), (236, 226, 202), (10, 78, 62), (122, 98, 54), (184, 146, 64), (255, 255, 255)),   # عاجي وذهبي
    ((16, 52, 64), (7, 22, 31), (255, 255, 255), (160, 214, 200), (120, 196, 176), (40, 140, 130)),     # ليلي فيروزي
    ((22, 104, 82), (9, 56, 45), (252, 243, 218), (238, 226, 196), (240, 214, 150), (90, 196, 150)),    # أخضر مشرق
]
GOLD = (226, 186, 92)


def font(path, size):
    return ImageFont.truetype(path, size, layout_engine=ImageFont.Layout.RAQM)


def gradient(w, h, top, bottom):
    g = Image.new("RGB", (1, 256))
    for y in range(256):
        t = y / 255
        g.putpixel((0, y), tuple(round(a + (b - a) * t) for a, b in zip(top, bottom)))
    return g.resize((w, h), Image.BICUBIC)


def star(draw, cx, cy, r, color, width, ring=True):
    """نجمة ثمانية (مربعان متراكبان) كما في زخرفة التطبيق"""
    for rot in (0, 45):
        pts = [(cx + r * math.cos(math.radians(rot + 45 + 90 * i)), cy + r * math.sin(math.radians(rot + 45 + 90 * i))) for i in range(4)]
        draw.polygon(pts, outline=color, width=width)
    if ring:
        draw.ellipse((cx - r * 0.42, cy - r * 0.42, cx + r * 0.42, cy + r * 0.42), outline=color, width=width)


def filled_star(draw, cx, cy, r, color):
    """نجمة ثمانية مصمتة صغيرة (زخرفة فوق العنوان)"""
    for rot in (0, 45):
        pts = [(cx + r * math.cos(math.radians(rot + 45 + 90 * i)), cy + r * math.sin(math.radians(rot + 45 + 90 * i))) for i in range(4)]
        draw.polygon(pts, fill=color)


def pattern(w, h, color, alpha, cell):
    """شبكة نجوم ثمانية خافتة على الخلفية كلها (زخرفة إسلامية هادئة)، تتلاشى نحو الأسفل"""
    layer = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    lw = max(2, cell // 70)
    for j, y in enumerate(range(-cell, h + cell, cell)):
        for x in range(-cell, w + cell, cell):
            ox = cell // 2 if j % 2 else 0
            star(d, x + ox, y, cell * 0.34, color + (alpha,), lw, ring=False)
    fade = Image.linear_gradient("L").resize((w, h)).point(lambda v: 255 - v * 0.75)
    a = layer.getchannel("A").point(lambda v: v)
    layer.putalpha(Image.composite(a, Image.new("L", (w, h), 0), fade).point(lambda v: v))
    return layer


def glow(w, h, cx, cy, r, color, alpha):
    """توهج ناعم خلف الجهاز"""
    layer = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    ImageDraw.Draw(layer).ellipse((cx - r, cy - r * 0.8, cx + r, cy + r * 0.8), fill=color + (alpha,))
    return layer.filter(ImageFilter.GaussianBlur(r * 0.45))


def fit_text(text, path, size, max_w, lang):
    while size > 20:
        f = font(path, size)
        if f.getbbox(text, direction=("rtl" if lang == "ar" else "ltr"))[2] <= max_w:
            return f
        size -= 4
    return font(path, size)


def device(shot: Image.Image, sw: int, ipad: bool) -> Image.Image:
    """إطار جهاز واقعي: حافة معدنية بتدرج ولمعة، وإطار أسود رفيع، وزوايا مستديرة، وجزيرة iPhone، وأزرار جانبية"""
    shh = round(sw * shot.height / shot.width)
    screen = shot.resize((sw, shh), Image.LANCZOS)
    landscape = shot.width > shot.height
    short = min(sw, shh)
    radius = round(short * (0.045 if ipad else 0.125))
    bezel = round(short * (0.035 if ipad else 0.032))      # الإطار الأسود حول الشاشة
    rim = max(6, round(short * (0.011 if ipad else 0.013)))  # الحافة المعدنية
    btn = 0 if ipad else round(rim * 0.9)                   # بروز الأزرار الجانبية
    fw, fh = sw + 2 * (bezel + rim) + 2 * btn, shh + 2 * (bezel + rim)
    img = Image.new("RGBA", (fw, fh), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    x0 = btn
    R = radius + bezel + rim
    # الأزرار الجانبية (iPhone): الصوت والإجراء يساراً، والتشغيل يميناً
    if not ipad and not landscape:
        bc = (58, 62, 64, 255)
        for y, hgt in ((0.17, 0.035), (0.225, 0.06), (0.30, 0.06)):
            d.rounded_rectangle((0, fh * y, btn + rim, fh * (y + hgt)), btn, fill=bc)
        d.rounded_rectangle((fw - btn - rim, fh * 0.25, fw, fh * 0.34), btn, fill=bc)
    # الحافة المعدنية: تدرج من فاتح إلى داكن ولمعة رفيعة
    metal = gradient(fw - 2 * btn, fh, (150, 152, 150), (70, 72, 72)).convert("RGBA")
    mmask = Image.new("L", metal.size, 0)
    ImageDraw.Draw(mmask).rounded_rectangle((0, 0, metal.width - 1, fh - 1), R, fill=255)
    img.paste(metal, (x0, 0), mmask)
    d.rounded_rectangle((x0 + 2, 2, fw - btn - 3, fh - 3), R - 2, outline=(215, 218, 216, 160), width=max(2, rim // 4))
    # الإطار الأسود
    d.rounded_rectangle((x0 + rim, rim, fw - btn - rim - 1, fh - rim - 1), radius + bezel, fill=(10, 11, 12, 255))
    # الشاشة
    smask = Image.new("L", (sw, shh), 0)
    ImageDraw.Draw(smask).rounded_rectangle((0, 0, sw - 1, shh - 1), radius, fill=255)
    img.paste(screen, (x0 + rim + bezel, rim + bezel), smask)
    # جزيرة iPhone (تغطي الجزيرة إن ظهرت في اللقطة، فتتطابق الصور كلها)
    if not ipad and not landscape:
        iw, ih = round(sw * 0.29), round(sw * 0.085)
        cx, top = x0 + rim + bezel + sw // 2, rim + bezel + round(sw * 0.027)
        d.rounded_rectangle((cx - iw // 2, top, cx + iw // 2, top + ih), ih // 2, fill=(0, 0, 0, 255))
    return img


def compose(raw: Path, out: Path, key: str, lang: str, index: int, size=None):
    shot = Image.open(raw).convert("RGB")
    W, H = shot.size
    if size:
        # مقاس ثابت للصورة (Google Play: 1080×1920، نسبة 9:16)
        W, H = size
    elif W < 1000:
        # لقطة صغيرة (صور الموقع 540×1137): الصورة بمقاس iPhone 6.9 بوصة، واللقطة بنسبتها داخل الإطار
        W, H = 1320, 2868
    ipad = max(W, H) > 2000 and min(W, H) > 1500
    landscape = W > H
    top, bottom, title_c, sub_c, orn_c, glow_c = PALETTES[index % len(PALETTES)]
    canvas = gradient(W, H, top, bottom).convert("RGBA")
    light = sum(top) > 600

    # زخرفة: شبكة نجوم خافتة، ونجمة كبيرة في زاوية متبدلة
    canvas = Image.alpha_composite(canvas, pattern(W, H, orn_c, 16 if light else 13, round(min(W, H) * 0.16)))
    orn = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    od = ImageDraw.Draw(orn)
    side = index % 2
    star(od, W * (0.95 if side else 0.05), H * 0.04, min(W, H) * 0.36, orn_c + (40,), max(3, W // 280))
    canvas = Image.alpha_composite(canvas, orn)

    # العنوان وسطر الشرح أعلى الصورة دائماً، وفوقهما نجمة ذهبية صغيرة
    margin = round(W * 0.08)
    title, sub = CAPTIONS[key][lang]
    direction = "rtl" if lang == "ar" else "ltr"
    base = min(W, H)
    tf = fit_text(title, BOLD, round(base * (0.068 if ipad else 0.092)), W - 2 * margin, lang)
    sf = fit_text(sub, REGULAR, round(base * (0.03 if ipad else 0.041)), W - 2 * margin, lang)
    th = tf.getbbox(title, direction=direction)[3]
    sh = sf.getbbox(sub, direction=direction)[3]
    ornament = round(base * 0.022)
    y0 = round(H * (0.05 if landscape else 0.055))
    d = ImageDraw.Draw(canvas)
    filled_star(d, W // 2, y0 + ornament, ornament, GOLD if not light else (190, 150, 62))
    line_w = round(base * 0.07)
    lc = (GOLD if not light else (190, 150, 62)) + (140,)
    d.line((W // 2 - ornament * 2 - line_w, y0 + ornament, W // 2 - ornament * 2, y0 + ornament), fill=lc, width=max(2, base // 400))
    d.line((W // 2 + ornament * 2, y0 + ornament, W // 2 + ornament * 2 + line_w, y0 + ornament), fill=lc, width=max(2, base // 400))
    ty = y0 + ornament * 2 + round(base * 0.03)
    d.text((W // 2, ty), title, font=tf, fill=title_c, anchor="mt", direction=direction)
    sy = ty + th + round(base * 0.022)
    d.text((W // 2, sy), sub, font=sf, fill=sub_c, anchor="mt", direction=direction)
    text_bottom = sy + sh

    # الجهاز: تحت العنوان، ويخرج طرفه السفلي من الصورة في العمودي (أسلوب صور المتجر الحديثة)
    gap = round(H * 0.045)
    if landscape:
        avail_h = H - text_bottom - gap - round(H * 0.05)
        dev = device(shot, 100, ipad)
        sw = round(100 * min(avail_h / dev.height, (W * 0.86) / dev.width))
    else:
        sw = round(W * (0.74 if ipad else 0.78))
    dev = device(shot, sw, ipad)
    fx = (W - dev.width) // 2
    fy = text_bottom + gap
    if landscape:
        fy = text_bottom + gap + max(0, (H - text_bottom - gap - round(H * 0.05) - dev.height) // 2)
    # توهج خلف الجهاز وظل ناعم تحته
    canvas = Image.alpha_composite(canvas, glow(W, H, W // 2, fy + dev.height * 0.4, dev.width * 0.62, glow_c, 70 if not light else 150))
    shadow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    ImageDraw.Draw(shadow).rounded_rectangle((fx + 6, fy + 26, fx + dev.width - 6, fy + dev.height + 26), round(dev.width * 0.12), fill=(0, 0, 0, 120 if not light else 70))
    shadow = shadow.filter(ImageFilter.GaussianBlur(W // 36))
    canvas = Image.alpha_composite(canvas, shadow)
    canvas.alpha_composite(dev, (fx, fy)) if fy + dev.height <= H else canvas.alpha_composite(dev.crop((0, 0, dev.width, H - fy)), (fx, fy))

    out.parent.mkdir(parents=True, exist_ok=True)
    canvas.convert("RGB").save(out, "PNG", optimize=True)


def key_of(name: str) -> str:
    stem = name.removesuffix(".png")
    if stem.startswith("ipad_"):
        stem = stem[len("ipad_"):]
    return stem.split("_", 1)[1]


def main(src: str, dst: str, size: str = ""):
    wh = tuple(int(x) for x in size.lower().split("x")) if size else None
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
                compose(f, dst_p / loc_dir.name / f.name, key, lang, i, wh)
                count += 1
    print(f"{count} framed screenshots in {dst_p}")


if __name__ == "__main__":
    main(*sys.argv[1:4])
