[CmdletBinding()]
param(
    [string]$Server = "[redacted-public-host]",
    [string]$User = "ubuntu",
    [int]$Port = 22,
    [string]$RemoteDir = "/opt/supervision",
    [string]$IdentityFile = ""
)

$ErrorActionPreference = "Stop"
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$stamp = Get-Date -Format "yyyyMMdd-HHmmss"
$archive = Join-Path ([System.IO.Path]::GetTempPath()) "supervision-$stamp.tar.gz"
$remoteArchive = "/tmp/supervision-$stamp.tar.gz"
$remoteRunner = "/tmp/supervision-deploy-$stamp.sh"

$tarCommand = (Get-Command tar.exe -ErrorAction SilentlyContinue).Source
$openSshDir = Join-Path $env:WINDIR "System32\OpenSSH"
$sshCommand = Join-Path $openSshDir "ssh.exe"
$scpCommand = Join-Path $openSshDir "scp.exe"
if (-not $tarCommand) { throw "缺少 tar.exe。" }
if (-not (Test-Path -LiteralPath $sshCommand) -or -not (Test-Path -LiteralPath $scpCommand)) {
    throw "缺少 Windows OpenSSH Client，请先安装该可选功能。"
}

$sshOptions = @("-p", $Port.ToString(), "-o", "ConnectTimeout=15", "-o", "ServerAliveInterval=15")
$scpOptions = @("-P", $Port.ToString(), "-o", "ConnectTimeout=15")
if ($IdentityFile) {
    $resolvedKey = (Resolve-Path $IdentityFile).Path
    $sshOptions += @("-i", $resolvedKey)
    $scpOptions += @("-i", $resolvedKey)
}

try {
    Write-Host "[1/4] 打包当前仓库..."
    Push-Location $repoRoot
    try {
        & $tarCommand -czf $archive --exclude=.git --exclude=.env --exclude=.pnpm-store --exclude=build/web/node_modules --exclude=build/web/dist .
        if ($LASTEXITCODE -ne 0) { throw "打包失败" }
    } finally {
        Pop-Location
    }

    Write-Host "[2/4] 上传发布包和远端执行脚本..."
    & $scpCommand @scpOptions $archive "${User}@${Server}:$remoteArchive"
    if ($LASTEXITCODE -ne 0) { throw "发布包上传失败" }
    & $scpCommand @scpOptions (Join-Path $PSScriptRoot "deploy-server.sh") "${User}@${Server}:$remoteRunner"
    if ($LASTEXITCODE -ne 0) { throw "远端执行脚本上传失败" }

    Write-Host "[3/4] 远端备份、构建并更新服务..."
    $remoteCommand = "chmod 700 '$remoteRunner' && '$remoteRunner' '$remoteArchive' '$RemoteDir'"
    & $sshCommand @sshOptions "${User}@${Server}" $remoteCommand
    if ($LASTEXITCODE -ne 0) { throw "远端部署失败；服务器已保留部署前备份，请检查上方日志" }

    Write-Host "[4/4] 部署完成：http://${Server}:8002/"
} finally {
    if (Test-Path -LiteralPath $archive) { Remove-Item -LiteralPath $archive -Force }
}
