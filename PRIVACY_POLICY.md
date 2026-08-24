# Privacy Policy for WallosMobile

**Last Updated:** August 10, 2026

## Introduction

WallosMobile is an unofficial Android client for [Wallos](https://github.com/ellite/Wallos), a
self-hosted subscription tracker. This privacy policy explains how the app handles your
information.

## Data Collection

**We do not collect, store, or transmit any personal data to our servers.** WallosMobile does not
have any analytics, tracking, or telemetry systems.

## Data Usage

### Wallos Authentication
Wallos authenticates with a single API key, not a username/password session. When you connect the
app to your Wallos server:
- **We do not store your Wallos password**
- The API key is stored **locally on your device only**, encrypted using Android's Keystore-backed
  secure storage
- The key is used solely to authenticate requests to your chosen Wallos server
- All communication occurs directly between your device and your chosen Wallos server

### Local Storage
The app stores data locally on your device to provide functionality, including:
- The API key (encrypted, as above) and your Wallos server's URL
- Cached subscriptions, currencies, and other Wallos content
- App preferences and settings

This data remains on your device and is never transmitted to us.

## Third-Party Services

### Wallos
WallosMobile connects directly to a Wallos server (self-hosted, either yours or one you're given
access to). When using the app:
- Your data is transmitted directly to and from that server
- Whoever operates that server governs how your data is handled there
- We recommend reviewing your server operator's own privacy practices if you are not the operator

## Permissions

The app requires the following Android permissions:

- **INTERNET**: Required to communicate with your Wallos server
- **ACCESS_NETWORK_STATE**: Used to check if your device has an active internet connection

## Data Security

- All data is stored locally on your device using Android's secure storage mechanisms; the API key
  specifically is encrypted via the Android Keystore
- Communications with your Wallos server use HTTPS encryption (when supported by your server)
- We do not have access to your device data or Wallos credentials

## Data Backup

Android's automatic backup feature may back up app data to your Google Drive account. You can
disable this in your device's Android settings if desired.

## Children's Privacy

WallosMobile does not knowingly collect any information from children. The app is a productivity
tool intended for users of the Wallos subscription-tracking system.

## Changes to This Policy

We may update this privacy policy from time to time. Changes will be reflected in the app
repository and in app store listings. Continued use of the app after changes constitutes
acceptance of the updated policy.

## Open Source

WallosMobile is open-source software. You can review the source code at
https://github.com/Grigoriym/Wallosmobile to verify our privacy practices.

## Contact

For questions about this privacy policy or the app, please:
- Open an issue on the GitHub repository at https://github.com/Grigoriym/Wallosmobile
- Contact us at grappimapps@gmail.com

## Your Rights

Since we do not collect any data:
- There is no data for us to delete, modify, or export
- All your data resides locally on your device and on your Wallos server
- You have full control over your local data through your device settings

You can delete all app data by:
1. Uninstalling the app, or
2. Clearing the app's data in Android Settings → Apps → WallosMobile → Storage → Clear Data
