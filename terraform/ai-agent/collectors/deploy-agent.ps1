param(
[Parameter(Mandatory = $true)]
[ValidateSet("product", "inventory")]
[string]$Service,


[Parameter(Mandatory = $true)]
[string]$Tag,

[string]$Profile = "terraform-dev",
[string]$Region = "us-east-1",
[string]$Cluster = "inventory-project-dev",

[switch]$NoCache,
[switch]$ShowLogs


)

$ErrorActionPreference = "Stop"

$ScriptDir = $PSScriptRoot
$ProjectRoot = Split-Path -Parent (Split-Path -Parent (Split-Path -Parent $ScriptDir))

if ($Service -eq "product") {
$EcrRepository = "inventory-project-dev-product"
$EcsService = "inventory-project-dev-product"
$ContainerName = "product"
$DockerContext = Join-Path $ProjectRoot "productservice"
$Dockerfile = Join-Path $ProjectRoot "productservice\Dockerfile"
$LogGroup = "/ecs/inventory-project/dev/product"
}
else {
$EcrRepository = "inventory-project-dev-inventory"
$EcsService = "inventory-project-dev-inventory"
$ContainerName = "inventory"
$DockerContext = Join-Path $ProjectRoot "inventoryservice"
$Dockerfile = Join-Path $ProjectRoot "inventoryservice\Dockerfile"
$LogGroup = "/ecs/inventory-project/dev/inventory"
}

$TempDirectory = $env:TEMP

if ([string]::IsNullOrWhiteSpace($TempDirectory)) {
    throw "TEMP environment variable is not available."
}

$TempTaskDefinition = Join-Path $TempDirectory "ecs-task-$Service.json"
$RegisterTaskDefinition = Join-Path $TempDirectory "ecs-register-$Service.json"

function Write-Step {
param(
[string]$Message
)


Write-Host ""
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host $Message -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan


}

try {


Write-Step "DEPLOYMENT START"

Write-Host "Service        : $Service"
Write-Host "Tag            : $Tag"
Write-Host "Profile        : $Profile"
Write-Host "Region         : $Region"
Write-Host "Cluster        : $Cluster"
Write-Host "ECS Service    : $EcsService"
Write-Host "ECR Repository : $EcrRepository"
Write-Host "Docker Context : $DockerContext"
Write-Host "Dockerfile     : $Dockerfile"

Write-Step "CHECKING TOOLS"

if (-not (Get-Command aws -ErrorAction SilentlyContinue)) {
    throw "AWS CLI was not found."
}

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw "Docker was not found."
}

Write-Host "AWS CLI: OK" -ForegroundColor Green
Write-Host "Docker : OK" -ForegroundColor Green

Write-Step "CHECKING AWS"

$IdentityJson = aws sts get-caller-identity --profile $Profile --region $Region --output json

if ($LASTEXITCODE -ne 0) {
    throw "AWS authentication failed."
}

$Identity = $IdentityJson | ConvertFrom-Json

Write-Host "Account: $($Identity.Account)" -ForegroundColor Green
Write-Host "ARN    : $($Identity.Arn)" -ForegroundColor Green

Write-Step "CHECKING SOURCE"

if (-not (Test-Path $DockerContext)) {
    throw "Docker context does not exist: $DockerContext"
}

if (-not (Test-Path $Dockerfile)) {
    throw "Dockerfile does not exist: $Dockerfile"
}

Write-Host "Docker context: OK" -ForegroundColor Green
Write-Host "Dockerfile    : OK" -ForegroundColor Green

Write-Step "GETTING ECR"

$RepositoryUri = aws ecr describe-repositories `
    --repository-names $EcrRepository `
    --profile $Profile `
    --region $Region `
    --query "repositories[0].repositoryUri" `
    --output text

if ($LASTEXITCODE -ne 0) {
    throw "ECR repository was not found: $EcrRepository"
}

if ([string]::IsNullOrWhiteSpace($RepositoryUri)) {
    throw "ECR repository URI is empty."
}

$ImageUri = "${RepositoryUri}:${Tag}"

Write-Host "Repository: $RepositoryUri" -ForegroundColor Green
Write-Host "Image     : $ImageUri"

Write-Step "ECR LOGIN"

$EcrPassword = aws ecr get-login-password `
    --profile $Profile `
    --region $Region

if ($LASTEXITCODE -ne 0) {
    throw "Could not obtain ECR password."
}

$EcrPassword | docker login --username AWS --password-stdin $RepositoryUri

if ($LASTEXITCODE -ne 0) {
    throw "Docker login failed."
}

Write-Host "ECR login successful." -ForegroundColor Green

Write-Step "BUILDING DOCKER IMAGE"

if ($NoCache) {
    Write-Host "Building with --no-cache" -ForegroundColor Yellow

    docker build `
        --no-cache `
        -f $Dockerfile `
        -t $ImageUri `
        $DockerContext
}
else {
    Write-Host "Building with Docker cache"

    docker build `
        -f $Dockerfile `
        -t $ImageUri `
        $DockerContext
}

if ($LASTEXITCODE -ne 0) {
    throw "Docker build failed."
}

Write-Host "Docker build successful." -ForegroundColor Green

Write-Step "PUSHING IMAGE TO ECR"

docker push $ImageUri

if ($LASTEXITCODE -ne 0) {
    throw "Docker push failed."
}

Write-Host "Docker push successful." -ForegroundColor Green

Write-Step "GETTING ECS TASK DEFINITION"

$CurrentTaskDefinitionArn = aws ecs describe-services `
    --cluster $Cluster `
    --services $EcsService `
    --profile $Profile `
    --region $Region `
    --query "services[0].taskDefinition" `
    --output text

if ($LASTEXITCODE -ne 0) {
    throw "Could not get ECS service."
}

if ([string]::IsNullOrWhiteSpace($CurrentTaskDefinitionArn)) {
    throw "Current task definition is empty."
}

Write-Host "Current task definition:"
Write-Host $CurrentTaskDefinitionArn

Write-Step "DOWNLOADING TASK DEFINITION"

aws ecs describe-task-definition `
    --task-definition $CurrentTaskDefinitionArn `
    --profile $Profile `
    --region $Region `
    --query "taskDefinition" `
    --output json |
    Out-File -FilePath $TempTaskDefinition -Encoding utf8

if ($LASTEXITCODE -ne 0) {
    throw "Could not download task definition."
}

$TaskDefinition = Get-Content $TempTaskDefinition -Raw | ConvertFrom-Json

Write-Step "UPDATING CONTAINER IMAGE"

$TargetContainer = $TaskDefinition.containerDefinitions |
    Where-Object { $_.name -eq $ContainerName }

if ($null -eq $TargetContainer) {
    throw "Container '$ContainerName' was not found."
}

Write-Host "Old image:"
Write-Host $TargetContainer.image

$TargetContainer.image = $ImageUri

Write-Host "New image:"
Write-Host $TargetContainer.image -ForegroundColor Green

$RemoveProperties = @(
    "taskDefinitionArn",
    "revision",
    "status",
    "requiresAttributes",
    "compatibilities",
    "registeredAt",
    "registeredBy"
)

foreach ($Property in $RemoveProperties) {
    if ($TaskDefinition.PSObject.Properties.Name -contains $Property) {
        $TaskDefinition.PSObject.Properties.Remove($Property)
    }
}

# --------------------------------------------------------
# CREATE REGISTER JSON
# --------------------------------------------------------

$TaskDefinitionJson = $TaskDefinition | ConvertTo-Json -Depth 30

[System.IO.File]::WriteAllText(
    $RegisterTaskDefinition,
    $TaskDefinitionJson,
    [System.Text.UTF8Encoding]::new($false)
)

if (-not (Test-Path -LiteralPath $RegisterTaskDefinition)) {
    throw "Register task definition file was not created."
}

$JsonValidation = Get-Content `
    -LiteralPath $RegisterTaskDefinition `
    -Raw

try {
    $JsonValidation | ConvertFrom-Json | Out-Null
    Write-Host "Task definition JSON: VALID" -ForegroundColor Green
}
catch {
    throw "Generated task definition JSON is invalid: $($_.Exception.Message)"
}

Write-Step "REGISTERING NEW TASK DEFINITION"

$NewTaskDefinitionArn = aws ecs register-task-definition `
    --cli-input-json "file://$RegisterTaskDefinition" `
    --profile $Profile `
    --region $Region `
    --query "taskDefinition.taskDefinitionArn" `
    --output text

if ($LASTEXITCODE -ne 0) {
    throw "Could not register new task definition."
}

Write-Host "New task definition:"
Write-Host $NewTaskDefinitionArn -ForegroundColor Green

Write-Step "UPDATING ECS SERVICE"

aws ecs update-service `
    --cluster $Cluster `
    --service $EcsService `
    --task-definition $NewTaskDefinitionArn `
    --force-new-deployment `
    --profile $Profile `
    --region $Region `
    --output json | Out-Null

if ($LASTEXITCODE -ne 0) {
    throw "Could not update ECS service."
}

Write-Host "ECS deployment started." -ForegroundColor Green

Write-Step "WAITING FOR ECS"

Write-Host "Waiting for ECS service to become stable..."

aws ecs wait services-stable `
    --cluster $Cluster `
    --services $EcsService `
    --profile $Profile `
    --region $Region

if ($LASTEXITCODE -ne 0) {
    Write-Warning "ECS waiter timed out. Continuing with task verification."
}
else {
    Write-Host "ECS service is stable." -ForegroundColor Green
}

Write-Step "CHECKING RUNNING TASKS"

$TaskArnsJson = aws ecs list-tasks `
    --cluster $Cluster `
    --service-name $EcsService `
    --desired-status RUNNING `
    --profile $Profile `
    --region $Region `
    --query "taskArns" `
    --output json

if ($LASTEXITCODE -ne 0) {
    throw "Could not list ECS tasks."
}

$TaskArns = $TaskArnsJson | ConvertFrom-Json

if ($null -eq $TaskArns -or $TaskArns.Count -eq 0) {
    throw "No running ECS tasks found."
}

Write-Host "Running tasks: $($TaskArns.Count)" -ForegroundColor Green

$DescribeArguments = @(
    "ecs",
    "describe-tasks",
    "--cluster",
    $Cluster,
    "--tasks"
)

foreach ($TaskArn in $TaskArns) {
    $DescribeArguments += $TaskArn
}

$DescribeArguments += @(
    "--profile",
    $Profile,
    "--region",
    $Region,
    "--output",
    "json"
)

$RunningTasksJson = & aws @DescribeArguments

if ($LASTEXITCODE -ne 0) {
    throw "Could not describe ECS tasks."
}

$RunningTasks = $RunningTasksJson | ConvertFrom-Json

foreach ($Task in $RunningTasks.tasks) {

    Write-Host ""
    Write-Host "TASK" -ForegroundColor Cyan
    Write-Host "ARN         : $($Task.taskArn)"
    Write-Host "Status      : $($Task.lastStatus)"
    Write-Host "Health      : $($Task.healthStatus)"
    Write-Host "Task Def    : $($Task.taskDefinitionArn)"

    foreach ($Container in $Task.containers) {

        Write-Host ""
        Write-Host "Container   : $($Container.name)"
        Write-Host "Image       : $($Container.image)"
        Write-Host "Status      : $($Container.lastStatus)"
        Write-Host "Health      : $($Container.healthStatus)"

        if ($Container.reason) {
            Write-Host "Reason      : $($Container.reason)" -ForegroundColor Yellow
        }

        if ($null -ne $Container.exitCode) {
            Write-Host "Exit Code   : $($Container.exitCode)"
        }
    }
}

Write-Step "CHECKING CLOUDWATCH ERRORS"

aws logs tail $LogGroup `
    --since 10m `
    --filter-pattern "ERROR" `
    --profile $Profile `
    --region $Region `
    --format short

if ($ShowLogs) {

    Write-Step "RECENT APPLICATION LOGS"

    aws logs tail $LogGroup `
        --since 10m `
        --profile $Profile `
        --region $Region `
        --format short
}

Write-Step "DEPLOYMENT RESULT"

Write-Host "Service         : $Service"
Write-Host "Image           : $ImageUri"
Write-Host "ECS Service     : $EcsService"
Write-Host "Task Definition : $NewTaskDefinitionArn"

Write-Host ""
Write-Host "DEPLOYMENT: PASS" -ForegroundColor Green


}
catch {


Write-Host ""
Write-Host "============================================================" -ForegroundColor Red
Write-Host "DEPLOYMENT FAILED" -ForegroundColor Red
Write-Host "============================================================" -ForegroundColor Red
Write-Host ""

Write-Host $_.Exception.Message -ForegroundColor Red

exit 1


}
finally {


if (Test-Path $TempTaskDefinition) {
    Remove-Item $TempTaskDefinition -Force -ErrorAction SilentlyContinue
}

if (Test-Path $RegisterTaskDefinition) {
    Remove-Item $RegisterTaskDefinition -Force -ErrorAction SilentlyContinue
}

}
