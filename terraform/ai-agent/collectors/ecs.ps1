
param(
    [Parameter(Mandatory = $true)]
    [string]$Cluster,

    [Parameter(Mandatory = $true)]
    [string]$Service,

    [string]$Profile = "terraform-dev",

    [string]$Region = "us-east-1"
)

$ErrorActionPreference = "Stop"

# ============================================================
# HELPER: Execute AWS CLI and return JSON
# ============================================================

function Invoke-AwsJson {
    param(
        [Parameter(Mandatory = $true)]
        [string[]]$Arguments
    )

    $output = & aws @Arguments

    if ($LASTEXITCODE -ne 0) {
        throw "AWS CLI command failed: aws $($Arguments -join ' ')"
    }

    if ([string]::IsNullOrWhiteSpace(($output -join ""))) {
        return $null
    }

    return ($output -join "`n") | ConvertFrom-Json
}

# ============================================================
# START
# ============================================================

Write-Host ""
Write-Host "============================================="
Write-Host " ECS COLLECTOR"
Write-Host "============================================="
Write-Host "Cluster : [$Cluster]"
Write-Host "Service : [$Service]"
Write-Host "Profile : [$Profile]"
Write-Host "Region  : [$Region]"
Write-Host ""

# ============================================================
# 1. DESCRIBE SERVICE
# ============================================================

Write-Host "[1/4] Collecting ECS service..."

$serviceData = Invoke-AwsJson @(
    "ecs",
    "describe-services",
    "--cluster", $Cluster,
    "--services", $Service,
    "--profile", $Profile,
    "--region", $Region,
    "--output", "json"
)

if ($null -eq $serviceData.services) {
    throw "AWS returned no services."
}

if ($serviceData.services.Count -eq 0) {
    throw "Service '$Service' was not found."
}

$serviceObject = $serviceData.services[0]

Write-Host "Service name : $($serviceObject.serviceName)"
Write-Host "Service ARN  : $($serviceObject.serviceArn)"
Write-Host "Status       : $($serviceObject.status)"
Write-Host "Desired      : $($serviceObject.desiredCount)"
Write-Host "Running      : $($serviceObject.runningCount)"
Write-Host "Pending      : $($serviceObject.pendingCount)"
Write-Host ""

# ============================================================
# 2. RUNNING TASKS
# ============================================================

Write-Host "[2/4] Collecting RUNNING tasks..."

$runningData = Invoke-AwsJson @(
    "ecs",
    "list-tasks",
    "--cluster", $Cluster,
    "--service-name", $Service,
    "--desired-status", "RUNNING",
    "--profile", $Profile,
    "--region", $Region,
    "--output", "json"
)

$runningTaskArns = @()

if ($null -ne $runningData.taskArns) {
    $runningTaskArns = @($runningData.taskArns)
}

Write-Host "Running tasks: $($runningTaskArns.Count)"

$tasks = @()

if ($runningTaskArns.Count -gt 0) {

    $taskArguments = @(
        "ecs",
        "describe-tasks",
        "--cluster", $Cluster,
        "--tasks"
    )

    $taskArguments += $runningTaskArns

    $taskArguments += @(
        "--include", "TAGS",
        "--profile", $Profile,
        "--region", $Region,
        "--output", "json"
    )

    $tasksData = Invoke-AwsJson $taskArguments

    if ($null -ne $tasksData.tasks) {
        $tasks = @($tasksData.tasks)
    }
}

# ============================================================
# 3. STOPPED TASKS
# ============================================================

Write-Host "[3/4] Collecting STOPPED tasks..."

$stoppedData = Invoke-AwsJson @(
    "ecs",
    "list-tasks",
    "--cluster", $Cluster,
    "--service-name", $Service,
    "--desired-status", "STOPPED",
    "--max-results", "10",
    "--profile", $Profile,
    "--region", $Region,
    "--output", "json"
)

$stoppedTaskArns = @()

if ($null -ne $stoppedData.taskArns) {
    $stoppedTaskArns = @($stoppedData.taskArns)
}

Write-Host "Stopped tasks: $($stoppedTaskArns.Count)"

$stoppedTasks = @()

if ($stoppedTaskArns.Count -gt 0) {

    $stoppedArguments = @(
        "ecs",
        "describe-tasks",
        "--cluster", $Cluster,
        "--tasks"
    )

    $stoppedArguments += $stoppedTaskArns

    $stoppedArguments += @(
        "--include", "TAGS",
        "--profile", $Profile,
        "--region", $Region,
        "--output", "json"
    )

    $stoppedTasksData = Invoke-AwsJson $stoppedArguments

    if ($null -ne $stoppedTasksData.tasks) {
        $stoppedTasks = @($stoppedTasksData.tasks)
    }
}

# ============================================================
# 4. BUILD RESULT
# ============================================================

Write-Host "[4/4] Building diagnostic result..."

$result = [ordered]@{
    timestamp = (Get-Date).ToUniversalTime().ToString("o")

    cluster = $Cluster

    service = [ordered]@{
        name = $serviceObject.serviceName
        arn = $serviceObject.serviceArn
        status = $serviceObject.status

        desiredCount = $serviceObject.desiredCount
        runningCount = $serviceObject.runningCount
        pendingCount = $serviceObject.pendingCount

        taskDefinition = $serviceObject.taskDefinition

        launchType = $serviceObject.launchType

        platformVersion = $serviceObject.platformVersion

        healthCheckGracePeriodSeconds =
            $serviceObject.healthCheckGracePeriodSeconds
    }

    deployments = @(
        $serviceObject.deployments | ForEach-Object {
            [ordered]@{
                id = $_.id
                status = $_.status
                rolloutState = $_.rolloutState
                rolloutStateReason = $_.rolloutStateReason

                desiredCount = $_.desiredCount
                runningCount = $_.runningCount
                pendingCount = $_.pendingCount
                failedTasks = $_.failedTasks

                taskDefinition = $_.taskDefinition
            }
        }
    )

    tasks = @(
        $tasks | ForEach-Object {

            [ordered]@{
                taskArn = $_.taskArn
                taskDefinitionArn = $_.taskDefinitionArn

                lastStatus = $_.lastStatus
                desiredStatus = $_.desiredStatus
                healthStatus = $_.healthStatus

                startedAt = $_.startedAt
                stoppingAt = $_.stoppingAt
                stoppedAt = $_.stoppedAt

                stoppedReason = $_.stoppedReason
                stopCode = $_.stopCode

                containers = @(
                    $_.containers | ForEach-Object {

                        [ordered]@{
                            name = $_.name
                            image = $_.image

                            lastStatus = $_.lastStatus
                            healthStatus = $_.healthStatus

                            exitCode = $_.exitCode
                            reason = $_.reason

                            health = $_.health
                        }
                    }
                )
            }
        }
    )

    stoppedTasks = @(
        $stoppedTasks | ForEach-Object {

            [ordered]@{
                taskArn = $_.taskArn
                taskDefinitionArn = $_.taskDefinitionArn

                lastStatus = $_.lastStatus
                desiredStatus = $_.desiredStatus
                healthStatus = $_.healthStatus

                startedAt = $_.startedAt
                stoppingAt = $_.stoppingAt
                stoppedAt = $_.stoppedAt

                stoppedReason = $_.stoppedReason
                stopCode = $_.stopCode

                containers = @(
                    $_.containers | ForEach-Object {

                        [ordered]@{
                            name = $_.name
                            image = $_.image

                            lastStatus = $_.lastStatus
                            healthStatus = $_.healthStatus

                            exitCode = $_.exitCode
                            reason = $_.reason

                            health = $_.health
                        }
                    }
                )
            }
        }
    )

    events = @(
        $serviceObject.events | ForEach-Object {
            [ordered]@{
                id = $_.id
                createdAt = $_.createdAt
                message = $_.message
            }
        }
    )
}

# ============================================================
# OUTPUT
# ============================================================

Write-Host ""
Write-Host "Collection completed."
Write-Host ""

$result | ConvertTo-Json -Depth 50
