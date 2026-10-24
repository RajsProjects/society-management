# Google Play Store Launch Readiness Audit & Release Runbook

**Application Name:** Samaaj (CivicLink)  
**Package Name:** `com.samaaj.societymanagement`  
**Current Version:** 1.0.0 (Version Code: 1)  
**Target Android Version:** Android 15 (API level 35)  
**Minimum Android Version:** Android 8.0 (API level 26)  
**Generated Release Artifact:** `android/app/build/outputs/bundle/release/app-release.aab` (3.2 MB)  

---

## 1. Executive Summary & Verdict

| Category | Status | Details |
|---|---|---|
| **Technical Specifications** | ✅ **READY** | Target SDK 35 (exceeds Google Play min 34), Min SDK 26, 64-bit compliant, R8 minification & resource shrinking verified. |
| **Release Artifact** | ✅ **READY** | Production signed `.aab` (Android App Bundle) successfully compiled and verified. |
| **Security & Hardening** | ✅ **HARDENED** | WebView debugging disabled in release, Mixed Content locked to `NEVER_ALLOW`, `allowBackup="false"`, cleartext traffic prohibited. |
| **API Endpoints** | ✅ **CONFIGURED** | Dynamic build-variant resolution (`https://api.samaaj.in/api/v1` in release, `10.0.2.2` in debug). |
| **Store Metadata & Assets** | ⚠️ **ACTION REQUIRED** | Need 512x512 High-Res Icon, 1024x500 Feature Graphic, phone screenshots, and public Privacy Policy URL. |
| **Google Play Policy Declarations** | ⚠️ **ACTION REQUIRED** | Complete Data Safety form, Account Deletion URL, and Financial Services questionnaire on Google Play Console. |

**Verdict:** The Android codebase, release bundle build pipeline, and backend security configurations are **technically 100% ready** for Google Play Store submission. Complete the administrative Play Console questionnaires and graphic asset uploads outlined below before opening the release track.

---

## 2. Technical Specifications Audit

### SDK & Architecture Compliance
- **Target SDK:** `35` (Android 15). Google Play requires `targetSdkVersion >= 34` for all new app submissions.
- **Minimum SDK:** `26` (Android 8.0 Oreo). Covers 95.8%+ of active Android devices globally.
- **64-bit Requirement:** App contains no unmanaged 32-bit native libraries (`.so`); fully compliant with Google Play 64-bit architecture requirements.
- **Format:** Android App Bundle (`.aab`). Google Play requires `.aab` for all new applications.

### Release Build Optimization
- **R8 Minification:** `minifyEnabled true` removes unused classes and obfuscates code.
- **Resource Shrinking:** `shrinkResources true` strips unreferenced drawables and XML files.
- **Bundle Footprint:** `3.2 MB` (extremely fast download time for users on mobile networks).

---

## 3. Security Hardening & Vulnerability Remediation

The following hardening measures have been implemented in the codebase:

### 1. WebView Debugging Disabled in Release
```java
if (BuildConfig.DEBUG) {
    WebView.setWebContentsDebuggingEnabled(true);
} else {
    WebView.setWebContentsDebuggingEnabled(false);
}
```
*Prevents attackers or rogue apps from inspecting sensitive resident JWT tokens or local web storage via Chrome DevTools.*

### 2. Mixed Content Policy Locked
```java
if (BuildConfig.DEBUG) {
    settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
} else {
    settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
}
```
*Guarantees no insecure HTTP requests are executed from within the WebView in production.*

### 3. Backup Extraction Disabled
```xml
<application
    android:allowBackup="false"
    android:usesCleartextTraffic="false" ...>
```
*Prevents extraction of cached authentication tokens via USB debugging (`adb backup`).*

### 4. Production API Host Resolution
In `android/app/build.gradle`:
- `release` build variant injects `API_BASE_URL = "https://api.samaaj.in/api/v1"`
- `debug` build variant injects `API_BASE_URL = "http://10.0.2.2:8081/api/v1"`
- In `assets/www/app.js`: Automatically detects and uses `window.AndroidBridge.getBackendHost()` on physical devices.

---

## 4. Google Play Console Administrative Checklist

Before submitting to the Closed Testing, Open Testing, or Production track:

### A. Privacy Policy (Mandatory)
- Host a public privacy policy accessible without requiring login (e.g., `https://samaaj.in/privacy`).
- Must disclose:
  - Account information collected (Name, Email, Flat Number).
  - Use of camera (used exclusively for photographing receipts/maintenance complaints).
  - Data retention policies.

### B. User Account Deletion (Google Play Policy Requirement)
- Google Play mandates that apps with account creation provide:
  1. An in-app account deletion option (located under Resident Profile / Settings).
  2. A public web link for requesting account deletion without reinstalling the app (e.g. `https://samaaj.in/delete-account`).
- The Samaaj backend already provides `DELETE /api/v1/users/me` and administrative deletion logic.

### C. Data Safety Form Responses
| Data Type | Collected? | Shared? | Purpose |
|---|---|---|---|
| **Name & Email** | Yes | No | Account management, authentication |
| **Address / Flat Unit** | Yes | No | App functionality (society resident verification) |
| **Photos / Receipts** | Yes (Optional) | No | Complaint ticketing & proof of maintenance payment |
| **Financial Info** | No | No | App delegates UPI payment directly to UPI apps via intents; does not store card/bank numbers |
| **Device Identifiers** | No | No | N/A |

### D. Required Visual Store Assets
| Asset | Dimensions | Specs |
|---|---|---|
| **App Icon** | 512 x 512 px | 32-bit PNG with alpha, max 1024 KB |
| **Feature Graphic** | 1024 x 500 px | JPEG or 24-bit PNG (no alpha) |
| **Phone Screenshots** | Min 2, Max 8 | 16:9 or 9:16 aspect ratio (e.g. 1080 x 2400 px) |
| **Short Description** | Max 80 chars | e.g., *"Modern residential society management: bills, complaints & community groups."* |
| **Full Description** | Max 4000 chars | Comprehensive overview of features (E2EE chats, online billing, complaints). |

---

## 5. Deployment Step: Uploading to Google Play

1. Log in to [Google Play Console](https://play.google.com/console).
2. Create app: **Samaaj** -> App -> Free -> Confirm policies.
3. Complete **App Content** questionnaires (Data Safety, Privacy Policy, Target Audience, News App, Financial Features).
4. Navigate to **Testing -> Closed testing** (or **Production**).
5. Create a new release and upload:
   ```
   android/app/build/outputs/bundle/release/app-release.aab
   ```
6. Google Play App Signing will automatically register the certificate.
7. Click **Save** -> **Review release** -> **Start rollout**.
