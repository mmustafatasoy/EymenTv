plugins { id("com.android.application") }
android {
    namespace = "com.eymen.tv"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.eymen.tv"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}
val syncWebUi by tasks.registering(Copy::class) {
    from(file("../..")) { include("index.html") }
    into(layout.projectDirectory.dir("src/main/assets"))
}
tasks.named("preBuild") { dependsOn(syncWebUi) }
