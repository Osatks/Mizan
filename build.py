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
            f'<a class="tool-card" href="{t["path"]}">'
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
        out.append(f'<a href="{href}"{cur}><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" '
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


def build():
    if DIST.exists():
        shutil.rmtree(DIST)
    shutil.copytree(SRC / "assets", DIST / "assets")
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
                    .replace("{{site_url}}", esc(CONFIG["site_url"])))

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
        dest.write_text(out, encoding="utf-8")
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
        "/assets/*\n  Cache-Control: public, max-age=604800\n", encoding="utf-8")

    print(f"\nتم بناء {len(pages)} صفحة في {DIST}")


if __name__ == "__main__":
    build()
