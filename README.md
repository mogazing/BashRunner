⚡ Bash Runner for Android

A lightweight, high-performance, standalone script launcher for Android designed to run local .sh and .bash scripts instantly with zero cloud dependencies.

✨ Features

🚀 One-Tap Instant Execution: Run scripts on demand without confirmation popups, extra dependencies, or cloud syncing.

⚙️ Dual Execution Modes: Supports standard sandboxed USER Mode as well as elevated ROOT Mode (su) for advanced system tasks.

🧰 Bundled Runtime Engine: Includes integrated GNU Bash and BusyBox environments so complex scripts execute reliably on modern Android.

📂 File Picker & Local Search: Quickly pick .sh files directly from local storage or search local paths on-device.

📊 Real-Time Console: Live monospaced stdout/stderr log viewer with exit code tracking, execution duration counters, and quick status badges.

⚡ 100% Standalone & Private: Zero background telemetry or remote server connections—completely private, local, and offline.

🛠️ Usage Quickstart

Launch Bash Runner on your Android device.

Tap Select .sh to pick a script from your storage.

Select your desired execution mode (USER or ROOT) in Settings & Diagnostics.

Tap Run to execute instantly.

Sample Diagnostic Script

#!/system/bin/sh

echo "=========================================="
echo "    BASH RUNNER DIAGNOSTIC TEST"
echo "=========================================="
echo "Timestamp : $(date)"
echo "Device    : $(getprop ro.product.model)"
echo "Android   : $(getprop ro.build.version.release)"
echo "------------------------------------------"
echo "Current Process UID / User:"
id
echo "=========================================="


📥 Installation

Download the latest app-release.apk from the Releases tab.

Install the APK on your Android device.

Grant standard storage access permissions when selecting local files.

📄 License

This project is licensed under the MIT License - see the LICENSE file for details.
