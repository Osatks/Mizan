#!/usr/bin/env python3
"""يتحقق من ترجمة لغة: نفس بنية الوسوم والمعرّفات والروابط والعناصر النائبة كالأصل العربي.
الاستخدام: python3 tools/check_i18n.py <code> [keys.json]"""
import json, re, sys
from html.parser import HTMLParser
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SRC = ROOT / "src"
KEEP_ATTRS = {"id", "class", "href", "src", "type", "name", "for", "data-tab", "role", "value", "min", "max", "step",
              "inputmode", "autocomplete", "target", "rel", "viewbox", "d", "fill", "stroke", "width", "height", "hidden"}
META_KEEP = {"path", "type", "nav", "scripts", "output", "noindex", "custom_hero", "body_class", "date", "updated"}


class Shape(HTMLParser):
    def __init__(self):
        super().__init__(convert_charrefs=True)
        self.seq = []
        self.spans = []

    def handle_starttag(self, tag, attrs):
        if tag == "span":
            # ترجمة الآية تضاف بجانبها في الترجمات ولا تُعد اختلافاً في البنية
            skip = dict(attrs).get("class") in ("verse-ar", "verse-tr")
            self.spans.append(skip)
            if skip:
                return
        a = tuple(sorted((k, v) for k, v in attrs if k in KEEP_ATTRS or k.startswith("data-")))
        self.seq.append(("<", tag, a))

    def handle_endtag(self, tag):
        if tag == "span" and self.spans and self.spans.pop():
            return
        self.seq.append(("/", tag))

    def handle_comment(self, data):
        if data.startswith("INCLUDE") or data in ("HERO", "/HERO", "AR-ONLY", "/AR-ONLY"):
            self.seq.append(("!", data.strip()))


def split(text):
    m = re.match(r"^\s*<!--META\s*(\{.*?\})\s*-->", text, re.S)
    meta = json.loads(m.group(1)) if m else None
    body = text[m.end():] if m else text
    faqs = None
    fm = re.search(r"<!--FAQ\s*(\[.*?\])\s*-->", body, re.S)
    if fm:
        faqs = json.loads(fm.group(1))
        body = body[:fm.start()] + body[fm.end():]
    return meta, body, faqs


def shape(html):
    p = Shape()
    p.feed(html)
    return p.seq


def ph(s):
    return sorted(re.findall(r"\{\{[^}]+\}\}|<!--INCLUDE[^>]*-->", s))


def compare(orig, tr, name, errs):
    mo, bo, fo = split(orig.read_text(encoding="utf-8"))
    try:
        mt, bt, ft = split(tr.read_text(encoding="utf-8"))
    except Exception as e:
        errs.append(f"{name}: JSON غير صالح ({e})"); return
    if mo is not None:
        if mt is None:
            errs.append(f"{name}: لا يوجد META"); return
        for k in mo:
            if k not in mt:
                errs.append(f"{name}: مفتاح META مفقود {k}")
            elif k in META_KEEP and mo[k] != mt[k]:
                errs.append(f"{name}: META.{k} يجب ألا يتغير")
    if (fo is None) != (ft is None) or (fo and len(fo) != len(ft)):
        errs.append(f"{name}: عدد أسئلة FAQ مختلف")
    if ph(bo) != ph(bt):
        errs.append(f"{name}: العناصر النائبة مختلفة {set(ph(bo)) ^ set(ph(bt))}")
    so, st = shape(bo), shape(bt)
    if so != st:
        for i, (x, y) in enumerate(zip(so, st)):
            if x != y:
                errs.append(f"{name}: البنية تختلف عند العنصر {i}: الأصل {x} / الترجمة {y}"); break
        else:
            errs.append(f"{name}: عدد العناصر مختلف ({len(so)} / {len(st)})")


def main():
    code = sys.argv[1]
    base = SRC / "i18n" / code
    errs = []
    for orig in sorted(p for p in (SRC / "pages").glob("*.html") if p.name != "404.html") + sorted((SRC / "content").glob("*.html")):
        sub = "pages" if orig.parent.name == "pages" else "content"
        tr = base / sub / orig.name
        if not tr.exists():
            errs.append(f"{sub}/{orig.name}: غير موجود"); continue
        compare(orig, tr, f"{sub}/{orig.name}", errs)
        if re.search(r"[؀-ۿ]", tr.read_text(encoding="utf-8")) and code not in ("ur", "fa"):
            n = len(re.findall(r"[؀-ۿ]+", re.sub(r'<(span|p) [^>]*class="verse-ar"[^>]*>.*?</\1>|﴿.*?﴾', "", tr.read_text(encoding="utf-8"), flags=re.S)))
            if n > 3:
                errs.append(f"{sub}/{orig.name}: {n} كلمة عربية خارج الآيات (تحقق أنها مقصودة)")
    d = SRC / "i18n" / f"{code}.json"
    if len(sys.argv) > 2:
        keys = json.loads(Path(sys.argv[2]).read_text(encoding="utf-8"))
        try:
            dic = json.loads(d.read_text(encoding="utf-8"))
        except Exception as e:
            errs.append(f"{code}.json: {e}"); dic = {}
        for k in keys:
            if not dic.get(k):
                errs.append(f"{code}.json: لا ترجمة لـ «{k}»")
            elif sorted(re.findall(r"\{\w+\}", k)) != sorted(re.findall(r"\{\w+\}", dic[k])):
                errs.append(f"{code}.json: العناصر النائبة {{x}} مختلفة في «{k}»")
    print("\n".join(errs) if errs else "OK")
    sys.exit(1 if errs else 0)


if __name__ == "__main__":
    main()
