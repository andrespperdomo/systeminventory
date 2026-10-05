param(
    [Parameter(Mandatory=$true)]
    [string]$Service
)

$Cluster = "inventory-project-dev"
$Profile = "terraform-dev"
$Region = "us-east-1"

Write-Host ""
Write-Host "====================================="
Write-Host "       AI DEVOPS DIAGNOSTIC AGENT"
Write-Host "====================================="
Write-Host ""

Write-Host "Service: $Service"
Write-Host "Cluster: $Cluster"
Write-Host ""

$ecs = .\collectors\ecs.ps1 `
    -Cluster $Cluster `
    -Service $Service `
    -Profile $Profile `
    -Region $Region

$ecs | Out-File ".\reports\ecs-$Service.json"

Write-Host "ECS collection complete."