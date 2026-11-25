import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}
dependencies {
    implementation(project(":shared"))

    implementation(libs.androidx.activity.compose)

    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)
}

// 로그인 키는 git에 없는 local.properties에서 읽는다. 없거나 비어 있으면 그 제공자는 가짜 로그인을 쓴다.
// providers.fileContents를 써야 구성 캐시가 local.properties 변경을 알아챈다.
fun localProperty(name: String): String =
    providers.fileContents(rootProject.layout.projectDirectory.file("local.properties"))
        .asText
        .map { text -> text.lineSequence().map { it.trim() }.firstOrNull { it.startsWith("$name=") }?.substringAfter("=")?.trim().orEmpty() }
        .orElse("")
        .get()

// 서명 값과 로그인 키는 환경변수(CI)를 먼저 보고, 없으면 local.properties(내 PC)에서 읽는다.
fun buildValue(name: String): String =
    providers.environmentVariable(name).orElse("").get().ifBlank { localProperty(name) }

val releaseStoreFile = buildValue("RELEASE_STORE_FILE")
val releaseStorePassword = buildValue("RELEASE_STORE_PASSWORD")
val releaseKeyAlias = buildValue("RELEASE_KEY_ALIAS")
val releaseKeyPassword = buildValue("RELEASE_KEY_PASSWORD")
// 네 값이 모두 있어야 서명한다. 하나라도 비면 서명 없이 빌드해서 키 없는 CI가 깨지지 않게 한다.
val hasReleaseSigning = listOf(releaseStoreFile, releaseStorePassword, releaseKeyAlias, releaseKeyPassword).all { it.isNotBlank() }

val googleWebClientId = buildValue("GOOGLE_WEB_CLIENT_ID")
val kakaoNativeAppKey = buildValue("KAKAO_NATIVE_APP_KEY")
val naverClientId = buildValue("NAVER_CLIENT_ID")
val naverClientSecret = buildValue("NAVER_CLIENT_SECRET")

// 버전은 CD가 태그(v1.2.3)에서 -PappVersionName=1.2.3으로 넘긴다. 없으면(로컬 빌드) 1 / "1.0".
// versionCode = major*10000 + minor*100 + patch. Play와 기기는 versionCode가 커져야 업데이트로 받아 준다.
val appVersionName = providers.gradleProperty("appVersionName").orElse("").get()
val appVersionCode = if (appVersionName.isBlank()) 1 else {
    val match = Regex("""(\d+)\.(\d+)\.(\d+)""").matchEntire(appVersionName)
        ?: error("appVersionName은 1.2.3 형식이어야 합니다: $appVersionName")
    val (major, minor, patch) = match.destructured.toList().map { it.toInt() }
    require(major >= 1 && minor < 100 && patch < 100) {
        "버전 범위를 벗어났습니다(major는 1 이상, minor/patch는 100 미만): $appVersionName"
    }
    major * 10000 + minor * 100 + patch
}

android {
    namespace = "com.mg.bubumeal"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.mg.bubumeal"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = appVersionCode
        versionName = appVersionName.ifBlank { "1.0" }
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$googleWebClientId\"")
        buildConfigField("String", "KAKAO_NATIVE_APP_KEY", "\"$kakaoNativeAppKey\"")
        buildConfigField("String", "NAVER_CLIENT_ID", "\"$naverClientId\"")
        buildConfigField("String", "NAVER_CLIENT_SECRET", "\"$naverClientSecret\"")
        // 매니페스트의 ${KAKAO_NATIVE_APP_KEY} 자리에 들어간다. 카카오 로그인이 끝나고 앱으로 돌아오는 주소(kakao{키}://oauth)를 만든다.
        manifestPlaceholders["KAKAO_NATIVE_APP_KEY"] = kakaoNativeAppKey
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = rootProject.file(releaseStoreFile)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }
    buildTypes {
        release {
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}