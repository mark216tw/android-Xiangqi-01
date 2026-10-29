# mini象棋

mini象棋是一款以 Kotlin 與 Jetpack Compose 開發的 Android 單機中國象棋遊戲，主打離線人機對弈。App 不需要網路權限，內建自製 Alpha-Beta 搜尋引擎、完整棋規、計時、悔棋、提示、棋譜存檔與回放。

- 套件名稱：`com.example.xiangqi`
- 目前版本：`1.0.0`
- 測試發行版：`1.0.0-prerelease`
- 授權：MIT License

## 功能特色

- 人機對弈，可選擇執紅或執黑
- 簡單／中等／困難三種 AI 難度
- 不限時、10 分鐘、10 分鐘 + 5 秒三種時間制度
- 標準中國象棋走法、將軍、將死、困斃、飛將
- 三次重複局面判和、60 回合無吃子判和
- 悔棋、提示、認輸、結束對局、再來一局
- AI 落子後以藍／青色外圈標示移動的棋子
- 被吃棋子與子力差顯示
- 低時間警示、觸覺回饋、對局與回放時保持螢幕喚醒
- 四種棋盤風格：經典木紋、素雅淺色、墨玉、青瓷
- 系統／淺色／深色顯示模式
- 棋譜自動存檔、續玩、列表、完整回放、單筆刪除、刪除已結束棋譜
- 回放支援播放／暫停、逐步、滑桿與點擊走法跳轉
- 執黑時棋盤自動翻轉
- App 圖示為象牙底紅圈「帥」字設計

## 安裝

### 測試發行版 APK

請到 GitHub Releases 下載 `app-prerelease.apk`：

<https://github.com/mark216tw/android-Xiangqi-01/releases>

`1.0.0-prerelease` 是使用 **Android Debug 金鑰簽署**、啟用 R8 壓縮的測試版本，**不是正式上線版本**，不建議直接上架或作為正式發佈使用。若裝置上已安裝不同簽章的版本，請先解除安裝再安裝。

### 從原始碼建置

需求：

- JDK 17
- Android SDK 35
- 可使用專案內附的 Gradle Wrapper

```powershell
# 單元測試
.\gradlew.bat :app:testDebugUnitTest

# Lint
.\gradlew.bat :app:lintDebug

# Debug APK
.\gradlew.bat :app:assembleDebug

# R8 壓縮、Debug 金鑰簽署的測試發行版
.\gradlew.bat :app:assemblePrerelease
```

產出位置：

```text
app/build/outputs/apk/debug/app-debug.apk
app/build/outputs/apk/prerelease/app-prerelease.apk
```

## 快速開始

1. 開啟 App，於主選單點選「新局」。
2. 選擇難度、執子方與時間制度。
3. 點選自己的棋子，再點選合法落點即可落子。
4. AI 落子後，終點棋子會顯示藍／青色外圈；下方會顯示被吃棋子與子力差。
5. 可使用「悔棋」、「提示」、「認輸」或右上角選單。
6. 對局會自動存檔，回到主選單後可從「繼續棋局」或「棋譜與回放」繼續。

完整操作說明請見 [使用指南](docs/USER_GUIDE.md)。

## 專案文件

- [使用指南](docs/USER_GUIDE.md)
- [系統架構與技術文件](docs/ARCHITECTURE.md)
- [系統設計文件](docs/DESIGN.md)
- [建置與發佈](docs/BUILD_AND_RELEASE.md)
- [開發紀錄](docs/DEVELOPMENT_LOG.md)
- [版本紀錄](CHANGELOG.md)

## 專案結構

```text
app/src/main/java/com/example/xiangqi/
├── MainActivity.kt
├── model/          # 棋子、盤面、走法與結果模型
├── rules/          # 走法產生、合法性檢查、中式記譜
├── game/           # 對局狀態、悔棋、勝負判定
├── ai/             # Zobrist、評估函式、Alpha-Beta 搜尋
├── data/           # DataStore 設定與 JSON 棋譜存取
├── audio/          # SoundPool 音效
└── ui/             # Compose 畫面、ViewModel、棋盤繪製
```

## 技術摘要

| 項目 | 內容 |
|---|---|
| 語言 | Kotlin 2.1.20 |
| UI | Jetpack Compose、Material 3 |
| 架構 | 單 Activity、MVVM、StateFlow |
| AI | Negamax + Alpha-Beta、迭代加深、置換表、靜態搜尋、增量 Zobrist |
| 設定儲存 | DataStore Preferences |
| 棋譜儲存 | 內部儲存空間 JSON 檔 |
| 建置 | Gradle 8.11.1、AGP 8.9.2、JDK 17 |
| Android | minSdk 26、targetSdk 35、compileSdk 35 |
| 測試 | JUnit 4、Perft、AI 搜尋測試、Android Lint |

## 已知限制

- 長將、長捉採簡化裁決：同一局面重複三次判和。
- AI 無開局庫、殘局庫，棋力受行動裝置時間預算限制。
- 尚未支援 PGN／FEN 匯入匯出、分享棋譜與線上對戰。
- 目前以直向手機版面為主。
- 棋盤 Canvas 尚無完整的 TalkBack 逐格操作語意。
- `prerelease` 版本使用 Debug 金鑰簽署，不可作為正式發佈版本。

## 授權

本專案採用 [MIT License](LICENSE)。
