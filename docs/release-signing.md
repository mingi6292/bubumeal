# release 서명 설정

release APK/AAB는 키스토어로 서명한다. 키스토어와 비밀번호는 **절대 git에 올리지 않는다**(`.gitignore`에 `*.jks`, `*.keystore`, `local.properties`).

## 동작 방식

`androidApp/build.gradle.kts`가 아래 네 값을 **환경변수 → `local.properties`** 순서로 읽는다.

| 이름 | 의미 |
|---|---|
| `RELEASE_STORE_FILE` | 키스토어 파일 경로 (프로젝트 루트 기준 상대 경로 또는 절대 경로) |
| `RELEASE_STORE_PASSWORD` | 키스토어 비밀번호 |
| `RELEASE_KEY_ALIAS` | 키 alias |
| `RELEASE_KEY_PASSWORD` | 키 비밀번호 |

- 네 값이 **모두** 있으면 release를 서명한다.
- 하나라도 비면 서명 없이 빌드한다(`androidApp-release-unsigned.apk`). 키가 없는 CI도 깨지지 않는다.
- 값은 있는데 키스토어 파일이 없거나 비밀번호가 틀리면 빌드가 실패한다(조용히 넘어가지 않음).

## 키스토어 만들기 (PowerShell)

한 번만 만든다. 비밀번호와 alias는 직접 정하고, 분실하면 같은 앱으로 업데이트를 못 하니 비밀번호 관리자 등에 백업한다.

```powershell
keytool -genkeypair -v `
  -keystore C:\경로\bubumeal-release.jks `
  -alias <alias> `
  -keyalg RSA -keysize 2048 -validity 10000
```

`keytool`이 PATH에 없으면 `& "C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe" ...`처럼 전체 경로로 실행한다.

키스토어는 저장소 밖(예: `C:\Users\<나>\keystores\`)에 두는 것을 권장한다.

## 로컬에서 서명 빌드

`local.properties`(git에 없음)에 추가한다.

```properties
RELEASE_STORE_FILE=C:/경로/bubumeal-release.jks
RELEASE_STORE_PASSWORD=...
RELEASE_KEY_ALIAS=...
RELEASE_KEY_PASSWORD=...
```

```powershell
.\gradlew.bat :androidApp:assembleRelease
```

## GitHub Secrets (CD 단계에서 사용)

저장소 Settings → Secrets and variables → Actions → New repository secret.

| Secret 이름 | 값 |
|---|---|
| `RELEASE_KEYSTORE_BASE64` | 키스토어 파일의 base64 |
| `RELEASE_STORE_PASSWORD` | 키스토어 비밀번호 |
| `RELEASE_KEY_ALIAS` | alias |
| `RELEASE_KEY_PASSWORD` | 키 비밀번호 |

base64 만들기(PowerShell, 결과를 클립보드에 복사):

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("C:\경로\bubumeal-release.jks")) | Set-Clipboard
```

워크플로에서는 시크릿을 파일로 풀고 환경변수로 넘긴다(CD 워크플로 작업 때 추가).

```yaml
- name: 키스토어 복원
  run: echo "${{ secrets.RELEASE_KEYSTORE_BASE64 }}" | base64 -d > "$RUNNER_TEMP/release.jks"
- name: release 빌드
  run: ./gradlew :androidApp:assembleRelease
  env:
    RELEASE_STORE_FILE: ${{ runner.temp }}/release.jks
    RELEASE_STORE_PASSWORD: ${{ secrets.RELEASE_STORE_PASSWORD }}
    RELEASE_KEY_ALIAS: ${{ secrets.RELEASE_KEY_ALIAS }}
    RELEASE_KEY_PASSWORD: ${{ secrets.RELEASE_KEY_PASSWORD }}
```

PR(포크 포함)에서는 시크릿이 전달되지 않으므로 기존 CI는 계속 서명 없이 빌드된다.

## 제공자 콘솔에 release 지문 등록

debug 키와 release 키는 지문이 달라서, release 빌드에서 로그인하려면 release 지문도 각 콘솔에 **추가**해야 한다(debug 값은 지우지 않는다).

먼저 release 키스토어의 지문을 뽑는다.

```powershell
keytool -list -v -keystore C:\경로\bubumeal-release.jks -alias <alias>
```

출력의 `SHA1:` 줄이 SHA-1이다.

### 구글 (Google Cloud Console)
- API 및 서비스 → 사용자 인증 정보 → **OAuth 클라이언트 ID 만들기** → 유형 Android
- 패키지 이름 `com.mg.bubumeal`, SHA-1에 **release SHA-1**을 넣는다(debug용 Android 클라이언트와 별개로 하나 더 만든다).
- 앱이 쓰는 `GOOGLE_WEB_CLIENT_ID`는 웹 클라이언트 ID 그대로이며 바꾸지 않는다.

### 카카오 (카카오 디벨로퍼스)
- 내 애플리케이션 → 앱 설정 → 플랫폼 → Android에 **키 해시 추가**.
- 키 해시는 release 키스토어의 SHA-1을 base64로 바꾼 값이다. PowerShell:

```powershell
keytool -exportcert -keystore C:\경로\bubumeal-release.jks -alias <alias> | & "C:\Program Files\Git\usr\bin\openssl.exe" sha1 -binary | & "C:\Program Files\Git\usr\bin\openssl.exe" base64
```

  (Git for Windows의 openssl 경로 기준. 암호 입력 프롬프트가 나오면 키스토어 비밀번호를 입력한다.)

### 네이버
- 네이버는 SHA-1/키 해시를 쓰지 않는다. Android 앱 패키지명(`com.mg.bubumeal`)으로 등록하므로 서명 변경에 따른 추가 작업은 없다.

### Play 앱 서명을 쓰는 경우
Play Console에 올리면 Google이 앱 서명 키로 다시 서명한다. 그 경우 구글/카카오 콘솔에는 Play Console의 **앱 서명 인증서 지문**(앱 무결성 → 앱 서명)도 추가해야 한다. 스토어 배포 단계에서 다룬다.
