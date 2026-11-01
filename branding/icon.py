"""
أيقونة ميزان: الميزان الذهبي على زمرّد عميق، وخلفه نجمة ثمانية (ربع الحزب) خافتة.
مصممة بمقاييس iOS (مربع 1024 ممتلئ بلا شفافية، يقصّه النظام بزواياه المستديرة)، ومنها تُولَّد:
  - مجموعة أيقونة iOS (عادي، وداكن على زمرّد قاتم، وملوَّن بتدرج رمادي) جاهزة لـ Xcode،
    والميزان وحده لشاشة الإقلاع (LaunchEmblem) والأيقونة صورةً لشاشة القفل أثناء التلاوة (NowPlayingArtwork)
  - أيقونات الموقع (apple-touch-icon وأيقونات التطبيق المثبّت maskable)
  - طبقات الأيقونة التكيفية لأندرويد (Vector) من الأشكال نفسها
الاستعمال:  python3 branding/icon.py   ثم   node branding/render.mjs   (يحتاج Playwright وChromium لتصدير PNG)
"""
import json
import math
import os

ROOT = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.dirname(ROOT)
S = 1024            # لوحة التصميم
C = S / 2

# الألوان: زمرّد التطبيق وذهب التذهيب
GREEN_LIGHT, GREEN_MID, GREEN_DEEP = "#16846C", "#0C5C4A", "#063A2F"
GOLD_HI, GOLD_MID, GOLD_LO = "#F8E08F", "#E5BA52", "#BF8B27"
# خلفية الأيقونة الداكنة في iOS: زمرّد قاتم يكاد يكون أسود
DARK_HI, DARK_MID, DARK_LO = "#123A31", "#0A231D", "#040D0B"

# ---------- الأشكال (إحداثيات اللوحة 1024) ----------
DY = 14                             # إنزال الشعار قليلاً ليتوسط بصرياً (الكفتان أثقل من الرأس)
FINIAL = (C, 262 + DY, 30)          # الكرة في أعلى العمود
PILLAR = (C - 19, 280 + DY, 38, 438)  # x, y, w, h
BEAM_Y, BEAM_X0, BEAM_X1, BEAM_W = 350 + DY, 250, S - 250, 34
HANG = 288                          # نقطة تعليق الكفة من طرف العاتق
RIM_Y = 566 + DY                    # حافة الكفتين
PAN_HALF, PAN_DEPTH = 104, 76       # نصف عرض الكفة وعمقها
CHAIN_W = 18
FOOT = [(C - 40, 690 + DY), (C + 40, 690 + DY), (C + 82, 724 + DY), (C - 82, 724 + DY)]
BASE = (C - 132, 718 + DY, 264, 36)  # x, y, w, h (مستدير الطرفين)
STAR_R, STAR_W = 408, 9             # نصف قطر النجمة الثمانية وسماكة خطها

def pans():
    out = []
    for cx in (HANG, S - HANG):
        x0, x1 = cx - PAN_HALF, cx + PAN_HALF
        out.append(f"M{x0},{RIM_Y} L{x1},{RIM_Y} A{PAN_HALF},{PAN_DEPTH} 0 0,1 {x0},{RIM_Y} Z")
    return out

def chains():
    out = []
    for cx in (HANG, S - HANG):
        for ex in (cx - PAN_HALF + 16, cx + PAN_HALF - 16):
            out.append(((cx, BEAM_Y), (ex, RIM_Y - 2)))
    return out

def rounded_rect_path(x, y, w, h, r):
    return (f"M{x + r},{y} L{x + w - r},{y} A{r},{r} 0 0,1 {x + w},{y + r} L{x + w},{y + h - r} "
            f"A{r},{r} 0 0,1 {x + w - r},{y + h} L{x + r},{y + h} A{r},{r} 0 0,1 {x},{y + h - r} "
            f"L{x},{y + r} A{r},{r} 0 0,1 {x + r},{y} Z")

def circle_path(cx, cy, r):
    return f"M{cx - r},{cy} A{r},{r} 0 1,0 {cx + r},{cy} A{r},{r} 0 1,0 {cx - r},{cy} Z"

def emblem_fills():
    """كل أجزاء الميزان أشكالاً مملوءة (لأندرويد ولـ SVG معاً)"""
    x, y, w, h = PILLAR
    bx, by, bw, bh = BASE
    beam = rounded_rect_path(BEAM_X0, BEAM_Y - BEAM_W / 2, BEAM_X1 - BEAM_X0, BEAM_W, BEAM_W / 2)
    foot = "M" + " L".join(f"{px},{py}" for px, py in FOOT) + " Z"
    return [circle_path(*FINIAL), rounded_rect_path(x, y, w, h, w / 2), beam, foot,
            rounded_rect_path(bx, by, bw, bh, bh / 2)] + pans()

def star_path(r):
    """نجمة ثمانية من مربعين متداخلين (ربع الحزب)"""
    pts = []
    for k in range(16):
        # رؤوس المربعين بالتناوب، والنقاط الداخلية حيث يتقاطع الضلعان
        ang = math.pi / 8 * k - math.pi / 2
        rad = r if k % 2 == 0 else r * math.cos(math.pi / 4) / math.cos(math.pi / 8)
        pts.append((C + rad * math.cos(ang), C + rad * math.sin(ang)))
    return "M" + " L".join(f"{px:.1f},{py:.1f}" for px, py in pts) + " Z"

# ---------- SVG ----------

def gold_gradient(gid, light=GOLD_HI, mid=GOLD_MID, low=GOLD_LO):
    top, bottom = FINIAL[1] - FINIAL[2], BASE[1] + BASE[3]
    return (f'<linearGradient id="{gid}" gradientUnits="userSpaceOnUse" x1="0" y1="{top}" x2="0" y2="{bottom}">'
            f'<stop offset="0" stop-color="{light}"/><stop offset=".55" stop-color="{mid}"/><stop offset="1" stop-color="{low}"/></linearGradient>')

def emblem_svg(fill, shadow=True, chain_color=None):
    chain_color = chain_color or fill
    parts = [f'<path d="{d}" fill="{fill}"/>' for d in emblem_fills()]
    parts += [f'<line x1="{a[0]}" y1="{a[1]}" x2="{b[0]}" y2="{b[1]}" stroke="{chain_color}" stroke-width="{CHAIN_W}" stroke-linecap="round"/>'
              for a, b in chains()]
    g = "".join(parts)
    return f'<g filter="url(#lift)">{g}</g>' if shadow else f"<g>{g}</g>"

def svg(kind):
    """kind: any (الأصل) | rounded (بزوايا مستديرة للحاسوب) | dark (على أخضر قاتم) | tinted (تدرج رمادي)
    | emblem (الميزان والنجمة وحدهما على شفاف، لشاشة الإقلاع في iOS) """
    if kind == "emblem":
        # مربع النجمة وحده (بهامش للظل): يُعرض في وسط شاشة الإقلاع فوق لون LaunchBackground
        m = STAR_R + STAR_W + 26
        defs = [gold_gradient("gold"),
                '<filter id="lift" x="-20%" y="-20%" width="140%" height="140%">'
                '<feDropShadow dx="0" dy="10" stdDeviation="14" flood-color="#021F19" flood-opacity=".35"/></filter>']
        body = [f'<path d="{star_path(STAR_R)}" fill="none" stroke="{GOLD_MID}" stroke-opacity=".30" stroke-width="{STAR_W}" stroke-linejoin="round"/>',
                emblem_svg("url(#gold)")]
        return (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="{C - m} {C - m} {2 * m} {2 * m}" width="{2 * m}" height="{2 * m}">'
                f'<defs>{"".join(defs)}</defs>{"".join(body)}</svg>\n')
    if kind == "rounded":
        # أيقونة الموقع العادية (any): زوايا مستديرة كأيقونات التطبيقات، وما خارجها شفاف
        inner = svg("any").split(">", 1)[1].rsplit("</svg>", 1)[0]
        r = S * 0.225
        return (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {S} {S}" width="{S}" height="{S}">'
                f'<clipPath id="round"><rect width="{S}" height="{S}" rx="{r}"/></clipPath>'
                f'<g clip-path="url(#round)">{inner}</g></svg>\n')
    defs = [gold_gradient("gold")]
    body = []
    if kind == "any":
        defs.append(f'<radialGradient id="bg" cx="{C}" cy="{C - 120}" r="{S * 0.78}" gradientUnits="userSpaceOnUse">'
                    f'<stop offset="0" stop-color="{GREEN_LIGHT}"/><stop offset=".55" stop-color="{GREEN_MID}"/>'
                    f'<stop offset="1" stop-color="{GREEN_DEEP}"/></radialGradient>')
        defs.append('<filter id="lift" x="-20%" y="-20%" width="140%" height="140%">'
                    '<feDropShadow dx="0" dy="10" stdDeviation="14" flood-color="#021F19" flood-opacity=".45"/></filter>')
        body.append(f'<rect width="{S}" height="{S}" fill="url(#bg)"/>')
        body.append(f'<path d="{star_path(STAR_R)}" fill="#FFFFFF" fill-opacity=".035" stroke="{GOLD_MID}" stroke-opacity=".32" stroke-width="{STAR_W}" stroke-linejoin="round"/>')
        body.append(emblem_svg("url(#gold)"))
    elif kind == "dark":
        # إرشادات Apple للأيقونة الداكنة: خلفية ملونة داكنة تعطي أوضح تباين، وألوان الأصل أهدأ قليلاً.
        # زمرّد قاتم يكاد يكون أسود (أقرب إلى خلفية النظام في الوضع الداكن)، والميزان بذهب أخفت لمعاناً
        defs = [gold_gradient("gold", "#EDD07F", "#D9AE4C", "#AE7F25")]
        defs.append(f'<radialGradient id="bg" cx="{C}" cy="{C - 120}" r="{S * 0.8}" gradientUnits="userSpaceOnUse">'
                    f'<stop offset="0" stop-color="{DARK_HI}"/><stop offset=".6" stop-color="{DARK_MID}"/>'
                    f'<stop offset="1" stop-color="{DARK_LO}"/></radialGradient>')
        defs.append('<filter id="lift" x="-20%" y="-20%" width="140%" height="140%">'
                    '<feDropShadow dx="0" dy="10" stdDeviation="14" flood-color="#000000" flood-opacity=".5"/></filter>')
        body.append(f'<rect width="{S}" height="{S}" fill="url(#bg)"/>')
        body.append(f'<path d="{star_path(STAR_R)}" fill="none" stroke="{GOLD_MID}" stroke-opacity=".22" stroke-width="{STAR_W}" stroke-linejoin="round"/>')
        body.append(emblem_svg("url(#gold)"))
    elif kind == "tinted":
        # يلوّنه النظام بلون المستخدم حسب الإضاءة: أبيض للشعار ورمادي خافت للنجمة على أسود
        defs = [gold_gradient("gray", "#FFFFFF", "#E6E6E6", "#B8B8B8"), '<filter id="lift"/>']
        body.append(f'<rect width="{S}" height="{S}" fill="#000000"/>')
        body.append(f'<path d="{star_path(STAR_R)}" fill="none" stroke="#FFFFFF" stroke-opacity=".28" stroke-width="{STAR_W}" stroke-linejoin="round"/>')
        body.append(emblem_svg("url(#gray)", shadow=False))
    return (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {S} {S}" width="{S}" height="{S}">'
            f'<defs>{"".join(defs)}</defs>{"".join(body)}</svg>\n')

# ---------- أندرويد (الأيقونة التكيفية 108dp: الظاهر منها 72dp في الوسط) ----------

def to_dp(v):
    """من لوحة 1024 إلى لوحة 108dp، حيث الأيقونة الظاهرة هي المربع 18..90"""
    return 18 + v * 72 / S

def scale_path(d):
    out, num = [], ""
    tokens = []
    for ch in d:
        if ch.isalpha():
            if num: tokens.append(num); num = ""
            tokens.append(ch)
        elif ch in ", ":
            if num: tokens.append(num); num = ""
        else:
            num += ch
    if num: tokens.append(num)
    cmd, idx = None, 0
    for t in tokens:
        if t.isalpha():
            cmd, idx = t, 0
            out.append(t)
            continue
        v = float(t)
        if cmd == "A":
            # rx ry rot large sweep x y: نصفا القطر يُقاسان، والمرجعان لا يتغيران
            pos = idx % 7
            if pos in (0, 1): v = v * 72 / S
            elif pos in (5, 6): v = to_dp(v)
            idx += 1
        else:
            v = to_dp(v)
        out.append(f"{v:.3f}".rstrip("0").rstrip("."))
    s = ""
    for t in out:
        s += (t if t.isalpha() else ("," if s and not s[-1].isalpha() else "") + t)
    return s

def android_gold(top, bottom):
    return ('<aapt:attr name="android:fillColor"><gradient android:type="linear" android:startX="54" '
            f'android:startY="{top:.2f}" android:endX="54" android:endY="{bottom:.2f}" android:startColor="{GOLD_HI}" '
            f'android:centerColor="{GOLD_MID}" android:endColor="{GOLD_LO}"/></aapt:attr>')

def android_foreground():
    top, bottom = to_dp(FINIAL[1] - FINIAL[2]), to_dp(BASE[1] + BASE[3])
    paths = []
    for d in emblem_fills():
        paths.append(f'    <path android:pathData="{scale_path(d)}">\n        {android_gold(top, bottom)}\n    </path>')
    chain_d = " ".join(f"M{a[0]},{a[1]} L{b[0]},{b[1]}" for a, b in chains())
    paths.append(f'    <path android:pathData="{scale_path(chain_d)}" android:strokeColor="{GOLD_MID}" '
                 f'android:strokeWidth="{CHAIN_W * 72 / S:.2f}" android:strokeLineCap="round"/>')
    return ('<?xml version="1.0" encoding="utf-8"?>\n'
            '<!-- مولَّد من branding/icon.py: الميزان الذهبي في المنطقة الآمنة للأيقونة التكيفية -->\n'
            '<vector xmlns:android="http://schemas.android.com/apk/res/android" xmlns:aapt="http://schemas.android.com/aapt"\n'
            '    android:width="108dp" android:height="108dp" android:viewportWidth="108" android:viewportHeight="108">\n'
            + "\n".join(paths) + "\n</vector>\n")

def android_monochrome():
    chain_d = " ".join(f"M{a[0]},{a[1]} L{b[0]},{b[1]}" for a, b in chains())
    paths = [f'    <path android:fillColor="#FFFFFFFF" android:pathData="{scale_path(d)}"/>' for d in emblem_fills()]
    paths.append(f'    <path android:pathData="{scale_path(chain_d)}" android:strokeColor="#FFFFFFFF" '
                 f'android:strokeWidth="{CHAIN_W * 72 / S:.2f}" android:strokeLineCap="round"/>')
    return ('<?xml version="1.0" encoding="utf-8"?>\n'
            '<!-- مولَّد من branding/icon.py: شكل الميزان وحده للأيقونات الملوّنة بلون النظام (أندرويد 13+) والإشعارات -->\n'
            '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
            '    android:width="108dp" android:height="108dp" android:viewportWidth="108" android:viewportHeight="108">\n'
            + "\n".join(paths) + "\n</vector>\n")

def android_background():
    # الخلفية تملأ 108dp كلها؛ مركز التدرج والنجمة في وسط الجزء الظاهر
    cy = to_dp(C - 120)
    star = scale_path(star_path(STAR_R))
    return ('<?xml version="1.0" encoding="utf-8"?>\n'
            '<!-- مولَّد من branding/icon.py: زمرّد بتدرج دائري وخلف الميزان نجمة ثمانية خافتة -->\n'
            '<vector xmlns:android="http://schemas.android.com/apk/res/android" xmlns:aapt="http://schemas.android.com/aapt"\n'
            '    android:width="108dp" android:height="108dp" android:viewportWidth="108" android:viewportHeight="108">\n'
            '    <path android:pathData="M0,0h108v108h-108z">\n'
            '        <aapt:attr name="android:fillColor"><gradient android:type="radial" android:centerX="54" '
            f'android:centerY="{cy:.2f}" android:gradientRadius="{S * 0.78 * 72 / S + 18:.2f}" android:startColor="{GREEN_LIGHT}" '
            f'android:centerColor="{GREEN_MID}" android:endColor="{GREEN_DEEP}"/></aapt:attr>\n'
            '    </path>\n'
            f'    <path android:pathData="{star}" android:fillColor="#09FFFFFF" android:strokeColor="#52E5BA52" '
            f'android:strokeWidth="{STAR_W * 72 / S:.2f}" android:strokeLineJoin="round"/>\n'
            '</vector>\n')

# ---------- iOS ----------

IOS_CONTENTS = {
    "images": [
        {"filename": "AppIcon-1024.png", "idiom": "universal", "platform": "ios", "size": "1024x1024"},
        {"appearances": [{"appearance": "luminosity", "value": "dark"}], "filename": "AppIcon-1024-dark.png",
         "idiom": "universal", "platform": "ios", "size": "1024x1024"},
        {"appearances": [{"appearance": "luminosity", "value": "tinted"}], "filename": "AppIcon-1024-tinted.png",
         "idiom": "universal", "platform": "ios", "size": "1024x1024"},
    ],
    "info": {"author": "xcode", "version": 1},
}

def write(path, text):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        f.write(text)

if __name__ == "__main__":
    for kind in ("any", "rounded", "dark", "tinted", "emblem"):
        write(os.path.join(ROOT, "icon", f"mizan-icon-{kind}.svg"), svg(kind))
    write(os.path.join(ROOT, "ios", "AppIcon.appiconset", "Contents.json"), json.dumps(IOS_CONTENTS, indent=2) + "\n")
    res = os.path.join(REPO, "android-app", "app", "src", "main", "res", "drawable")
    write(os.path.join(res, "ic_launcher_foreground.xml"), android_foreground())
    write(os.path.join(res, "ic_launcher_background.xml"), android_background())
    write(os.path.join(res, "ic_launcher_monochrome.xml"), android_monochrome())
    print("SVG, Android vectors and iOS Contents.json written")
