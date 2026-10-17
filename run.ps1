param([ValidateSet('demo', 'interactive', 'test')][string]$Mode = 'demo')
$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot
try {
    $javac = 'javac'
    $java = 'java'
    if ($env:JAVA_HOME) {
        $javac = Join-Path $env:JAVA_HOME 'bin/javac.exe'
        $java = Join-Path $env:JAVA_HOME 'bin/java.exe'
    }
    $sources = @(Get-ChildItem -LiteralPath src -Recurse -Filter '*.java' | ForEach-Object { $_.FullName })
    if ($Mode -eq 'test') {
        $sources += @(Get-ChildItem -LiteralPath tests -Recurse -Filter '*.java' | ForEach-Object { $_.FullName })
    }
    & $javac --release 17 -encoding UTF-8 -Xlint:all -Werror -d build/classes $sources
    if ($LASTEXITCODE -ne 0) { throw 'Compilation failed.' }
    if ($Mode -eq 'test') {
        & $java -cp build/classes assignment4.OrderFacadeTest
    } elseif ($Mode -eq 'interactive') {
        & $java -cp build/classes assignment4.Main --interactive
    } else {
        & $java -cp build/classes assignment4.Main
    }
    if ($LASTEXITCODE -ne 0) { throw 'Execution failed.' }
} finally {
    Pop-Location
}
