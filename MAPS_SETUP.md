# Google Maps Setup Instructions

## Getting Google Maps API Key

To use the location functionality in this app, you need to obtain a Google Maps API key:

### Step 1: Get API Key
1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select an existing one
3. Enable the following APIs:
   - Maps SDK for Android
   - Places API (if needed later)
   - Geocoding API (if needed later)

### Step 2: Create API Key
1. Go to "Credentials" in the Google Cloud Console
2. Click "Create Credentials" → "API Key"
3. Copy the generated API key

### Step 3: Restrict API Key (Recommended)
1. Click on your API key to edit it
2. Under "Application restrictions":
   - Select "Android apps"
   - Add package name: `com.tpl.hemen_lazim`
   - Add SHA-1 certificate fingerprint (get it from Android Studio or using keytool)

### Step 4: Add to Project
1. Open `app/src/main/res/values/google_maps_api.xml`
2. Replace `YOUR_API_KEY_HERE` with your actual API key

### Getting SHA-1 Fingerprint
Run this command in your project root:
```bash
./gradlew signingReport
```

Or in Android Studio:
1. Open Gradle panel
2. Navigate to: app → Tasks → android → signingReport
3. Copy the SHA1 fingerprint from the debug keystore

### Important Security Notes
- Never commit your API key to version control
- Consider using build variants and gradle properties for different environments
- Restrict your API key to prevent unauthorized usage
- Monitor your API usage in Google Cloud Console

## Testing Location Features
After setting up the API key:
1. Run the app on a physical device (emulator has limited GPS functionality)
2. Grant location permissions when prompted
3. Enable GPS when requested
4. Test the "Mevcut Konumumu Al" button
5. Test tapping on the map to select different locations


