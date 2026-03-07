$ErrorActionPreference = 'Stop'

function Get-JavaMajorVersion([string]$javaExe) {
    try {
        $firstLine = & $javaExe -version 2>&1 | Select-Object -First 1
        if ($firstLine -match '"(\d+)(?:[._].*)?"') {
            return [int]$matches[1]
        }
    }
    catch {
    }
    return 0
}

function Resolve-Jdk21Home {
    if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME 'bin\\java.exe'))) {
        if ((Get-JavaMajorVersion (Join-Path $env:JAVA_HOME 'bin\\java.exe')) -ge 21) {
            return $env:JAVA_HOME
        }
    }

    if ($env:JDK21_HOME -and (Test-Path (Join-Path $env:JDK21_HOME 'bin\\java.exe'))) {
        if ((Get-JavaMajorVersion (Join-Path $env:JDK21_HOME 'bin\\java.exe')) -ge 21) {
            return $env:JDK21_HOME
        }
    }

    $javaCmd = Get-Command java -ErrorAction SilentlyContinue
    if ($javaCmd) {
        $major = Get-JavaMajorVersion $javaCmd.Source
        if ($major -ge 21) {
            return (Split-Path -Parent (Split-Path -Parent $javaCmd.Source))
        }
    }

    return $null
}

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$jdkHome = Resolve-Jdk21Home
if (-not $jdkHome) {
    throw 'JDK 21 not found. Set JDK21_HOME or JAVA_HOME to a JDK 21 installation, then retry.'
}

$javaBin = Join-Path $jdkHome 'bin'
$env:JAVA_HOME = $jdkHome
$env:Path = "$javaBin;$env:Path"

Push-Location $projectRoot
try {
    .\\gradlew.bat --stop | Out-Null
    .\\gradlew.bat test
}
finally {
    Pop-Location
}