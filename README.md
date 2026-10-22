# Inventory Hub

Inventory Hub is an Android application for managing a shop's inventory and day-to-day stock transactions. It provides screens for products, purchases, receiving stock, sales, customer and supplier records, and business reports. The app supports a local/offline mode backed by an on-device database and an online mode that communicates with a configured Inventory Hub API server.

## Features

- **Inventory management:** Maintain products, categories, units of measure, SKU/HSN details, stock batches, and expiry information.
- **Purchasing and receiving:** Create purchase orders, view order history, and record incoming stock.
- **Sales:** Build sales from selected products, keep sales history, and view or print bills.
- **Customers and suppliers:** Manage buyer and supplier records used by sales and purchases.
- **Product tools:** Import/export product data using CSV, scan product codes, and generate product QR codes.
- **Reports:** View sales and purchase registers, GSTR-1, user-wise reports, and the audit trail. Report screens provide CSV, Excel, and PDF export options where supported.
- **Backup and restore:** Create and restore password-protected database backups.
- **Settings:** Configure app preferences, including the appearance theme.
- **Offline and online modes:** Use local Room storage offline, or connect to a compatible server in online mode.

## Screenshots

The `screenshots/` folder is ready for application captures. No screenshots were present in the project when this README was created, so the image links below are placeholders. Add your screenshots using these filenames to show them here.

### Login

![Login screen](screenshots/login-screen.png)

### Dashboard

![Dashboard screen](screenshots/dashboard.png)

### Inventory

![Inventory screen](screenshots/inventory.png)

### Purchase and receiving

![Purchase screen](screenshots/purchase.png)

### Sales

![Sales screen](screenshots/sales.png)

### Reports

![Reports screen](screenshots/reports.png)

## Technology

- Native Android application written in Java
- Gradle Wrapper, Android Gradle Plugin, and AndroidX
- Room for the local SQLite database
- Retrofit and OkHttp for online API communication
- CameraX and ZXing for camera-based scanning and QR/barcode utilities
- OpenCSV and Apache POI for CSV and spreadsheet support

## Prerequisites

1. **Android Studio** with Android SDK tools installed, or the Android SDK command-line tools.
2. **JDK 17 or newer** available to Gradle.
3. **Android SDK Platform 37, extension level 1** (the app's compile SDK), plus the Android SDK Build Tools installed through the SDK Manager.
4. An emulator or Android device running **Android 8.1 (API 27) or newer** to install and run the app. The app targets Android 16 (API 36).
5. Internet access during the initial build so Gradle can download project dependencies.

The project includes its Gradle Wrapper and the compile-only platform SDK library at `app/libs/platform_sdk_v4.1.0326.jar`; you do not need to download those separately.

## Build

### Android Studio

1. Open the project root in Android Studio.
2. Allow Gradle to sync and install any missing SDK components when prompted.
3. Select the `app` run configuration and run it on an emulator or connected device.

### Command line

Run these commands from the project root:

**Windows PowerShell**

```powershell
.\gradlew.bat :app:assembleDebug
```

**macOS / Linux**

```sh
./gradlew :app:assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/`.

To create the release variant, run `:app:assembleRelease` with the same platform-specific wrapper (`.\gradlew.bat` on Windows or `./gradlew` on macOS/Linux). Configure release signing credentials securely for your distribution workflow; do not publish a development-signed build as a production release.

## Tests

Run the local JVM unit tests:

```text
.\gradlew.bat :app:testDebugUnitTest
```

On macOS/Linux, use `./gradlew :app:testDebugUnitTest`. Instrumentation tests can be run on a connected device or emulator with `:app:connectedDebugAndroidTest`.

## Data and online mode

Offline mode stores app data in the on-device Room database. Online mode uses the app's configured server URL and API; using it requires network access and a compatible backend, and may require valid server/device authentication. Backend deployment and credentials are not included in this repository.

## Project structure

```text
app/
  src/main/
    java/in/gbtsolutions/inventoryhub/
      activities/       Android screens and user flows
      adapters/         RecyclerView and list adapters
      backup/           Backup and restore implementation
      dao/               Room database access objects
      fragments/         Purchase and sales flow fragments
      helpers/           Shared utilities and device features
      models/            Local data and transaction models
      online/            API client, online models, and repositories
      reports/           Report calculations and export formats
      repository/        Data access and local/online coordination
    res/                 Layouts, menus, themes, and other resources
  src/test/              JVM unit tests
  src/androidTest/       Android instrumentation tests
gradle/                  Gradle version catalog and wrapper settings
screenshots/             Application screenshots for this README
```

## Application configuration

The Android application module is `:app`. Its application ID is `in.gbtsolutions.inventoryhub`; it requires Android API 27 or newer, compiles against API 37 extension level 1, and targets API 36. Online mode uses the server configuration maintained by the app; a usable backend is not part of this repository.
