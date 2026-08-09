$ErrorActionPreference = "Stop"
$port = 8787
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $root
$env:DIETCOACH_PORT = "$port"

$ip = (Get-NetIPAddress -AddressFamily IPv4 |
  Where-Object { $_.IPAddress -like '192.168.*' -or $_.IPAddress -like '10.*' } |
  Select-Object -First 1 -ExpandProperty IPAddress)
if (-not $ip) { $ip = "127.0.0.1" }

Write-Host ""
Write-Host "DietCoach 安装页已启动（APK 将以安装包 MIME 提供，便于跳转安装器）"
Write-Host "电脑浏览器:  http://127.0.0.1:$port/"
Write-Host "手机请打开:  http://${ip}:$port/"
Write-Host "务必手机与电脑连同一个 Wi-Fi，并用系统浏览器打开（不要用微信内置浏览器）"
Write-Host "按 Ctrl+C 停止服务"
Write-Host ""

python "$root\install_server.py"
