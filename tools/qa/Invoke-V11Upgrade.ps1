[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][string]$Adb,
    [Parameter(Mandatory = $true)][string]$Serial,
    [Parameter(Mandatory = $true)][ValidateRange(10, 999)][int]$UserId,
    [Parameter(Mandatory = $true)][string]$V1Apk,
    [Parameter(Mandatory = $true)][string]$V11Apk,
    [Parameter(Mandatory = $true)][string]$InstrumentationApk,
    [string]$EvidenceDirectory = '.build-outputs/v1.1/upgrade'
)
$ErrorActionPreference = 'Stop'
$package = 'com.steadfast.app'
$component = 'com.steadfast.app.test/com.steadfast.app.smoke.OptimizedSmokeRunner'
if ($Serial -notmatch '^emulator-\d+$') { throw 'This harness accepts an explicitly selected emulator serial only.' }
foreach ($path in @($Adb, $V1Apk, $V11Apk, $InstrumentationApk)) {
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) { throw "Missing input file: $path" }
}
$expectedV1Hash = '0876b06211c66d9617a51c855b6bb28eac9ead61e4d05a2c3ad37ed55373a6f3'
if ((Get-FileHash -LiteralPath $V1Apk -Algorithm SHA256).Hash.ToLowerInvariant() -ne $expectedV1Hash) {
    throw 'V1 APK does not match the delivered private optimized universal baseline.'
}
New-Item -ItemType Directory -Force -Path $EvidenceDirectory | Out-Null

function Invoke-UpgradeAdb {
    param([string[]]$AdbArguments, [string]$LogName)
    $output = @(& $Adb -s $Serial @AdbArguments 2>&1)
    $exitCode = $LASTEXITCODE
    $text = $output -join "`n"
    if ($LogName) { $text | Set-Content -LiteralPath (Join-Path $EvidenceDirectory $LogName) -Encoding utf8 }
    if ($exitCode -ne 0 -or $text -match 'INSTALL_FAILED|Failure \[|Error:|INSTRUMENTATION_FAILED') {
        throw "adb failed ($exitCode): $text"
    }
    return $text
}
function Stop-UpgradeApp {
    Invoke-UpgradeAdb -AdbArguments @('shell', 'am', 'force-stop', '--user', "$UserId", $package) | Out-Null
}
function Invoke-UpgradeMode {
    param([string]$Mode)
    Stop-UpgradeApp
    $text = Invoke-UpgradeAdb -AdbArguments @('shell', 'am', 'instrument', '--user', "$UserId", '-w', '-r', '-e', 'mode', $Mode,
        '-e', 'fixtureUser', "$UserId", $component) -LogName "$Mode.log"
    if ($text -notmatch "OK ${Mode}:" -or $text -notmatch 'INSTRUMENTATION_CODE: -1') {
        throw "Upgrade assertion failed in ${Mode}: $text"
    }
    Write-Host "Passed $Mode"
}

$state = Invoke-UpgradeAdb -AdbArguments @('get-state')
if ($state.Trim() -ne 'device') { throw 'Selected emulator is not online.' }
$qemu = Invoke-UpgradeAdb -AdbArguments @('shell', 'getprop', 'ro.kernel.qemu')
if ($qemu.Trim() -ne '1') { throw 'Selected Android target is not an emulator.' }
$currentUser = Invoke-UpgradeAdb -AdbArguments @('shell', 'am', 'get-current-user')
if ($currentUser.Trim() -ne "$UserId") { throw 'Switch to and unlock the isolated fixture user before running the UI regression.' }

# Every package operation preserves data. Never add uninstall, clear, wipe, -d, or adb root here.
Stop-UpgradeApp
Invoke-UpgradeAdb -AdbArguments @('install', '--user', "$UserId", '-r', $V1Apk) -LogName 'install-v1.log' | Out-Null
Invoke-UpgradeAdb -AdbArguments @('install', '--user', "$UserId", '-r', '-t', $InstrumentationApk) -LogName 'install-test.log' | Out-Null
Invoke-UpgradeAdb -AdbArguments @('shell', 'am', 'start', '--user', "$UserId", '-W', '-n', "$package/.MainActivity") -LogName 'bootstrap-v1.log' | Out-Null
Start-Sleep -Seconds 3
Invoke-UpgradeMode 'upgradeSeed'
Invoke-UpgradeMode 'upgradeCheckV1'
Stop-UpgradeApp
Invoke-UpgradeAdb -AdbArguments @('install', '--user', "$UserId", '-r', $V11Apk) -LogName 'install-v11.log' | Out-Null
Invoke-UpgradeMode 'upgradeVerify'
Invoke-UpgradeMode 'upgradeUnlockFixture'
Invoke-UpgradeMode 'upgradeFeatures'
Invoke-UpgradeMode 'upgradeFeaturesRelaunch'
Stop-UpgradeApp
Write-Host "Upgrade regression passed. Logs: $EvidenceDirectory"
