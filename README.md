# ParkNav - 地下停車場室內導航

Phase 1 驗證版：離線室內導航 POC，使用 ARCore VIO + IMU PDR 融合定位。

## 技術棧
- **語言**: Kotlin
- **UI**: Jetpack Compose + Material 3
- **定位**: ARCore VIO + PDR (Pedestrian Dead Reckoning)
- **融合**: 簡易 Kalman Filter (ARCore + PDR)
- **最低 SDK**: API 24 (Android 7.0)

## Phase 1 Checklist

- [x] ARCore VIO 追蹤 — 即時相對位置 (x, y, z)
- [x] IMU 感測器讀取 — 加速計、陀螺儀、磁力計
- [x] PDR 步伐偵測 — Weinberg 模型 + 峰值偵測
- [x] 方向估算 — 磁力計 + 陀螺儀互補濾波
- [x] Kalman Filter 融合 — ARCore + PDR 位置融合
- [x] 停車標記 — 一鍵記錄車位
- [x] 軌跡記錄 — 即時記錄行走路徑
- [x] 2D 軌跡圖 — Canvas 俯瞰圖即時繪製
- [x] 回溯導航 — 反向路徑 + 方向箭頭
- [x] 數據匯出 — CSV 格式（軌跡 + 感測器）
- [x] 感測器面板 — 可展開/收合的即時數值顯示
- [ ] AR 箭頭疊加（Phase 2）
- [ ] 地圖匹配（Phase 2）

## 專案結構
```
app/src/main/java/com/parknav/
├── MainActivity.kt          # 主 Activity + 融合邏輯
├── ui/
│   ├── MainScreen.kt        # 主畫面 Compose
│   ├── NavigationScreen.kt  # 導航方向指引
│   ├── SensorPanel.kt       # 感測器面板
│   └── TrajectoryView.kt    # 2D 軌跡 Canvas
├── sensor/
│   ├── SensorManager.kt     # IMU 感測器收集
│   ├── StepDetector.kt      # 步伐偵測 (Weinberg)
│   └── OrientationEstimator.kt # 方向互補濾波
├── navigation/
│   ├── PDREngine.kt         # PDR 引擎
│   ├── KalmanFilter.kt      # 簡易 Kalman Filter
│   └── PathRecorder.kt      # 路徑記錄器
├── ar/
│   └── ARTracker.kt         # ARCore VIO 封裝
└── data/
    ├── DataExporter.kt      # CSV 匯出
    └── ParkingSpot.kt       # 資料模型
```

## 使用方式
1. 用 Android Studio 開啟專案
2. 確認已安裝 ARCore (Google Play Services for AR)
3. Build & Run 到支援 ARCore 的裝置
4. 停好車 → 標記車位 → 開始記錄 → 走到目的地 → 停止記錄
5. 回程：導航回車 → 跟著箭頭走

## 演算法說明

### PDR (Pedestrian Dead Reckoning)
- **步伐偵測**: 加速計合成向量峰值偵測，窗口 30 samples
- **步長**: Weinberg 模型 `L = K × (a_max - a_min)^0.25`, K=0.5
- **方向**: 互補濾波 `θ = 0.98 × θ_gyro + 0.02 × θ_mag`

### 融合策略
- ARCore 追蹤正常時：Kalman Filter 融合 ARCore + PDR
- ARCore 丟失時：自動退回純 PDR
