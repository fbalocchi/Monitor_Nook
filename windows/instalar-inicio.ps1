# Windows PowerShell5.1. APK update and UTC clock only; private settings preserved.
[CmdletBinding()]
param(
 [Parameter(Mandatory=$true)][ValidatePattern('^[A-Za-z0-9._:-]+$')][string]$Serial,
 [string]$AdbPath = "$env:USERPROFILE\Downloads\platform-tools-latest-windows\platform-tools\adb.exe"
)
$ErrorActionPreference = 'Stop'
$Package = 'cl.balocchi.nookpanel'
$Apk = Join-Path (Split-Path $PSScriptRoot -Parent) 'releases\Panel-NOOK-1.1.3.apk'
$ExpectedHash = '28a1dded7f60f44632331b0e1806d10b84ab6d7b8821975d61b4d3e616fe5ddf'
$Remote = '/data/local/tmp/nook-custom-' + [Guid]::NewGuid().ToString('N') + '.apk'
$Encoding = New-Object Text.UTF8Encoding($false)
$ReportDir = Join-Path "$env:USERPROFILE\Downloads" ('NOOK-custom-instalacion-' + (Get-Date -Format 'yyyyMMdd-HHmmss'))
$null = New-Item -ItemType Directory -Path $ReportDir -Force
$Stage = 'Conexion'
. (Join-Path $PSScriptRoot 'native-functions.ps1')
try {
 if (-not (Test-Path -LiteralPath $AdbPath)) { throw 'No se encontro adb.exe.' }
 if (-not (Test-Path -LiteralPath $Apk)) { throw 'Extrae TODO el ZIP antes de ejecutar.' }
 if ((Get-FileHash -LiteralPath $Apk -Algorithm SHA256).Hash.ToLowerInvariant() -ne $ExpectedHash) { throw 'El APK no coincide con el paquete comprobado.' }
 if ((Adb -ArgumentList @('get-state')).Trim() -ne 'device') { throw 'Conecta el NOOK iniciado en su sistema habitual, con ADB USB activo.' }
 if ((Shell 'id') -notmatch '\buid=0\b') { throw 'Se esperaba el ADB root que ya habilitamos.' }
 if ((Shell 'getprop ro.build.version.sdk').Trim() -ne '7') { throw 'Esta version requiere el NOOK Android2.1/API7.' }
 Write-Host '[1/4] Comprobando fecha para HTTPS...'
 $Stage = 'Fecha'
 $unix = [long](Shell 'TZ=UTC /system/bin/date -u +%s')
 $epoch = [DateTime]::SpecifyKind([DateTime]'1970-01-01', [DateTimeKind]::Utc)
 $pcUnix = [long][Math]::Floor(((Get-Date).ToUniversalTime() - $epoch).TotalSeconds)
 if ([Math]::Abs($pcUnix - $unix) -gt 120) {
  $stamp = (Get-Date).ToUniversalTime().ToString('yyyyMMdd.HHmmss', [Globalization.CultureInfo]::InvariantCulture)
  $null = Shell "TZ=UTC /system/bin/date -s $stamp"
  $unix = [long](Shell 'TZ=UTC /system/bin/date -u +%s')
  $pcUnix = [long][Math]::Floor(((Get-Date).ToUniversalTime() - $epoch).TotalSeconds)
  if ([Math]::Abs($pcUnix - $unix) -gt 120) { throw 'No se pudo comprobar el reloj del NOOK.' }
 }
 Write-Host '[2/4] Copiando Panel NOOK Custom1.1.3...'
 $Stage = 'Copia APK'
 Write-Host (Adb -ArgumentList @('push','-Z',$Apk,$Remote))
 Write-Host '[3/4] Actualizando la app. Maximo120 segundos...'
 $Stage = 'Instalacion'
 $install = Shell "pm install -r $Remote" -TimeoutSeconds 120
 [IO.File]::WriteAllText((Join-Path $ReportDir 'instalacion.txt'),$install,$Encoding)
 Write-Host $install
 if ($install -notmatch '(?m)^Success\s*$') { throw 'Android no confirmo la instalacion.' }
 Write-Host '[4/4] Abriendo el panel...'
 $Stage = 'Apertura inicial'
 $launch = Shell "am start -f 0x14000000 -n $Package/.MainActivity"
 [IO.File]::WriteAllText((Join-Path $ReportDir 'apertura.txt'),$launch,$Encoding)
 if ($launch -match '(?i)(error:|exception)') { throw 'Android no pudo abrir el panel.' }
 Start-Sleep -Seconds 4
 $processes = Shell 'ps'
 [IO.File]::WriteAllText((Join-Path $ReportDir 'procesos.txt'),$processes,$Encoding)
 if ($processes -notmatch ('(?m)\b' + [regex]::Escape($Package) + '\s*$')) { throw 'El proceso no permanece abierto tras la comprobacion inicial.' }
 Write-Host ''
 Write-Host 'APP ACTUALIZADA Y ABIERTA EN LA COMPROBACION INICIAL.'
 Write-Host 'Se conserva la configuracion que ya tenias, incluido el enlace de Google.'
 Write-Host 'Botones inferiores: Panel, Agenda, Notas y Ajustes.'
 Write-Host 'Ajustes permite cambiar Wi-Fi, clima, Calendar y frecuencia.'
 Write-Host 'Reloj HH:mm en la esquina superior derecha, con hora de Chile.'
 Write-Host 'El boton ACTUALIZAR queda junto a la ubicacion. La hora cambia cada minuto.'
 Write-Host 'Si vuelve al menu NOOK, ejecuta diagnosticar-inicio.ps1 y adjunta el ZIP.'
}
catch {
 $errorText = 'DETENIDO EN ' + $Stage + ': ' + $_.Exception.Message
 Write-Host $errorText
 [IO.File]::WriteAllText((Join-Path $ReportDir 'error.txt'),$errorText,$Encoding)
 foreach ($name in @('crash.txt','activity.log','calendar-error.txt','calendar-tls.txt')) {
  try { $data = Adb -ArgumentList @('shell',"cat /data/data/$Package/files/$name") -TimeoutSeconds 10; [IO.File]::WriteAllText((Join-Path $ReportDir $name),$data,$Encoding) } catch {}
 }
 try { Compress-Archive -Path (Join-Path $ReportDir '*') -DestinationPath ($ReportDir + '.zip') -Force; Write-Host ('Adjunta: ' + $ReportDir + '.zip') } catch { Write-Host ('Registro en: ' + $ReportDir) }
 exit 1
}
finally { try { $null = Adb -ArgumentList @('shell',"rm $Remote") -TimeoutSeconds 10 } catch {} }
