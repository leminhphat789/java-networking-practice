$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot
try {
    New-Item -ItemType Directory -Force build/classes | Out-Null
    $sources = Get-ChildItem src -Recurse -Filter *.java | ForEach-Object FullName
    & javac -encoding UTF-8 -Xlint:all -d build/classes $sources
    if ($LASTEXITCODE -ne 0) { throw 'Java compilation failed' }
    Write-Host 'BUILD SUCCESS'
} finally { Pop-Location }
