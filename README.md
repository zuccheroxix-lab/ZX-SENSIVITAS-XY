# ZX X/Y Sensitivity Optimizer

**ZX X/Y Sensitivity Optimizer** adalah aplikasi Android tingkat lanjut untuk kalibrasi sensitivitas sentuhan sumbu X dan Y, kalibrasi kecepatan penunjuk sistem (*System Pointer Speed*), overlay *tactical crosshair*, dan pemantau performa perangkat keras (*hardware telemetry*) yang 100% nyata tanpa trik visual atau fitur fiktif (*no dummy, no root*).

---

## Fitur Utama

### 1. X/Y Sensitivity Engine
- **Independent X & Y Sensitivity**: Pengaturan sensitivitas sumbu horizontal (X) dan vertikal (Y) secara presisi.
- **Dynamic X/Y Ratio**: Menghitung rasio tarikan drag secara otomatis untuk kestabilan bidikan (*aim*).
- **Drag Smoothness & Aim Stability**: Kalibrasi input gestur sentuh.
- **Android System Pointer Speed**: Mengubah `Settings.System.POINTER_SPEED` (-7 hingga +7) secara langsung melalui izin sistem / Shizuku dengan verifikasi ganda.
- **Hardware Touch Calibration Pad**: Kanvas uji coba interaktif yang mengukur *sampling rate* layar sentuh aktual (Hz) dan variansi *jitter* gestur secara *real-time* langsung dari digitizer perangkat.

### 2. Shizuku Privileged Engine (Non-Root)
- Eksekusi perintah sistem level ADB tanpa membutuhkan akses root (`dev.rikka.shizuku:api`).
- Pengecekan status real-time: `CONNECTED`, `PERMISSION_REQUIRED`, `SERVICE_NOT_RUNNING`, atau `UNSUPPORTED`.
- Diagnosis versi API Shizuku dan UID proses (UID 2000 ADB shell).
- Panduan terintegrasi untuk aktivasi *Wireless Debugging* di Android 11+ dan kabel ADB di Android 8-10.

### 3. Floating Tactical Crosshair
- Menggunakan `WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY` untuk rendering crosshair mengambang di atas game.
- 7 gaya reticle: *Classic Cross*, *Center Dot*, *Circle & Dot*, *Tactical T*, *Gap Cross*, *Box Cross*, dan *Custom Image* (menggunakan Android Photo Picker yang aman dan sesuai kebijakan privasi Play Store).
- Kustomisasi ukuran (*size*), ketebalan garis (*thickness*), jarak reticle (*gap*), transparansi (*opacity*), dan warna taktikal (*Cyan, Emerald, Crimson, Amber, White, Violet*).
- Posisi koordinat offset X/Y dengan tombol reset dan kunci posisi (*lock position*).
- Sistem penyimpanan preset lokal dengan Room Database.

### 4. Game Compatibility & Profiles
- Profil khusus untuk **Free Fire (Standard)**, **Free Fire MAX**, **PUBG Mobile**, **Call of Duty: Mobile**, dan **Mobile Legends**.
- Deteksi otomatis apakah game terinstal di perangkat (`isInstalled`).
- Tombol satu ketukan untuk *Apply Profile to System* dan *Launch Game*.
- Kemampuan menambah profil game kustom.
- **Pemberitahuan Kepatuhan**: Aplikasi tidak menyuntikkan kode memori (*zero memory injection*) atau memodifikasi file APK game. Semua pengaturan murni bekerja pada level subsistem input layar dan overlay Android.

### 5. Genuine Hardware Telemetry & Optimizer
- **Baterai**: Persentase, status pengisian, sumber daya (AC, USB, Wireless), suhu baterai (°C) dari `BatteryManager.EXTRA_TEMPERATURE`, voltase (mV), dan kesehatan baterai.
- **Thermal Headroom**: Status suhu dan peringatan *thermal throttling* menggunakan Android PowerManager API.
- **RAM & Storage**: Penggunaan RAM riil dari `ActivityManager.MemoryInfo` dan penyimpanan dari `StatFs`.
- **Display**: Resolusi fisik layar, *refresh rate* aktual, serta daftar seluruh *supported refresh rates* perangkat.
- **CPU & SoC**: Jumlah *core* CPU aktif, arsitektur, dan model prosesor.
- **Performance Modes**: Mode *Balanced*, *Extreme Performance* (memaksimalkan kecepatan respons penunjuk dan *refresh rate*), serta *Battery Saver*.

---

## Persyaratan Sistem

- **Sistem Operasi**: Android 7.0 (API 24) hingga Android 15 (API 35+)
- **Root**: **TIDAK MEMBUTUHKAN ROOT**
- **Shizuku (Opsional tapi Direkomendasikan)**: Untuk modifikasi display density (DPI) dan *Game Mode* tanpa root.

---

## Panduan Perizinan (Permissions)

1. **Draw Over Other Apps (`SYSTEM_ALERT_WINDOW`)**:
   Dibutuhkan agar overlay Crosshair dapat muncul di atas game.
2. **Foreground Service (`FOREGROUND_SERVICE` & `FOREGROUND_SERVICE_SPECIAL_USE`)**:
   Memastikan layanan overlay crosshair tetap stabil saat pengguna memainkan game.
3. **Modify System Settings (`WRITE_SETTINGS`)**:
   Digunakan untuk mengubah `pointer_speed` perangkat jika Shizuku tidak tersedia.
4. **Shizuku Authorization**:
   Diberikan satu kali di dalam antarmuka Shizuku untuk eksekusi perintah shell non-root.

---

## Instruksi Build & Kompilasi

Proyek ini dibangun menggunakan Gradle (Kotlin DSL):

```bash
# Debug APK Build
gradle assembleDebug

# Release APK Build
gradle assembleRelease

# Unit Test
gradle test
```

File APK hasil kompilasi akan berada di:
- Debug: `app/build/outputs/apk/debug/app-debug.apk`
- Release: `app/build/outputs/apk/release/app-release.apk`

---

## Lisensi
Hak Cipta (c) 2026. Didistribusikan di bawah lisensi MIT.
