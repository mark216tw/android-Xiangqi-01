# mini象棋 系統設計文件

本文件說明畫面流程、對局狀態機、主要操作序列、資料模型、並行設計與設計取捨。

## 1. 設計目標

- 完全離線，不需要網路權限
- 可在 Android 手機上完整下一局中國象棋
- 內建可調整難度的 AI
- 對局自動存檔，離開後可繼續
- 棋譜可回放與管理
- UI 以 Compose 宣告式設計，狀態單一來源

## 2. 畫面與導覽

```text
MENU
├── 新局設定對話框 → GAME
├── 繼續棋局 → GAME
├── 棋譜與回放 → RECORDS
│                   └── 回放 → REPLAY → RECORDS
└── 設定 → SETTINGS
```

- `AppScreen` 為畫面列舉。
- 返回鍵由 `BackHandler` 統一處理。
- 所有畫面共用同一個 `GameViewModel`。

## 3. 對局狀態機

### 3.1 高層狀態

```text
IDLE（主選單）
  └── startNewGame / continue
        ↓
PLAYING（輪到玩家或 AI）
  ├── 玩家落子 → AI_THINKING
  ├── 悔棋 → PLAYING
  ├── 提示 → PLAYING
  └── 認輸 / 超時 / 將死 / 困斃 → FINISHED
        ↓
FINISHED（結果對話框）
  ├── 再來一局 → PLAYING
  ├── 查看棋盤 → FINISHED
  └── 回主選單 → IDLE
```

### 3.2 對局內部狀態

`Game` 持有：

- `board`
- `sideToMove`
- `result`
- `halfMoveClock`
- `fullMoveNumber`
- `moves`
- `snapshots`
- `positionCounts`

## 4. 主要操作序列

### 4.1 新局

```text
使用者：新局 → 選擇難度／執子／時間 → 開始
MenuScreen → GameViewModel.startNewGame()
  → 取消既有 Job
  → 建立 Game
  → 重設計時與 UI 狀態
  → saveGame()
  → startTicker()
  → 若玩家執黑，延遲觸發 AI
```

### 4.2 玩家落子

```text
點擊棋子
  → onSquareTap()
  → 選取並計算合法落點
  → 點擊落點
  → Game.play()
  → 播放音效、建立 MoveAnimation
  → 加上時間增量
  → refresh()
  → saveGame()
  → 若對局結束：handleGameOver()
  → 否則：triggerAi()
```

### 4.3 AI 回合

```text
triggerAi()
  → thinking = true
  → 延遲約 280–550ms
  → withContext(Dispatchers.Default)
  → searchMutex.withLock { SearchEngine.search() }
  → 套用最佳走法
  → 播放音效、建立動畫
  → saveGame()
  → 若對局結束：handleGameOver()
```

AI 與提示共用同一個 `SearchEngine`，以 `searchMutex` 保證同一時間只有一個搜尋。

### 4.4 悔棋

```text
undo()
  → 若對局已結束則直接返回
  → 取消 AI／提示
  → 清除結果對話框
  → 依序退回玩家上一手與 AI 應手
  → refresh()
  → saveGame()
  → 必要時重新啟動計時或觸發 AI
```

### 4.5 結束對局

```text
認輸 → Game.resign()
超時 → Game.timeout()
手動結束 → Game.abort()

handleGameOver()
  → 停止 AI／提示／計時
  → 顯示結果對話框
  → 播放勝利／失敗音效
  → saveGame()
  → refresh()
```

### 4.6 存檔與續玩

```text
saveGame()
  → 建立 SavedGame 快照
  → viewModelScope.launch
  → saveMutex.withLock { GameStore.save() }
  → 成功後 upsert 到 UiState.games

continueGame()
  → 若有進行中的記憶體對局：reopenGame()
  → 否則讀取最新未完成棋譜
  → 重演 moves
  → 還原計時與設定
```

### 4.7 回放

```text
openReplay(saved)
  → 由初始盤面重演所有 moves
  → 產生每一步盤面、輪走方與記譜
  → 建立 ReplayUi
  → 顯示 ReplayScreen
```

回放操作：

- `replayStep(delta)`：前後一步
- `replayTo(index)`：跳到指定步
- `toggleReplayPlay()`：自動播放／暫停
- 點擊走法 chip：呼叫 `replayTo(index + 1)`

### 4.8 刪除棋譜

- `deleteSaved(id)`：刪除單一棋譜。
- `deleteFinishedSaved()`：讀取全部棋譜，只刪除 `finished == true` 的檔案。
- 刪除後呼叫 `refreshGamesInternal()` 更新列表與續玩狀態。
- 刪除動作與存檔共用 `saveMutex`，避免競態。

## 5. 資料模型

### 5.1 SavedGame

```kotlin
@Serializable
data class SavedGame(
    val schemaVersion: Int = 1,
    val id: String,
    val createdAt: Long,
    val updatedAt: Long,
    val difficulty: String,
    val humanSide: String,
    val timeControl: String,
    val moves: List<SavedMove>,
    val outcome: String,
    val reason: String,
    val finished: Boolean,
    val redTimeMs: Long = 0L,
    val blackTimeMs: Long = 0L
)
```

### 5.2 SavedMove

```kotlin
@Serializable
data class SavedMove(val from: Int, val to: Int)
```

### 5.3 UiState

`UiState` 包含：

- 畫面與導覽
- 對局盤面與走法
- 計時與難度
- 設定與棋譜列表
- 回放狀態
- 動畫與結果對話框
- AI 落子標記

### 5.4 其他模型

- `MoveRecord`：走法、走子方、棋子、被吃棋子、記譜、是否將軍
- `GameResult`：結果與原因
- `ReplayUi`：回放盤面快照、輪走方、記譜、目前索引、播放狀態
- `MoveAnimation`：移動動畫資訊

## 6. 並行與生命週期

### 6.1 協程

- `viewModelScope`：UI 流程、存檔、回放播放
- `Dispatchers.Default`：AI 搜尋
- `Dispatchers.IO`：DataStore 與棋譜檔案存取

### 6.2 互斥鎖

- `searchMutex`：保護共用 `SearchEngine`
- `saveMutex`：序列化存檔、刪除與列表讀取

### 6.3 計時器

- 每 250ms 更新一次內部時間
- 只有顯示秒數變化時才更新 UI
- 背景時取消 ticker，回前景重新啟動
- 超時由 `Game.timeout()` 判定

### 6.4 螢幕喚醒

對局與回放畫面設定 `keepScreenOn = true`，離開畫面時關閉。

## 7. UI 設計決策

### 7.1 棋盤翻轉

玩家執黑時，棋盤以座標映射翻轉 180 度，棋子文字保持正向。

### 7.2 AI 落子提示

AI 移動後：

- 終點棋子外加藍／青色外圈
- 終點加淡色光暈
- 起點加小型空心標記
- 持續到玩家下一次落子

顏色依棋盤風格調整，避免與金色提示箭頭和綠色合法落點混淆。

### 7.3 被吃棋子與子力差

由目前盤面缺少的棋子推算被吃棋子，依子力價值顯示差距。棋子過多時可橫向捲動。

### 7.4 低時間警示

- 30 秒內：時間數字變紅
- 10 秒內：卡片轉紅底並加紅框

### 7.5 主選單自適應

依螢幕高度切換較小間距與可捲動版面，避免小螢幕內容溢出。

## 8. 錯誤處理

- JSON 讀取使用 `runCatching`，損毀檔案不影響其他棋譜。
- 列舉值解析失敗時使用預設值。
- 存檔先寫入暫存檔再 rename。
- AI 搜尋每 1024 節點檢查取消與時間。
- 對局結束後禁止悔棋，避免復活已認輸／超時的對局。

## 9. 設計取捨

| 決策 | 原因 |
|---|---|
| 單 module、單 Activity | App 規模小，降低建置與導覽複雜度 |
| 自製 AI | 完全離線、可控難度與行為 |
| JSON 檔而非資料庫 | 棋譜數量不大，易於備份與除錯 |
| 三次重複判和 | 簡化長將／長捉裁決 |
| 棋盤 Canvas 自繪 | 可精細控制棋子、動畫、提示與主題 |
| Debug 金鑰簽署 prerelease | 測試發行方便，不需正式簽章 |

## 10. 後續可發展方向

- 完整長將／長捉裁決
- 開局庫與殘局知識
- PGN／FEN 匯入匯出與分享
- 橫向／平板版面
- 無障礙逐格操作
- 多語系
- 正式 Release 簽章與上架流程
