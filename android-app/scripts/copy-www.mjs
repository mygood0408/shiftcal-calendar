// 웹앱 원본(상위 폴더)에서 APK에 들어갈 파일만 www/로 복사합니다.
// 백업 파일, 원본 보관 폴더, 샘플 폴더, 이 android-app 폴더는 복사하지 않습니다.
import { cpSync, copyFileSync, mkdirSync, rmSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const here = dirname(fileURLToPath(import.meta.url));
const root = join(here, "..", "..");
const www = join(here, "..", "www");
rmSync(www, { recursive: true, force: true });
mkdirSync(www, { recursive: true });
for (const f of ["index.html", "manifest.json", "icon-192.png", "icon-512.png"]) {
  copyFileSync(join(root, f), join(www, f));
}
cpSync(join(root, "fonts"), join(www, "fonts"), { recursive: true });
console.log("www ready");
