# Requires: already logged in via  C:\code\tools\gh\gh.exe auth login
$ErrorActionPreference = "Stop"
$gh = "C:\code\tools\gh\gh.exe"
Set-Location "C:\code\DietCoach"

& $gh auth status
$user = (& $gh api user --jq .login).Trim()
if (-not $user) { throw "Not logged in. Run: C:\code\tools\gh\gh.exe auth login -h github.com -p https -w" }

$repoName = "DietCoach"
Write-Host "Creating/pushing github.com/$user/$repoName ..."

# Create repo if missing, then push
$exists = & $gh repo view "$user/$repoName" 2>$null
if ($LASTEXITCODE -ne 0) {
  & $gh repo create $repoName --public --source=. --remote=origin --push --description "DietCoach Android app (Compose + Qwen)"
} else {
  git remote remove origin 2>$null
  git remote add origin "https://github.com/$user/$repoName.git"
  git push -u origin HEAD
}

# Upload share APK as Release (no API key baked in)
$apk = "C:\code\DietCoach\release\DietCoach-share.apk"
$zip = Get-ChildItem "C:\code\DietCoach\release\DietCoach-*.zip" -ErrorAction SilentlyContinue | Select-Object -First 1
if (-not (Test-Path $apk)) { throw "Missing share APK: $apk" }

$tag = "v1.0.0"
$rel = & $gh release view $tag -R "$user/$repoName" 2>$null
if ($LASTEXITCODE -ne 0) {
  $assets = @("-a", $apk)
  if ($zip) { $assets += @("-a", $zip.FullName) }
  & $gh release create $tag @assets `
    --title "DietCoach $tag" `
    --notes "Share APK without DashScope key. Install via Releases download; WeChat users prefer the zip." `
    -R "$user/$repoName"
} else {
  & $gh release upload $tag $apk --clobber -R "$user/$repoName"
  if ($zip) { & $gh release upload $tag $zip.FullName --clobber -R "$user/$repoName" }
}

Write-Host ""
Write-Host "Repo:     https://github.com/$user/$repoName"
Write-Host "Release:  https://github.com/$user/$repoName/releases/tag/$tag"
