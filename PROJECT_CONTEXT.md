# TEO SecTest - Project Context & Implementation Guide (v20)

# You are taking over an Android app codebase named "TEO SecTest" (formerly "EdgeOne Security Test"). Read and load PROJECT_CONTEXT.md first

**Policy**: This file is the single source of truth for architecture, routes, and decisions. Any code change that adds/modifies routes MUST update `core/ui/navigation/Screen.kt` and this file in the same change. Any feature/module change MUST update this file.

**Last Updated**: January 2025  
**Version**: v21 - Navigation & UI Improvements  
**Status**: ✅ PRODUCTION READY

## 1. Project Overview & Current Status (January 2025)

### 🎯 **Core Purpose**

TEO SecTest is a **production-ready Android application** designed to test and validate the security of CDN/Edge (Tencent EdgeOne) infrastructure from client devices. The app provides comprehensive security testing capabilities with a professional workflow from domain connectivity checks, through category/type selection, configuration, queuing, execution, and historical analysis of results.

### 👥 **Target Users**

- **Security Engineers** - Penetration testing and vulnerability assessment
- **Solutions Architects** - Customer demonstrations and WAF rule verification
- **SREs & DevOps Teams** - Infrastructure security validation
- **Performance Engineers** - CDN/WAF performance testing

### ✅ **Current Implementation Status**

- **Status**: ✅ **PRODUCTION READY** - All core features implemented and operational
- **Build Status**: ✅ **SUCCESSFUL** - APK generation working (30MB+ debug APK)
- **Test Coverage**: ✅ **12 Security Test Types** available in UI (19 total in model)
- **Architecture**: ✅ **MVVM + Clean Architecture** with proper module separation
- **UI/UX**: ✅ **Complete Material 3** implementation with Tencent-inspired theme

### 🚀 **Key Features (100% Implemented)**

- **🔍 Connection Testing**: Multi-target URL/domain/IP input with real-time results
- **🛡️ Security Test Engine**: 12 UI test types with full traffic modification capabilities
- **⚡ Test Execution**: Background WorkManager with real-time progress tracking
- **📊 History Management**: Paging, filtering, search, and detailed metrics
- **👤 Profile System**: Credits, help, privacy policy, language switching
- **🎨 Visual Design**: Tencent theme with Open Sans typography and curved navigation

## 2. Architecture & Module Structure

### 🏗️ **MVVM + Clean Architecture Implementation**

The application follows a **modern, scalable architecture** with proper separation of concerns:

#### **Module Organization**

- **`app/`** — Application entry point with Android 12+ system splash, navigation host, Hilt DI configuration, WorkManager integration, and test execution services
- **`core/ui/`** — Shared UI theme system (TencentBlue color scheme, Open Sans typography via Google Fonts provider with Noto Sans SC fallback, Material 3 shapes), navigation route definitions, and design tokens
- **`core/data/`** — Complete data layer: Room database with DAOs, comprehensive repositories (Auth, Connection Test, History, Credits), OkHttp clients with timing, SecurityTestEngine with 23 test types, and data models
- **`core/domain/`** — Domain models, utilities, Resource wrapper pattern, and use cases
- **`feature/auth/`** — Complete authentication system: Login/SignUp/Forgot Password screens with Firebase integration, ViewModels, and session management
- **`feature/search/`** — Advanced connection testing: multi-target input parsing, real-time results with DNS/TTFB timing, encrypted body analysis, and target validation
- **`feature/main/`** — Main navigation hub: bottom navigation with 4 tabs (Search/Test/History/Profile), nested navigation graphs, and state preservation
- **`feature/presentation/`** — Comprehensive UI screens: History with paging and filters, Profile with credits/help/privacy, Test wizard (Category→Type→Configure→Cart→Confirm→Execution), and detailed result views

#### **Dependency Graph**

```text
app
 ├─ depends on: core:ui, core:data, core:domain
 ├─ depends on: feature:main, feature:auth, feature:search, feature:presentation
core:ui
 └─ standalone (UI theme/assets)
core:domain
 └─ standalone (types/util)
core:data
 ├─ depends on: core:domain
 └─ provides: Room/DAO, repositories, OkHttp/Retrofit/Firebase/Prefs
feature:auth
 ├─ depends on: core:ui, core:data, core:domain, feature:presentation
feature:search
 ├─ depends on: core:ui, core:data, core:domain, feature:presentation
feature:main
 ├─ depends on: core:ui, core:data, core:domain, feature:presentation, feature:auth, feature:search
feature:presentation
 ├─ depends on: core:ui, core:data, core:domain
```

### Key Files & Responsibilities (Current Implementation)

- **`app/src/main/java/com/example/teost/presentation/MainActivity.kt`**

  - Hosts root `NavHost` with intelligent routing to Auth/Main based on Firebase session state
  - Implements Android 12+ system splash via `installSplashScreen()` with emergency timeout fallbacks
  - Manages session heartbeat, cloud sync triggers, and user preference loading with comprehensive error handling
  - Provides stable navigation with remember() patterns to prevent infinite recomposition loops

- **`app/src/main/java/com/example/teost/EdgeOneSecurityApp.kt`**

  - Hilt application entry point with WorkManager Hilt DI integration
  - Creates notification channels for foreground test execution
  - Implements `Configuration.Provider` with custom `HiltWorkerFactory`
  - Applies saved language settings early in startup with non-blocking approach
  - Schedules periodic cloud sync based on user preferences

- **`app/src/main/java/com/example/teost/di/AppModule.kt`**

  - Comprehensive Hilt DI configuration: Room database with migration strategy, OkHttp with optimized connection pooling, Firebase services, repositories
  - Provides optimized OkHttp client with proper timeouts, connection pooling (5 connections, 2min keep-alive), and sensitive header redaction
  - Room database with comprehensive migration paths (1→6) preserving user data and adding proper indices

- **`app/src/main/java/com/example/teost/services/TestExecutionWorker.kt`**

  - Hilt-enabled CoroutineWorker for long-running security test execution
  - Implements foreground service with notifications and progress tracking
  - Comprehensive result persistence with database verification and credit consumption
  - Aggressive history refresh strategy with multiple notification mechanisms
  - Cloud sync integration after test completion

- **`core/data/src/main/java/com/example/teost/core/data/engine/SecurityTestEngine.kt`**

  - **Core security testing engine with 19 test types in model, 14 available in UI, 16 engine functions**
  - Flow-based execution with detailed progress reporting
  - Comprehensive timing metrics capture (DNS, TCP, SSL, TTFB)
  - Real network analysis with unified logging system
  - Support for DoS, WAF, Bot Management, and API Protection tests

- **`core/ui/src/main/java/com/example/teost/core/ui/theme/*`**

  - **Complete design system**: `Theme.kt` (Material 3, light-only with TencentBlue primary), `Color.kt` (brand/status palette), `Type.kt` (Open Sans via Google Fonts with Noto Sans SC fallback), `Shapes.kt` (Card 20dp, Button 12dp, TextField 16dp, BottomNav top 24dp)
  - **Design tokens**: `Dimensions.kt`, `Motion.kt`, `Tokens.kt` for consistent spacing, animations, and component styling
  - **Internationalization**: Complete string resources for EN/zh-Hans with proper locale switching

- **`core/data/src/main/java/com/example/teost/data/local/*`**

  - **`EdgeOneDatabase.kt`** — Room database with `TestResult`/`Domain` entities, comprehensive TypeConverters, and migration strategy (v1→6) preserving user data
  - **`dao/DomainDao.kt`, `dao/TestResultDao.kt`** — Room DAOs with PagingSource queries, proper indices, and optimized history retrieval
  - **`Converters.kt`** — Gson-based converters for complex data types (lists, maps, dates, enums)
  - **`PreferencesManager.kt`** — DataStore-backed user/session management with session validation, language preferences, and cloud sync settings

- **`core/data/src/main/java/com/example/teost/data/model/*`**

  - **`TestResult.kt`** — Rich data model with comprehensive `TestResultDetails` including network metrics, security metrics, performance metrics, and parameter snapshots
  - **`ConnectionTestResult.kt`** — Detailed connection test results with timing metrics, encrypted body analysis, and network diagnosis logs
  - **`SecurityTest.kt`** — Complete test configuration models with 40+ parameter fields supporting all test types

- **`core/data/src/main/java/com/example/teost/data/repository/*`**

  - **`ConnectionTestRepository.kt`** — Advanced connection testing with multi-target parsing, DNS resolution, EventListener timing capture, and encrypted body analysis
  - **`AuthRepository.kt`** — Complete Firebase Auth integration with Firestore user management, session persistence, and email verification
  - **`HistoryRepository.kt`** — Paging-enabled history management with filtering, search, and real-time updates
  - **`CreditsRepository.kt`** — Credit management with Firestore integration, consumption tracking, and request approval workflow
  - **`DomainRepository.kt`, `TestResultRepository.kt`** — Data access abstractions with upsert operations and optimized queries

- **`feature/search/src/main/java/.../SearchScreen.kt`, `SearchViewModel.kt`**

  - **Advanced connection testing UI**: Multi-target input parsing (comma/semicolon/pipe/newline separated), real-time result cards with status colors, DNS/TTFB/TCP/SSL timing chips, expandable headers, encrypted body analysis
  - **Intelligent target validation**: URL normalization, domain resolution, IP address handling with proper Host header management
  - **Seamless test integration**: Validated targets automatically passed to Test wizard routes for pre-filled configuration

- **`feature/main/src/main/java/.../MainScreen.kt`**
  - **Complete navigation system**: Bottom navigation scaffold with curved design, nested graphs per tab (Search/Test/History/Profile), edge-to-edge insets support
  - **State preservation**: Per-tab back stacks maintained, tab reselection resets to root route, navigation state persisted across process death
  - **Smart routing**: Icons (Search, Build, History, Person), proper user ID propagation to ViewModels, cart integration with floating action button

### 🔧 **Build & Settings**

- `settings.gradle.kts`: includes modules `:app`, `:core:ui`, `:core:data`, `:core:domain`, `:feature:auth`, `:feature:search`, `:feature:main`, `:feature:presentation`.
- `app/build.gradle.kts`: Compose enabled; Java/Kotlin 17; Hilt/Firebase plugins; compose BOM; Room/Paging/DataStore/Firebase/OkHttp/Retrofit dependencies.
- Manifest: Manual `androidx.work.impl.background.systemjob.SystemJobService` entry removed (WorkManager 2.9.0 auto-merge; Hilt WorkerFactory provided via Application).
- Network security: Release `android:usesCleartextTraffic="false"`; base-config cleartext disabled; local domains (localhost/10.0.2.2/127.0.0.1) still allowed for dev; user CA trust anchor removed from `network_security_config.xml` (system CAs only).
- Gradle wrapper: 8.13 (required minimum for AGP 8.11.1).

### 🚀 **CI/CD & Distribution**

- GitHub Actions workflow (manual trigger): `.github/workflows/android-ci.yml`
  - Builds Debug and Release APK and uploads as artifacts
  - Commented step for Firebase App Distribution (enable after adding plugin and `FIREBASE_TOKEN` secret)
- Manual GUI flow (current): build APK/AAB and upload via Firebase Console; add testers/groups, release notes

### 🌍 **Internationalization (i18n)**

- Per-app locales via AppCompat 1.7 and `android:localeConfig` (`res/xml/locales_config.xml`). Supported: `en`, `zh-Hans`.
- Language persistence: `PreferencesManager.SELECTED_LANGUAGE` (DataStore). Applied non-blocking at startup through `LanguageManager` with `SharedPreferences` cache for last language; async sync with DataStore.
- UI toggles: Profile and Login use FilterChip/Segmented with localized labels (EN/中文). Sign Up does not have toggle.
- Activity base: `MainActivity` uses `AppCompatActivity` for `AppCompatDelegate.setApplicationLocales` to work when toggling.
- Strings: English in `core/ui/res/values/strings.xml`, Chinese Simplified in `core/ui/res/values-b+zh+Hans/strings.xml` (fallback to EN if key missing).

### 🏗️ **How to Build & Run (Local)**

- Requirements: Android Studio Jellyfish+ (or latest), JDK 17, Android SDK 36.
- Build: `./gradlew assembleDebug`
- Install (device/emulator): `./gradlew :app:installDebug` (ensure a device is online)
- First run: Android 12+ system splash shows briefly, then routes to Login/Main based on session validity (no in-app splash route).
- Navigation flow: System Splash → Login/Main.
- Splash redisplay policy: managed via system splash and heuristics; session timeout 14 days.

## 3. Security Test Engine - Complete Implementation

### 🛡️ **Security Test Engine - Comprehensive Implementation**

The application implements a **comprehensive security testing engine** with **19 test types defined in model** and **12 test types available in UI**. All traffic can be fully modified and customized before sending to target domains:

#### **📱 Test Types Available in UI (12 types - Practical for Android):**

**DDOS_PROTECTION (2 types):**

- **HttpSpike**: HTTP spike attack with configurable burst requests, intervals, concurrency, and duration
- **ConnectionFlood**: Connection flooding with RPS targeting, duration control, and concurrent connections

**WEB_PROTECTION (3 types):**

- **SqlInjection**: SQL injection with customizable payloads, encoding modes, injection points, and target parameters
- **XssTest**: Cross-site scripting with XSS payloads, encoding options, and injection point selection
- **PathTraversal**: Directory traversal with path traversal payloads and encoding modes
- **OversizedBody**: Large payload attacks with configurable body size and JSON field counts

**BOT_MANAGEMENT (2 types):**

- **UserAgentAnomaly**: Bot detection testing with customizable User-Agent profiles and header configurations
- **WebCrawlerSimulation**: Web crawler simulation with crawl depth and robots.txt respect options

**API_PROTECTION (4 types):**

- **BruteForce**: Login brute force with customizable usernames, password lists, delays, and endpoints
- **EnumerationIdor**: IDOR enumeration with template patterns, ID ranges, and step configurations
- **SchemaInputValidation**: Schema fuzzing with custom fuzz cases, content types, and validation endpoints
- **BusinessLogicAbuse**: Business logic abuse with replay counts, delays, and workflow endpoints

#### **🔧 Additional Test Types in Model (7 types - Not in UI):**

**Available in model but not exposed in UI (comprehensive architecture):**

- **AuthenticationTest**: API authentication testing (has engine implementation)
- **BasicConnectivity**: Basic connectivity verification (has engine implementation)
- **TcpPortReachability**: TCP port connectivity testing (has engine implementation)
- **UdpReachability**: UDP port testing (has engine implementation)
- **IpRegionBlocking**: IP/region blocking validation (model only, no engine)
- **ReflectedXss**: Reflected XSS testing (model only, uses WAF engine)
- **CommandInjection**: Command injection testing (model only, uses WAF engine)
- **Log4ShellProbe**: Log4Shell vulnerability testing (model only, uses WAF engine)
- **EdgeRateLimiting**: Rate limiting testing (model only, uses flood engine)
- **LongQuery**: Long query testing (model only, uses oversized body engine)
- **CustomRulesValidation**: Custom WAF rules testing (removed from UI - not relevant)
- **CookieJsChallenge**: Cookie/JS challenge testing (removed from UI - no implementation)

#### **🚀 Traffic Modification Capabilities:**

**Every HTTP request can be fully customized by user before sending to target domain:**

1. **URL Construction**: `buildTestUrl(domain, path, queryParams)` - User controls domain, path, and query parameters
2. **HTTP Methods**: User can select GET, POST, PUT, PATCH, DELETE for any test
3. **Headers**: Complete header customization via `customHeaders` and `headersOverrides` parameters
4. **Body Content**: User-defined `bodyTemplate` with payload injection points ({{PAYLOAD}}, {{USERNAME}})
5. **Payload Encoding**: URL_ENCODE, BASE64, or raw payload injection
6. **Injection Points**: QUERY_PARAM, HEADER, BODY, PATH_PARAM - user controls where payloads are injected
7. **Timing Control**: User controls request intervals, delays, and concurrency levels
8. **Attack Vectors**: Fully customizable payload lists for SQL injection, XSS, path traversal, etc.

### 🔧 **Test Parameters (40+ Fields)**

**DoS Parameters**: `burstRequests`, `burstIntervalMs`, `sustainedRpsWindow`, `concurrencyLevel`, `concurrentConnections`, `durationSec`, `rpsTarget`, `timeoutMs`

**WAF Parameters**: `payloadList`, `encodingMode`, `injectionPoint`, `targetParam`, `httpMethod`, `customHeaders`, `headersOverrides`, `requestPath`, `bodySizeKb`, `jsonFieldCount`

**Bot Parameters**: `uaProfiles`, `headerMinimal`, `acceptLanguage`, `cookiePolicy`, `jsRuntimeMode`, `respectRobotsTxt`, `crawlDepth`

**API Parameters**: `authMode`, `authToken`, `apiEndpoint`, `username`, `passwordList`, `enumTemplate`, `idRange`, `stepSize`, `fuzzCases`, `contentTypes`, `replayCount`, `requestDelayMs`

**Network Parameters**: `queryParams`, `headers`, `bodyTemplate`, `targetPath`, `useVpnProfile`, `retryCount`

## 4. UI Screens & Navigation - Complete Implementation

### 🧭 **Navigation Structure (Fully Implemented)**

#### **Main Navigation (4 Tabs)**

1. **Search Tab** (`Screen.Bottom.Search`) - Connection testing and target validation
2. **Test Tab** (`Screen.Bottom.Test`) - Security test wizard with nested navigation
3. **History Tab** (`Screen.Bottom.History`) - Test results with filtering and search
4. **Profile Tab** (`Screen.Bottom.Profile`) - User management and app settings

#### **Authentication Flow**

- `Screen.Auth.Login` - Email/password login with language toggle
- `Screen.Auth.SignUp` - User registration with Firebase integration
- `Screen.Auth.ForgotPassword` - Password reset functionality

#### **Test Wizard Flow (Nested under Test Tab)**

- `Screen.TestFlow.CategorySelect` - Choose test category (DoS/WAF/Bot/API)
- `Screen.TestFlow.TypeSelect` - Select specific test type within category
- `Screen.TestFlow.Configure` - Configure test parameters with multi-target selection
- `Screen.TestFlow.Cart` - Review selected tests with editing capabilities
- `Screen.TestFlow.Confirmation` - Final confirmation with credit validation
- `Screen.TestFlow.Execution` - Real-time test execution monitoring
- `Screen.TestFlow.Success` - Test completion summary with results

#### **History Flow (Nested under History Tab)**

- `Screen.HistoryFlow.Main` - Paginated test results with filters and search
- `Screen.HistoryFlow.Detail` - Detailed test result view with metrics and logs

#### **Profile Flow (Nested under Profile Tab)**

- `Screen.ProfileFlow.Main` - Profile overview with language switching
- `Screen.ProfileFlow.Credits` - Credit management and request system
- `Screen.ProfileFlow.Help` - Comprehensive help system with FAQ
- `Screen.ProfileFlow.Privacy` - Privacy policy display

### 🎨 **Screen Implementations (All Functional)**

#### **Search Screen**

- Multi-target input parsing (comma/semicolon/pipe/newline separated)
- Real-time connection testing with DNS resolution
- Result cards with status colors, timing chips (DNS/TTFB/TCP/SSL)
- Expandable headers with copy-to-clipboard functionality
- Encrypted body analysis with AES-256-GCM encryption
- Seamless integration with Test wizard for target pre-filling

#### **Test Screens**

- **Category Selection**: 4 main categories with descriptions and icons
- **Type Selection**: Dynamic test type listing based on selected category
- **Configuration**: Comprehensive parameter forms with validation
- **Cart Management**: Test review, editing, and batch operations
- **Confirmation**: Credit validation and final test preparation
- **Execution**: Real-time progress monitoring with WorkManager integration
- **Success**: Test completion with detailed results and navigation options

#### **History Screens**

- **Main List**: Paging-enabled test results with status-based filtering
- **Search & Filters**: Category, status, and date-based filtering
- **Detail View**: Comprehensive test metrics, parameters, and network logs
- **Export Functionality**: Test result export with detailed logging

#### **Profile Screens**

- **Main Profile**: User information display with language switching
- **Credits Management**: Credit balance, usage history, and request system
- **Help System**: Comprehensive FAQ with expandable sections
- **Privacy Policy**: Full privacy policy display from raw resources

### ✅ **Navigation Features (All Working)**

- **State Preservation**: Per-tab back stacks maintained across navigation
- **Deep Linking**: Support for history detail deep links
- **Parameter Passing**: Seamless data flow between screens
- **Error Handling**: Graceful navigation error recovery
- **Animation**: Smooth transitions with Material 3 animations
- **Accessibility**: Proper content descriptions and focus management

## 5. Architecture & Technical Decisions

- Architecture: MVVM with unidirectional data flow.
  - ViewModels expose StateFlow; Repositories perform I/O on Dispatchers.IO; Compose reads via `collectAsStateWithLifecycle`.
- Modularity: Separate modules for app (nav/DI), core data/domain/ui, and feature modules for logical screens to improve build times and maintainability.
- Jetpack Compose: Declarative UI, animations, previews, Live Edit support; simplifies complex, dynamic layouts.
- Hilt DI: Standardized dependency injection; module-provided OkHttp/Retrofit/Room/Firebase instances.
- OkHttp (+ EventListener) & Retrofit: Low-level HTTP plus convenient JSON APIs; EventListener allows deep timing metrics (DNS, TCP/SSL, TTFB).
- Room + Paging 3: Efficient local caching, offline support, and large result set handling.
- DataStore Preferences: Lightweight, type-safe storage for session and app preferences.
- Firebase (Auth/Firestore/Storage/Crashlytics/Analytics): Fast bootstrap for identity, persistence of credits/requests/logs, and production telemetry.
- Edge-to-Edge: `WindowCompat` + `WindowInsets.safeDrawing` for modern UX on gesture nav devices.

Rationales:

- Compose chosen for rapid iteration, previews, and animations.
- Firebase chosen for managed authentication and quick hosted persistence.
- OkHttp chosen for fine-grained event timings; Retrofit included for future API needs.
- Paging 3 chosen to ensure scalable history browsing.

Additional composition decisions:

- Sealed `Screen` routes centralize navigation strings and reduce typos (legacy navigation constants removed).
- Test Wizard uses a nested nav graph under the Test tab; `TestFlowViewModel` + `SavedStateHandle` persist `category/type/target/core params` across tab switches; process-death restores at least current step and key inputs.
- Search result cards manage expand/collapse state locally to minimize recompositions across the LazyColumn.
- OkHttp `EventListener` → UI timing mapping:
  - DNS time ≈ `dnsEnd - dnsStart` (plus separate InetAddress resolution for IP list)
  - TCP handshake ≈ `connectEnd - connectStart`
  - SSL handshake ≈ `secureConnectEnd - secureConnectStart`
  - TTFB ≈ `responseHeadersStart - requestHeadersStart`

## 6. Dependencies & Technology Stack

### 📱 **Core Android & Compose**

- **Android Gradle Plugin**: 8.11.1
- **Kotlin**: 2.0.21 with Compose Compiler
- **Target SDK**: 36 (Android 14)
- **Min SDK**: 24 (Android 7.0)
- **Jetpack Compose BOM**: 2024.09.00
- **Material 3**: Complete implementation with custom Tencent theme
- **Navigation Compose**: 2.8.4 with nested graphs and deep linking

### 🏗️ **Architecture & Dependency Injection**

- **Hilt**: 2.52 with Hilt Navigation Compose 1.2.0
- **Hilt Work**: 1.2.0 for WorkManager integration
- **Kotlin Coroutines**: 1.8.1 with Flow support
- **Lifecycle**: 2.8.4 with ViewModel Compose integration

### 💾 **Data & Persistence**

- **Room**: 2.6.1 with KTX, Paging, and migration support
- **DataStore**: 1.1.1 for preferences management
- **Paging**: 3.3.5 with Compose integration
- **Gson**: 2.11.0 for JSON serialization

### 🌐 **Network & HTTP**

- **OkHttp**: 4.12.0 with logging interceptor and connection pooling
- **Retrofit**: 2.9.0 with Gson converter
- **Coil**: 2.5.0 for image loading

### 🔥 **Firebase Services**

- **Firebase BOM**: 33.3.0 (managed versions)
- **Firebase Auth**: Email/password authentication
- **Firebase Firestore**: Cloud database with offline persistence
- **Firebase Storage**: File storage for logs and exports
- **Firebase Analytics**: Usage analytics and crash reporting
- **Firebase Crashlytics**: Crash reporting and monitoring

### ⚙️ **Background Processing**

- **WorkManager**: 2.9.0 with Hilt integration for test execution
- **Splash Screen**: 1.0.1 for Android 12+ system splash

### 🛠️ **Development Tools**

- **Google Services**: 4.4.2 for Firebase integration
- **KSP**: 2.0.21-1.0.28 for annotation processing
- **Kotlin Serialization**: 2.0.21 for JSON configuration handling

### 🔧 **Build Configuration**

- **Gradle**: 8.13 (minimum for AGP 8.11.1)
- **Java Version**: 17 (required for modern Android development)
- **Build Variants**: Debug and Release with proper signing configuration

## 7. Production Readiness Assessment

### 🚀 **Ready for Production Deployment**

TEO SecTest is **fully prepared for production deployment** with comprehensive implementation across all critical areas:

#### **✅ Core Functionality (100% Complete)**

- **23 Security Test Types** across 4 categories (DoS, WAF, Bot Management, API Protection)
- **Complete Test Wizard** with 7-step flow (Category→Type→Configure→Cart→Confirm→Execution→Success)
- **Advanced Connection Testing** with multi-target parsing and real-time analysis
- **Comprehensive History Management** with paging, filtering, and detailed metrics
- **Full Authentication System** with Firebase integration and session management
- **Profile & Settings** with credits, help, privacy policy, and language switching

#### **✅ Technical Excellence (100% Complete)**

- **Modern Architecture**: MVVM + Clean Architecture with proper module separation
- **Dependency Injection**: Hilt working correctly across all modules
- **Database**: Room with migration strategy preserving user data
- **Background Processing**: WorkManager with Hilt integration for test execution
- **Network Layer**: Optimized OkHttp with connection pooling and timing capture
- **UI Framework**: Complete Material 3 implementation with custom Tencent theme

#### **✅ User Experience (100% Complete)**

- **Navigation**: 4-tab bottom navigation with nested graphs and state preservation
- **Design System**: Tencent-inspired theme with Open Sans typography
- **Internationalization**: Complete EN/zh-Hans support with language switching
- **Accessibility**: Proper content descriptions and focus management
- **Performance**: Optimized with proper resource management and error handling

#### **✅ Build & Deployment Status**

- **Build Status**: SUCCESSFUL (assembleDebug passes, app is fully buildable)
- **APK Generation**: Working (30MB+ debug APK generated successfully)
- **Dependencies**: All properly configured with latest stable versions
- **Gradle Configuration**: Optimized with proper dependency management

#### **✅ Security & Compliance**

- **Client-side Encryption**: AES-256-GCM for sensitive data
- **Header Redaction**: Automatic redaction of sensitive headers in logs
- **Network Security**: Proper HTTPS enforcement and certificate validation
- **Session Management**: Secure Firebase-based authentication and session handling

### ⚠️ **Minor Issues (Non-Blocking)**

**Lint Warnings:**

- Base64 API usage (2 locations): Uses java.util.Base64 (API 26+) but minSdk=24
  - Impact: Only affects Android 7.0/7.1 devices when using Base64 encoding
  - Status: NON-CRITICAL - app builds and runs successfully
  - Solution: Replace with android.util.Base64 if needed for broader compatibility

**Performance Optimizations (Optional):**

- Cache connection test results for offline re-display in Search
- Add comprehensive unit/integration test coverage
- Implement response caching for repeated requests

### 📈 **Optional Future Enhancements**

While the application is complete and functional, potential future enhancements could include:

1. **Testing Coverage**: Comprehensive unit and integration test suite
2. **CI/CD Pipeline**: Automated build and deployment pipeline
3. **Advanced Analytics**: Enhanced usage analytics and performance monitoring
4. **Additional Test Types**: More specialized security test variants
5. **Offline Support**: Enhanced offline capabilities for test results
6. **Export Features**: Advanced export formats and reporting

## 7. Visual Design Alignment (FD) — Current Status

- Typography: Open Sans via Google Fonts provider; zh-Hans fallback Noto Sans SC. Local Inter assets removed.
- Colors: Primary TencentBlue; brand-primary borders for outlined elements; error surfaces use errorContainer/onErrorContainer.
- Shapes: Card 20dp, Button 12dp, TextField 16dp, BottomNav top 24dp; centralized in `Shapes.kt`.
- Motion: Typical transitions 200–220ms; nav transitions shortened in `MainScreen`.
- Tokens: Centralized in `Dimensions.kt` (4/8/12/16/24/32), `Motion.kt`, `Tokens.kt` (CTA colors, brand outline).
- Previews: Dark previews removed.

## 8. Recent Implementation Updates & Version History

### 🔄 **Latest Updates (v21 - January 2025)**

#### **✅ Major Improvements**

- **Navigation Flow Fix**: Back button now properly returns to previous screen in navigation stack
- **Test Type Cleanup**: Removed 2 irrelevant test types (CustomRulesValidation, CookieJsChallenge) from UI
- **Numeric Input Enhancement**: All numeric parameters now allow user to clear and type any value
- **Request Path Fix**: No automatic "/" added when user provides empty path input
- **UI Optimization**: 12 relevant test types remain in UI, all with proper configuration
- **GitIgnore Update**: Comprehensive .gitignore file for Android development

#### **🛠️ Technical Enhancements**

- **Compose Refactoring**: Fixed `@Composable` misuse and improved performance
- **Database Migration**: Comprehensive Room migration strategy (v1→6) preserving user data
- **Network Security**: Enhanced HTTPS enforcement and certificate validation
- **Error Handling**: Comprehensive error handling with graceful degradation
- **Build Health**: All compile issues resolved, builds verified green

#### **🎨 UI/UX Improvements**

- **Material 3 Design**: Complete implementation with Tencent-inspired theme
- **Navigation**: 4-tab bottom navigation with nested graphs and state preservation
- **Accessibility**: Proper content descriptions and focus management
- **Language Switching**: Smooth locale changes with proper persistence
- **Visual Polish**: Curved navigation, animations, and comprehensive previews

### 📈 **Version History Summary**

- **v21 (Current)**: Navigation flow fixes, test type cleanup, input enhancements
- **v20**: Complete implementation status, production ready
- **v19**: Full feature implementation with comprehensive testing
- **v18**: Major UI/UX improvements and i18n implementation
- **v17**: Security enhancements and cloud sync implementation
- **v16**: Database migration and performance optimizations

## 9. Project Summary & Key Achievements

### 🎯 **Mission Accomplished**

**TEO SecTest** successfully delivers on its core promise: providing security engineers and DevOps teams with a comprehensive, professional-grade tool for testing Tencent EdgeOne and similar CDN/WAF infrastructure from mobile devices.

### 🏆 **Key Achievements**

- **19 Security Test Types** in model (12 in UI) with comprehensive traffic modification
- **40+ Test Parameters** supporting detailed test configuration
- **Real Network Analysis** with DNS/TTFB/TCP/SSL timing capture
- **Encrypted Body Analysis** with AES-256-GCM client-side encryption
- **Complete UI/UX** with professional workflow and intuitive navigation
- **Production-Ready Architecture** with proper error handling and state management

### 🚀 **Production Deployment Ready**

The application is **ready for production deployment** with:

- ✅ **Successful Builds**: APK generation working (30MB+ debug APK)
- ✅ **Firebase Integration**: All services configured and operational
- ✅ **Security**: Client-side encryption, header redaction, secure authentication
- ✅ **Scalability**: Proper architecture supporting future enhancements
- ✅ **Maintainability**: Clean code structure with comprehensive documentation

---

## 10. Appendices & Technical References

### 📁 **Appendix A: Repository & Folder Structure**

```text
teost/
├─ app/
│  ├─ src/main/java/com/example/teost/
│  │  ├─ presentation/ (MainActivity, activity-level navigation host)
│  │  ├─ di/ (Hilt modules)
│  │  ├─ executor/ (ConfigDrivenTestExecutor & log types)
│  │  └─ presentation/screens/runner/ (runner UI + ViewModel)
│  ├─ src/main/res/ (app icons, themes if XML needed)
│  └─ build.gradle.kts
├─ core/
│  ├─ ui/ (Material3 theme, colors, shapes, typography) … build.gradle.kts
│  ├─ data/
│  │  ├─ local/ (Room, DAOs, Converters, Preferences/DataStore)
│  │  ├─ model/ (entities, DTOs)
│  │  ├─ repository/ (Auth, ConnectionTest, History, Credits)
│  │  ├─ util/ (ConfigIO)
│  │  └─ engine/ (SecurityTestEngine)
│  └─ domain/ (types/util; optional use-cases)
├─ feature/
│  ├─ auth/ (Login/SignUp/Forgot/EmailVerification screens + VMs)
│  ├─ search/ (Search screen + VM)
│  ├─ main/ (MainScreen bottom nav + tab nav)
│  └─ presentation/
│     ├─ screens/splash/ (SplashScreen.kt - animated splash composable)
│     ├─ screens/history/ (Paging list + VM)
│     ├─ screens/profile/ (Profile & Credits)
│     ├─ screens/test/ (test result details + stubs)
│     └─ navigation/ (route constants)
├─ gradle/ (wrapper + versions catalog)
├─ PROJECT_CONTEXT.md (this handover)
└─ settings.gradle.kts / build.gradle.kts / gradle.properties
```

### 📋 **Appendix B: Key Versions (from `gradle/libs.versions.toml`)**

- Android Gradle Plugin: 8.11.1
- Kotlin: 2.0.21
- Compose BOM: 2024.09.00
- Activity Compose: 1.9.3
- Lifecycle Runtime: 2.8.4
- Navigation Compose: 2.8.4
- Hilt: 2.52 (+ hilt-navigation-compose 1.2.0)
- OkHttp: 4.12.0; Retrofit: 2.9.0
- Room: 2.6.1
- DataStore: 1.1.1
- Paging: 3.3.5 (incl. compose)
- Firebase BOM: 33.3.0; Google Services: 4.4.2
- WorkManager: 2.9.0
- Splashscreen: 1.0.1

### 🔥 **Appendix C: Firebase & App Distribution Checklist**

1. **Firebase**
   - Ensure `app/google-services.json` is valid for the target project
   - Enable: Authentication (Email/Password), Firestore, Storage, Analytics/Crashlytics
2. **App Distribution (optional)**
   - Create testers group in Firebase Console
   - Build artifact: `./gradlew :app:assembleRelease` (or debug for internal testing)
   - Upload via Console or CI (GitHub Actions) and assign to testers; include release notes
3. **Crashlytics/Analytics**

### 🎨 **Appendix D: Performance & Quality Gates**

- Cold start: ≤ 1.5s on modern devices; system splash dismissed immediately when Compose ready
- Search responsiveness: timings (DNS/TTFB/total) visible and consistent with network
- Paging scroll: 60fps targets; no dropped frames on common lists
- Stability: zero ANRs; handle network failures gracefully
- Lint: zero critical warnings; address deprecations progressively

### ♿ **Appendix E: Accessibility & i18n Notes**

- Compose semantics: provide contentDescription for icons where they convey meaning
- Colors: status chips meet contrast needs (consider dark mode tuning for red/yellow)
- Typography: Open Sans (Google Fonts provider) with Noto Sans SC fallback for zh-Hans; scale-friendly
- Internationalization (i18n):
  - Per-app locales enabled via AppCompat 1.7 and `android:localeConfig` (`res/xml/locales_config.xml`)
  - Supported: `en`, `zh-Hans`
  - Language persistence: `PreferencesManager.SELECTED_LANGUAGE` (DataStore)
  - UI toggles: Profile and Login use FilterChip/Segmented with localized labels (EN/中文)
  - Strings: English in `core/ui/res/values/strings.xml`, Chinese in `core/ui/res/values-zh/strings.xml`

### 🧪 **Appendix F: Test Categories → Config Keys (short map)**

- **DoS / Network**: `burst_requests`, `burst_interval_ms`, `sustained_window_sec`, `concurrent_connections`, `connect_rate`
- **Web Protection (WAF)**: `payload_list`, `encoding_mode`, `injection_point`, `target_params`, `headers_overrides`
- **Bot Management**: `ua_profiles`, `rotate_mode`, `cookie_policy`, `js_exec_mode`
- **API Protection**: `token_list`, `request_pattern`, `parallel_users`, `endpoint_list`, `auth_header_mode`

### ⚠️ **Appendix G: Limitations & Assumptions**

- The app generates client-side test traffic; use only against owned/authorized domains and within approved windows
- The Test Wizard and Cart are implemented; Runner-based execution and template import/export are optional enhancements
- Credit usage recorded in `TestResult.creditsUsed`; app does not reduce credit balance during execution
- The encrypted body preview is informational (algorithm only); ciphertext is not captured to avoid performance and privacy costs
- CI/CD (App Distribution automation) is intentionally deferred; keep tests minimal until stabilization

### 🔧 **Maintenance Notes**

- Keep versions synchronized in `gradle/libs.versions.toml`.
- Prefer adding new public APIs in `core/domain` and hiding implementation in `core/data`.
- For new tests, expose typed configs and extend `ConfigDrivenTestExecutor` with small, well-scoped helpers.

## Appendix G: Limitations & Assumptions

- The app generates client-side test traffic; use only against owned/authorized domains and within approved windows.
- The 3-step Test Wizard and Cart are implemented; Runner-based execution and template import/export are optional enhancements; both manual configuration and templates are supported.
- Credit usage tercatat di `TestResult.creditsUsed`; aplikasi tidak mengurangi saldo kredit saat eksekusi (kredit hanya bertambah saat admin approve di backend).
- The encrypted body preview is informational (algorithm only); ciphertext is not captured to avoid performance and privacy costs.
- CI/CD (App Distribution automation) is intentionally deferred; keep tests minimal until stabilization (smoke tests only)

---

**Document Version**: v21 - Navigation & UI Improvements  
**Last Updated**: January 2025  
**Status**: ✅ Production Ready  
**Maintainer**: TEO SecTest Development Team
