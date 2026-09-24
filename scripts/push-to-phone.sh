#!/usr/bin/env bash
# Copies the newest release APK from the Windows apks folder to the phone's Download folder over USB (MTP).
# Requires: phone plugged in with USB mode set to "File transfer". No USB debugging needed.
# Then on the phone: Files > Downloads > tap the APK > Install.
set -euo pipefail
DIR="C:\\Users\\nghho\\accountabilityApp\\apks"
APK=$(ls -t /mnt/c/Users/nghho/accountabilityApp/apks/ember-release-*.apk 2>/dev/null | head -1)
[ -n "$APK" ] || { echo "No APK in $DIR. Run scripts/deploy.sh first."; exit 1; }
NAME=$(basename "$APK")
powershell.exe -NoProfile -Command "
\$shell = New-Object -ComObject Shell.Application
\$dev = \$shell.NameSpace(17).Items() | Where-Object { \$_.Name -match 'Pixel' -or \$_.Type -match 'Portable|Mobile' } | Select-Object -First 1
if (-not \$dev) { Write-Output 'Phone not found. Plug it in and choose File transfer.'; exit 1 }
\$storage = \$dev.GetFolder.Items() | Select-Object -First 1
\$dl = \$storage.GetFolder.ParseName('Download')
\$src = \$shell.NameSpace('$DIR').ParseName('$NAME')
\$dl.GetFolder.CopyHere(\$src, 16)
Start-Sleep -Seconds 8
if (\$dl.GetFolder.ParseName('$NAME')) { Write-Output ('Copied ' + '$NAME' + ' to ' + \$dev.Name + '/Download') } else { Write-Output 'Copy did not complete'; exit 1 }
" | tr -d '\r'
