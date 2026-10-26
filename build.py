#!/usr/bin/env python3
"""
مُولّد موقع "ميزان" الثابت.

الاستخدام:
    python build.py

يقرأ الصفحات من src/pages ويُركّبها داخل القالب src/templates/base.html
ثم يكتب الموقع الجاهز للنشر في مجلد dist/ مع sitemap.xml و robots.txt و ads.txt.
لا يحتاج أي مكتبات خارجية.
"""
import html
import math
import sys
import json
import re
import shutil
from datetime import date
from pathlib import Path

try:
    sys.stdout.reconfigure(encoding="utf-8")
except Exception:
    pass

ROOT = Path(__file__).parent
SRC = ROOT / "src"
DIST = ROOT / "dist"
CONFIG = json.loads((ROOT / "config.json").read_text(encoding="utf-8"))

META_RE = re.compile(r"^\s*<!--META\s*(\{.*?\})\s*-->", re.S)
INCLUDE_RE = re.compile(r"<!--INCLUDE\s+([\w/.\-]+)\s*-->")
FAQ_RE = re.compile(r"<!--FAQ\s*(\[.*?\])\s*-->", re.S)

# سجل الأدوات: يُستخدم في الصفحة الرئيسية والقائمة و"أدوات ذات صلة"
TOOLS = [
    {"key": "zakat", "path": "/zakat-calculator/", "name": "حاسبة الزكاة", "icon": "coins",
     "desc": "احسب زكاة المال والذهب والفضة والأسهم وعروض التجارة مع النصاب الحالي."},
    {"key": "fitr", "path": "/zakat-al-fitr/", "name": "حاسبة زكاة الفطر", "icon": "wheat",
     "desc": "احسب زكاة الفطر عن أفراد أسرتك بالصاع أو بالقيمة النقدية."},
    {"key": "inheritance", "path": "/inheritance-calculator/", "name": "حاسبة المواريث", "icon": "scale",
     "desc": "قسّم التركة وفق علم الفرائض مع بيان أصل المسألة والعول والرد."},
    {"key": "hijri", "path": "/hijri-converter/", "name": "محوّل التاريخ الهجري", "icon": "calendar",
     "desc": "حوّل التاريخ من الهجري إلى الميلادي والعكس وفق تقويم أم القرى."},
    {"key": "age", "path": "/age-calculator/", "name": "حاسبة العمر", "icon": "hourglass",
     "desc": "اعرف عمرك بالهجري والميلادي بالسنوات والأشهر والأيام."},
    {"key": "prayer", "path": "/prayer-times/", "name": "مواقيت الصلاة", "icon": "mosque",
     "desc": "مواقيت الصلاة الدقيقة لمدينتك مع العد التنازلي للصلاة القادمة."},
    {"key": "qibla", "path": "/qibla-direction/", "name": "اتجاه القبلة", "icon": "compass",
     "desc": "حدد اتجاه القبلة من موقعك مع بوصلة تفاعلية على الجوال."},
    {"key": "tasbih", "path": "/tasbih/", "name": "المسبحة الإلكترونية", "icon": "beads",
     "desc": "سبّح واذكر الله مع عدّاد يحفظ تقدمك وأذكار جاهزة."},
    {"key": "events", "path": "/islamic-events/", "name": "المناسبات الإسلامية", "icon": "moon",
     "desc": "عدّ تنازلي لرمضان والعيدين ويوم عرفة وبقية المناسبات."},
    {"key": "fidya", "path": "/fidya-kaffarah/", "name": "حاسبة الفدية والكفارة", "icon": "hand",
     "desc": "احسب فدية الصيام وكفارة اليمين وكفارة الإفطار المتعمد."},
]

# ---------------------------------------------------------------------------
# ألوان التطبيق (منقولة من android-app/.../ui/theme/Theme.kt)
# كل لون يغيّر درجة (hue) الألوان الخضراء فقط ويحفظ إضاءتها في OKLCH، فيبقى التباين
# كما هو. الذهبي والكريمي والأحمر ثابتة. تُولَّد الألوان هنا وتُحقن في style.css.
# ---------------------------------------------------------------------------
ACCENTS = [  # (key, hue, chroma, الاسم كما في التطبيق)
    ("emerald", 172, 1.0, "زمردي"),
    ("teal", 212, 1.25, "فيروزي"),
    ("blue", 255, 1.45, "أزرق"),
    ("purple", 300, 1.5, "بنفسجي"),
    ("pink", 350, 1.6, "وردي"),
    ("maroon", 20, 1.45, "عنابي"),
    ("orange", 50, 1.5, "برتقالي"),
    ("honey", 80, 1.45, "عسلي"),
    ("brown", 60, 0.75, "بني"),
    ("graphite", 255, 0.15, "رمادي"),
]

# ألوان الموقع الأساسية (الزمردي) — مطابقة لـ LightColors / DarkColors في التطبيق
PALETTE_LIGHT = {
    "--bg": "#f8f5ee", "--bg-alt": "#f1ece0", "--surface": "#ffffff", "--surface-2": "#fbf9f4",
    "--text": "#1d2b27", "--text-soft": "#4f5e59", "--muted": "#7a8783", "--border": "#e5dfd0",
    "--primary": "#0d5c4b", "--primary-strong": "#08463a", "--primary-soft": "#e3efe9",
    "--primary-bright": "#1a8a6f", "--on-primary": "#ffffff",
    "--hero-1": "#0b4a3d", "--hero-2": "#0f6a55",
}
PALETTE_DARK = {
    "--bg": "#0e1715", "--bg-alt": "#121e1b", "--surface": "#16231f", "--surface-2": "#1a2925",
    "--text": "#e8efec", "--text-soft": "#b5c3be", "--muted": "#8a9a95", "--border": "#263a34",
    "--primary": "#3fb595", "--primary-strong": "#6fd0b3", "--primary-soft": "#183a31",
    "--primary-bright": "#1a8a6f", "--on-primary": "#07231c",
    "--hero-1": "#082e26", "--hero-2": "#0c4538",
}
BRAND_GREEN, BRAND_GREEN_DEEP = "#0f6a55", "#08463a"


def _lin(c):
    return c / 12.92 if c <= 0.04045 else ((c + 0.055) / 1.055) ** 2.4


def _gam(c):
    return 12.92 * c if c <= 0.0031308 else 1.055 * c ** (1 / 2.4) - 0.055


def _cbrt(x):
    return math.copysign(abs(x) ** (1 / 3), x)


def _to_oklch(hex_color):
    r, g, b = (_lin(int(hex_color[i:i + 2], 16) / 255) for i in (1, 3, 5))
    l = _cbrt(0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b)
    m = _cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b)
    s = _cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b)
    L = 0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s
    A = 1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s
    B = 0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s
    h = math.degrees(math.atan2(B, A)) % 360
    return L, math.hypot(A, B), h


def _oklch_linear(L, c, h):
    a, b = c * math.cos(math.radians(h)), c * math.sin(math.radians(h))
    l = (L + 0.3963377774 * a + 0.2158037573 * b) ** 3
    m = (L - 0.1055613458 * a - 0.0638541728 * b) ** 3
    s = (L - 0.0894841775 * a - 1.2914855480 * b) ** 3
    return (4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s,
            -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s,
            -0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s)


def _oklch_hex(L, c, h):
    """لون من OKLCH؛ إن خرج عن نطاق sRGB يُخفَّض التشبّع حتى يدخل، فتبقى الإضاءة والدرجة."""
    def ok(v):
        return all(-1e-4 <= x <= 1.0001 for x in v)
    rgb = _oklch_linear(L, c, h)
    if not ok(rgb):
        lo, hi = 0.0, c
        for _ in range(20):
            mid = (lo + hi) / 2
            lo, hi = (mid, hi) if ok(_oklch_linear(L, mid, h)) else (lo, mid)
        rgb = _oklch_linear(L, lo, h)
    return "#" + "".join(f"{round(_gam(min(1, max(0, x))) * 255):02x}" for x in rgb)


def recolor(hex_color, hue, chroma):
    """يحوّل لوناً أخضر إلى درجة اللون المختار؛ الألوان الأخرى تبقى كما هي (Color.recolor في التطبيق)."""
    L, c, h = _to_oklch(hex_color)
    if c < 0.004 or not (140 <= h <= 210):
        return hex_color
    return _oklch_hex(L, c * chroma, hue)


def accent_css():
    """كتل CSS لكل لون: فاتح، وداكن (يدوي وتلقائي حسب النظام)."""
    out = ["/* ألوان التطبيق — مولّدة من build.py (ACCENTS) */"]
    for key, hue, chroma, _ in ACCENTS[1:]:
        def block(pal):
            return "".join(f"{k}:{recolor(v, hue, chroma)};" for k, v in pal.items()
                           if recolor(v, hue, chroma) != v)
        light, dark = block(PALETTE_LIGHT), block(PALETTE_DARK)
        out.append(f':root[data-accent="{key}"]{{{light}}}')
        out.append(f':root[data-theme="dark"][data-accent="{key}"]{{{dark}}}')
        out.append(f'@media (prefers-color-scheme: dark){{:root:not([data-theme="light"])[data-accent="{key}"]{{{dark}}}}}')
    return "\n".join(out)


def appearance_sheet():
    """ورقة «المظهر»: الوضع (تلقائي/فاتح/داكن) وعشرة ألوان في صفين، كما في إعدادات التطبيق."""
    modes = [("system", "تلقائي"), ("light", "فاتح"), ("dark", "داكن")]
    seg = "".join(f'<button type="button" role="radio" aria-checked="false" data-mode="{k}">{n}</button>'
                  for k, n in modes)
    check = ('<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" '
             'stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M5 12.5l4.5 4.5L19 7.5"/></svg>')
    sw = "".join(
        f'<button type="button" class="sw" role="radio" aria-checked="false" data-accent="{k}" '
        f'data-name="{n}" aria-label="{n}" title="{n}" '
        f'style="--a:{recolor(BRAND_GREEN, h, c) if k != "emerald" else BRAND_GREEN};'
        f'--b:{recolor(BRAND_GREEN_DEEP, h, c) if k != "emerald" else BRAND_GREEN_DEEP}">{check}</button>'
        for k, h, c, n in ACCENTS)
    return ('<div class="appearance" id="appearance" hidden>'
            '<div class="ap-scrim" data-ap-close></div>'
            '<section class="ap-sheet" role="dialog" aria-modal="true" aria-labelledby="ap-title">'
            '<span class="ap-grab" aria-hidden="true"></span>'
            '<div class="ap-head"><h2 id="ap-title">المظهر</h2>'
            '<button type="button" class="icon-btn ap-x" data-ap-close aria-label="إغلاق">'
            '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">'
            '<path d="M6 6l12 12M18 6L6 18"/></svg></button></div>'
            f'<div class="segmented ap-mode" role="radiogroup" aria-label="الوضع">{seg}</div>'
            '<p class="ap-label">لون التطبيق · <b data-accent-name>زمردي</b></p>'
            f'<div class="ap-swatches" role="radiogroup" aria-label="لون التطبيق">{sw}</div>'
            '<p class="ap-note">نفس ألوان تطبيق ميزان للهواتف، ويُحفظ اختيارك على هذا الجهاز.</p>'
            '</section></div>')


ICONS = {
    "coins": '<circle cx="9" cy="9" r="6"/><path d="M15.5 9.5a6 6 0 1 1-6 6"/><path d="M9 6v6M7 8h4"/>',
    "wheat": '<path d="M12 22V8"/><path d="M12 8c-2-1-3-3-3-5 2 0 3 2 3 5zM12 8c2-1 3-3 3-5-2 0-3 2-3 5z"/><path d="M12 13c-2-1-4-2-4-4 2 0 4 1 4 4zM12 13c2-1 4-2 4-4-2 0-4 1-4 4zM12 18c-2-1-4-2-4-4 2 0 4 1 4 4zM12 18c2-1 4-2 4-4-2 0-4 1-4 4z"/>',
    "scale": '<path d="M12 3v18M5 21h14M4 7h16"/><path d="M7 7l-3 7a3 3 0 0 0 6 0zM17 7l-3 7a3 3 0 0 0 6 0z"/>',
    "calendar": '<rect x="3" y="5" width="18" height="16" rx="2"/><path d="M3 10h18M8 3v4M16 3v4"/><path d="M15 14.5a2.5 2.5 0 1 1-2-3 2 2 0 0 0 2 3z"/>',
    "hourglass": '<path d="M6 2h12M6 22h12M7 2c0 6 10 6 10 10S7 16 7 22M17 2c0 6-10 6-10 10s10 4 10 10"/>',
    "mosque": '<path d="M12 2v3"/><path d="M6 12a6 6 0 0 1 12 0"/><path d="M4 22V12h16v10"/><path d="M10 22v-5a2 2 0 0 1 4 0v5"/><path d="M2 22h20"/>',
    "compass": '<circle cx="12" cy="12" r="9"/><path d="M15.5 8.5l-2 5-5 2 2-5z"/>',
    "beads": '<circle cx="12" cy="4" r="2"/><circle cx="18" cy="8" r="2"/><circle cx="18" cy="15" r="2"/><circle cx="6" cy="8" r="2"/><circle cx="6" cy="15" r="2"/><path d="M12 19v3"/><circle cx="12" cy="18" r="1.5"/>',
    "moon": '<path d="M20 14.5A8 8 0 1 1 9.5 4a6.5 6.5 0 0 0 10.5 10.5z"/><path d="M17 3l.6 1.4L19 5l-1.4.6L17 7l-.6-1.4L15 5l1.4-.6z"/>',
    "hand": '<path d="M7 11V6a1.5 1.5 0 0 1 3 0v4M10 10V4.5a1.5 1.5 0 0 1 3 0V10M13 10V5.5a1.5 1.5 0 0 1 3 0V11M16 11V8a1.5 1.5 0 0 1 3 0v6a7 7 0 0 1-7 7h-1a6 6 0 0 1-5-3l-2.5-4a1.5 1.5 0 0 1 2.5-1.6L7 14"/>',
    "book": '<path d="M4 4h6a2 2 0 0 1 2 2v14a2 2 0 0 0-2-2H4zM20 4h-6a2 2 0 0 0-2 2v14a2 2 0 0 1 2-2h6z"/>',
}


def icon(name, cls="icon"):
    return (f'<svg class="{cls}" viewBox="0 0 24 24" fill="none" stroke="currentColor" '
            f'stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">'
            f'{ICONS.get(name, "")}</svg>')


def esc(s):
    return html.escape(str(s), quote=True)


def read_page(path):
    raw = path.read_text(encoding="utf-8")
    m = META_RE.match(raw)
    if not m:
        raise SystemExit(f"الصفحة {path} لا تحتوي على كتلة META")
    meta = json.loads(m.group(1))
    body = raw[m.end():]
    return meta, body


def resolve_includes(body, depth=0):
    if depth > 5:
        return body

    def repl(m):
        f = SRC / m.group(1)
        if not f.exists():
            print(f"  تحذير: ملف التضمين غير موجود {f}")
            return ""
        return resolve_includes(f.read_text(encoding="utf-8"), depth + 1)

    return INCLUDE_RE.sub(repl, body)


def extract_faq(body):
    """يستخرج كتل <!--FAQ [...]--> ويحوّلها إلى قسم أسئلة شائعة + بيانات منظمة."""
    faqs = []

    def repl(m):
        items = json.loads(m.group(1))
        faqs.extend(items)
        out = ['<section class="faq card" aria-labelledby="faq-title">',
               '<h2 id="faq-title">الأسئلة الشائعة</h2>']
        for q, a in items:
            out.append(f'<details><summary>{esc(q)}</summary><div class="faq-a"><p>{a}</p></div></details>')
        out.append("</section>")
        return "\n".join(out)

    body = FAQ_RE.sub(repl, body)
    return body, faqs


def tools_grid(exclude=None, limit=None):
    items = [t for t in TOOLS if t["key"] != exclude]
    if limit:
        items = items[:limit]
    cards = []
    for t in items:
        cards.append(
            f'<a class="tool-card" href="{t["path"]}" data-tool="{t["key"]}">'
            f'<span class="tool-ico">{icon(t["icon"])}</span>'
            f'<span class="tool-name">{esc(t["name"])}</span>'
            f'<span class="tool-desc">{esc(t["desc"])}</span>'
            f'<span class="tool-go" aria-hidden="true">←</span></a>')
    return '<div class="tools-grid">' + "".join(cards) + "</div>"


def articles_list(articles, limit=None):
    items = sorted(articles, key=lambda a: a["date"], reverse=True)
    if limit:
        items = items[:limit]
    out = ['<div class="articles-grid">']
    for a in items:
        out.append(
            f'<a class="article-card" href="{a["path"]}">'
            f'<span class="article-tag">{esc(a.get("category", "مقالات"))}</span>'
            f'<h3>{esc(a["h1"])}</h3><p>{esc(a["description"])}</p>'
            f'<span class="article-meta">{esc(a.get("read_time", ""))}</span></a>')
    out.append("</div>")
    return "".join(out)


def nav_html(active):
    links = [
        ("home", "/", "الرئيسية"),
        ("zakat", "/zakat-calculator/", "الزكاة"),
        ("inheritance", "/inheritance-calculator/", "المواريث"),
        ("prayer", "/prayer-times/", "مواقيت الصلاة"),
        ("hijri", "/hijri-converter/", "التاريخ الهجري"),
        ("articles", "/articles/", "مقالات"),
        ("app", "/app/", "التطبيق"),
    ]
    out = []
    for key, href, label in links:
        cur = ' aria-current="page"' if key == active else ""
        out.append(f'<a href="{href}"{cur}>{label}</a>')
    # قائمة منسدلة لكل الأدوات
    tools = "".join(f'<a href="{t["path"]}">{icon(t["icon"], "icon sm")}<span>{esc(t["name"])}</span></a>'
                    for t in TOOLS)
    out.append(f'<div class="nav-more"><button type="button" class="nav-more-btn" aria-expanded="false">'
               f'كل الأدوات <span aria-hidden="true">▾</span></button>'
               f'<div class="nav-menu">{tools}</div></div>')
    return "".join(out)


TABS = [
    ("home", "/", "الرئيسية", '<path d="M3 11l9-8 9 8"/><path d="M5 10v10h14V10"/><path d="M10 20v-6h4v6"/>'),
    ("prayer", "/prayer-times/", "الصلاة", ICONS["mosque"]),
    ("qibla", "/qibla-direction/", "القبلة", ICONS["compass"]),
    ("tasbih", "/tasbih/", "المسبحة", ICONS["beads"]),
    ("tools", "/#tools", "الأدوات", '<rect x="3" y="3" width="7" height="7" rx="1.5"/><rect x="14" y="3" width="7" height="7" rx="1.5"/><rect x="3" y="14" width="7" height="7" rx="1.5"/><rect x="14" y="14" width="7" height="7" rx="1.5"/>'),
]


def tabbar(active):
    """شريط التنقل السفلي (يظهر عند تشغيل الموقع كتطبيق مثبت)."""
    tool_keys = {t["key"] for t in TOOLS} - {"prayer", "qibla", "tasbih"}
    out = []
    for key, href, label, svg in TABS:
        on = key == active or (key == "tools" and active in tool_keys | {"articles"})
        cur = ' aria-current="page"' if on else ""
        out.append(f'<a href="{href}" data-tab="{key}"{cur}><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" '
                   f'stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">{svg}</svg>'
                   f'<span>{label}</span></a>')
    return '<nav class="app-tabbar" aria-label="تنقل التطبيق">' + "".join(out) + "</nav>"


def breadcrumb(meta):
    if meta["path"] == "/":
        return "", None
    trail = [("الرئيسية", "/")]
    if meta.get("type") == "article":
        trail.append(("مقالات", "/articles/"))
    trail.append((meta.get("breadcrumb", meta["h1"]), meta["path"]))
    html_parts = []
    for i, (name, href) in enumerate(trail):
        if i == len(trail) - 1:
            html_parts.append(f'<span aria-current="page">{esc(name)}</span>')
        else:
            html_parts.append(f'<a href="{href}">{esc(name)}</a>')
    crumbs = '<nav class="breadcrumb" aria-label="مسار التنقل">' + '<span class="sep">/</span>'.join(html_parts) + "</nav>"
    ld = {
        "@context": "https://schema.org", "@type": "BreadcrumbList",
        "itemListElement": [
            {"@type": "ListItem", "position": i + 1, "name": n, "item": CONFIG["site_url"] + h}
            for i, (n, h) in enumerate(trail)
        ],
    }
    return crumbs, ld


def fingerprint_assets():
    """يضيف بصمة المحتوى إلى أسماء ملفات CSS وJS (style.css ← style.3df49246.css).

    المتصفح يحتفظ بالملفات الثابتة أسبوعًا، وعامل الخدمة يخزّنها، فلو بقي الاسم نفسه بعد تحديث
    لظهرت الصفحات الجديدة بتنسيق قديم. باسم جديد لكل محتوى جديد لا تُستعمل نسخة قديمة أبدًا.
    """
    import hashlib
    mapping = {}
    for f in sorted((DIST / "assets").rglob("*")):
        if f.is_file() and f.suffix in (".css", ".js"):
            h = hashlib.sha1(f.read_bytes()).hexdigest()[:8]
            new = f.with_name(f"{f.stem}.{h}{f.suffix}")
            f.rename(new)
            mapping["/" + f.relative_to(DIST).as_posix()] = "/" + new.relative_to(DIST).as_posix()
    return mapping


def apply_fingerprints(text, mapping):
    for old, new in mapping.items():
        text = text.replace(f'"{old}"', f'"{new}"')
    return text


def build():
    if DIST.exists():
        shutil.rmtree(DIST)
    shutil.copytree(SRC / "assets", DIST / "assets")
    css = DIST / "assets" / "css" / "style.css"
    css.write_text(css.read_text(encoding="utf-8").replace("/*__ACCENTS__*/", accent_css()), encoding="utf-8")
    assets = fingerprint_assets()
    template = (SRC / "templates" / "base.html").read_text(encoding="utf-8")

    pages = []
    for p in sorted((SRC / "pages").rglob("*.html")):
        meta, body = read_page(p)
        meta.setdefault("h1", meta["title"])
        pages.append((p, meta, body))
    articles = [m for _, m, _ in pages if m.get("type") == "article"]

    year = date.today().year
    sitemap = []
    for src_path, meta, body in pages:
        body = resolve_includes(body)
        body, faqs = extract_faq(body)
        body = (body.replace("{{tools_grid}}", tools_grid())
                    .replace("{{articles_list}}", articles_list(articles))
                    .replace("{{articles_latest}}", articles_list(articles, 3))
                    .replace("{{site_name}}", esc(CONFIG["site_name"]))
                    .replace("{{contact_email}}", esc(CONFIG["contact_email"]))
                    .replace("{{site_url}}", esc(CONFIG["site_url"]))
                    .replace("{{play_url}}", esc(CONFIG["play_url"])))

        if meta.get("type") == "tool":
            body += ('<section class="related"><h2>أدوات أخرى قد تفيدك</h2>'
                     + tools_grid(exclude=meta.get("nav"), limit=6) + "</section>")
        if meta.get("type") == "article":
            body += ('<section class="related"><h2>جرّب أدواتنا</h2>'
                     + tools_grid(limit=3) + "</section>")

        crumbs, crumb_ld = breadcrumb(meta)
        url = CONFIG["site_url"] + meta["path"]
        full_title = (meta["title"] if meta["path"] == "/"
                      else f'{meta["title"]} | {CONFIG["site_name"]}')

        ld = []
        if meta["path"] == "/":
            ld.append({"@context": "https://schema.org", "@type": "WebSite",
                       "name": CONFIG["site_name"], "url": CONFIG["site_url"] + "/",
                       "inLanguage": "ar"})
        if crumb_ld:
            ld.append(crumb_ld)
        if meta.get("type") == "tool":
            ld.append({"@context": "https://schema.org", "@type": "WebApplication",
                       "name": meta["h1"], "url": url, "applicationCategory": "UtilitiesApplication",
                       "operatingSystem": "Any", "inLanguage": "ar",
                       "offers": {"@type": "Offer", "price": "0", "priceCurrency": "USD"}})
        if meta.get("type") == "app":
            ld.append({"@context": "https://schema.org", "@type": "MobileApplication",
                       "name": CONFIG["site_name"], "url": url, "operatingSystem": "Android 8.0+",
                       "applicationCategory": "LifestyleApplication", "inLanguage": "ar",
                       "installUrl": CONFIG["play_url"], "downloadUrl": CONFIG["play_url"],
                       "image": CONFIG["site_url"] + "/assets/img/icon-512.png",
                       "offers": {"@type": "Offer", "price": "0", "priceCurrency": "USD"}})
        if meta.get("type") == "article":
            ld.append({"@context": "https://schema.org", "@type": "Article",
                       "headline": meta["h1"], "description": meta["description"],
                       "datePublished": meta["date"], "dateModified": meta.get("updated", meta["date"]),
                       "inLanguage": "ar", "mainEntityOfPage": url,
                       "author": {"@type": "Organization", "name": CONFIG["site_name"]},
                       "publisher": {"@type": "Organization", "name": CONFIG["site_name"]}})
        if faqs:
            ld.append({"@context": "https://schema.org", "@type": "FAQPage",
                       "mainEntity": [{"@type": "Question", "name": q,
                                       "acceptedAnswer": {"@type": "Answer",
                                                          "text": re.sub("<[^>]+>", "", a)}}
                                      for q, a in faqs]})
        ld_html = "".join(f'<script type="application/ld+json">{json.dumps(x, ensure_ascii=False)}</script>'
                          for x in ld)

        head_extra = ""
        if CONFIG.get("adsense_client"):
            head_extra += ('<script async src="https://pagead2.googlesyndication.com/pagead/js/'
                           f'adsbygoogle.js?client={esc(CONFIG["adsense_client"])}" crossorigin="anonymous"></script>')
        if CONFIG.get("google_analytics_id"):
            gid = esc(CONFIG["google_analytics_id"])
            head_extra += (f'<script async src="https://www.googletagmanager.com/gtag/js?id={gid}"></script>'
                           "<script>window.dataLayer=window.dataLayer||[];function gtag(){dataLayer.push(arguments)}"
                           f"gtag('js',new Date());gtag('config','{gid}');</script>")
        if meta.get("noindex"):
            head_extra += '<meta name="robots" content="noindex">'

        scripts = "".join(f'<script src="/assets/js/{s}" defer></script>' for s in meta.get("scripts", []))

        hero = ""
        hm = re.search(r"<!--HERO-->(.*?)<!--/HERO-->", body, re.S)
        if hm:
            hero = hm.group(1)
            body = body[:hm.start()] + body[hm.end():]
        elif not meta.get("custom_hero"):
            lead = f'<p class="lead">{meta["lead"]}</p>' if meta.get("lead") else ""
            extra = ""
            if meta.get("type") == "article":
                extra = (f'<p class="article-info"><span>{esc(meta.get("category", ""))}</span>'
                         f'<span>نُشر: <time datetime="{meta["date"]}">{meta["date"]}</time></span>'
                         f'<span>{esc(meta.get("read_time", ""))}</span></p>')
            hero = (f'<header class="page-hero"><div class="container">{crumbs}'
                    f'<h1>{esc(meta["h1"])}</h1>{lead}{extra}</div></header>')

        out = template
        for k, v in {
            "{{title}}": esc(full_title),
            "{{description}}": esc(meta["description"]),
            "{{canonical}}": esc(url),
            "{{og_type}}": "article" if meta.get("type") == "article" else "website",
            "{{og_image}}": esc(CONFIG["site_url"] + CONFIG["default_image"]),
            "{{site_name}}": esc(CONFIG["site_name"]),
            "{{site_tagline}}": esc(CONFIG["site_tagline"]),
            "{{nav}}": nav_html(meta.get("nav", "")),
            "{{tabbar}}": tabbar(meta.get("nav", "")),
            "{{appearance}}": appearance_sheet(),
            "{{hero}}": hero,
            "{{body_class}}": esc(meta.get("body_class", meta.get("type", "page"))),
            "{{head_extra}}": head_extra,
            "{{jsonld}}": ld_html,
            "{{scripts}}": scripts,
            "{{year}}": str(year),
            "{{footer_tools}}": "".join(f'<li><a href="{t["path"]}">{esc(t["name"])}</a></li>' for t in TOOLS),
        }.items():
            out = out.replace(k, v)
        out = out.replace("{{content}}", body)

        rel = meta.get("output") or (meta["path"].strip("/") + "/index.html" if meta["path"] != "/" else "index.html")
        dest = DIST / rel.lstrip("/")
        dest.parent.mkdir(parents=True, exist_ok=True)
        dest.write_text(apply_fingerprints(out, assets), encoding="utf-8")
        if not meta.get("noindex"):
            sitemap.append((url, meta.get("updated", meta.get("date", date.today().isoformat())),
                            "1.0" if meta["path"] == "/" else ("0.9" if meta.get("type") == "tool" else "0.7")))
        print(f"  ✓ {meta['path']}")

    # sitemap.xml
    xml = ['<?xml version="1.0" encoding="UTF-8"?>',
           '<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">']
    for u, d, pr in sitemap:
        xml.append(f"<url><loc>{esc(u)}</loc><lastmod>{d}</lastmod><priority>{pr}</priority></url>")
    xml.append("</urlset>")
    (DIST / "sitemap.xml").write_text("\n".join(xml), encoding="utf-8")

    (DIST / "robots.txt").write_text(
        f"User-agent: *\nAllow: /\n\nSitemap: {CONFIG['site_url']}/sitemap.xml\n", encoding="utf-8")

    if CONFIG.get("adsense_client"):
        pub = CONFIG["adsense_client"].replace("ca-", "")
        (DIST / "ads.txt").write_text(f"google.com, {pub}, DIRECT, f08c47fec0942fa0\n", encoding="utf-8")

    # ملفات الجذر (عامل الخدمة، .well-known لتطبيق Android)
    root_src = SRC / "root"
    if root_src.exists():
        for f in root_src.rglob("*"):
            if f.is_file():
                dest = DIST / f.relative_to(root_src)
                dest.parent.mkdir(parents=True, exist_ok=True)
                shutil.copy2(f, dest)

    publish_language_packs()

    # عامل الخدمة: يخزّن كل الصفحات والملفات ليعمل التطبيق دون إنترنت
    sw_tpl = SRC / "templates" / "sw.js"
    if sw_tpl.exists():
        import hashlib
        precache = ["/"]
        for _, meta, _ in pages:
            if meta.get("noindex") and meta["path"] != "/404.html":
                continue
            if meta["path"] not in precache:
                precache.append(meta["path"])
        for f in sorted((DIST / "assets").rglob("*")):
            if f.is_file() and f.name != "og-image.png":
                precache.append("/" + f.relative_to(DIST).as_posix())
        h = hashlib.sha1()
        for f in sorted(DIST.rglob("*")):
            if f.is_file():
                h.update(f.read_bytes())
        sw = (sw_tpl.read_text(encoding="utf-8")
              .replace("__VERSION__", "mizan-" + h.hexdigest()[:10])
              .replace("__PRECACHE__", json.dumps(precache, ensure_ascii=False)))
        (DIST / "sw.js").write_text(sw, encoding="utf-8")

    # ملفات إعدادات الاستضافة (Netlify / Cloudflare Pages)
    (DIST / "_headers").write_text(
        "/assets/*\n  Cache-Control: public, max-age=604800\n"
        "/lang/*\n  Cache-Control: public, max-age=3600\n  Access-Control-Allow-Origin: *\n", encoding="utf-8")

    print(f"\nتم بناء {len(pages)} صفحة في {DIST}")


def publish_language_packs():
    """حزم لغات التطبيق: تُنشر في dist/lang مع ترجمة معانيها وفهرس index.json يقرؤه التطبيق.

    المصادر: الحزم المضمّنة في التطبيق (android-app/app/src/main/assets/lang) وحزم الموقع فقط
    (src/lang/*.json مع ملفات ترجمتها). لإضافة لغة دون تحديث التطبيق: ترجم src/lang/en.template.json
    إلى src/lang/<code>.json، وضع ملف ترجمة المعاني بجانبه، وارفع إصدار "version" عند كل تعديل.
    """
    app = ROOT / "android-app" / "shared" / "src" / "commonMain" / "assets"
    site = SRC / "lang"
    out = DIST / "lang"
    out.mkdir(parents=True, exist_ok=True)
    files = sorted((app / "lang").glob("*.json")) + sorted(f for f in site.glob("*.json") if not f.name.endswith(".template.json"))
    packs = {}
    for f in files:
        pack = json.loads(f.read_text(encoding="utf-8"))
        packs[pack["code"]] = (f, pack)  # حزمة الموقع تحل محل المضمّنة بالرمز نفسه
    index = []
    for code, (f, pack) in sorted(packs.items()):
        # ترجمة الأذكار المضمّنة في التطبيق تُرفق بالحزمة المنشورة
        adhkar = app / "adhkar" / f"{code}.json"
        if adhkar.exists() and "adhkar" not in pack:
            pack["adhkar"] = json.loads(adhkar.read_text(encoding="utf-8"))
        (out / f"{code}.json").write_text(json.dumps(pack, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
        size = (out / f"{code}.json").stat().st_size
        q = pack.get("quran")
        if q:
            qf = next((c for c in (site / q["file"], app / "tafsir" / f"{q['id']}.txt") if c.exists()), None)
            if qf is None:
                raise SystemExit(f"language pack {code}: Quran translation {q['file']} not found")
            shutil.copy2(qf, out / q["file"])
            size += qf.stat().st_size
        index.append({"code": code, "native": pack["native"], "english": pack.get("english", code),
                      "version": pack.get("version", 1), "format": pack.get("format", 1),
                      "sizeMb": round(size / 1e6, 1), "file": f"{code}.json"})
    (out / "index.json").write_text(json.dumps({"format": 1, "packs": index}, ensure_ascii=False, indent=1), encoding="utf-8")
    tpl = site / "en.template.json"
    if tpl.exists():
        shutil.copy2(tpl, out / "en.template.json")
    print(f"حزم اللغات: {', '.join(packs)}")


if __name__ == "__main__":
    build()
