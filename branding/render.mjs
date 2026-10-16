// يصدّر صور PNG من ملفات SVG التي يولّدها icon.py، بمحرك Chromium نفسه (أدق تنعيم للحواف).
// الاستعمال: node branding/render.mjs   (يحتاج حزمة playwright)
import { createRequire } from 'node:module';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

// require يبحث أيضاً في NODE_PATH حيث تُثبَّت playwright عادة على الجهاز
const { chromium } = createRequire(import.meta.url)('playwright');
const root = path.dirname(fileURLToPath(import.meta.url));
const web = path.join(root, '..', 'src', 'assets', 'img');
const ios = path.join(root, 'ios', 'AppIcon.appiconset');
const jobs = [
  // [المصدر، الناتج، المقاس، شفاف؟]
  ['any', path.join(ios, 'AppIcon-1024.png'), 1024, false],
  ['dark', path.join(ios, 'AppIcon-1024-dark.png'), 1024, true],
  ['tinted', path.join(ios, 'AppIcon-1024-tinted.png'), 1024, false],
  ['any', path.join(web, 'icon-180.png'), 180, false],           // apple-touch-icon: مربع ممتلئ يقصّه iOS
  ['any', path.join(web, 'icon-maskable-512.png'), 512, false],  // الشعار داخل الدائرة الآمنة (80%)
  ['rounded', path.join(web, 'icon-192.png'), 192, true],
  ['rounded', path.join(web, 'icon-512.png'), 512, true],
  ['any', path.join(root, 'icon', 'mizan-icon-1024.png'), 1024, false],
];

const browser = await chromium.launch();
const page = await browser.newPage({ deviceScaleFactor: 1 });
for (const [kind, out, size, transparent] of jobs) {
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
