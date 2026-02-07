# 🧪 測試指南

## 自動測試

### 執行方式

用 Android Studio 或命令列執行：

```bash
# 全部測試
./gradlew test

# 單一模組
./gradlew :app:testDebugUnitTest

# 單一測試類別
./gradlew :app:testDebugUnitTest --tests "com.parknav.navigation.KalmanFilterTest"
```

### 測試清單

| 測試檔案 | 測試項目 | 數量 |
|----------|----------|------|
| `KalmanFilterTest` | 初始化、收斂、濾波平滑、重置、Q/R 參數行為 | 5 |
| `StepDetectorTest` | 靜止不觸發、行走波形觸發、最短間隔、Weinberg 步長、重置 | 5 |
| `PDREngineTest` | 初始位置、重置、位置設定、accessor、向北行走 | 5 |
| `PathRecorderTest` | 未記錄時忽略、記錄中新增、200ms 節流、反轉路徑、停止、清空 | 6 |
| `PositionTest` | 3D 距離、全軸距離、2D 距離、同點距離為零 | 4 |
| **合計** | | **25** |

### 未覆蓋

| 模組 | 原因 |
|------|------|
| `OrientationEstimator` | 依賴 `android.hardware.SensorManager` 靜態方法，純 JVM 無法測試。未來可用 Robolectric 或抽象化介面 |
| `ARTracker` | 依賴 ARCore Session，需實機或 instrumented test |
| `DataExporter` | 依賴 Android 檔案系統，需 instrumented test |
| UI 相關 | Compose UI 測試需另外設置 |

### 測試檔案位置

```
app/src/test/java/com/parknav/
├── data/
│   └── PositionTest.kt
├── navigation/
│   ├── KalmanFilterTest.kt
│   ├── PDREngineTest.kt
│   └── PathRecorderTest.kt
└── sensor/
    └── StepDetectorTest.kt
```
