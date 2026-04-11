# NammaSpend AI

An offline-first Android expense tracking app with AI-powered voice input and automatic SMS transaction detection.

## Features

### Core Features
- **SMS Auto-Tracking**: Automatically detects and records transactions from bank SMS
- **Voice Input**: Speak your expenses in natural language, AI converts to structured data
- **Manual Entry**: Quick manual expense/income entry with category selection
- **Offline First**: All data stored locally using Room database
- **No Backend**: Works completely offline, no server required

### Analytics & Insights
- **Pie Charts**: Category-wise expense breakdown
- **Bar Charts**: Monthly income vs expense trends
- **Top Categories**: Quick view of highest spending areas
- **Predictions**: Simple trend-based predictions for recurring bills

### Data Management
- **CSV Export**: Export transactions to CSV format
- **Excel Export**: Export to .xlsx format with formatting
- **SMS Import**: Bulk import historical SMS transactions
- **Secure**: App lock with PIN and biometric authentication

## Tech Stack

- **Language**: Kotlin
- **Architecture**: MVVM with Repository pattern
- **DI**: Hilt
- **Database**: Room
- **Networking**: Retrofit + OkHttp
- **Charts**: MPAndroidChart
- **Export**: OpenCSV, Apache POI

## API Configuration

The app uses Groq API for voice-to-expense parsing. To configure:

1. Get your API key from [Groq Console](https://console.groq.com)
2. Add it to your GitHub repository secrets as `GROQ_API_KEY`
3. Or create `local.properties` file with:
   ```
   GROQ_API_KEY=your_api_key_here
   ```

## Building

### Local Build
```bash
./gradlew assembleDebug    # Debug APK
./gradlew assembleRelease  # Release APK
```

### GitHub Actions
The repository includes GitHub Actions workflows that automatically build APKs on push/PR. Built artifacts are available in the Actions tab.

## Project Structure

```
NammaSpendAI/
├── app/
│   ├── src/main/java/com/nammaspendai/
│   │   ├── data/
│   │   │   ├── model/          # Data classes
│   │   │   ├── dao/            # Room DAOs
│   │   │   ├── local/          # Database
│   │   │   ├── remote/         # API
│   │   │   └── repository/     # Repositories
│   │   ├── di/                 # Hilt modules
│   │   ├── ui/                 # UI organized by feature
│   │   │   ├── home/
│   │   │   ├── addexpense/
│   │   │   ├── insights/
│   │   │   └── settings/
│   │   ├── util/               # Utilities
│   │   └── receiver/           # SMS receiver
│   └── src/main/res/           # Android resources
├── .github/workflows/          # CI/CD
└── gradle/                     # Gradle wrapper
```

## Permissions

The app requires the following permissions:
- `READ_SMS`: To read bank SMS
- `RECEIVE_SMS`: To detect new transactions in real-time
- `RECORD_AUDIO`: For voice input
- `USE_BIOMETRIC`: For app lock

## License

MIT License

## Privacy

All data is stored locally on your device. No data is sent to any server except for voice processing via Groq API (only when you use voice input).
