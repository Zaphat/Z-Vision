## 🧪 Running the App on a Physical Android Device

### 📋 Prerequisites
- Android Studio installed
- USB Debugging enabled on your device:
  - Go to **Settings > About phone**
  - Tap **Build number** 7 times to enable **Developer options**
  - Go to **Developer options** → Enable **USB debugging**
- USB cable connected to your PC

---

### 🔧 ADB Setup (Android Debug Bridge)

1. Locate the `adb` binary:
   - Typically at:
     ```
     C:\Users\<YourUsername>\AppData\Local\Android\Sdk\platform-tools
     ```

2. Add `platform-tools` to your system PATH:
   - **Windows**:
     - Open **System Properties > Environment Variables**
     - Under **System variables**, edit the `Path`
     - Add the path to `platform-tools`
     - Restart your terminal
   - **macOS/Linux**:
     ```bash
     echo 'export PATH=$PATH:$HOME/Library/Android/sdk/platform-tools' >> ~/.zshrc
     source ~/.zshrc
     ```

3. Verify `adb`:
   ```bash
   adb devices
   ```
