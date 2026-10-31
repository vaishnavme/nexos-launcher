# On-device ADB discovery

Run this on the T507 head unit **before** implementing real QN8035 tuner or AK7604 EQ control. Dock tiles currently only launch stock apps.

Connect USB debugging (or wireless ADB), then capture:

```bash
adb shell wm size
adb shell wm density
adb shell dumpsys display | grep -E "mBaseDisplayInfo|density"
adb shell getprop | grep -Ei "ro.product|ro.build|mcu|fyt|tw\.|xyauto"
adb shell pm list packages
adb shell cmd package query-activities -a android.intent.action.MAIN -c android.intent.category.LAUNCHER
```

## Display

Record `widthPixels`, `heightPixels`, and `densityDpi`. The launcher also logs these at boot (`adb logcat -s NexOS`) and shows them on the home canvas. Do not lock icon/font pixel sizes until this dump exists. Expected family: 800×480 or 1024×600; dpi is not reliably 160.

## Identify Radio / EQ / Bluetooth

From `pm list packages`, note packages whose names or labels contain `radio`, `fm`, `eq`, `dsp`, `bt`, or `bluetooth`. Then:

```bash
# Replace PACKAGE with the real name, e.g. com.tw.radio
adb shell pm path PACKAGE
adb pull /system/priv-app/.../PACKAGE.apk ./oem/
aapt dump xmltree PACKAGE.apk AndroidManifest.xml
aapt dump xmltree PACKAGE.apk --include-meta-data
```

Look for:

- Launcher activity (`MAIN` / `LAUNCHER`)
- Broadcast actions (`*.radio.*`, `*.eq.*`, `*.dsp.*`)
- Services that accept `startService` commands
- Custom permissions that would block a third-party launcher

Store the real package names in `OemPackages.kt` and only then implement `RadioGateway.tune()` / `seek()` / `presets()` or DSP sliders.

## What not to assume

Do not treat FYT `com.syu.radio`, Topway/NavRadio, or Allwinner `com.tw.radio` / `TWUtil` as present until this dump lists them. Direct I2C to QN8035/AK7604 and MCU UART (`android.tw.john.TWUtil`) require system UID or root and are out of scope.
