// 교대달력 service worker: 앱 파일을 기기에 저장해 오프라인에서도 열리게 합니다.
// 파일을 수정해서 다시 배포할 때는 CACHE 이름의 버전 숫자를 올려주세요 (예: v2).
const CACHE = "shiftcal-v5";
const APP_SHELL = [
  "./",
  "./index.html",
  "./manifest.json",
  "./icon-192.png",
  "./icon-512.png",
  "./fonts/gowun-dodum/files/gowun-dodum-korean-400-normal.woff2",
  "./fonts/gowun-dodum/files/gowun-dodum-latin-400-normal.woff2",
  "./fonts/gowun-dodum/fonts.css",
  "./fonts/ibm-plex-sans-kr/files/ibm-plex-sans-kr-korean-400-normal.woff2",
  "./fonts/ibm-plex-sans-kr/files/ibm-plex-sans-kr-korean-500-normal.woff2",
  "./fonts/ibm-plex-sans-kr/files/ibm-plex-sans-kr-korean-600-normal.woff2",
  "./fonts/ibm-plex-sans-kr/files/ibm-plex-sans-kr-korean-700-normal.woff2",
  "./fonts/ibm-plex-sans-kr/files/ibm-plex-sans-kr-latin-400-normal.woff2",
  "./fonts/ibm-plex-sans-kr/files/ibm-plex-sans-kr-latin-500-normal.woff2",
  "./fonts/ibm-plex-sans-kr/files/ibm-plex-sans-kr-latin-600-normal.woff2",
  "./fonts/ibm-plex-sans-kr/files/ibm-plex-sans-kr-latin-700-normal.woff2",
  "./fonts/ibm-plex-sans-kr/fonts.css",
  "./fonts/jua/files/jua-korean-400-normal.woff2",
  "./fonts/jua/files/jua-latin-400-normal.woff2",
  "./fonts/jua/fonts.css",
  "./fonts/noto-sans-kr/files/noto-sans-kr-korean-400-normal.woff2",
  "./fonts/noto-sans-kr/files/noto-sans-kr-korean-500-normal.woff2",
  "./fonts/noto-sans-kr/files/noto-sans-kr-korean-700-normal.woff2",
  "./fonts/noto-sans-kr/files/noto-sans-kr-korean-900-normal.woff2",
  "./fonts/noto-sans-kr/files/noto-sans-kr-latin-400-normal.woff2",
  "./fonts/noto-sans-kr/files/noto-sans-kr-latin-500-normal.woff2",
  "./fonts/noto-sans-kr/files/noto-sans-kr-latin-700-normal.woff2",
  "./fonts/noto-sans-kr/files/noto-sans-kr-latin-900-normal.woff2",
  "./fonts/noto-sans-kr/fonts.css",
  "./fonts/space-grotesk/files/space-grotesk-latin-500-normal.woff2",
  "./fonts/space-grotesk/files/space-grotesk-latin-700-normal.woff2",
  "./fonts/space-grotesk/fonts.css"
];

self.addEventListener("install", (event) => {
  event.waitUntil(
    caches.open(CACHE).then((cache) => cache.addAll(APP_SHELL)).then(() => self.skipWaiting())
  );
});

self.addEventListener("activate", (event) => {
  event.waitUntil(
    caches.keys()
      .then((keys) => Promise.all(keys.filter((k) => k !== CACHE).map((k) => caches.delete(k))))
      .then(() => self.clients.claim())
  );
});

self.addEventListener("fetch", (event) => {
  if (event.request.method !== "GET") return;
  event.respondWith(
    caches.match(event.request).then((cached) => {
      if (cached) return cached;
      return fetch(event.request)
        .then((res) => {
          // 같은 출처 파일과 구글 폰트(opaque 응답)를 저장해 둡니다.
          if (res.ok || res.type === "opaque") {
            const copy = res.clone();
            caches.open(CACHE).then((cache) => cache.put(event.request, copy));
          }
          return res;
        })
        .catch(() => caches.match("./index.html"));
    })
  );
});
