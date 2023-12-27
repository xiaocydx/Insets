Insets是一个帮助处理WindowInsets的库
<br><br>
[Insets的使用说明](https://www.yuque.com/u12192380/khwdgb/ua7cgzqu6k384i8s)
<br><br>
1. 在根目录的settings.gradle添加
```
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

2. 在module的build.gradle添加
```
dependencies {
    implementation "com.github.xiaocydx:Insets:1.0.0"
}
```
