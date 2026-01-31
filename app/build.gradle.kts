plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}
android {
    namespace = "com.book.reader"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.sda.books.reader"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
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
    // === AndroidX 核心库 ===
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
    implementation("androidx.startup:startup-runtime:1.1.1")  // 新增：启动优化

    // === 视频播放 ===
    implementation("androidx.media3:media3-exoplayer:1.0.0")
    implementation("androidx.media3:media3-ui:1.0.0")
    
    // === 图片加载 ===
    implementation("com.github.bumptech.glide:glide:4.12.0")

    // === 网络层（现代化）===
    implementation("com.squareup.okhttp3:okhttp:4.12.0")  // 升级到最新版
    implementation("com.squareup.retrofit2:retrofit:2.9.0")  // 新增：现代化网络库
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")  // 新增：JSON转换器
    
    // === 数据解析 ===
    implementation("com.google.code.gson:gson:2.10.1")
    
    // === UI 工具 ===
    implementation("com.github.GrenderG:Toasty:1.5.2")
    implementation("com.blankj:utilcodex:1.31.1")
    implementation("com.geyifeng.immersionbar:immersionbar:3.2.2")
    implementation("com.geyifeng.immersionbar:immersionbar-ktx:3.2.2")
    implementation("com.github.AleynP:MVVMLin:2.0.0")
    implementation("com.github.zcweng:switch-button:0.0.3@aar")  // 保留：设置页面使用
    implementation("org.sufficientlysecure:html-textview:4.0")  // 保留：设置预览使用

    // === 数据库 ===
    implementation("androidx.sqlite:sqlite:2.5.2")
    implementation("androidx.sqlite:sqlite-ktx:2.5.2")
    implementation("androidx.sqlite:sqlite-framework:2.5.2")
    
    // === 权限管理 ===
    implementation("com.guolindev.permissionx:permissionx:1.8.1")
    
    // === 其他工具 ===
    implementation("com.github.ansen666:ShapeView:1.3.5")
    implementation("com.google.android.play:asset-delivery:2.3.0")
    
    // ========== 已删除的旧依赖 ==========
    // implementation("com.lzy.net:okgo:3.0.4")  // 已替换为 Retrofit
    // implementation("com.eightbitlab:rxbus:1.0.2")  // 已替换为 Kotlin Flow
    // implementation("me.grantland:autofittextview:0.2.+")  // 已替换为原生 autoSizeTextType
    
    // ========== 测试依赖 ==========
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.3.1")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.0.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test:rules:1.5.0")
}
