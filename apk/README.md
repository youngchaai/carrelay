# apk/

공모전 제출용 release APK (v1.1.0, 2026-11-09 서명 빌드 — 회사 차 모드·운행기록부, 운전자 추가·편집, 화면 피드백, 시작 화면·로고 포함).

| 파일 | 빌드 | 크기 | 언제 쓰나 |
|---|---|---|---|
| `CarRelay_release.apk` | Pleos SDK 빌드 (`carrelay.pleosSdk=true`) — Vehicle · NaviHelper · FusedLocation · SpeechToText 실제 연동 | 17.7MB | Pleos Connect Emulator + **Virtual Console** 로 차량 값을 바꾸며 볼 때 (권장) |
| `CarRelay_virtual_release.apk` | 가상 차량 빌드 — 앱 안의 **가상 차량** 패널로 기어·속도·연료를 바꿈 | 9.7MB | Virtual Console 없이 전체 흐름을 볼 때 |

- 두 파일은 같은 앱(`com.carrelay.vehicle`)이라 **하나만** 설치합니다: `adb install apk/CarRelay_release.apk`
- 회사 차 모드: 앱의 설정 → 차량 용도 → **회사 차**. 시연용 지난 운행은 계기판 누적거리가 100km 이상일 때만 함께 보입니다(새 가상 차량은 0km라 Virtual Console 의 Odometer 를 먼저 올려 두세요).
- 제출할 때 파일 이름 앞에 팀명을 붙입니다: `팀명_CarRelay_release.apk` (공식 형식 `팀명_앱이름_release.apk`, 팀명은 영어 권장)
- 지원 CPU: `arm64-v8a`(차량·Apple Silicon 에뮬레이터), `x86_64`(Windows·Intel 에뮬레이터)
- 다시 만드는 방법: [docs/01_개발환경_세팅.md 8번](../docs/01_개발환경_세팅.md#8-제출용-release-apk)
- 서명 키(`*.jks`)와 비밀번호는 이 저장소에 올리지 않습니다(저장소 밖 `carrelay-keys/`).
