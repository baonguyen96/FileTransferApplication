"Building..."

$start = Get-Date
$thisDirectory = "$PSScriptRoot"

Set-Location ..
mvn dependency:purge-local-repository
mvn clean install -U
mvn package
Set-Location $thisDirectory

$end = Get-Date
$duration = New-TimeSpan -Start $start -End $end

"Finished after " + $duration.Minutes + " minute(s) " + ($duration.Seconds % 60) + " second(s)"
