# 💰 Budget Tracker

<p align="center">
  <img src="docs/images/app_icon.png" alt="Budget Tracker Logo" width="120"/>
</p>

<p align="center">
  <strong>A modern, privacy-first personal finance app for Android</strong>
</p>

<p align="center">
  <a href="#features">Features</a> •
  <a href="#screenshots">Screenshots</a> •
  <a href="#architecture">Architecture</a> •
  <a href="#getting-started">Getting Started</a> •
  <a href="#tech-stack">Tech Stack</a> •
  <a href="#contributing">Contributing</a>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-green.svg" alt="Platform"/>
  <img src="https://img.shields.io/badge/Kotlin-1.9+-purple.svg" alt="Kotlin"/>
  <img src="https://img.shields.io/badge/Jetpack%20Compose-Material3-blue.svg" alt="Compose"/>
  <img src="https://img.shields.io/badge/Min%20SDK-30-orange.svg" alt="Min SDK"/>
  <img src="https://img.shields.io/badge/License-Apache%202.0-blue.svg" alt="License"/>
</p>

---

## 🌟 Overview

Budget Tracker is a **cyberpunk-themed** personal finance application that helps users manage their daily spending through an innovative "Vitality Rings" system inspired by Apple Watch fitness rings. Unlike traditional budget apps that shame users for overspending, Budget Tracker takes a **non-judgmental approach** with gentle nudges and positive reinforcement.

### Philosophy

- **Privacy First**: All data stays on your device. No cloud sync, no tracking, no ads.
- **Non-Judgmental**: Instead of "Over Budget!", we show "Adjusted" with helpful guidance.
- **Gamification**: Earn credits for logging expenses, build streaks, and unlock achievements.
- **Empathy-Driven**: Features like "Ghost Mode" for mental health breaks and "Didn't Buy" celebrations.

---

## ✨ Features

### 🎯 Core Features

#### Vitality Rings System
Three concentric rings visualize your financial health at a glance:
- **Pulse Ring** (Outer) - Daily spending limit tracking with rolling budget adjustment
- **Shield Ring** (Middle) - Monthly savings progress with emergency expense protection
- **Clarity Ring** (Inner) - Bill payment tracking with variable bill support

#### Smart Daily Allowance
```
Daily Allowance = (Income - Fixed Expenses - Savings Goal) / Days Until Payday
```
The app dynamically recalculates your safe-to-spend amount based on your spending patterns.

#### Automatic Expense Tracking
- Reads bank notifications to automatically log transactions
- Manual quick-add with predictive category suggestions based on time of day
- Support for multiple currencies (RSD, EUR, USD, BAM)

### 📊 Analytics & Insights

- **Spending Velocity Chart** - Compare current vs previous month spending pace
- **Top Leaks Analysis** - Identify categories draining your budget
- **Consistency Calendar** - Visual heatmap of daily spending habits
- **Custom Canvas Charts** - No pie charts! Clean, modern visualizations

### 🔔 Smart Notifications (WorkManager)

Three daily check-ins act as your financial coach:
- **Morning Briefing (08:00)** - "Good morning! You have X safe to spend today"
- **Danger Zone Alert (14:00)** - Only triggers if >80% of daily budget spent
- **Victory Lap (21:00)** - End-of-day summary with streak updates

### 💡 Empathy Features

#### Ghost Mode
Take a break from tracking without guilt. Rings fade out, no notifications, just peace.

#### "Didn't Buy" Feature
Celebrate **not** buying something! Log items you resisted and watch your micro-savings grow.

#### Emergency Expenses
Mark unexpected expenses as "emergency" - they're deducted from savings instead of affecting your daily budget.

### 📋 Bill Manager

Full bill tracking system replacing simple fixed expenses:
- Add individual bills with names, amounts, and due dates
- Mark bills as paid with actual amounts
- **Variable Bill Logic**:
  - Paid less than estimated? Difference goes to savings! 🎉
  - Paid more? Excess is spread across remaining daily budgets

---

## 📱 Screenshots

<p align="center">
  <i>Screenshots coming soon</i>
</p>

<!-- 
<p align="center">
  <img src="docs/images/screenshot_home.png" width="200"/>
  <img src="docs/images/screenshot_insights.png" width="200"/>
  <img src="docs/images/screenshot_bills.png" width="200"/>
</p>
-->

---

## 🏗️ Architecture

Budget Tracker follows **Clean Architecture** principles with **MVVM** pattern:

```
app/
├── data/
│   ├── local/
│   │   ├── dao/          # Room DAOs
│   │   ├── entity/       # Room Entities
│   │   └── BudgetTrackerDatabase.kt
│   ├── repository/       # Repository Implementations
│   └── preferences/      # DataStore Preferences
├── domain/
│   ├── model/            # Domain Models
│   └── repository/       # Repository Interfaces
├── presentation/
│   ├── dashboard/        # Main screen with Vitality Rings
│   ├── onboarding/       # Setup wizard
│   ├── settings/         # App configuration
│   ├── insights/         # Analytics screens
│   ├── history/          # Transaction history
│   └── components/       # Reusable UI components
├── navigation/           # Compose Navigation
├── notification/         # WorkManager workers
├── di/                   # Hilt modules
└── ui/theme/             # Material3 theming
```

### Key Design Decisions

1. **Single Source of Truth**: Room database is the only source of truth for all financial data
2. **Reactive Streams**: Kotlin Flow for all data observation
3. **Dependency Injection**: Hilt for compile-time DI
4. **Modular ViewModels**: Each screen has its own ViewModel with clear responsibilities

---

## 🛠️ Tech Stack

| Category | Technology |
|----------|------------|
| **Language** | Kotlin 1.9+ |
| **UI Framework** | Jetpack Compose with Material3 |
| **Architecture** | MVVM + Clean Architecture |
| **DI** | Hilt |
| **Database** | Room (SQLite) |
| **Preferences** | DataStore |
| **Async** | Kotlin Coroutines + Flow |
| **Background Work** | WorkManager |
| **Navigation** | Compose Navigation |
| **Build System** | Gradle with Kotlin DSL |

---

## 🚀 Getting Started

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Android SDK 34
- Min SDK 26 (Android 8.0)

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/BudgetTracker.git
   cd BudgetTracker
   ```

2. **Open in Android Studio**
   - File → Open → Select the project directory

3. **Build the project**
   ```bash
   ./gradlew assembleDebug
   ```

4. **Run on device/emulator**
   - Select a device and click Run (▶️)

### Permissions

The app requires the following permissions:
- `NOTIFICATION_LISTENER_SERVICE` - To read bank notifications for automatic expense tracking
- `POST_NOTIFICATIONS` - To send smart notification reminders

---

## 📁 Project Structure

### Database Schema (Version 4)

```kotlin
@Database(
    entities = [
        TransactionEntity::class,
        BudgetEntity::class,
        UserProfileEntity::class,
        UserBudgetEntity::class,
        FixedBillEntity::class  // New in v4
    ],
    version = 4
)
```

### Key Models

| Model | Purpose |
|-------|---------|
| `UserBudget` | Core budget configuration (income, pay day, savings goal) |
| `FixedBill` | Individual recurring bill with payment tracking |
| `Transaction` | Income/expense record with category and source |
| `VitalityRingsState` | UI state for the three-ring visualization |

---

## 🎨 Design System

### Color Palette

The app uses a cyberpunk-inspired dark theme:

| Color | Hex | Usage |
|-------|-----|-------|
| Cyan | `#56CCF2` | Primary accent, Pulse ring |
| Purple | `#9C27B0` | Secondary accent, Shield ring |
| Green | `#00E676` | Success states, savings |
| Orange | `#FF9800` | Warnings, adjustments |
| Background | `#1A1A2E` | Dark gradient base |

### Custom Components

- **VitalityRings** - Canvas-drawn concentric progress rings with glow effects
- **MiniVitalityRings** - Compact version for cards and headers
- **QuickAddTransactionSheet** - Bottom sheet with predictive categories
- **BillChecklistDialog** - Interactive bill payment manager

---

## 🧪 Testing

```bash
# Run unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest

# Run all checks
./gradlew check
```

---

## 🗺️ Roadmap

### Planned Features

- [ ] **Widgets** - Home screen widgets for quick glance at daily budget
- [ ] **Export/Import** - Backup and restore data
- [ ] **Recurring Transactions** - Automatic logging of subscriptions
- [ ] **Budget Categories** - Set limits per spending category
- [ ] **Dark/Light Theme** - Theme switching support
- [ ] **Localization** - Multi-language support

### Future Considerations

- [ ] Optional cloud sync (privacy-preserving)
- [ ] Premium features for power users
- [ ] Family budget sharing

---

## 🤝 Contributing

Contributions are welcome! Please read our [Contributing Guidelines](CONTRIBUTING.md) before submitting PRs.

### Development Setup

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Code Style

- Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use meaningful commit messages
- Write documentation for public APIs
- Add tests for new features

---

## 📄 License

```
Copyright 2024-2025 GiboWorks

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

See [LICENSE](LICENSE) for the full license text.

---

## 🙏 Acknowledgments

- Inspired by Apple Watch Fitness Rings
- Material Design 3 guidelines by Google
- The Kotlin and Android community

---

<p align="center">
  Made with ❤️ by <a href="https://giboworks.site">GiboWorks</a>
</p>

<p align="center">
  <a href="https://github.com/yourusername/BudgetTracker/issues">Report Bug</a> •
  <a href="https://github.com/yourusername/BudgetTracker/issues">Request Feature</a>
</p>
