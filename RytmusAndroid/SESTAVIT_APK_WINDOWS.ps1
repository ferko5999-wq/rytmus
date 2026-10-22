$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $root

if (-not $env:JAVA_HOME) {
    $jbr = 'C:\Program Files\Android\Android Studio\jbr'
    if (Test-Path "$jbr\bin\java.exe") { $env:JAVA_HOME = $jbr }
}
if ($env:JAVA_HOME) { $env:PATH = "$env:JAVA_HOME\bin;" + $env:PATH }

$sdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } elseif ($env:ANDROID_SDK_ROOT) { $env:ANDROID_SDK_ROOT } else { Join-Path $env:LOCALAPPDATA 'Android\Sdk' }
if (-not (Test-Path $sdk)) { throw "Android SDK nebylo nalezeno: $sdk. Nejdrive nainstaluj Android Studio a SDK API 35." }
$forwardSdk = $sdk.Replace('\', '/')
Set-Content -Path (Join-Path $root 'local.properties') -Value "sdk.dir=$forwardSdk" -Encoding ASCII

$gradleVersion = '8.9'
$gradlePath = Join-Path $root ".gradle-portable\gradle-$gradleVersion"
$gradleBat = Join-Path $gradlePath 'bin\gradle.bat'
if (-not (Test-Path $gradleBat)) {
    $cache = Join-Path $root '.gradle-portable'
    New-Item -ItemType Directory -Path $cache -Force | Out-Null
    $zip = Join-Path $cache "gradle-$gradleVersion-bin.zip"
    Write-Host "Stahuji oficialni Gradle $gradleVersion..."
    Invoke-WebRequest -Uri "https://services.gradle.org/distributions/gradle-$gradleVersion-bin.zip" -OutFile $zip
    Expand-Archive -Path $zip -DestinationPath $cache -Force
    Remove-Item $zip
}
Write-Host 'Sestavuji debug APK...'
& $gradleBat --no-daemon assembleDebug
if ($LASTEXITCODE -ne 0) { throw "Gradle sestaveni skoncilo chybou $LASTEXITCODE" }
$apk = Join-Path $root 'app\build\outputs\apk\debug\app-debug.apk'
if (-not (Test-Path $apk)) { throw "APK se nenaslo na: $apk" }
Write-Host "APK hotovo: $apk"
