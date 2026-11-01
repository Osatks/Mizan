// يصدّر صور PNG من ملفات SVG التي يولّدها icon.py، بمحرك Chromium نفسه (أدق تنعيم للحواف).
// الاستعمال: node branding/render.mjs [مجموعة…]   (يحتاج حزمة playwright)
//   بلا مجموعات: كل الصور. المجموعات: icons (الأيقونة والموقع)، dark (الأيقونة الداكنة في iOS)،
//   launch (شعار شاشة الإقلاع في iOS)، artwork (صورة شاشة القفل أثناء التلاوة)
import { createRequire } from 'node:module';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

// require يبحث أيضاً في NODE_PATH حيث تُثبَّت playwright عادة على الجهاز
const { chromium } = createRequire(import.meta.url)('playwright');
const root = path.dirname(fileURLToPath(import.meta.url));
const web = path.join(root, '..', 'src', 'assets', 'img');
const ios = path.join(root, 'ios', 'AppIcon.appiconset');
// كتالوج أصول تطبيق iOS نفسه (iosApp/iosApp/Assets.xcassets)
const assets = path.join(root, '..', 'iosApp', 'iosApp', 'Assets.xcassets');
const launch = path.join(assets, 'LaunchEmblem.imageset');
const jobs = [
  // [المصدر، الناتج، المقاس، شفاف؟، المجموعة]
  ['any', path.join(ios, 'AppIcon-1024.png'), 1024, false, 'icons'],
  ['dark', path.join(ios, 'AppIcon-1024-dark.png'), 1024, false, 'dark'],
  ['dark', path.join(assets, 'AppIcon.appiconset', 'AppIcon-1024-dark.png'), 1024, false, 'dark'],
  ['tinted', path.join(ios, 'AppIcon-1024-tinted.png'), 1024, false, 'icons'],
  ['any', path.join(web, 'icon-180.png'), 180, false, 'icons'],           // apple-touch-icon: مربع ممتلئ يقصّه iOS
  ['any', path.join(web, 'icon-maskable-512.png'), 512, false, 'icons'],  // الشعار داخل الدائرة الآمنة (80%)
  ['rounded', path.join(web, 'icon-192.png'), 192, true, 'icons'],
  ['rounded', path.join(web, 'icon-512.png'), 512, true, 'icons'],
  ['any', path.join(root, 'icon', 'mizan-icon-1024.png'), 1024, false, 'icons'],
  // الميزان في وسط شاشة الإقلاع: 160 نقطة بثلاث كثافات
  ['emblem', path.join(launch, 'LaunchEmblem.png'), 160, true, 'launch'],
  ['emblem', path.join(launch, 'LaunchEmblem@2x.png'), 320, true, 'launch'],
  ['emblem', path.join(launch, 'LaunchEmblem@3x.png'), 480, true, 'launch'],
  // صورة «يُشغَّل الآن» في شاشة القفل أثناء التلاوة والأذان
  ['any', path.join(assets, 'NowPlayingArtwork.imageset', 'NowPlayingArtwork.png'), 600, false, 'artwork'],
];

const groups = process.argv.slice(2);
const browser = await chromium.launch();
const page = await browser.newPage({ deviceScaleFactor: 1 });
for (const [kind, out, size, transparent, group] of jobs) {
  if (groups.length && !groups.includes(group)) continue;
  const svg = fs.readFileSync(path.join(root, 'icon', `mizan-icon-${kind}.svg`));
  await page.setViewportSize({ width: size, height: size });
  await page.setContent(`<html><body style="margin:0;background:transparent">
    <img src="data:image/svg+xml;base64,${svg.toString('base64')}" width="${size}" height="${size}" style="display:block"></body></html>`);
  await page.waitForFunction(() => document.images[0].complete);
  fs.mkdirSync(path.dirname(out), { recursive: true });
  await page.screenshot({ path: out, omitBackground: transparent, clip: { x: 0, y: 0, width: size, height: size } });
  console.log('wrote', path.relative(path.join(root, '..'), out), size);
}
await browser.close();
