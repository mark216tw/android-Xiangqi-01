# mini象棋 — 開發紀錄

> 專案：`android-Xiangqi-01`  
> 套件：`com.example.xiangqi`  
> 版本：1.0.0（Debug）  
> 開發期間：2026-10-28 ～ 2026-10-29  
> 文件更新：2026-10-29

> 本文件保留開發歷程與早期設計紀錄。最新使用說明、架構、系統設計與建置流程請見 `README.md`、`docs/USER_GUIDE.md`、`docs/ARCHITECTURE.md`、`docs/DESIGN.md`、`docs/BUILD_AND_RELEASE.md`。  
> 測試發行版：`1.0.0-prerelease`（R8 壓縮、Android Debug 金鑰簽署）。

---

## 1. 專案簡介

「mini象棋」是一款 Android 單機中國象棋遊戲，主打人與電腦對弈。整體採離線設計，不需要網路權限，包含完整棋規、內建 AI、計時、悔棋、提示、棋譜存檔與回放。

### 核心目標

- 可在 Android 手機上完整下一局標準中國象棋
- 內建三種難度的電腦對手
- 支援執紅／執黑、計時與悔棋
- 對局自動存檔，離開後可繼續
- 棋譜列表與完整回放
- 離線可用、無廣告、無網路需求

### 最終功能一覽

| 分類 | 功能 |
|---|---|
| 對局 | 人機對弈、執紅／執黑、紅先黑後 |
| 規則 | 標準走法、將軍、將死、困斃、飛將、三次重複和、60 回合無吃子 |
| AI | 簡單／中等／困難、迭代加深、置換表、靜態搜尋 |
| 輔助 | 悔棋、提示（脈動箭頭＋文字建議）、認輸、結束對局 |
| 計時 | 不限時、10 分鐘、10 分鐘 + 5 秒 |
| 記錄 | 自動存檔、續玩、棋譜列表、完整回放、刪除 |
| 外觀 | 系統／淺色／深色模式、4 種棋盤風格、移動與吃子動畫 |
| 音效 | 落子、吃子、將軍、勝利、失敗 |
| 圖示 | 「帥」字 Adaptive Icon 與 Android 13 主題單色圖示 |

---

## 2. 需求演進

### 第一階段：初始規劃

使用者最初詢問中國象棋規則，接著確認要開發 Android 人機對弈 App。經過選項確認後定案：

- 技術棧：Kotlin + Jetpack Compose
- AI：自製 Minimax + Alpha-Beta，不整合外部引擎
- 規則：標準規則 + 簡化禁例（重複局面判和）
- 功能：悔棋、提示、音效與動畫、計時與難度、棋譜與存檔
- App 名稱：`mini象棋`
- 棋譜需支援完整回放

### 第二階段：功能增補

完成第一版後，追加以下需求：

1. 設定頁新增「顯示模式」：系統／淺色／深色，預設系統
2. 狀態列與導覽列需跟隨主介面主題
3. 棋盤風格新增 2 種（定案為「墨玉」與「青瓷」）
4. 提示要更明顯
5. 對局頁右上角新增 `more_vert` 選單與「結束對局」
6. App 圖示中央加上「帥」字

「結束對局」的行為經確認後定為：**結束本局、保存棋譜、標記「已結束」、返回主選單**，與「認輸」及「返回（保留續玩）」區隔。

---

## 3. 技術環境

| 項目 | 版本／設定 |
|---|---|
| 語言 | Kotlin 2.1.20 |
| UI | Jetpack Compose（BOM 2025.05.01）＋ Material 3 |
| 建置 | Gradle 8.11.1、AGP 8.9.2 |
| JDK | Java 17 |
| compileSdk / targetSdk | 35 |
| minSdk | 26（Android 8.0） |
| 架構 | 單 Activity、MVVM、StateFlow |
| 持久化 | DataStore Preferences＋kotlinx.serialization JSON |
| 測試 | JUnit 4、kotlinx-coroutines-test |
| 開發環境 | Windows、Android SDK 35/36、AVD `MiniReaderTest`（1080×2400） |

### 主要依賴

- `androidx.activity:activity-compose`
- `androidx.compose.material3:material3`
- `androidx.lifecycle:lifecycle-runtime-compose` / `viewmodel-compose`
- `androidx.datastore:datastore-preferences`
- `org.jetbrains.kotlinx:kotlinx-serialization-json`

---

## 4. 專案架構

```text
app/src/main/java/com/example/xiangqi/
├── MainActivity.kt              # 單一 Activity、生命週期與 ViewModel 建立
├── model/
│   ├── Pieces.kt                # 棋子編碼、初始盤面、棋盤座標
│   └── Move.kt                  # 走法編碼、勝負結果、走子紀錄
├── rules/
│   ├── MoveGenerator.kt         # 走法產生、攻擊偵測、合法性過濾
│   └── Notation.kt              # 中式記譜
├── game/
│   └── Game.kt                  # 對局狀態、走子、悔棋、勝負判定、重複局面
├── ai/
│   ├── Zobrist.kt               # 盤面雜湊
│   ├── Evaluator.kt             # 子力與位置評估
│   └── SearchEngine.kt          # Alpha-Beta、迭代加深、置換表、靜態搜尋
├── data/
│   ├── SettingsStore.kt         # DataStore 設定（主題、音效、難度…）
│   └── GameStore.kt             # 棋譜 JSON 讀寫
├── audio/
│   └── SoundManager.kt          # SoundPool 音效播放
└── ui/
    ├── App.kt                   # 主題解析、系統列、畫面導覽、BackHandler
    ├── Common.kt                # 日期、結果文字等共用工具
    ├── theme/Theme.kt           # 淺色／深色 Material 3 配色
    ├── board/XiangqiBoard.kt    # Canvas 棋盤、棋子、動畫、提示箭頭
    ├── menu/MenuScreen.kt       # 主選單與新局設定
    ├── game/GameScreen.kt       # 對局畫面與對話框
    ├── game/GameViewModel.kt    # 對局流程、AI 排程、計時、存檔、回放
    ├── records/RecordsScreen.kt # 棋譜列表
    ├── replay/ReplayScreen.kt   # 回放控制與棋盤
    └── settings/SettingsScreen.kt
```

其他資源：

```text
app/src/main/res/
├── drawable/ic_launcher_foreground.xml   # 「帥」字 App 圖示
├── drawable/ic_launcher_monochrome.xml   # 主題圖示
├── mipmap-anydpi-v26/                    # Adaptive Icon
├── raw/*.wav                             # 五種音效
└── values/                               # 字串、顏色、主題

tools/
├── generate_sounds.ps1     # 以程式合成 WAV 音效
└── generate_icon_glyph.py  # 從 Noto Serif TC 取出「帥」字輪廓
```

---

## 5. 棋盤與規則設計

### 5.1 盤面表示

- 棋盤固定 9 欄 × 10 列，共 90 格。
- 以 `IntArray(90)` 表示，索引 `row * 9 + col`。
- 紅方位於下方（row 9 底線），黑方位於上方（row 0 底線），紅先。
- 棋子以整數編碼：
  - 紅：`1..7`（帥、仕、相、馬、車、炮、兵）
  - 黑：`9..15`（將、士、象、馬、車、砲、卒）
  - `0` 為空位
- 走法以 `from * 90 + to` 編碼，降低搜尋時配置成本。

### 5.2 走法產生

`MoveGenerator` 針對每種棋子產生虛擬走法：

- 車：四方向直線，遇到棋子停止。
- 炮：移動同車；吃子必須隔一個炮架。
- 馬：走日字，依行進方向檢查蹩馬腿。
- 相／象：走田字，檢查塞象眼，且不可過河。
- 仕／士：九宮內斜走一步。
- 將／帥：九宮內直走一步；另外以「將帥照面」判定飛將。
- 兵／卒：向前一步；過河後可左右移動。

合法性過濾流程：

1. 產生虛擬走法
2. 暫時套用走法
3. 檢查己方將帥是否被攻擊（含飛將）
4. 還原盤面
5. 只保留合法走法

### 5.3 攻擊偵測

`isAttacked()` 採反向掃描：

- 車：四方向找第一個棋子
- 炮：跳過第一個炮架後找第二個棋子
- 馬：由目標格反推八個馬位置並檢查馬腿
- 兵／卒：依方向與是否過河判斷
- 仕／士、相／象：補齊圖樣
- 將／帥：相鄰格與同欄飛將
- 將帥照面：同欄且中間無子即視為被攻擊

### 5.4 勝負與和棋

- 將死：對方被將軍且無合法走法 → 勝
- 困斃：對方未被將軍但無合法走法 → 中國象棋規則判負
- 三次重複：以 Zobrist 盤面雜湊計算，同一局面出現 3 次 → 和棋
- 60 回合無吃子：`halfMoveClock >= 120`（120 半回合）→ 和棋
- 認輸、超時、手動結束（`Outcome.ABORTED`）

### 5.5 中式記譜

`Notation` 產生傳統記譜，例如：

- `炮二平五`
- `馬八進七`
- `卒七進一`

處理重點：

- 紅方檔案由右至左為一～九；黑方由自身右側編號
- 同欄同種棋子使用「前／中／後」或數字區分
- 直行子（車、炮、兵、將）縱向記步數；斜行子（馬、相、仕）記目的檔位

### 5.6 Perft 驗證

以初始局面驗證走法產生正確性：

| 深度 | 節點數 |
|---|---|
| 1 | 44 |
| 2 | 1,920 |
| 3 | 79,666 |
| 4 | 3,290,240（未納入例行測試，耗時較長） |

實際單元測試執行深度 1～3，全數符合。

---

## 6. AI 設計

### 6.1 評估函式

子力價值：

| 棋子 | 分值 |
|---|---|
| 車 | 900 |
| 炮 | 450 |
| 馬 | 400 |
| 仕／士 | 200 |
| 相／象 | 200 |
| 兵／卒 | 100（過河另有加成） |
| 將／帥 | 0（不計入） |

加上依棋子種類設計的位置分表（PST），紅黑鏡像：

- 兵卒獎勵前進與過河
- 馬炮獎勵中央與活躍位置
- 車獎勵機動與深入
- 將帥獎勵留在後方安全位置

### 6.2 搜尋

- Negamax + Alpha-Beta
- 迭代加深，時間截止後保留上一層完整結果
- 靜態搜尋（Quiescence）：僅延伸吃子；被將軍時延伸全走法
- 置換表：Zobrist 雜湊、直接映射，大小 `2^19`
- 走法排序：置換表走法 → MVV-LVA 吃子 → Killer → History
- 檢查將軍與將死分數：`MATE = 30000`
- 根節點依難度決定是否全窗搜尋（簡單模式需精確分數以加入隨機）

### 6.3 難度設定

| 難度 | 深度上限 | 時間上限 | 特性 |
|---|---|---|---|
| 簡單 | 2 | 320 ms | 加入 110 分噪聲，從前段走法隨機挑選 |
| 中等 | 4 | 800 ms | 穩定搜尋 |
| 困難 | 8 | 1800 ms | 時間內盡量加深 |

### 6.4 執行方式

- AI 於 `Dispatchers.Default` 執行，不阻塞 UI。
- 使用 `aiCancel` 旗標與協程取消，在新局、悔棋、離開畫面時中止搜尋。
- 提示功能固定使用「中等」深度計算，並將建議走法轉為中式記譜顯示。
- 搜尋引擎每個對局重建，開新局時清除置換表。

---

## 7. 對局流程與狀態管理

`GameViewModel` 是整個 App 的流程核心：

- 持有 `Game`、目前難度、執子方、計時器、AI Job、提示 Job、計時 Ticker。
- 以 `UiState` + `StateFlow` 提供 Compose 畫面狀態。
- 棋盤、選取格、合法目標、上一手、將軍狀態、動畫、提示、結果對話框等皆由 ViewModel 統一管理。

主要流程：

1. `startNewGame()`：重置對局、初始化計時、存檔、必要時觸發 AI 先手。
2. 玩家點擊棋子：計算合法目標並高亮。
3. 玩家落子：套用走法、播放音效、啟動動畫、存檔、輪到 AI。
4. AI 思考：延遲約 300 ms 後於背景搜尋，完成後套用走法。
5. 每步後檢查將死、困斃、重複局面、60 回合無吃子。
6. 結束時儲存為 finished 並顯示結果。
7. 悔棋會退回玩家上一手（必要時連同 AI 應手），並可取消 AI 思考。
8. 離開對局會保存進度；「結束對局」則標記為已結束並清除目前對局。

計時：

- 每 100 ms 更新一次
- 只有輪到的一方扣時
- 套用時間增量（10 分 + 5 秒模式）
- 逾時由 `Game.timeout()` 判定
- 切到背景時暫停，回到前景重新計時

---

## 8. 介面與互動設計

### 8.1 主選單

- 中央「帥」字圓形標誌
- 新局、繼續棋局、棋譜與回放、設定
- 新局對話框可選難度、執紅／執黑、時間控制

### 8.2 對局畫面

- 頂部：返回、對局資訊、思考指示、`more_vert` 選單
- 雙方計時卡：標示「你／電腦」與目前輪到的一方
- 棋盤：自繪木質／淺色／墨玉／青瓷四種風格
- 狀態列：輪到誰、將軍、提示文字
- 走法列表：橫向滾動，自動捲到最新一手
- 底部：悔棋、提示、認輸
- 右上選單：結束對局

### 8.3 棋盤繪製

使用 Compose `Canvas`：

- 圓角木紋背景（線性漸層）
- 9×10 格線、九宮斜線、楚河漢界文字
- 棋子以圓形＋內外框＋書法字呈現
- 紅黑雙方棋字顏色不同
- 上一手、選取、合法落點、被將軍皆以不同標記呈現
- 棋子移動以 `Animatable` 做 230 ms 補間
- 吃子時被吃棋子淡出

### 8.4 提示強化

提示視覺經過一輪強化：

- 提示箭頭改繪於棋子之上，不會被棋子遮住
- 起點與終點加上脈動金色雙圈
- 白色描邊＋粗金色箭頭與箭頭尖端
- 狀態列右側顯示填色標籤「建議：炮二平五」
- 文字副標提示「依亮色箭頭落子」

### 8.5 顯示模式與系統列

- 設定提供「系統／淺色／深色」
- 系統模式使用 `isSystemInDarkTheme()`
- 主題解析移到 `MiniXiangqiApp`，切換時即時重組
- `SystemBarsEffect` 使用 `WindowInsetsControllerCompat`：
  - 淺色主題：狀態列／導覽列圖示為深色
  - 深色主題：圖示為淺色
  - 狀態列與導覽列透明，維持 edge-to-edge
  - API 29+ 關閉導覽列對比強制背景

### 8.6 棋盤風格

| 風格 | 色系 |
|---|---|
| 經典木紋 | 暖木色、深棕線、象牙棋子 |
| 素雅淺色 | 米白底、淺棕線、白色棋子 |
| 墨玉 | 深綠黑底、淺綠線、象牙棋子 |
| 青瓷 | 淺青綠底、深青線、白色棋子 |

### 8.7 結束對局

- 右上 `more_vert` →「結束對局」
- 確認對話框說明會保存棋譜並返回主選單
- 對局結果寫入 `Outcome.ABORTED`（顯示「已結束 · 手動結束」）
- 棋譜保留，可進入回放

---

## 9. 資料與存檔

### 9.1 設定

以 DataStore Preferences 儲存：

- 音效開關
- 動畫開關
- 顯示模式（`SYSTEM` / `LIGHT` / `DARK`）
- 棋盤風格（`WOOD` / `LIGHT` / `INK_JADE` / `CELADON`）
- 預設難度
- 預設執子方
- 預設計時模式

首次安裝或缺少欄位時採用預設值（顯示模式為系統、棋盤為經典木紋、難度中等、執紅、10 分鐘）。

### 9.2 棋譜

`GameStore` 以 JSON 儲存在 `filesDir/games/{id}.json`。

`SavedGame` 欄位：

```text
id, createdAt, updatedAt,
difficulty, humanSide, timeControl,
moves[{from,to}], outcome, reason, finished,
redTimeMs, blackTimeMs
```

行為：

- 每次落子後自動存檔
- 未結束的對局可續玩
- 棋譜列表依更新時間排序
- 棋譜可刪除
- 結束對局與認輸皆會保存為 finished

### 9.3 回放

- 由初始盤面重演 `SavedGame.moves`
- 產生每一步的盤面快照、輪到的一方、中式記譜
- 支援上一步／下一步、跳開局／終局、滑桿、自動播放（900 ms）
- 棋盤唯讀，套用目前設定的棋盤風格
- 顯示該局結果與步數

---

## 10. 音效與圖示資產

### 10.1 音效

`tools/generate_sounds.ps1` 以程式合成 22,050 Hz、16-bit 單聲道 WAV：

- `move.wav`：木質落子聲
- `capture.wav`：較重的吃子撞擊聲
- `check.wav`：兩段提示音
- `win.wav`：上升音階
- `lose.wav`：下降音階

由 `SoundManager` 使用 SoundPool 播放，跟隨音效設定開關。

### 10.2 App 圖示

需求為「圖案中間寫一個帥字」。實作方式：

1. 使用開源 `NotoSerifTC-VF.ttf`（OFL 授權），以 fontTools 將字重設為 700。
2. 取出「帥」字輪廓，轉為 Android Vector `pathData`。
3. 以 108×108 viewport 為基準，將字置中縮放（前景 24 單位、單色圖示 30 單位）。
4. 保留木色圓底與紅色外圈，中央改為象牙色棋子圓＋紅色「帥」字。
5. 同步產生 Android 13 主題單色圖示。
6. 產生器：`tools/generate_icon_glyph.py`。

圖示路徑使用非零填充（nonZero）以正確呈現書法字的筆畫與鏤空。

---

## 11. 開發里程碑

| 階段 | 內容 | 結果 |
|---|---|---|
| M1 | 建立 Gradle 專案、Manifest、主題、資源 | 可成功 `assembleDebug` |
| M2 | 盤面模型、走法產生、攻擊偵測、勝負、記譜 | Perft 1～3 通過 |
| M3 | AI 評估、Alpha-Beta、置換表、難度 | 一步殺測試通過 |
| M4 | Compose 畫面、Canvas 棋盤、動畫、音效 | 可完整人機對弈 |
| M5 | DataStore、JSON 存檔、棋譜列表、回放 | 可續玩與回放 |
| M6 | 單元測試、Lint、模擬器實測 | 測試與 Lint 通過 |
| M7 | 顯示模式、新棋盤、提示強化、結束對局、新圖示 | 功能增補完成並重建 APK |

---

## 12. 問題與修正紀錄

| # | 問題 | 原因 | 修正 |
|---|---|---|---|
| 1 | 記譜顯示成「二炮平五」 | 記譜組裝順序錯誤，數字前綴被放在棋子名前 | 改為「棋子名＋檔案數字」；只有前／中／後才放在前面 |
| 2 | 單元測試語法錯誤 `record!!notation` | Kotlin 空值斷言後少加 `.` | 改為 `record!!.notation` |
| 3 | 一步殺測試局面不正確 | 測試盤面的掩護車位置放錯 | 調整車的位置，使將死局面成立 |
| 4 | Lint 失敗：`BoxWithConstraints` scope 未使用 | 棋盤元件不需要 constraints scope | 改回一般 `Box` |
| 5 | Adaptive Icon 移到 `mipmap-anydpi` 後 AAPT 找不到資源 | adaptive-icon 需要 v26 資源目錄 | 放回 `mipmap-anydpi-v26`，保留 Lint ObsoleteSdkInt 警告 |
| 6 | 短螢幕可能讓棋盤與控制列超出畫面 | 棋盤固定以寬度決定高度 | 以 `BoxWithConstraints`＋`weight(1f)` 依剩餘高度縮放棋盤 |
| 7 | 提示箭頭被棋子遮住、不夠明顯 | 提示圖層順序在棋子之前且線條太細 | 移到棋子後繪製，加粗、白描邊、脈動雙圈與箭頭 |
| 8 | 「結束對局」與「認輸／返回」定義重疊 | 需求未明確定義 | 確認後新增 `ABORTED` 結果，結束並保存、返回主選單 |
| 9 | 圖示筆畫可能因 evenOdd 填色出現破洞 | 字型輪廓應以非零填充呈現 | 移除 `evenOdd`，改用預設 nonZero |
| 10 | 模擬器出現「System UI isn't responding」 | 無視窗模擬器與軟體 GPU 效能較慢 | 屬環境問題，選擇 Wait 後可繼續測試 |
| 11 | APK 圖示需要確認是否真的換成「帥」 | 無法直接目視截圖 | 以 UI dump 取得圖示區域，再用像素取樣確認中央紅色筆畫範圍 |

---

## 13. 測試與驗證紀錄

### 13.1 自動化測試

執行指令：

```powershell
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:lintDebug
.\gradlew.bat :app:assembleDebug
```

`RulesTest` 共 13 個測試：

1. 初始局面 Perft（44 / 1,920 / 79,666）
2. 將帥照面視為被將軍
3. 不可走出飛將局面
4. 蹩馬腿
5. 塞象眼與不可過河
6. 炮需炮架才能吃子
7. 將死局面無合法走法
8. 中式記譜
9. AI 找到一步殺
10. 對局走子與悔棋
11. 三次重複局面判和
12. 手動結束標記 `ABORTED`
13. AI 從初始局面回傳合法走法且時間受控

結果：全數通過。

### 13.2 Lint

- `lintDebug` 通過。
- 保留的非阻斷性警告：
  - targetSdk 35 非最新版
  - 鎖定直向（棋盤類 App 的刻意設計）
  - Adaptive Icon 位於 `-v26` 目錄（AAPT 要求）
  - 依賴版本有更新版本可用

### 13.3 模擬器手動驗證

環境：AVD `MiniReaderTest`，1080×2400，Android 模擬器（無視窗）。

實際操作並確認：

- 主選單載入正常
- 新局設定可選難度、執紅／執黑、時間
- 執紅與執黑都能正常落子；AI 會自動應手
- 悔棋可退回玩家上一手，並取消進行中的 AI 搜尋
- 提示可產生箭頭與「建議：…」文字
- 認輸、結束對局流程正常
- 關閉 App 再開可續玩，計時與盤面保留
- 棋譜列表可進入回放，支援逐步與自動播放
- 設定切換後重啟仍保留
- 顯示模式切換：
  - 深色：背景取樣 `RGB(27,22,17)`
  - 淺色：背景取樣 `RGB(246,235,213)`
  - 系統模式：以 `cmd uimode night yes/no` 切換系統深色，App 隨之變化
  - 狀態列圖示在淺色主題為深色、深色主題為淺色
- 棋盤風格：
  - 墨玉：深綠底與淺綠線
  - 青瓷：淺青綠底與深青線
- `more_vert` 選單可開啟「結束對局」，確認後返回主選單，棋譜顯示「已結束 · 手動結束」
- App 圖示中央有紅色「帥」字筆畫

---

## 14. 建置與安裝

```powershell
# 單元測試
.\gradlew.bat :app:testDebugUnitTest

# 靜態檢查
.\gradlew.bat :app:lintDebug

# 建置 Debug APK
.\gradlew.bat :app:assembleDebug

# 安裝到已連線裝置
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

APK 產出位置：

```text
app/build/outputs/apk/debug/app-debug.apk
```

目前產出資訊：

- 版本：`1.0.0-debug`（versionCode 1）
- 大小：18,856,547 bytes（約 18.0 MiB）
- minSdk：26
- targetSdk：35

---

## 15. 已知限制

- 長將、長捉僅以「三次重複局面判和」簡化處理，未實作完整亞洲規則裁決。
- 60 回合無吃子以 120 半回合計；未細分兵卒推進等其他和棋條件。
- AI 無開局庫、殘局庫與置換表持久化，棋力以行動裝置時間預算為上限。
- 提示固定使用中等搜尋，未依目前難度調整。
- 僅支援直向手機版面，尚未針對平板、橫向或折疊機最佳化。
- 尚無 PGN／FEN 匯入匯出、分享棋譜、線上對戰。
- 尚無 Instrumentation 測試，主要以 JVM 單元測試與模擬器手動驗證。
- 棋譜存檔尚未加入 schema 版本欄位，未來格式變更需額外遷移處理。
- 無障礙（TalkBack）與多語系尚未完整調整。

## 16. 後續可發展方向

1. 完整實作長將、長捉、一將一殺等禁例裁決。
2. 加入開局庫與殘局知識，提升 AI 棋力。
3. 棋譜 PGN／FEN 匯入匯出與分享。
4. 橫向／平板版面與自適應棋盤。
5. 增加倒數計時、每步限時、加秒等更多時間制度。
6. 無障礙語意與多語系（英文、簡體中文）。
7. 建立 CI：自動跑測試、Lint、組裝 APK。
8. 加入 Instrumentation／Compose UI 測試與截圖比對。
9. 正式簽署 Release APK／AAB 並上架流程。

---

## 17. 版本紀錄

### 1.0.0（2026-10-29）

- 完成人機對弈、完整棋規、AI 三難度
- 悔棋、提示、計時、認輸
- 棋譜自動存檔、續玩、列表、回放
- 音效與棋子動畫
- 顯示模式（系統／淺色／深色）與系統列同步
- 四種棋盤風格
- 明顯化提示箭頭與文字建議
- 右上 more_vert 選單與結束對局
- 「帥」字 App 圖示與單色主題圖示

---

## 18. 實作順序回顧

1. 確認產品方向：原生 Android、自製 AI、標準規則、指定功能清單。
2. 檢查本機工具鏈（Android SDK 35/36、JDK、Gradle 快取），沿用可建置的 AGP 8.9.2／Kotlin 2.1.20／Gradle 8.11.1 組合。
3. 建立專案骨架：Manifest、主題、字串、顏色、Adaptive Icon、Gradle Wrapper。
4. 實作盤面模型、初始局面、走法產生、攻擊偵測與合法性過濾。
5. 實作勝負判定、重複局面、悔棋快照與中式記譜。
6. 以 Perft 與規則單元測試驗證引擎，修正記譜順序與測試語法問題。
7. 實作 AI 評估、Alpha-Beta、置換表、走法排序、靜態搜尋與難度參數。
8. 加入 AI 一步殺與初始局面合法性測試。
9. 建立 Compose 畫面：主選單、新局設定、對局畫面與 Canvas 棋盤。
10. 加入棋子動畫、音效、計時器、提示與結果對話框。
11. 實作 DataStore 設定、JSON 棋譜、續玩、棋譜列表與回放。
12. 以程式合成音效 WAV、完成第一版 APK，於模擬器走查主要流程。
13. 依回饋增補顯示模式、系統列同步、墨玉／青瓷棋盤、提示強化、結束對局選單。
14. 以 fontTools 從 Noto Serif TC 產生「帥」字向量圖示。
15. 執行單元測試、Lint、模擬器回歸測試，重建 `app-debug.apk`。

