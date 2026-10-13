# Junba 智慧提醒 Android v0.1

Android 8+ 原生 Java 專案（測試目標：vivo V2413 / Android 16）。作者：峻爸。

## 功能
- 每次啟動或返回前景時重新查詢授權日曆、系統下一次鬧鐘與 App 記事排程
- 完整日曆帳號名稱 / 日曆篩選；唯讀展示 60 天內活動、提醒提前分鐘及 RRULE
- 新增、刪除記事及指定日期、時間、提前提醒；每日或每週重複
- Android 精確鬧鐘權限入口、通知權限與測試提醒
- 原廠時鐘開啟、公開 ACTION_SET_ALARM 建立請求（需使用者確認）
- Gemini API 金鑰臨時輸入與真實 HTTP 連線測試；不儲存金鑰

## 不支援 / 系統限制
- 無法取得或批次修改 vivo 所有原廠鬧鐘，亦無原廠稍後提醒次數
- 不保證裝置待機、省電管理情境的實際響鈴；需真機測試
- 不讀取語音助理歷史；僅讀取同步至手機的日曆
- v0.1 未加入自動把日曆活動轉成原廠鬧鐘，以防重複建立
- App 自有提醒使用高重要性通知，不是 vivo 原廠時鐘鈴聲

## 編譯
將此專案推到 GitHub，Actions → Build Android APK → Run workflow，完成後於 Artifacts 下載 `JunbaSmartReminder-debug-apk`。或安裝 Android Studio (JDK 17, Android SDK 35)，執行 `gradle assembleDebug`。Debug APK 僅供測試，發行版本需正式簽署。

## 驗收注意
允許「日曆／通知／精確鬧鐘」相關權限，並於 vivo 電池管理允許 App 後台運作。請用 1 分鐘測試提醒確認實際通知／聲音。原廠時鐘的新增與內容必須回到 vivo 時鐘人工核對。

## R2 GitHub Actions build fix (2026-10-13)

- Removed `android-actions/setup-android@v3`, which failed during old `sdkmanager tools` installation on GitHub hosted runner.
- Uses `ubuntu-24.04` with preinstalled Android SDK command-line tools; installs Android 35 and build-tools 35.0.0 explicitly.
- Uses Java 17, Gradle 8.10.2, and verifies APK existence before uploading artifact.
- Artifact name: `JunbaSmartReminder-v0.1-R2-debug-APK`.
- This package includes **source code and workflow**, not a prebuilt APK. Build and install have not yet been verified on a device.

### Upload correctly
Unzip this archive. Upload **the contents of the `JunbaSmartReminder/` directory** to the repository root (not a nested `JunbaSmartReminder/` directory). Ensure `.github/workflows/build-apk.yml` is committed. Delete the old conflicting workflow if needed; under Actions run `Build Android APK` → `Run workflow`. After success, download artifact ZIP and install `app-debug.apk`. A debug APK is for testing only.
