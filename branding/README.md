# أيقونة ميزان

الميزان الذهبي على زمرّد عميق، وخلفه نجمة ثمانية خافتة. مصممة بمقاييس iOS: مربع 1024 ممتلئ بلا شفافية ولا زوايا مستديرة (يقصّها النظام)، والشعار داخل الدائرة الآمنة فيصلح أيضاً لأقنعة أندرويد.

- `icon/` الأصل (SVG) بأنواعه، وPNG بمقاس 1024
- `ios/AppIcon.appiconset/` جاهزة لـ Xcode: عادية، وداكنة (الميزان على زمرّد قاتم يكاد يكون أسود، كما توصي Apple بخلفية ملونة داكنة)، وملوّنة (رمادي يلوّنه النظام)؛ والداكنة تُكتب أيضاً في `iosApp/iosApp/Assets.xcassets`
- في كتالوج تطبيق iOS أيضاً: `LaunchEmblem` (الميزان وحده لشاشة الإقلاع فوق لون `LaunchBackground`) و`NowPlayingArtwork` (صورة شاشة القفل أثناء التلاوة والأذان)
- تُولَّد منها أيضاً أيقونات الموقع (`src/assets/img/icon-*.png`) وطبقات أيقونة أندرويد (`res/drawable/ic_launcher_*.xml`)

لتعديلها: غيّر الأشكال أو الألوان في `icon.py` ثم:

```bash
python3 branding/icon.py      # SVG وطبقات أندرويد
node branding/render.mjs      # صور PNG (يحتاج playwright)؛ أو بعضها: node branding/render.mjs dark launch artwork
python3 build.py              # الموقع
```
