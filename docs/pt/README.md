# 최종 PT 자료 (12/3–4)

## 목업 (확장 예정 화면)

`mockups/` 폴더의 두 화면은 **아직 만들지 않은 확장 계획을 보여 주는 목업**입니다. 지금 동작하는 것은 차량 앱(`app-vehicle`)뿐이고, 폰 앱과 웹 관리 화면은 만들지 않았습니다. 그래서 두 그림 모두 상단에 **"목업 · 확장 예정 화면"** 표시가 들어 있습니다.

글꼴(Pretendard)과 색, 카드 모양, 차 그림(위에서 본 차)은 차량 앱과 같게 맞췄습니다.

| 파일 | 내용 |
|---|---|
| `mockups/phone.png` (`phone.html`) | **가족 폰 앱** 세 화면. ① 엄마의 폰 "우리 차" 홈: 배터리 72% · 410km · 주차 중, 확인할 것(뒤 범퍼 긁힘 처리 중 · 아빠의 메모), 오늘의 운전자 추천(워치 수면 요약) ② 딸의 폰 보호자 알림: 할아버지 케어 모드 무응답 → 성남졸음쉼터 안내 시작, 시간순 기록, 전화하기 / 확인했어요 ③ 할아버지의 폰 프로필 · 설정: 면허 갱신 D-20, 큰 글씨 모드, 케어 모드(본인 동의 · 보호자 딸), 차에서 말로 남긴 메모(말로 남기기는 차량 앱에 구현됨) |
| `mockups/admin.png` (`admin.html`) | **법인차 관리 웹** (가상 회사 "한결상사" 영업지원팀). 차 3대 상태, 처리 안 된 사고·파손(차 그림에 위치 표시 · 며칠째인지), 인수 점검 기록, 이번 주 운전 습관(100km당 급정거·급가속, 점수·순위 없이 안전 코칭용), 면허 갱신 임박(D-15), 개인정보 안내 |

회사·사람 이름과 번호판은 모두 가상입니다. 실제 회사 이름이나 로고는 쓰지 않았습니다.

## 다시 만들기

HTML을 고친 뒤 Microsoft Edge 헤드리스로 PNG를 다시 찍습니다. PowerShell 에서 저장소 폴더(`C:\dev\carrelay`) 기준:

```powershell
$edge = "C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe"

& $edge --headless=new --disable-gpu --hide-scrollbars --force-device-scale-factor=2 --virtual-time-budget=4000 --window-size=1500,1000 --screenshot="C:\dev\carrelay\docs\pt\mockups\phone.png" "file:///C:/dev/carrelay/docs/pt/mockups/phone.html"

& $edge --headless=new --disable-gpu --hide-scrollbars --force-device-scale-factor=2 --virtual-time-budget=4000 --window-size=1440,900 --screenshot="C:\dev\carrelay\docs\pt\mockups\admin.png" "file:///C:/dev/carrelay/docs/pt/mockups/admin.html"
```

- 결과: `phone.png` 3000×2000, `admin.png` 2880×1800 (2배 해상도, 각각 1MB 안팎).
- 글꼴은 `app-vehicle/src/main/res/font/` 의 Pretendard 를 상대 경로로 불러옵니다. HTML 파일을 다른 곳으로 옮기면 글꼴 경로도 함께 고쳐야 합니다. 인터넷 연결은 필요 없습니다.
- `--virtual-time-budget` 은 글꼴을 다 불러온 뒤에 찍도록 기다리게 합니다.
