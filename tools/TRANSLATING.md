# Translating the Mizan site

Mizan (mizan-al-muslem.com) is a free Islamic tools site. Arabic is the source; each other language lives in
`src/i18n/<code>/` and is built to `/<code>/…` by `build.py`. Articles stay Arabic-only — do not translate `src/pages/articles/`.

## What to produce for language `<code>`

1. `src/i18n/<code>/pages/<name>.html` — one per file in `src/pages/*.html` **except 404.html** (18 files).
2. `src/i18n/<code>/content/<name>.html` — one per file in `src/content/*.html` (10 files: tool explanations and evidence).
3. `src/i18n/<code>.json` — a flat JSON object `{ "<Arabic string>": "<translation>" }` covering every string in
   `tools/i18n_keys_build.json` (site chrome: menu, footer, tool names/descriptions, appearance sheet).
   Later you may be sent a second key list (strings from the JavaScript tools); add those to the same file.

## Rules

- Translate all human-visible text, `title`/`aria-label`/`placeholder`/`alt` attribute text, and the META JSON fields
  `title`, `h1`, `description`, `lead`, `breadcrumb` (and any other prose field). Keep `path`, `type`, `nav`, `scripts`,
  `output`, `noindex`, `custom_hero`, `body_class` exactly as they are.
- Keep **every** tag, `id`, `class`, `data-*`, `href`, `src`, `name`, `for`, `value`, input attribute, SVG, `{{placeholder}}`,
  `<!--INCLUDE …-->`, `<!--HERO-->`, `<!--/HERO-->`, `<!--AR-ONLY-->` marker exactly as in the Arabic file, in the same order.
  Do not add, remove or reorder elements (the only exception is the verse spans below). Internal links stay as Arabic paths
  (`href="/zakat-calculator/"`); the build rewrites them to `/<code>/…`.
- `<!--FAQ [[q,a],…]-->`: keep valid JSON with the same number of pairs; translate q and a (a may contain HTML; keep its tags).
- In `{x}`-style placeholders inside JSON values keep the names unchanged (`{d}`, `{month}`, `{n}` …); you may move them.
- Quran verses: keep the Arabic text and add the meaning after it, exactly like this:
  `<blockquote class="verse"><span class="verse-ar" lang="ar">﴿…Arabic unchanged…﴾</span><span class="verse-tr">“translated meaning”</span><cite>translated surah reference</cite></blockquote>`
  Hadith (`blockquote.hadith`) and adhkar: translate the meaning (you may keep a short Arabic phrase where it is a known
  formula, e.g. in the tasbih page data), cite translated.
- Arrows: Arabic uses `←` for "go/next"; in LTR languages use `→` (RTL languages ur and fa keep `←`).
- The brand: "ميزان" → "Mizan" in Latin/Cyrillic scripts (Urdu/Persian: میزان; Bengali: মীযান). Site name in the footer etc.
- Use the established Islamic terminology of the language (e.g. zakat, nisab, fidya, kaffarah, faraid, hijri), the way
  Muslim readers of that language write it. Numbers stay as Western digits. Fiqh content must stay accurate — translate, do
  not summarise or drop sentences.
- Replace sentences that only make sense in Arabic (e.g. "واجهة عربية") with the natural equivalent ("interface in 15 languages").
- Copy nothing from `src/pages/articles`. The home page "latest articles" block is between `<!--AR-ONLY-->` markers; keep it
  as is (it is removed for non-Arabic builds) — you may leave its inner text in Arabic.
- Files are UTF-8, no BOM. Do not edit anything outside `src/i18n/<code>/` and `src/i18n/<code>.json`.
- **Do not run `build.py`** (other translators run in parallel and it wipes `dist/`). Validate with
  `python3 tools/check_i18n.py <code> tools/i18n_keys_build.json` until it prints `OK` (Arabic-word warnings are fine
  only for ur/fa or intentional Arabic).

Work file by file (read the Arabic file, write the translation with the Write tool). Big files (index.html, app.html,
about.html, the inheritance/zakat content) can be written in one go; keep full fidelity.
