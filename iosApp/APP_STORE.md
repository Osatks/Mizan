# نشر ميزان على App Store · Publishing Mizan on the App Store

[العربية](#العربية) · [English](#english)

ما في المستودع جاهز: سير عمل البناء الموقَّع والرفع (`.github/workflows/ios-release.yml`)، وسير عمل اللقطات (`.github/workflows/ios-screenshots.yml`)، وبيانات الخصوصية (`iosApp/Privacy/`)، ونصوص المتجر بـ12 لغة (`iosApp/fastlane/metadata/`). يبقى عليك إنشاء المفاتيح والضغط على الأزرار أدناه، مرة واحدة.

---

## العربية

### 0) ملخص سريع
| الخطوة | أين | الوقت |
|---|---|---|
| أ. معرّفات التطبيق و App Group وتسجيل iPhone | developer.apple.com | 10 دقائق |
| ب. مفتاح App Store Connect API و Team ID | appstoreconnect.apple.com | 5 دقائق |
| ج. سجل التطبيق | App Store Connect | 3 دقائق |
| د. أسرار GitHub | GitHub ← Settings | 5 دقائق |
| هـ. الخصوصية والتصنيف العمري والسعر | App Store Connect | 10 دقائق |
| و. تشغيل اللقطات ثم الإصدار | GitHub ← Actions | نحو ساعة انتظار |

### أ) developer.apple.com (Certificates, Identifiers & Profiles)
**الشهادات: لا شيء.** التوقيع تلقائي: Xcode على GitHub ينشئ الشهادات وملفات التوقيع بمفتاح API (الخطوة ب). لكن أنشئ المعرّفات بنفسك، لأن نموذج «تطبيق جديد» في App Store Connect لا يعرض إلا معرّفاً مسجلاً مسبقاً، ولأن تسجيل App Group يدوياً أضمن من تركه للتوقيع التلقائي.

1. افتح https://developer.apple.com/account ← **Certificates, IDs & Profiles** ← **Identifiers**.
2. **App Group**: زر **+** ← اختر **App Groups** ← Continue ← Description: `Mizan` ، Identifier: `group.com.mizan.almuslim` ← Continue ← Register.
3. **معرّف التطبيق**: **+** ← **App IDs** ← Continue ← **App** ← Continue:
   - Description: `Mizan` ، Bundle ID: **Explicit** ← `com.mizan.almuslim`
   - في Capabilities فعّل **App Groups** ← Continue ← Register.
   - افتح المعرّف الجديد ← بجانب App Groups اضغط **Configure** (أو Edit) ← اختر `group.com.mizan.almuslim` ← Continue ← Save.
4. **معرّف الأدوات**: كرر الخطوة 3 بـ Description: `Mizan Widgets` و Bundle ID: `com.mizan.almuslim.widgets` مع App Groups ومجموعة `group.com.mizan.almuslim` نفسها.
5. **سجّل جهاز iPhone واحداً** (مطلوب: الأرشفة بالتوقيع التلقائي تحتاج ملف تطوير، وApple لا تنشئه لحساب بلا أجهزة): **Devices** ← **+** ← Platform iOS ، Device Name: `My iPhone` ، Device ID (UDID).
   - لمعرفة UDID على Windows: وصّل الجهاز ← تطبيق **Apple Devices** أو iTunes ← صفحة الجهاز ← اضغط على **Serial Number** فيتحول إلى UDID ← انسخه. (Sideloadly يعرضه أيضاً.)

### ب) مفتاح App Store Connect API و Team ID
1. https://appstoreconnect.apple.com ← **Users and Access** ← تبويب **Integrations** ← **App Store Connect API** ← **Team Keys**.
   - أول مرة: **Request Access** ثم وافق على الشروط.
2. **Generate API Key** (أو +): Name: `GitHub Actions` ، Access: **Admin**.
   - لماذا Admin؟ دور App Manager يكفي للرفع والإرسال للمراجعة، لكن إنشاء الشهادات وملفات التوقيع تلقائياً (cloud signing) يحتاج Admin.
3. **Download API Key** ← ملف `AuthKey_XXXXXXXXXX.p8`. **يُنزَّل مرة واحدة فقط**؛ احفظه في مكان آمن ولا تضعه في المستودع.
4. انسخ **Key ID** (في سطر المفتاح) و **Issuer ID** (فوق الجدول).
5. **Team ID**: https://developer.apple.com/account ← **Membership details** ← Team ID (10 أحرف وأرقام).

### ج) سجل التطبيق في App Store Connect
**Apps** ← **+** ← **New App**:
- Platforms: **iOS**
- Name: `ميزان: القرآن ومواقيت الصلاة` (إن قيل إن الاسم مستعمل جرّب `ميزان المسلم: القرآن والصلاة`، ثم عدّل `metadata/ar-SA/name.txt` بالاسم نفسه)
- Primary Language: **Arabic**
- Bundle ID: `com.mizan.almuslim` (يظهر في القائمة بعد الخطوة أ)
- SKU: `mizan-ios`
- User Access: Full Access ← **Create**

### د) أسرار GitHub
المستودع ← **Settings** ← **Secrets and variables** ← **Actions** ← **New repository secret**:

| الاسم | القيمة |
|---|---|
| `ASC_KEY_ID` | Key ID |
| `ASC_ISSUER_ID` | Issuer ID |
| `ASC_KEY_P8` | ملف .p8 بترميز base64 (الأمر أدناه) |
| `APPLE_TEAM_ID` | Team ID |
| اختياري: `REVIEW_FIRST_NAME` ، `REVIEW_LAST_NAME` ، `REVIEW_PHONE` (مثل `+966500000000`) ، `REVIEW_EMAIL` | بيانات تواصل فريق المراجعة معك (أو اكتبها مرة في App Store Connect ← App Review Information) |

ترميز ملف .p8 في PowerShell (كما فعلنا لمفتاح أندرويد) — ينسخ النتيجة إلى الحافظة:
```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("$HOME\Downloads\AuthKey_XXXXXXXXXX.p8")) | Set-Clipboard
```
ثم الصق في قيمة `ASC_KEY_P8`.

### هـ) الخصوصية والتصنيف العمري والسعر (في صفحة التطبيق على App Store Connect)
**App Privacy** ← Get Started ← «Do you or your third-party partners collect data from this app?» ← **No, we do not collect data from this app** ← Save ← **Publish**.
السبب: «الجمع» عند Apple يعني نقل البيانات خارج الجهاز. الموقع يُعالج على الجهاز فقط لحساب المواقيت والقبلة ولا يُرسل؛ لا حساب ولا تحليلات ولا إعلانات ولا مكتبات تتبع؛ الاتصالات بالشبكة تنزيل محتوى عام (تلاوات everyayah.com، تفاسير cdn.jsdelivr.net، حزم لغات وأصوات أذان mizan-al-muslem.com) بلا أي بيانات شخصية. هذا يطابق `PrivacyInfo.xcprivacy` (NSPrivacyTracking = false ، لا أنواع بيانات مجموعة).
- Privacy Policy URL: `https://mizan-al-muslem.com/privacy-policy/` (يرفعه fastlane أيضاً).

**App Information**:
- **Age Rating** ← Edit: أجب **None / No** عن كل الأسئلة (العنف، المحتوى الجنسي، الألفاظ، الرعب، المخدرات، القمار، المسابقات، الوصول غير المقيد للويب، المحتوى من المستخدمين، المراسلة، الإعلانات، المعلومات الطبية…) ← النتيجة **4+**.
- **Category**: Primary **Reference** ، Secondary **Lifestyle** (يضبطها fastlane). Reference أنسب لأن جوهر التطبيق مرجع: المصحف والتفسير والأذكار بأدلتها وحاسبات الفقه.
- **Content Rights**: «Does your app contain, show, or access third-party content?» ← **Yes**، و«I have the necessary rights» (نص Tanzil بترخيصه، والتفاسير، وتلاوات EveryAyah). يرسلها fastlane أيضاً.
- **Digital Services Act (الاتحاد الأوروبي)**: في Business أو App Information اختر حالة التاجر: **non-trader** إن كنت تنشره فرداً بلا ربح، وإلا trader مع بياناتك. بدونها لا يُنشر في دول الاتحاد.

**Pricing and Availability**: Price ← **Free** ← Availability ← كل الدول، **مع إلغاء China mainland** (تشترط Apple هناك رقم ترخيص ICP).

**التشفير**: لا شيء — `ITSAppUsesNonExemptEncryption = false` في Info.plist، فلا يُسأل عن كل بناء.

### و) التشغيل
1. **اللقطات**: GitHub ← **Actions** ← **iOS Screenshots (App Store)** ← **Run workflow** ← الفرع ← locales: `ar-SA en-US` ← commit: ✓ ← Run. (نحو 30–40 دقيقة.) افتح الملف المرفق `mizan-app-store-screenshots` وراجع الصور؛ تُضاف إلى الفرع في `iosApp/fastlane/screenshots/` تلقائياً. إن كان الفرع محمياً من الدفع فاختر commit: ✗ ثم ضع الصور يدوياً في ذلك المجلد. اللغات التي بلا لقطات خاصة يعرض لها المتجر لقطات اللغة الأساسية (العربية)؛ لإضافة لغة: `ar-SA en-US fr-FR tr …`.
2. **تجربة أولى عبر TestFlight** (مستحسن): **iOS Release (App Store)** ← Run workflow ← submit: ✗. بعد 5–30 دقيقة يظهر البناء في App Store Connect ← TestFlight. أضف نفسك في **Internal Testing** وثبّته من تطبيق TestFlight على iPhone.
3. **الإرسال للمراجعة**: **iOS Release (App Store)** ← Run workflow ← submit: ✓. يبني ويرفع، ثم يرفع النصوص واللقطات، وينتظر معالجة البناء، ويرسله للمراجعة مع **النشر التلقائي بعد القبول**. (المراجعة عادة 24–48 ساعة.)
   - `metadata_only: ✓` يرفع النصوص واللقطات وحدها دون بناء ولا إرسال.
   - في كل إصدار لاحق: حدّث `release_notes.txt` في كل لغة ثم شغّل submit: ✓ مرة أخرى (رقم البناء = عدد الـ commits فيزيد وحده).

### ز) ما تسأل عنه المراجعة عادة وكيف تجيب (بالإنجليزية في Resolution Center)
ملاحظات المراجِع جاهزة في `metadata/review_information/notes.txt` وتُرفع تلقائياً.
- **Guideline 2.5.4 (Background audio)**: «Background audio is used only for Quran recitation, which keeps playing when the screen is locked: open a surah, long-press an ayah > Listen, then lock the device.»
- **Guideline 5.1.1 (Location)**: «Location is optional and processed only on the device to compute prayer times and the qibla. Users can pick a city manually instead.»
- **Guideline 4.2 / 2.3 (Metadata)**: اللقطات من التطبيق الحقيقي؛ لا تذكر أندرويد أو Google Play في النصوص.
- **Guideline 5.2 (Third-party content / Quran text)**: أرسل مصادر المحتوى وترخيصها: Tanzil (tanzil.net، يسمح بإعادة النشر الحرفي مع ذكر المصدر)، EveryAyah.com، التفسير الميسر (مجمع الملك فهد). المصادر مذكورة في الإعدادات.
- **iPad**: الإصدار الأول لـ iPhone فقط، لكن Apple تجرب تطبيقات iPhone على iPad أيضاً (في وضع التوافق): تأكد أنه لا يتعطل هناك. دعم iPad لاحقاً: `TARGETED_DEVICE_FAMILY: "1,2"` في project.yml ولقطات iPad 13 بوصة.
- **Guideline 2.1 (crash/bug)**: اطلب سجل التعطل (crash log) وجرّب البناء نفسه من TestFlight.

### ح) كلفة دقائق GitHub Actions
- في المستودع **الخاص** تُحسب دقيقة macOS بعشر دقائق من الحصة. كل تشغيل للإصدار نحو 25–35 دقيقة (≈ 250–350 دقيقة من الحصة)، واللقطات نحو 30–40 دقيقة. الخطة المجانية 2000 دقيقة شهرياً ≈ 6 تشغيلات macOS.
- في المستودع **العام** دقائق المشغّلات القياسية مجانية.
- لذلك كل سير عمل iOS يدوي فقط (workflow_dispatch)، ولا تشغّل اللقطات إلا عند تغيّر الواجهة.

### ما لا يمكن التحقق منه إلا بتشغيل على Mac
التوقيع التلقائي بالمفتاح (إنشاء الشهادة وملفات التطوير والتوزيع)، ورفع البناء، وتوافق Kotlin 2.1.21 مع Xcode 26، وتنقل اللقطات بالروابط وأزمنة الانتظار، واختيار fastlane للبناء. إن فشلت الأرشفة فسجلها الكامل في الملف المرفق `mizan-ios-release-N`.
- «No profiles / no devices»: سجّل جهازاً (أ‑5).
- «maximum number of certificates»: في Certificates احذف شهادات Apple Development القديمة التي أنشأها «Xcode (API)».
- «Cannot create cloud managed certificate / permission»: المفتاح ليس Admin.

---

## English

### 0) Overview
Everything that does not need your Apple account is in the repo: the signed build + upload workflow (`.github/workflows/ios-release.yml`), the screenshot workflow (`.github/workflows/ios-screenshots.yml`), privacy manifests (`iosApp/Privacy/`), localized Info.plist strings, and App Store metadata in 12 locales (`iosApp/fastlane/metadata/`: ar-SA, en-US, fr-FR, de-DE, es-ES, tr, id, ms, ru, uk, ur-PK, bn-BD). Persian, Uzbek and Kazakh are not App Store locales.

### a) developer.apple.com (Certificates, Identifiers & Profiles)
**Certificates: nothing to do.** Signing is automatic: Xcode on the GitHub runner creates certificates and profiles with the API key. Create the identifiers yourself though: App Store Connect's “New App” form only lists registered bundle IDs, and registering the App Group by hand is more reliable than leaving it to automatic signing.
1. https://developer.apple.com/account → **Certificates, IDs & Profiles** → **Identifiers**.
2. **+** → **App Groups** → Description `Mizan`, Identifier `group.com.mizan.almuslim` → Register.
3. **+** → **App IDs** → **App** → Description `Mizan`, **Explicit** Bundle ID `com.mizan.almuslim`, tick **App Groups** → Register. Open it → App Groups **Configure** → select `group.com.mizan.almuslim` → Save.
4. Repeat for `com.mizan.almuslim.widgets` (Description `Mizan Widgets`) with the same App Group.
5. **Devices** → **+** → register one iPhone UDID. This is required: automatic signing needs a development profile to archive, and Apple won't create one for a team with no devices. On Windows: the Apple Devices app or iTunes → device page → click **Serial Number** to show the UDID. Sideloadly also shows it.

### b) App Store Connect API key and Team ID
1. https://appstoreconnect.apple.com → **Users and Access** → **Integrations** → **App Store Connect API** → **Team Keys**. The first time, click **Request Access** and accept.
2. **Generate API Key**: Name `GitHub Actions`, Access **Admin**. App Manager is enough to upload and submit, but creating cloud-managed certificates and profiles needs Admin.
3. **Download** `AuthKey_XXXXXXXXXX.p8`. You can download it **only once**.
4. Note the **Key ID** and the **Issuer ID** (above the table).
5. **Team ID**: developer.apple.com/account → **Membership details**.

### c) App record
**Apps** → **+** → **New App**: Platform **iOS** · Name `ميزان: القرآن ومواقيت الصلاة` · Primary Language **Arabic** · Bundle ID `com.mizan.almuslim` · SKU `mizan-ios` · Full Access → Create. If the name is taken, choose another one and put the same text in `metadata/ar-SA/name.txt`.

### d) GitHub secrets
Repo → **Settings** → **Secrets and variables** → **Actions** → New repository secret: `ASC_KEY_ID`, `ASC_ISSUER_ID`, `ASC_KEY_P8` (base64 of the .p8), `APPLE_TEAM_ID`. Optional reviewer contact: `REVIEW_FIRST_NAME`, `REVIEW_LAST_NAME`, `REVIEW_PHONE`, `REVIEW_EMAIL`.
```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("$HOME\Downloads\AuthKey_XXXXXXXXXX.p8")) | Set-Clipboard
```

### e) App Privacy, age rating, price
- **App Privacy** → Get Started → “Do you or your third-party partners collect data from this app?” → **No** → Publish (**Data Not Collected**). Apple defines “collect” as transmitting data off the device. Location is processed on the device only. There is no account, analytics, ads or tracking SDK. Network requests only download public content (recitations from everyayah.com, tafsirs from cdn.jsdelivr.net, language packs and adhan voices from mizan-al-muslem.com) and carry no personal data. Privacy policy: `https://mizan-al-muslem.com/privacy-policy/`.
- **Age Rating**: answer None/No to every question → **4+**.
- **Category**: Primary **Reference**, Secondary **Lifestyle**. Reference is recommended because the core of the app is reference content: the Mushaf, tafsir, adhkar with sources, and fiqh calculators. fastlane sets this.
- **Content Rights**: contains third-party content → **Yes**, you have the rights (Tanzil text under its license, tafsirs, EveryAyah recitations). fastlane sends this too.
- **EU Digital Services Act**: declare trader or non-trader status, otherwise the app is not distributed in the EU.
- **Pricing and Availability**: **Free**, all countries except **China mainland**, which requires an ICP filing.
- **Encryption**: `ITSAppUsesNonExemptEncryption = false` is already in Info.plist.

### f) Run
1. Actions → **iOS Screenshots (App Store)** → Run workflow (locales `ar-SA en-US`, commit ✓). Check the `mizan-app-store-screenshots` artifact. The run commits the images to `iosApp/fastlane/screenshots/`. On a protected branch, use commit ✗ and add the images yourself. Locales without their own screenshots show the primary-language (Arabic) set. Add more with `locales: ar-SA en-US fr-FR tr …`.
2. Optional TestFlight check: **iOS Release (App Store)** → submit ✗. The build appears in TestFlight after 5–30 min.
3. **iOS Release (App Store)** → submit ✓: builds, uploads, uploads metadata and screenshots, waits for processing, submits for review, and releases automatically after approval. `metadata_only` ✓ uploads only the texts and screenshots. For later releases, update each `release_notes.txt` first.

### g) Common review questions
Reviewer notes are in `metadata/review_information/notes.txt` and are uploaded automatically. Background audio (2.5.4) is used for Quran recitation on the lock screen. Location (5.1.1) is optional and on-device only. For third-party content (5.2), cite Tanzil, EveryAyah and King Fahd Complex. iPhone-only apps are still tested on iPad in compatibility mode, so make sure the app does not break there. Answer in English in the Resolution Center.

### h) GitHub Actions cost
In private repos, macOS minutes count 10×. A release run takes about 25–35 min (≈250–350 quota minutes) and a screenshot run about 30–40 min. The free plan's 2,000 minutes ≈ 6 macOS runs per month. Standard runners are free in public repos.

### Only verifiable on a Mac run
These can only be confirmed by a real run: automatic signing with the API key (certificate and profile creation), the upload, Kotlin 2.1.21 + Xcode 26 compatibility, screenshot navigation timings, and fastlane picking the build. Archive logs are attached as the `mizan-ios-release-N` artifact. To fix common failures: register a device (a‑5), revoke old “Apple Development” certificates created by Xcode (API), or give the key the Admin role.
