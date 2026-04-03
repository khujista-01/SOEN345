import org.gradle.testing.jacoco.tasks.JacocoReport
import java.util.Properties
jacoco {
    toolVersion = "0.8.10"
}
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("jacoco")
}


val localProperties = Properties()
val localPropsFile = rootProject.file("local.properties")
if (localPropsFile.exists()) {
    localPropsFile.inputStream().use { stream: java.io.InputStream ->
        localProperties.load(stream)
    }
}

android {
    namespace = "com.soen345.ticketreservation"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.soen345.ticketreservation"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val supabaseUrl = localProperties.getProperty("SUPABASE_URL") ?: ""
        val supabaseAnonKey = localProperties.getProperty("SUPABASE_ANON_KEY") ?: ""

        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"$supabaseAnonKey\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            enableUnitTestCoverage = true
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
    testOptions {
        unitTests.all {
            it.extensions.configure<JacocoTaskExtension> {
                isIncludeNoLocationClasses = true
                excludes = listOf("jdk.internal.*")
            }
        }
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.mockwebserver)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    testImplementation(kotlin("test"))
    testImplementation("org.json:json:20240303")
}

tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testDebugUnitTest")

    reports {
        html.required.set(true)
        xml.required.set(true)
    }

    val fileFilter = listOf(
        "**/*Activity*.*",
        "**/*Composable*.*",
        "**/R.class", "**/R$*.class",
        "**/BuildConfig.*", "**/Manifest*.*",
        "**/*Test*.*"
    )

     val mainClasses = fileTree(
        "$buildDir/intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes"
    ) {
        exclude(fileFilter)
    }

    classDirectories.setFrom(files(mainClasses))
    sourceDirectories.setFrom(files(
        "src/main/java",
        "src/main/kotlin"
    ))

    executionData.setFrom(fileTree(buildDir) {
        include(
            "jacoco/testDebugUnitTest.exec",
            "outputs/unit_test_code_coverage/debugUnitTest/*.exec"
        )
    })
}
