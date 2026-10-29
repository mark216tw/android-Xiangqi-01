# mini象棋 建置與發佈

本文件說明本機建置、測試、`prerelease` APK 與 GitHub Release 流程。

## 1. 環境需求

- JDK 17
- Android SDK 35
- 專案內附 Gradle Wrapper
- Windows PowerShell 或相容終端機

## 2. 常用指令

```powershell
# 單元測試
.\gradlew.bat :app:testDebugUnitTest

# 靜態檢查
.\gradlew.bat :app:lintDebug

# Debug APK
.\gradlew.bat :app:assembleDebug

# R8 壓縮、資源縮減、Debug 金鑰簽署的測試發行版
.\gradlew.bat :app:assemblePrerelease
```

## 3. Build Types

| Build Type | R8 | 資源縮減 | 簽章 | 版本名稱 |
|---|---|---|---|---|
| `debug` | 否 | 否 | Android Debug | `1.0.0-debug` |
| `release` | 否 | 否 | 未設定 | `1.0.0` |
| `prerelease` | 是 | 是 | Android Debug | `1.0.0-prerelease` |

`prerelease` 的設定位於 `app/build.gradle.kts`：

```kotlin
create("prerelease") {
    initWith(getByName("release"))
    isMinifyEnabled = true
    isShrinkResources = true
    signingConfig = signingConfigs.getByName("debug")
    versionNameSuffix = "-prerelease"
    proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro"
    )
}
```

## 4. Prerelease APK

建置完成後產出：

```text
app/build/outputs/apk/prerelease/app-prerelease.apk
```

特性：

- 啟用 R8 程式碼壓縮
- 啟用資源縮減
- 使用 Android Debug 金鑰簽署
- 版本名稱：`1.0.0-prerelease`
- 僅供測試，不可作為正式上架版本

R8 產出：

```text
app/build/outputs/mapping/prerelease/mapping.txt
app/build/outputs/mapping/prerelease/seeds.txt
app/build/outputs/mapping/prerelease/usage.txt
app/build/outputs/mapping/prerelease/resources.txt
```

`mapping.txt` 應保留，以便分析壓縮後的堆疊追蹤。

## 5. 驗證 APK

### 5.1 版本與套件資訊

```powershell
aapt2 dump badging app/build/outputs/apk/prerelease/app-prerelease.apk
```

確認：

- `package: name='com.example.xiangqi'`
- `versionName='1.0.0-prerelease'`
- `application-label` 為「mini象棋」

### 5.2 簽章

```powershell
apksigner verify --print-certs app/build/outputs/apk/prerelease/app-prerelease.apk
```

確認簽章者為：

```text
C=US, O=Android, CN=Android Debug
```

### 5.3 安裝測試

```powershell
adb install -r app/build/outputs/apk/prerelease/app-prerelease.apk
```

若裝置上已有不同簽章的相同套件，需先解除安裝：

```powershell
adb uninstall com.example.xiangqi
```

## 6. GitHub 發佈

### 6.1 建立公開儲存庫

```powershell
git init
git config user.name "mark216tw"
git config user.email "mark216tw@gmail.com"
git add -A
git commit -m "mini象棋 1.0.0：離線人機對弈、AI、棋譜與回放"
git branch -M main
gh repo create mark216tw/android-Xiangqi-01 --public --source=. --remote=origin --push --description "mini象棋 — 離線 Android 中國象棋人機對弈 App（Kotlin + Jetpack Compose，MIT License）"
```

### 6.2 建立 Pre-release

```powershell
gh release create v1.0.0-prerelease `
  "app/build/outputs/apk/prerelease/app-prerelease.apk" `
  --repo mark216tw/android-Xiangqi-01 `
  --title "mini象棋 1.0.0-prerelease" `
  --notes-file RELEASE_NOTES.md `
  --prerelease
```

Release 說明需明確標示：

- 此版本為 Pre-release 測試版本
- 使用 Android Debug 金鑰簽署
- 啟用 R8 壓縮
- 不適合正式發佈或上架

## 7. 授權

本專案採用 MIT License，詳細內容請見根目錄 `LICENSE`。
