$ErrorActionPreference = 'Stop'
$c = (Invoke-WebRequest 'https://maven.fabricmc.net/net/fabricmc/fabric-loader/maven-metadata.xml' -UseBasicParsing -TimeoutSec 120).Content
($c -split "`n" | Where-Object { $_ -match '<version>0\.16\.' } | Select-Object -Last 8) | ForEach-Object { $_.Trim() }
