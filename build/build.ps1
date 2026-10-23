param(
    [string]$JavaHome = $env:JAVA_HOME,
    [string]$OutputName = 'SimYukkuri-makaizou.jar'
)
$ErrorActionPreference = 'Stop'
$repo = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
if ([IO.Path]::GetFileName($OutputName) -ne $OutputName -or !$OutputName.EndsWith('.jar')) {
    throw 'OutputName must be a JAR filename.'
}
if ($JavaHome) {
    $javac = Join-Path $JavaHome 'bin/javac.exe'
    $jar = Join-Path $JavaHome 'bin/jar.exe'
} else {
    $javac = (Get-Command javac -ErrorAction Stop).Source
    $jar = Join-Path (Split-Path $javac) 'jar.exe'
}
$tempRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '.makaizou-build'))
New-Item -ItemType Directory -Path $tempRoot -Force | Out-Null
$buildWork = Join-Path $tempRoot ('SimYukkuri-makaizou-' + [Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path "$buildWork/classes", "$buildWork/libraries" -Force | Out-Null
try {
    $sourcePaths = @(Get-ChildItem -LiteralPath "$repo/src/main/java" -Recurse -Filter '*.java' |
        ForEach-Object { '"' + $_.FullName.Replace('\', '/') + '"' })
    [IO.File]::WriteAllLines("$buildWork/sources.txt", $sourcePaths, [Text.UTF8Encoding]::new($false))
    $compilerVersion = (& $javac -version 2>&1 | Out-String)
    $compatibility = @('--release', '8')
    if ($compilerVersion -match 'javac 1\.8\.') { $compatibility = @('-source', '8', '-target', '8') }
    & $javac @compatibility -encoding UTF-8 -cp "$repo/lib/*" -d "$buildWork/classes" "@$buildWork/sources.txt"
    if ($LASTEXITCODE -ne 0) { throw 'Compilation failed; no JAR was replaced.' }
    Push-Location -LiteralPath "$buildWork/libraries"
    try {
        foreach ($library in Get-ChildItem -LiteralPath "$repo/lib" -Filter 'jackson-*.jar') {
            & $jar xf $library.FullName
            if ($LASTEXITCODE -ne 0) { throw "Cannot unpack $($library.Name)" }
        }
        foreach ($signature in Get-ChildItem -LiteralPath 'META-INF' -File -ErrorAction SilentlyContinue |
            Where-Object Extension -in '.SF', '.DSA', '.RSA') {
            Remove-Item -LiteralPath $signature.FullName
        }
    } finally {
        Pop-Location
    }
    $candidate = Join-Path $buildWork $OutputName
    & $jar "-J-Djava.io.tmpdir=$buildWork" cfe $candidate org.simyukkuri.SimYukkuri -C "$buildWork/classes" . `
        -C "$buildWork/libraries" . -C $repo data -C $repo images -C $repo resources `
        -C $repo LICENSE -C $repo NOTICE.md
    if ($LASTEXITCODE -ne 0) { throw 'JAR creation failed; no JAR was replaced.' }
    Copy-Item -LiteralPath $candidate -Destination (Join-Path $PSScriptRoot $OutputName) -Force
    Write-Output "Built: $(Join-Path $PSScriptRoot $OutputName)"
} finally {
    $resolvedWork = [IO.Path]::GetFullPath($buildWork)
    if (!$resolvedWork.StartsWith($tempRoot, [StringComparison]::OrdinalIgnoreCase) -or
        [IO.Path]::GetFileName($resolvedWork) -notlike 'SimYukkuri-makaizou-*') {
        throw "Unsafe temporary cleanup path: $resolvedWork"
    }
    Remove-Item -LiteralPath $resolvedWork -Recurse -Force
}
