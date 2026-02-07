# 🅿️ ParkNav — 地下停車場 AR 室內導航

**離線優先的 Android 停車場導航 APP**
無需 GPS、無需網路、無需額外硬體，純靠手機感測器實現室內定位與 AR 導引。

## 🎯 目標

解決地下停車場「位置飄移」問題，讓使用者能：
1. **記錄停車位置**（停好車後標記）
2. **AR 導航回車位**（購物完回來找車）
3. **離線運作**（地下室無 GPS/網路也能用）

## 🏗️ 技術架構

### 核心定位引擎（三層融合）

```
┌─────────────────────────────┐
│     AR 顯示層 (ARCore)       │  ← 螢幕疊加導航箭頭
├─────────────────────────────┤
│   感測器融合層 (Kalman Filter) │  ← 多源數據融合
├──────┬──────┬───────────────┤
│ VIO  │ PDR  │ 磁場指紋校正    │  ← 三大定位來源
└──────┴──────┴───────────────┘
```

| 層級 | 技術 | 功能 | 精度 |
|------|------|------|------|
| **主定位** | ARCore VIO | 視覺慣性里程計，追蹤相對位移 | ~公分級 |
| **備援** | PDR (加速計+陀螺儀) | 行人航位推算，步數+方向 | 步級 |
| **校正** | 磁場指紋比對 | 利用鋼筋結構磁場特徵修正漂移 | 2-5m |

### 為什麼這個組合？

- **ARCore VIO** 精度最高但會累計漂移 → 用磁場指紋定期校正
- **PDR** 在 ARCore 失效時（光線不足）當備援
- **磁場指紋** 不需要任何外部設備，停車場鋼筋結構天然產生獨特磁場

## 📱 功能規劃

### Phase 1: 感測器數據採集與驗證
- [ ] ARCore VIO 追蹤（位置 + 姿態）
- [ ] IMU 感測器讀取（加速計、陀螺儀、磁力計）
- [ ] PDR 步伐偵測與方向估算
- [ ] 數據記錄與視覺化（驗證精度用）

### Phase 2: 停車標記與找車導航
- [ ] 一鍵標記停車位置
- [ ] 路徑記錄（從車走到出口）
- [ ] 反向路徑導航（從入口走回車位）
- [ ] AR 箭頭導引顯示

### Phase 3: 磁場指紋校正
- [ ] 磁場數據採集工具
- [ ] 磁場地圖建立
- [ ] 即時磁場比對校正
- [ ] Kalman Filter 多源融合

## 🔧 開源專案參考

### AR 室內導航
| 專案 | 技術 | 可取之處 | 限制 |
|------|------|----------|------|
| [rcj9719/ar-indoor-navigation](https://github.com/rcj9719/ar-indoor-navigation) | ARCore + Sceneform + 感測器 | **完整的 AR 箭頭導航 + 離線設計** | 僅限特定空間 |
| [hkuchynski/Indoor-Navigation-ARCore](https://github.com/hkuchynski/Indoor-Navigation-ARCore) | ARCore | AR 室內導航基礎框架 | 文檔少 |
| [ashishgopalhattimare/PathMarkAR](https://github.com/ashishgopalhattimare/PathMarkAR) | Unity + ARCore | **路徑標記概念** — 走過的路留下 AR 標記 | Unity 架構較重 |

### 感測器融合 / Kalman Filter
| 專案 | 技術 | 可取之處 | 限制 |
|------|------|----------|------|
| [maddevsio/mad-location-manager](https://github.com/maddevsio/mad-location-manager) | GPS + 加速計 + Kalman | **成熟的 Kalman Filter 融合庫（Java AAR）** | 依賴 GPS（需改造） |
| [iamjaspreetsingh/GPS-IMU-android](https://github.com/iamjaspreetsingh/GPS-IMU-android) | GPS + IMU + Kalman | 100Hz 高頻感測器融合 | 同上 |

### PDR 行人航位推算
| 專案 | 技術 | 可取之處 | 限制 |
|------|------|----------|------|
| [Aymdi/DeadReckoning](https://github.com/Aymdi/DeadReckoning) | 加速計 + 磁力計 + 陀螺儀 | **Continental 實習成果，含步長估算演算法** | Python（需移植） |
| [wuzhiguocarter/OpenPDR](https://github.com/wuzhiguocarter/OpenPDR) | 多種濾波器 | **完整 PDR 框架：KF/EKF/UKF/PF 都有** | 學術導向 |
| [aureliencousin/PDR](https://github.com/aureliencousin/PDR) | Android PDR | 原生 Android 實作 | 功能基礎 |
| [nisargnp/DeadReckoning](https://github.com/nisargnp/DeadReckoning) | Android 即時定位 | 即時手機感測器定位 | 精度有限 |

### 磁場定位（學術參考）
| 來源 | 內容 | 可取之處 |
|------|------|----------|
| [PMC6308508](https://pmc.ncbi.nlm.nih.gov/articles/PMC6308508/) | PDR + 磁場指紋匹配論文 | **核心演算法：ICCP 磁場軌跡匹配** |
| [MDPI Sensors 2023](https://www.mdpi.com/1424-8220/23/23/9348) | 深度學習磁場校正 | CNN 抗磁場干擾方法 |

## 🧬 技術融合策略

從各專案取精華：

```
mad-location-manager  → Kalman Filter 融合引擎（改造：GPS 輸入替換為磁場校正）
ar-indoor-navigation  → ARCore AR 箭頭渲染
Aymdi/DeadReckoning   → PDR 步伐偵測 + 步長估算演算法
PMC6308508 論文        → 磁場指紋 ICCP 匹配方法
PathMarkAR            → 路徑標記 UX 概念
```

## 📋 環境需求

- Android 7.0+ (API 24+)
- ARCore 支援的裝置
- Android Studio + Kotlin
- 無需網路、無需 GPS

## 📄 License

MIT

## 🤝 Contributing

歡迎 PR！請先開 Issue 討論。
