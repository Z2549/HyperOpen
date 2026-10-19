param(
    [string]$Target = "10.0.0.209:43305",
    [string]$ApkPath = "$PSScriptRoot\app\build\outputs\apk\release\app-release.apk",
    [switch]$BumpVersion = $false
)

Write-Host "=== 正在准备编译并推送最新 Release APK 到设备: $Target ===" -ForegroundColor Cyan

# 自动管理版本号 (从 0.10 开始，每次构建小版本递增 0.01)
$gradleFile = "$PSScriptRoot\app\build.gradle.kts"
if (Test-Path $gradleFile) {
    $content = Get-Content $gradleFile -Raw
    if ($content -match 'versionName\s*=\s*"(\d+\.\d+)"') {
        $currentVerStr = $matches[1]
        if ($BumpVersion) {
            $newVer = [double]$currentVerStr + 0.01
            $newVerStr = "{0:N2}" -f $newVer
            $newCode = [int]($newVer * 100)
            $content = $content -replace 'versionCode\s*=\s*\d+', "versionCode = $newCode"
            $content = $content -replace 'versionName\s*=\s*"\d+\.\d+"', "versionName = `"$newVerStr`""
            Set-Content -Path $gradleFile -Value $content -NoNewline
            Write-Host "已自动递增版本号: $currentVerStr -> $newVerStr (Code: $newCode)" -ForegroundColor Magenta
        } else {
            Write-Host "当前版本号: $currentVerStr" -ForegroundColor Magenta
        }
    }
}

# 强制清理旧构建产物并重新执行 assembleRelease
if (Test-Path $ApkPath) {
    Remove-Item $ApkPath -Force
}

Write-Host "正在执行 assembleRelease 全量编译构建..." -ForegroundColor Yellow
& "$PSScriptRoot\gradlew.bat" assembleRelease --no-daemon
if ($LASTEXITCODE -ne 0) {
    Write-Error "编译构建失败，终止发送。"
    exit 1
}

# 1. 尝试直接通过 IP:Port 连接
Write-Host "尝试连接目标端口 $Target ..."
& adb connect $Target

# 2. 检查可用设备列表
$devices = & adb devices | Select-String -Pattern "device$" | ForEach-Object { ($_ -split "`t")[0] }

if ($devices.Count -eq 0) {
    Write-Error "未检测到任何通过 ADB 连接的设备，请确认手机已开启无线调试且局域网互通。"
    exit 1
}

Write-Host "检测到已连接设备: $($devices -join ', ')" -ForegroundColor Green

# 3. 执行安装 (优先通过 /data/local/tmp 结合 root 权限静默安装，彻底规避系统弹窗拦截)
Write-Host "正在通过 ADB 将 APK 发送至设备临时目录并静默安装..." -ForegroundColor Cyan
& adb push $ApkPath "/data/local/tmp/app-release.apk"
$installOutput = & adb shell "su -c 'pm install -r -d /data/local/tmp/app-release.apk'"
if ($installOutput -notmatch "Success") {
    Write-Host "Root 静默安装回退尝试: $installOutput，改用普通 adb install..." -ForegroundColor Yellow
    & adb install -r -d $ApkPath
} else {
    Write-Host "静默安装成功: $installOutput" -ForegroundColor Green
}

# 4. 同步推送文件至手机下载目录
Write-Host "正在将 APK 文件推送到手机 /sdcard/Download/ ..." -ForegroundColor Cyan
& adb push $ApkPath "/sdcard/Download/HyperOS_QuickOpen_release.apk"

# 5. 实时同步手机系统当前桌面真实壁纸到 App 私有目录，供预览界面 100% 真实呈现
Write-Host "正在同步手机真实桌面壁纸供预览使用..." -ForegroundColor Cyan
& adb shell "su -c 'mkdir -p /data/user/0/com.hyperos.quickopen/files && cp /data/system/users/0/wallpaper /data/user/0/com.hyperos.quickopen/files/desktop_wallpaper.jpg && chmod 666 /data/user/0/com.hyperos.quickopen/files/desktop_wallpaper.jpg'"

# 5. 软重启 SystemUI 进程以使 Xposed Hook 即时生效 (优先使用 su 权限)
Write-Host "正在软重启 SystemUI 进程以重新加载模块 Hook..." -ForegroundColor Yellow
& adb shell "su -c 'pkill -f com.android.systemui' || pkill -f com.android.systemui"

Write-Host "=== 发送完成！手机已成功安装最新构建版本并重载 SystemUI ===" -ForegroundColor Green
