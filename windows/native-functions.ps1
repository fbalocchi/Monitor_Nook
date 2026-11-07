function Start-AdbProcess {
 param([string[]]$Arguments)
 $info = New-Object Diagnostics.ProcessStartInfo
 $info.FileName = $AdbPath
 $allArguments = @('-s',$Serial) + $Arguments
 # All arguments are fixed options/paths; Windows paths cannot contain a double quote.
 $info.Arguments = ($allArguments | ForEach-Object { '"' + $_.Replace('"','\"') + '"' }) -join ' '
 $info.UseShellExecute = $false
 $info.CreateNoWindow = $true
 $info.RedirectStandardOutput = $true
 $info.RedirectStandardError = $true
 $info.StandardOutputEncoding = [Text.Encoding]::UTF8
 $info.StandardErrorEncoding = [Text.Encoding]::UTF8
 $process = New-Object Diagnostics.Process
 $process.StartInfo = $info
 [void]$process.Start()
 return [pscustomobject]@{ Process=$process; Out=$process.StandardOutput.ReadToEndAsync(); Err=$process.StandardError.ReadToEndAsync() }
}
function Finish-AdbProcess {
 param($Running,[int]$TimeoutSeconds = 15)
 $done = $false
 for ($second = 1; $second -le $TimeoutSeconds; $second++) {
  if ($Running.Process.WaitForExit(1000)) { $done = $true; break }
  if ($TimeoutSeconds -ge 30 -and ($second % 10) -eq 0) { Write-Host ("  Esperando respuesta... $second/$TimeoutSeconds s") }
 }
 if (-not $done) { try { $Running.Process.Kill() } catch {}; [void]$Running.Process.WaitForExit(5000) }
 [void]$Running.Out.Wait(5000); [void]$Running.Err.Wait(5000)
 $outText = if ($Running.Out.IsCompleted) { $Running.Out.Result } else { '[stdout incompleto]' }
 $errText = if ($Running.Err.IsCompleted) { $Running.Err.Result } else { '[stderr incompleto]' }
 $code = if ($Running.Process.HasExited) { $Running.Process.ExitCode } else { -1 }
 $Running.Process.Dispose()
 return [pscustomobject]@{ TimedOut=(-not $done); Code=$code; Text=($outText + "`n" + $errText).Trim() }
}
function Adb {
    param([string[]]$ArgumentList,[int]$TimeoutSeconds=30)
    $result = Finish-AdbProcess -Running (Start-AdbProcess -Arguments $ArgumentList) -TimeoutSeconds $TimeoutSeconds
    if ($result.TimedOut) { throw "ADB no respondio en $TimeoutSeconds segundos. Ultima salida: $($result.Text)" }
    if ($result.Code -ne 0) { throw "ADB fallo ($($result.Code)): $($result.Text)" }
    return $result.Text
}
function Shell {
    param([string]$Command,[int]$TimeoutSeconds=30)
    $output = Adb -TimeoutSeconds $TimeoutSeconds -ArgumentList @('shell', "$Command && echo __NP_OK__")
    if ($output -notmatch '(?m)^__NP_OK__\s*$') { throw "El NOOK no completo el comando: $output" }
    return ($output -replace '(?m)^__NP_OK__\s*$', '').Trim()
}