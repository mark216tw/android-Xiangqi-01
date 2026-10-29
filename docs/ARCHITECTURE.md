# mini象棋 系統架構與技術文件

本文件說明 mini象棋的技術棧、模組劃分、執行流程、AI 引擎、資料儲存與建置方式。

## 1. 技術概觀

| 項目 | 內容 |
|---|---|
| 語言 | Kotlin 2.1.20 |
| UI | Jetpack Compose、Material 3 |
| 架構 | 單 Activity、MVVM、StateFlow |
| 非同步 | Kotlin Coroutines、Dispatchers.Default |
| 設定儲存 | DataStore Preferences |
| 棋譜儲存 | 內部儲存空間 JSON 檔 |
| 序列化 | kotlinx.serialization JSON |
| 音效 | SoundPool |
| 建置 | Gradle 8.11.1、AGP 8.9.2、JDK 17 |
| Android | minSdk 26、targetSdk 35、compileSdk 35 |
| 測試 | JUnit 4、Perft、AI 搜尋測試、Lint |

## 2. 專案結構

```text
app/src/main/java/com/example/xiangqi/
├── MainActivity.kt              # 單一 Activity、Compose 入口、生命週期
├── model/
│   ├── Pieces.kt                # 棋子編碼、初始盤面、座標與區域判斷
│   └── Move.kt                  # 走法編碼、結果、走子紀錄
├── rules/
│   ├── MoveGenerator.kt         # 走法產生、合法性、攻擊偵測
│   └── Notation.kt              # 中式記譜
├── game/
│   └── Game.kt                  # 對局狀態、悔棋、勝負與重複局面
├── ai/
│   ├── Zobrist.kt               # 盤面雜湊
│   ├── Evaluator.kt             # 子力與位置評估
│   └── SearchEngine.kt          # Alpha-Beta、迭代加深、置換表、靜態搜尋
├── data/
│   ├── SettingsStore.kt         # DataStore 設定
│   └── GameStore.kt             # 棋譜 JSON 存取
├── audio/
│   └── SoundManager.kt          # SoundPool 音效管理
└── ui/
    ├── App.kt                   # 主題、導覽、系統列、螢幕喚醒
    ├── Common.kt                # 日期、結果文字、共用工具
    ├── theme/Theme.kt           # Material 3 配色
    ├── board/XiangqiBoard.kt    # Canvas 棋盤、棋子、動畫、提示、AI 落子標記
    ├── menu/MenuScreen.kt       # 主選單、新局設定、回應式版面
    ├── game/GameScreen.kt       # 對局畫面、計時、被吃棋子、對話框
    ├── game/GameViewModel.kt    # 對局流程、AI 排程、計時、存檔、回放
    ├── records/RecordsScreen.kt # 棋譜列表、刪除
    ├── replay/ReplayScreen.kt   # 回放控制
    └── settings/SettingsScreen.kt
```

## 3. 執行架構

```text
MainActivity
  └── MiniXiangqiApp
        ├── collectAsStateWithLifecycle(GameViewModel.ui)
        ├── MiniXiangqiTheme
        ├── 系統列外觀 / keepScreenOn
        └── AppScreen 導覽
              ├── MENU
              ├── GAME
              ├── RECORDS
              ├── SETTINGS
              └── REPLAY
```

`GameViewModel` 是流程核心，持有：

- `Game`：目前對局與規則狀態
- `SearchEngine`：AI 搜尋引擎
- `SettingsStore`、`GameStore`
- `SoundManager`
- AI／提示／計時／回放協程 Job
- `UiState` 與 `MutableStateFlow`

UI 只透過 `UiState` 顯示資料，並呼叫 ViewModel 的公開方法執行操作。

## 4. 盤面與規則

### 4.1 盤面表示

- 棋盤為 9 欄 × 10 列，共 90 格。
- 以 `IntArray(90)` 表示，索引為 `row * 9 + col`。
- 紅方位於下方，黑方位於上方，紅先。
- 棋子編碼：
  - 紅：`1..7`（帥、仕、相、馬、車、炮、兵）
  - 黑：`9..15`（將、士、象、馬、車、砲、卒）
  - `0` 為空位
- 走法以 `from * 90 + to` 編碼。

### 4.2 走法產生

`MoveGenerator` 依棋子類型產生虛擬走法：

- 車：四方向直線
- 炮：移動同車；吃子需隔一個炮架
- 馬：走日字，檢查蹩馬腿
- 相／象：走田字，檢查塞象眼，不可過河
- 仕／士：九宮內斜走一步
- 將／帥：九宮內直走一步，並以同欄照面判定飛將
- 兵／卒：向前一步，過河後可左右移動

合法性流程：

1. 產生虛擬走法
2. 暫時套用走法
3. 檢查己方將帥是否被攻擊（含飛將）
4. 還原盤面
5. 只保留合法走法

`generateLegalInto()` 可將合法走法寫入呼叫端提供的 buffer，供 AI 搜尋重用，避免每個節點配置陣列。

### 4.3 攻擊偵測

`isAttacked()` 採反向掃描：

- 車：四方向找第一個棋子
- 炮：跳過炮架後找第二個棋子
- 馬：由目標格反推八個馬位置並檢查馬腿
- 兵／卒：依方向與是否過河判斷
- 仕／士、相／象：補齊圖樣
- 將／帥：相鄰格與同欄飛將

### 4.4 勝負與和棋

- 將死：對方被將軍且無合法走法，判負
- 困斃：對方未被將軍但無合法走法，中國象棋規則判負
- 三次重複：以 Zobrist 盤面雜湊計算，同一局面出現三次判和
- 60 回合無吃子：`halfMoveClock >= 120` 判和
- 認輸、超時、手動結束

`Game` 使用 snapshot 保存每一步之前的盤面、輪走方、結果、半回合計數與重複局面計數，支援悔棋。

### 4.5 中式記譜

`Notation` 產生：

- 炮二平五
- 馬八進七
- 卒七進一

處理紅黑檔案編號方向、同欄同種棋子的前／中／後區分，以及直行子與斜行子的記法差異。

## 5. AI 引擎

### 5.1 評估函式

`Evaluator` 使用子力價值加上位置分表（PST），紅黑鏡像：

| 棋子 | 分值 |
|---|---:|
| 車 | 900 |
| 炮 | 450 |
| 馬 | 400 |
| 仕／士 | 200 |
| 相／象 | 200 |
| 兵／卒 | 100 |
| 將／帥 | 0 |

評估結果以「目前輪走方」為正負方向。

### 5.2 搜尋

- Negamax + Alpha-Beta
- 迭代加深，時間截止後保留上一層完整結果
- 靜態搜尋（Quiescence）：延伸吃子；被將軍時延伸全走法
- 置換表：Zobrist 雜湊、直接映射，大小 `2^19`
- 走法排序：置換表走法 → MVV-LVA 吃子 → Killer → History
- 將死分數 `MATE = 30000`
- 支援 `maxNodes` 與 `maxTimeMs` 搜尋上限，測試可固定資源
- 每 1024 個節點檢查時間與取消旗標

### 5.3 效能設計

- **增量 Zobrist**：make/unmake 時同步 XOR 棋子與輪走方，避免每個節點重算 90 格。
- **Per-ply 走法 buffer**：每個深度層重用固定 `IntArray`，降低 GC 壓力。
- **走法排序 buffer 重用**。
- **將帥位置快取**：合法走法檢查時只查一次將帥位置。
- **置換表延遲配置**：第一次搜尋時才配置，避免 App 啟動時在主執行緒配置大型陣列。

### 5.4 難度

| 難度 | 深度上限 | 時間上限 | 特性 |
|---|---:|---:|---|
| 簡單 | 2 | 320ms | 加入 110 分噪聲，從前段走法隨機挑選 |
| 中等 | 4 | 800ms | 穩定搜尋 |
| 困難 | 8 | 1800ms | 時間內盡量加深 |

提示功能固定使用中等難度。

### 5.5 執行與取消

- AI 於 `Dispatchers.Default` 執行，不阻塞 UI。
- ViewModel 使用 `searchMutex` 確保 AI 與提示不會同時操作同一個 `SearchEngine`。
- 新局、悔棋、離開畫面時會取消 AI／提示／計時／回放 Job。
- AI 與提示搜尋各自使用協程 `isActive` 與取消旗標。

## 6. 狀態管理

`UiState` 集中描述畫面所需資料：

- 目前畫面、盤面、輪走方、玩家執子方
- 選取格、合法落點、上一手、AI 落子標記
- 將軍、結果、走法記譜
- 思考中、提示狀態
- 難度、時間制度、雙方剩餘時間
- 設定、棋譜列表、續玩狀態
- 回放狀態、移動動畫、結果對話框

計時器每 250ms 更新一次內部時間，但只有顯示秒數變化時才更新 UI，降低重組頻率。

## 7. 資料儲存

### 7.1 設定

`SettingsStore` 使用 DataStore Preferences 儲存：

- 音效
- 動畫
- 顯示模式
- 棋盤風格
- 預設難度
- 預設執子方
- 預設計時模式

### 7.2 棋譜

`GameStore` 將棋譜存為 `filesDir/games/{id}.json`。

`SavedGame` 欄位包含：

```text
schemaVersion, id, createdAt, updatedAt,
difficulty, humanSide, timeControl,
moves[{from,to}], outcome, reason, finished,
redTimeMs, blackTimeMs
```

寫入策略：

- 使用 Mutex 序列化存檔
- 先寫入 `.tmp` 再 rename，降低寫入中斷造成檔案損毀的風險
- 每次落子後自動存檔
- 存檔成功後以記憶體 upsert 更新列表，避免每步重新掃描全部檔案

讀取時使用 `runCatching` 容錯，損毀或缺少欄位的檔案會被忽略或使用預設值。

## 8. 音效

`SoundManager` 使用 SoundPool 播放：

- 落子
- 吃子
- 將軍
- 勝利
- 失敗

音效由 `tools/generate_sounds.ps1` 合成，跟隨設定中的音效開關。

## 9. UI 與棋盤繪製

`XiangqiBoard` 使用 Compose Canvas 繪製：

- 圓角漸層背景、九宮斜線、楚河漢界
- 棋子圓形、內外框與書法字
- 上一手標記、選取、合法落點、將軍
- 棋子移動動畫與吃子淡出
- 提示箭頭與脈動圓圈
- AI 落子外圈與起點標記
- 執黑時棋盤翻轉

其他 UI 設計：

- 主選單依螢幕高度調整間距，小螢幕可捲動
- 棋盤風格選項顯示代表色
- 被吃棋子與子力差列
- 低時間警示
- 對局與回放期間保持螢幕喚醒
- 觸覺回饋

## 10. 建置與測試

### 10.1 Build Types

| Build Type | R8 | 資源縮減 | 簽章 | 版本後綴 |
|---|---|---|---|---|
| debug | 否 | 否 | Debug | `-debug` |
| release | 否 | 否 | 未設定 | 無 |
| prerelease | 是 | 是 | Debug | `-prerelease` |

`prerelease` 供測試發行使用，不具正式簽章。

### 10.2 測試

```powershell
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:lintDebug
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:assemblePrerelease
```

測試內容包含：

- 初始局面 Perft 深度 1～3
- 飛將、蹩馬腿、塞象眼、炮架
- 將死局面
- 中式記譜
- AI 一步殺
- AI 合法走法與固定 node/time 上限
- 增量 Zobrist 與走法 buffer 正確性
- 三次重複局面
- 手動結束

## 11. 已知限制

- 長將、長捉僅以三次重複判和簡化處理。
- 無開局庫、殘局庫。
- 尚未支援 PGN／FEN、分享棋譜、線上對戰。
- 目前以直向手機版面為主。
- 棋盤 Canvas 尚無完整 TalkBack 逐格操作語意。
- `prerelease` 使用 Debug 金鑰簽署，不可作為正式發佈。
