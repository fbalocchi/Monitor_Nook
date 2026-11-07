# Bounded collection of sanitised app diagnostics, clock and memory.
[CmdletBinding()]
param(
 [Parameter(Mandatory=$true)][ValidatePattern('^[A-Za-z0-9._:-]+$')][string]$Serial,
 [string]$AdbPath = "$env:USERPROFILE\Downloads\platform-tools-latest-windows\platform-tools\adb.exe"
)
$ErrorActionPreference = 'Stop'
$Package = 'cl.balocchi.nookpanel'
$Encoding = New-Object Text.UTF8Encoding($false)
$ReportDir = Join-Path "$env:USERPROFILE\Downloads" ('NOOK-custom-registro-' + (Get-Date -Format 'yyyyMMdd-HHmmss'))
$null = New-Item -ItemType Directory -Path $ReportDir -Force
. (Join-Path $PSScriptRoot 'native-functions.ps1')
if (-not (Test-Path -LiteralPath $AdbPath)) { throw 'No se encontro adb.exe.' }
if ((Adb -ArgumentList @('get-state')).Trim() -ne 'device') { throw 'Conecta el NOOK encendido normalmente por USB.' }
[IO.File]::WriteAllText((Join-Path $ReportDir 'pc-utc.txt'),(Get-Date).ToUniversalTime().ToString('o'),$Encoding)
foreach ($name in @('state.json','crash.txt','activity.log','weather-error.txt','calendar-error.txt','agenda-error.txt','panel-calendar-error.txt','weather-tls.txt','calendar-tls.txt')) {
 try { $data = Adb -ArgumentList @('shell',"cat /data/data/$Package/files/$name") -TimeoutSeconds 15; [IO.File]::WriteAllText((Join-Path $ReportDir $name),$data,$Encoding) } catch {}
}
foreach ($item in @(@('nook-utc.txt','date -u'),@('procesos.txt','ps'),@('memoria.txt',"dumpsys meminfo $Package"))) {
 try { $data = Adb -ArgumentList @('shell',$item[1]) -TimeoutSeconds 15; [IO.File]::WriteAllText((Join-Path $ReportDir $item[0]),$data,$Encoding) } catch {}
}
try {
 $raw = Adb -ArgumentList @('logcat','-d','-v','brief','ActivityManager:I','lowmemorykiller:I','*:S') -TimeoutSeconds 20
 $filtered = @($raw -split "`r?`n" | Where-Object { $_ -match 'cl\.balocchi\.nookpanel|lowmemorykiller|Low Memory' }) -join "`r`n"
 [IO.File]::WriteAllText((Join-Path $ReportDir 'sistema-filtrado.txt'),$filtered,$Encoding)
} catch {}
Compress-Archive -Path (Join-Path $ReportDir '*') -DestinationPath ($ReportDir + '.zip') -Force
Write-Host ('Adjunta este archivo: ' + $ReportDir + '.zip')
Write-Host 'Incluye estado, errores de clases, memoria y actividad; no copia configuracion, calendario ni notas.'
