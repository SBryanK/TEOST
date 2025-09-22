# TEO SecTest 🛡️

A modern Android application for testing and validating the security of Tencent EdgeOne CDN infrastructure.

## 🚀 Current Implementation Status

### ✅ Completed Features (Phases 1-7)

- **Complete project architecture** with MVVM + Clean Architecture pattern
- **Authentication system** with Firebase (Login, SignUp, Forgot Password)
- **Custom Tencent theme** with TencentBlue branding (#4A7BFF), Open Sans typography (Google Fonts provider; fallback Noto Sans SC)
- **Navigation architecture** with 4-tab bottom navigation and nested graphs
- **Connection testing** with multi-target batch support
- **Room database** with comprehensive migration strategy (v1→6)
- **DataStore** for preferences and session management
- **14 Security Test Types** exposed in UI (19 model-defined, 16 engine functions)
- **WorkManager** for background test execution with progress tracking
- **Firebase integration** (Auth, Firestore, Storage, Analytics, Crashlytics)
- **Internationalization** (English + Simplified Chinese)
- **Security test engine** with real malicious traffic generation
- **Credit system** with real-time validation and consumption
- **Test history** with filtering, pagination, and detailed metrics

### 🎨 UI Features

- **Splash Screen** with animation
- **Login Screen** with full validation
- **Main Screen** with curved bottom navigation
- **Search/Connection Test Screen** with expandable results
- **@Preview support** for all screens
- **Light theme only** (per product requirement)
- **Material 3** design system

### 📱 Screens Implemented

1. ✅ Splash Screen (with logo animation)
2. ✅ Login Screen (with email/password validation)
3. ✅ Main Screen (with bottom navigation)
4. ✅ Search Screen (connection testing)
5. ✅ History (list, filters, detail metrics)
6. ✅ Test Wizard (Category → Type → Configure → Cart → Confirm → Execution)
7. ✅ Test Execution (WorkManager observer screen)
8. ✅ Profile (credits, favorites)

## 🛠️ Setup Instructions

### 1. Firebase Setup

1. Create a Firebase project at [console.firebase.google.com](https://console.firebase.google.com)
2. Add an Android app with package name: `com.tencent.edgeone.securitytest`
3. Download `google-services.json` and replace the placeholder file in `/app/`
4. Enable Firebase Authentication (Email/Password)
5. Enable Firebase Firestore
6. Enable Firebase Storage

### 2. Add Required Assets

Place these images in `app/src/main/res/drawable/`:

- `teoc.png` - Main logo for splash screen
- `teoc_logo.png` - Logo for headers

### 3. Build the Project

```bash
# Windows
.\gradlew.bat assembleDebug

# Mac/Linux
./gradlew assembleDebug
```

## 🏗️ Architecture

### Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose
- **Architecture**: MVVM with Repository pattern
- **DI**: Hilt
- **Database**: Room
- **Network**: Retrofit + OkHttp
- **Async**: Coroutines + Flow
- **Backend**: Firebase (Auth, Firestore, Storage)

### Package Structure

```
com.example.teost/
├── data/
│   ├── local/         # Room database, DAOs, DataStore
│   ├── model/         # Data models
│   ├── remote/        # API services
│   └── repository/    # Repository implementations
├── di/                # Dependency injection modules
├── domain/            # Use cases (to be added)
├── presentation/
│   ├── navigation/    # Navigation setup
│   ├── screens/       # All UI screens
│   └── components/    # Reusable UI components
├── services/          # Background services
├── ui/
│   └── theme/        # Colors, typography, shapes
└── util/             # Utility classes
```

## 🎯 Key Features Ready to Use

### Connection Testing ✅

- Multi-target testing (comma/semicolon/pipe/newline separated)
- DNS resolution with IP display
- Response time measurement
- Header inspection with expand/collapse
- Status code color coding
- Copy to clipboard functionality

### Security Test Engine ✅

**14 Test Types Exposed in UI** (19 model-defined, 16 engine functions):

- **DoS Protection (6 types)**: HttpSpike, ConnectionFlood, TcpPortReachability, UdpReachability, IpRegionBlocking, BasicConnectivity
- **Web Protection/WAF (10 types)**: SqlInjection, XssTest, ReflectedXss, PathTraversal, CommandInjection, Log4ShellProbe, CustomRulesValidation, EdgeRateLimiting, LongQuery, OversizedBody
- **Bot Management (3 types)**: UserAgentAnomaly, CookieJsChallenge, WebCrawlerSimulation
- **API Protection (5 types)**: AuthenticationTest, BruteForce, EnumerationIdor, SchemaInputValidation, BusinessLogicAbuse

### Data Persistence ✅

- Test results saved to Room database
- Domain favorites management
- Session management with DataStore
- Cart system for test queue

### Multi-Select Targets in Configure ✅

- Dropdown multi-select di layar Configure akan otomatis menampilkan domain dari hasil Search terakhir yang berstatus HTTP 2xx.
- Aksi tersedia: Select All dan Clear. Jika tidak ada daftar atau ingin input manual, gunakan kolom "Single target (fallback)".
- Saat menekan "Add to Cart", aplikasi membuat satu konfigurasi per domain yang dipilih (fan-out), sehingga mudah melakukan batch testing.
- Seleksi target dipertahankan melalui state ViewModel (`SavedStateHandle`), sehingga retest domain sebelumnya dapat dilakukan tanpa re-input.

## 📝 Next Development Steps

### Phase 5: Test Configuration System (Complete)

- [x] Implement 3-step wizard UI
- [x] Create test configuration forms (DoS/WAF/Bot/API + extras)
- [x] Build cart management UI
- [x] Add credit calculation logic (1 credit per test)

### Phase 6: Test Execution Engine (Complete)

- [x] Integrate WorkManager with HiltWorker for long-running tests
- [x] Foreground notifications during execution
- [x] Credits consumption before queuing
- [x] Security test engine (14 UI-exposed test types with real network execution)
- [x] Background execution via WorkManager (with connected-network constraint)
- [x] Real-time progress tracking (observer screen)
- [x] Result persistence (Room) and details view
- [x] Client-side encryption (AES-256-GCM) for sensitive data

### Phase 7: Performance, Accessibility, Offline-lite (Complete)

- [x] History list with Paging 3 + filters (category/status/type)
- [x] Detail metrics with params snapshot
- [x] Accessibility passes (content descriptions, focus order where applicable)
- [x] Offline-lite queue: work enqueued with NetworkType.CONNECTED constraint
- [x] Performance polish across screens

### Phase 8: Observability & Privacy (Skipped per product decision)

- Skipped for now; can be enabled later (non-PII analytics, breadcrumbs, redaction checks)

### Phase 9: Polish & Documentation (Complete)

- [x] Replace deprecated Divider usages with HorizontalDivider
- [x] Open Sans typography alignment (Google Fonts provider; fallback Noto Sans SC)
- [x] Update README and PROJECT_CONTEXT
- [x] Establish change policy: Any route changes MUST update `core/ui/navigation/Screen.kt` and `PROJECT_CONTEXT.md` in the same PR
- [x] Complete internationalization (EN/zh-Hans) with 500+ translated strings
- [x] Production-ready status with comprehensive testing and optimization

## 🔧 Development Commands

```bash
# Build Debug APK
.\gradlew.bat assembleDebug

# Build Release APK
.\gradlew.bat assembleRelease

# Run Tests
.\gradlew.bat test

# Clean Project
.\gradlew.bat clean

# Check Dependencies
.\gradlew.bat dependencies
```

## ✅ EdgeOne Rate-Limiting Verification Tips

- Gunakan tipe pengujian DoS Spike atau Connection Flood untuk memicu rate limiting pada edge/WAF.
- Contoh konfigurasi (di Tencent EdgeOne Console):
  - WAF → Rate Limiting → Create Rule: scope per-IP atau fingerprint; threshold mis. 10 request / 10 detik; action Block/429.
  - (Opsional) Set custom response 429 dan header Retry-After: 10; aktifkan logging.
- Hasil yang diharapkan saat test: HTTP 429/403, header Retry-After (bila diset), serta event tercatat pada log/analytics.
- Lihat panduan detail di EDGEONE_PENTEST_GUIDE.md.

## 🐛 Known Issues

1. Replace placeholder `google-services.json` with actual Firebase config
2. Add logo assets (teoc.png, teoc_logo.png)
3. Minor UI inconsistencies in some test configuration screens

## 📱 Minimum Requirements

- Android 7.0 (API 24)
- Target SDK: 36
- Kotlin: 2.0.21
- Gradle: 8.13

## 🎨 Design System

- **Primary Color**: Tencent Blue (#4A7BFF)
- **Secondary Color**: Light Blue (#E8F0FF)
- **Typography**: Open Sans (Google Fonts provider, fallback: Noto Sans SC; final fallback: SansSerif)
- **Card Radius**: 20dp (token `CardShape`)
- **Button Radius**: 12dp (token `ButtonShape`)

## 📄 License

Copyright © 2024 TEO SecTest

---

**Note**: This is a security testing application designed for testing your own infrastructure only. Always ensure you have proper authorization before testing any systems.
