# 교대달력 PWA 인수인계 문서

> 이 문서는 Claude 웹 채팅에서 만든 작업을 Claude Code에서 이어서 진행하기 위한 인수인계 자료입니다.
> 작업 날짜: 2026-10-13 / 사용자: mygood0408 (Danchu)

---

## 1. 목표

원래 HTML 한 파일로 된 **교대달력**(교대 근무표·휴가 관리 달력)을,
휴대폰 홈 화면에 설치해 **앱처럼** 쓸 수 있는 PWA(Progressive Web App)로 바꾸고,
**무료 GitHub Pages**에 배포하는 것.

---

## 2. 현재 상태 (완료된 것)

| 항목 | 상태 |
|---|---|
| 원본 HTML을 PWA 작업본으로 복사 | 완료 |
| 설치용 메타 태그, manifest 연결 | 완료 (`index.html` head) |
| `manifest.json` 작성 | 완료 |
| `sw.js` (오프라인 캐시) 작성 | 완료 |
| 아이콘 `icon.svg`, `icon-192.png`, `icon-512.png` 생성 | 완료 (Pillow로 생성) |
| 백업 내보내기 수정 (`window.claude` 의존 제거 → Blob 다운로드) | 완료 |
| 로컬 git 저장소 + 첫 커밋 | 완료 (커밋 `9adf84a`, 브랜치 `main`) |
| GitHub 저장소 생성 | **미완료** (웹 채팅 세션에서는 저장소 생성 API가 막혀 있었음) |
| GitHub Pages 활성화 | **미완료** |
| 실제 휴대폰에서 설치 테스트 | **미완료** |

---

## 3. 파일 구성

작업 폴더: `/home/claude/shiftcal-pwa/` (git 저장소)

```
shiftcal-pwa/
├── index.html       # 메인 앱 (원본 HTML + PWA 수정)
├── manifest.json    # 앱 이름, 색상, 아이콘, standalone 실행 설정
├── sw.js            # service worker: 앱 셸 캐시 + 런타임 캐시 (CACHE = "shiftcal-v1")
├── icon.svg         # 아이콘 원본
├── icon-192.png     # 아이콘 192px
├── icon-512.png     # 아이콘 512px (maskable 겸용)
└── HANDOFF.md       # 이 문서
```

원본 파일(읽기 전용): `/mnt/user-data/uploads/교대달력-4.html`
→ 웹 채팅에서 받은 원본이므로, 앱 내용을 바꿀 때는 **작업본 `index.html`만** 수정하세요.

---

## 4. index.html에서 바뀐 부분 (원본 대비)

1. **head에 추가**
   - `theme-color`, `mobile-web-app-capable`, `apple-mobile-web-app-*` 메타
   - `<link rel="manifest" href="manifest.json">`
   - `<link rel="icon">`, `<link rel="apple-touch-icon">` (icon-192.png)
2. **백업 내보내기 함수 (`#exportBtn` 클릭 핸들러)**
   - 기존: `window.claude.use("downloads")` 사용 → 웹/앱에서 동작하지 않음
   - 변경: `Blob` + `<a download>`로 JSON 파일 다운로드
3. **스크립트 끝 (boot 다음)**
   - `navigator.serviceWorker.register("sw.js")` 추가

그 외 달력 로직, 휴가 계산, 근무형태 계산, 스타일은 **원본 그대로**입니다.

---

## 5. 다음에 할 일 (순서대로)

### 5-1. GitHub 저장소 만들고 푸시 (Claude Code에서)

```bash
cd /home/claude/shiftcal-pwa   # 또는 로컬 작업 폴더
gh repo create shiftcal-calendar --public --source=. --remote=origin --push \
  --description "교대 근무표·휴가 관리 달력 (PWA)"
```

- `gh repo create`가 막히면(GraphQL 403 등) 웹(github.com)에서 **빈 공개 저장소**를 먼저 만들고:
  ```bash
  git remote add origin https://github.com/mygood0408/shiftcal-calendar.git
  git push -u origin main
  ```

### 5-2. GitHub Pages 켜기

웹에서: 저장소 → **Settings → Pages** → Source: *Deploy from a branch* → Branch: `main` / `(root)` → Save

또는 CLI:
```bash
gh api -X POST repos/mygood0408/shiftcal-calendar/pages \
  -f "source[branch]=main" -f "source[path]=/"
```

배포 주소(예상): `https://mygood0408.github.io/shiftcal-calendar/`
1~2분 뒤 접속되는지 확인.

### 5-3. 배포 후 점검 체크리스트

- [ ] 데스크톱 크롬에서 주소 접속 → 달력이 정상 표시되는지
- [ ] 개발자도구(F12) → Application → Manifest에 오류가 없는지
- [ ] Application → Service Workers에 `sw.js`가 *activated* 상태인지
- [ ] 안드로이드 크롬으로 접속 → 메뉴 → "앱 설치" / "홈 화면에 추가"
- [ ] 설치된 앱이 주소창 없이 전체 화면으로 열리는지
- [ ] 비행기 모드에서 설치된 앱을 열었을 때 달력이 뜨는지
- [ ] 설정 → 백업 파일 내보내기 → JSON 파일이 실제로 받아지는지 (**iOS에서 특히 확인**)
- [ ] 아이폰 사파리 → 공유 → 홈 화면에 추가 → 아이콘과 이름이 맞는지

---

## 6. 알려진 이슈 / 주의사항

1. **데이터는 기기 브라우저에만 저장됨 (localStorage)**
   - 앱을 새로 설치하거나 주소가 바뀌면 기존 데이터가 따라오지 않음.
   - 설치 전에 설정 → 백업 파일 내보내기 권장.
   - 불러오기는 설정 → 백업 파일 불러오기.
2. **공휴일 목록은 일부 추정치**
   - 원본 코드 주석에 "추정치일 수 있어 확인 필요"라고 적혀 있음.
   - 특히 2025년 대체공휴일, 2027~2028년 음력 명절·부처님오신날 날짜 확인 필요.
   - 위치: `index.html`의 `HOLIDAYS` 객체.
3. **service worker 캐시 버전**
   - `sw.js`의 `CACHE = "shiftcal-v1"`.
   - `index.html`을 수정해서 재배포하면 **반드시 `shiftcal-v2`처럼 버전을 올릴 것.**
   - 올리지 않으면 기존 사용자 기기에서 옛날 버전이 계속 보임.
4. **Google Fonts**
   - 폰트는 외부 CDN에서 불러옴. 첫 방문 때 인터넷 필요.
   - 이후에는 `sw.js`가 캐시하지만, 캐시 실패 시 시스템 폰트로 대체됨.
5. **아이콘 디자인은 간단한 임시 버전**
   - 달력 모양 + 원본 근무 색상. 나중에 디자인을 바꾸려면 `icon.svg`를 수정하고 PNG를 다시 생성.
   - PNG 재생성 시: Pillow 스크립트 또는 `icon.svg`를 PNG로 변환하는 도구 사용 (ImageMagick은 이 환경에서 SVG 렌더러가 없어 실패함).
6. **실제 기기 테스트 미완료**
   - 설치 화면, 전체 화면 동작, 오프라인 동작은 아직 확인 안 됨.

---

## 7. 앞으로 개선하면 좋을 것 (선택)

- 아이콘 디자인 다듬기 (원본 색상 팔레트 `--accent: #1f5f7a` 기준)
- `manifest.json`의 `id`, `screenshots` 추가 (안드로이드 설치 화면 품질 향상)
- 앱 업데이트 알림 (새 버전이 있으면 "새로고침" 안내 띄우기)
- 데이터 자동 백업을 클라우드(Google Drive 등)로 연결 — 필요할 때만

---

## 8. 작업 시 참고

- 사용자는 **글쓰기 프로필/메모리에 저장된 선호**가 있음:
  - 파일을 만들거나 수정하면 **다운로드 가능한 파일로도 함께 전달**할 것.
  - 설정/문서에서 **불일치나 복붙 오류가 보이면 먼저 알려줄 것.**
- 커밋 메시지 끝에는 다음 줄을 붙일 것:
  ```
  Co-Authored-By: Claude Haiku 5.5 <noreply@anthropic.com>
  Claude-Session: https://claude.ai/code/session_01SmpKRBcYNNycfKaqcB2hPP
  ```
- 사용자는 한국어로 대화합니다. 설명도 한국어로.
