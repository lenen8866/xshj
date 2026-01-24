plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}
android {
    namespace = "com.book.reader"
    compileSdk = 36
    //assetPacks = intArrayOf(":install-time-asset_pack")  //PAD资源分发
// 声明Asset Pack关联（AGP 8.x Kotlin DSL 写法）


   // assetPacks = [":install_time_asset_pack"]
    //assetPacks = intArrayOf(":install-time-asset_pack")
    //assetPacks = [":install_time_asset_pack"]
    //assetPacks = [":asset-pack-name", ":asset-pack2-name"]
    //assetPacks += listOf<String>("install_time_asset_pack")

    defaultConfig {
        applicationId = "com.sda.books.reader"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        //assetPacks = [':assetsPackGameRes']
    }

    buildTypes {
        release {
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
    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        viewBinding = true
        dataBinding = true
    }
}

dependencies {
    // 必须添加这个库，用于将 OkHttp3 连接到 FileDownloader
    implementation("cn.dreamtobe.filedownloader:filedownloader-okhttp3-connection:1.1.0")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")

    implementation("com.github.zcweng:switch-button:0.0.3@aar")
    implementation("me.grantland:autofittextview:0.2.+")
    implementation("androidx.media3:media3-exoplayer:1.0.0")
    implementation("androidx.media3:media3-ui:1.0.0")
    // https://mvnrepository.com/artifact/com.github.bumptech.glide/glide
    implementation("com.github.bumptech.glide:glide:4.12.0")

    implementation("com.liulishuo.filedownloader:library:1.7.7")

    implementation("com.github.GrenderG:Toasty:1.5.2")
    //implementation ("org.sufficientlysecure:html-textview:4.0")
    //implementation ("com.github.CarGuo.RickText:textUtilsLib-kotlin:v2.1.5")
    implementation("com.github.AleynP:MVVMLin:2.0.0")
    implementation("com.google.code.gson:gson:2.10.1") // 使用最新版本
    implementation("com.squareup.okio:okio:1.15.0")
    implementation("com.squareup.okhttp3:okhttp:3.14.2")
    implementation("com.blankj:utilcodex:1.31.1")
    // 基础依赖包，必须要依赖
    implementation("com.geyifeng.immersionbar:immersionbar:3.2.2")
// kotlin扩展（可选）
    implementation("com.geyifeng.immersionbar:immersionbar-ktx:3.2.2")
    implementation ("org.sufficientlysecure:html-textview:4.0")
    implementation ("com.alibaba:fastjson:1.2.24")
    implementation("com.eightbitlab:rxbus:1.0.2")

    val sqlite_version = "2.5.2"

    // Java language implementation
    implementation("androidx.sqlite:sqlite:$sqlite_version")

    // Kotlin
    implementation("androidx.sqlite:sqlite-ktx:$sqlite_version")

    // Implementation of the AndroidX SQLite interfaces via the Android framework APIs.
   // implementation("androidx.sqlite:sqlite-framework:$sqlite_version")
    implementation("androidx.sqlite:sqlite-framework:2.5.2")
    //必须使用
    implementation("com.lzy.net:okgo:3.0.4")
    implementation("com.guolindev.permissionx:permissionx:1.8.1")
    implementation("com.github.ansen666:ShapeView:1.3.5")
    // So, make sure you also include that repository in your project’s build.gradle file.
    implementation("com.google.android.play:asset-delivery:2.3.0")
    //implementation ("com.google.android.play:core:2.1.0")

// For Kotlin users also add the Kotlin extensions library for Play Core:
   //implementation ("com.google.android.play:core-ktx:2.1.0")



    //implementation ("com.lzy.net:okrx2:2.0.2")
   // implementation ("com.lzy.net:okserver:2.0.5")

}