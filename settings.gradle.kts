pluginManagement {
    repositories {
        /*google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
        jcenter()*/
        maven (url = "https://maven.aliyun.com/repository/public") { }
        maven (url = "https://maven.aliyun.com/repository/central"){  }
        maven (url = "https://maven.aliyun.com/repository/google"){}
        maven (url = "https://repo.huaweicloud.com/repository/maven"){  }
        //google()
        mavenCentral()
        gradlePluginPortal()
        maven (url = "https://jitpack.io") {  }
        maven (url = "https://tencent-tds-maven.pkg.coding.net/repository/shiply/repo") {  }

    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        /*google()
        mavenCentral()
        jcenter()
        maven (url = "https://oss.sonatype.org/content/repositories/snapshots/"){ }
        maven (url = "https://jitpack.io"){ }*/
        maven (url = "https://maven.aliyun.com/repository/public") { }
        maven (url = "https://maven.aliyun.com/repository/central"){  }
        maven (url = "https://maven.aliyun.com/repository/google"){}
        maven (url = "https://repo.huaweicloud.com/repository/maven"){  }
        //google()
        mavenCentral()
        gradlePluginPortal()
        maven (url = "https://jitpack.io") {  }
        maven (url = "https://tencent-tds-maven.pkg.coding.net/repository/shiply/repo") {  }
        maven (url = "https://oss.sonatype.org/content/repositories/snapshots/"){ }

    }

}

rootProject.name = "Reader"
include(":app")

