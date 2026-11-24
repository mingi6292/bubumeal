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

// release 서명 값은 환경변수(CI)를 먼저 보고, 없으면 local.properties(내 PC)에서 읽는다.
fun signingValue(name: String): String =
    providers.environmentVariable(name).orElse("").get().ifBlank { localProperty(name) }

val releaseStoreFile = signingValue("RELEASE_STORE_FILE")
val releaseStorePassword = signingValue("RELEASE_STORE_PASSWORD")
val releaseKeyAlias = signingValue("RELEASE_KEY_ALIAS")
val releaseKeyPassword = signingValue("RELEASE_KEY_PASSWORD")
// 네 값이 모두 있어야 서명한다. 하나라도 비면 서명 없이 빌드해서 키 없는 CI가 깨지지 않게 한다.
val hasReleaseSigning = listOf(releaseStoreFile, releaseStorePassword, releaseKeyAlias, releaseKeyPassword).all { it.isNotBlank() }

val googleWebClientId = localProperty("GOOGLE_WEB_CLIENT_ID")
val kakaoNativeAppKey = localProperty("KAKAO_NATIVE_APP_KEY")
val naverClientId = localProperty("NAVER_CLIENT_ID")
val naverClientSecret = localProperty("NAVER_CLIENT_SECRET")

android {
    namespace = "com.mg.bubumeal"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.mg.bubumeal"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
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