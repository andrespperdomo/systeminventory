
param(
    [string]$Profile = "terraform-dev",
    [string]$Region = "us-east-1",

    [string]$Cluster = "inventory-project-dev",

    [string]$Service = "inventory-project-dev-product",

    [string]$InventoryService = "inventory-project-dev-inventory",

    [string]$BrokerName = "inventory-project-dev-rabbitmq",

    # RabbitMQ topology
    [string]$Exchange = "reservation-exchange",
    [string]$RoutingKey = "reservation.created",
    [string]$Queue = "",

    # Optional RabbitMQ Management API.
    # Example:
    # https://<broker-host>
    #
    # If omitted, the agent will inspect topology where possible
    # but will not pretend that publishing succeeded.
    [string]$RabbitManagementUrl = "",

    # RabbitMQ credentials can be supplied through environment variables:
    #
    # $env:RABBITMQ_DIAG_USER
    # $env:RABBITMQ_DIAG_PASSWORD
    #
    [string]$RabbitUser = "",
    [string]$RabbitPassword = "",

    # Optional existing Product API endpoint capable of causing
    # the Product service to publish a RabbitMQ event.
    #
    # Example:
    # -PublishUrl "https://my-api.example.com/products/123"
    #
    [string]$PublishUrl = "",

    # Optional JSON payload for PublishUrl.
    [string]$PublishBody = "",

    [string]$PublishMethod = "POST",

    [int]$PublishTimeoutSeconds = 30,

    # How long to wait for Inventory to consume the event.
    [int]$CorrelationTimeoutSeconds = 60,

    # CloudWatch
    [switch]$ShowLogs,

    [int]$LogMinutes = 15,

    # If specified, searches CloudWatch logs for this string.
    [string]$CorrelationId = ""
)

$ErrorActionPreference = "Stop"

# ============================================================
# GLOBAL STATE
# ============================================================

$script:Checks = @()

$script:CorrelationIdValue = ""

$script:RabbitHost = ""
$script:RabbitPort = 5671
$script:RabbitEndpoint = ""

$script:ProductTaskId = ""
$script:ProductContainerName = ""

$script:InventoryTaskId = ""
$script:InventoryContainerName = ""

$script:ProductLogGroup = ""
$script:InventoryLogGroup = ""

# ============================================================
# OUTPUT HELPERS
# ============================================================

function Write-Step {
    param(
        [string]$Message
    )

    Write-Host ""
    Write-Host "============================================================" -ForegroundColor DarkGray
    Write-Host $Message -ForegroundColor Cyan
    Write-Host "============================================================" -ForegroundColor DarkGray
}

function Write-OK {
    param(
        [string]$Message,
        [string]$Check = ""
    )

    Write-Host "[PASS] $Message" -ForegroundColor Green

    if ($Check) {
        $script:Checks += [PSCustomObject]@{
            Name   = $Check
            Status = "PASS"
            Detail = $Message
        }
    }
}

function Write-Fail {
    param(
        [string]$Message,
        [string]$Check = ""
    )

    Write-Host "[FAIL] $Message" -ForegroundColor Red

    if ($Check) {
        $script:Checks += [PSCustomObject]@{
            Name   = $Check
            Status = "FAIL"
            Detail = $Message
        }
    }
}

function Write-Warn {
    param(
        [string]$Message,
        [string]$Check = ""
    )

    Write-Host "[WARN] $Message" -ForegroundColor Yellow

    if ($Check) {
        $script:Checks += [PSCustomObject]@{
            Name   = $Check
            Status = "WARN"
            Detail = $Message
        }
    }
}

function Write-Skip {
    param(
        [string]$Message,
        [string]$Check = ""
    )

    Write-Host "[SKIP] $Message" -ForegroundColor DarkYellow

    if ($Check) {
        $script:Checks += [PSCustomObject]@{
            Name   = $Check
            Status = "SKIP"
            Detail = $Message
        }
    }
}

# ============================================================
# AWS HELPERS
# ============================================================

function Invoke-AwsJson {
    param(
        [Parameter(Mandatory = $true)]
        [string[]]$Arguments
    )

    $output = & aws @Arguments 2>&1

    if ($LASTEXITCODE -ne 0) {
        throw ($output -join "`n")
    }

    $text = ($output -join "`n").Trim()

    if ([string]::IsNullOrWhiteSpace($text)) {
        return $null
    }

    return $text | ConvertFrom-Json
}

function Invoke-AwsText {
    param(
        [Parameter(Mandatory = $true)]
        [string[]]$Arguments
    )

    $output = & aws @Arguments 2>&1

    if ($LASTEXITCODE -ne 0) {
        throw ($output -join "`n")
    }

    return ($output -join "`n").Trim()
}

# ============================================================
# GENERAL HELPERS
# ============================================================

function New-CorrelationId {

    $timestamp = Get-Date -Format "yyyyMMdd-HHmmss"

    $random = [Guid]::NewGuid().ToString("N").Substring(0, 8)

    return "diag-$timestamp-$random"
}

function Test-CommandExists {
    param(
        [string]$Command
    )

    return $null -ne (Get-Command $Command -ErrorAction SilentlyContinue)
}

function ConvertTo-BasicAuthHeader {
    param(
        [string]$Username,
        [string]$Password
    )

    $bytes = [System.Text.Encoding]::ASCII.GetBytes(
        "$Username`:$Password"
    )

    $encoded = [Convert]::ToBase64String($bytes)

    return @{
        Authorization = "Basic $encoded"
    }
}

function Invoke-RabbitManagement {

    param(
        [Parameter(Mandatory = $true)]
        [string]$Path,

        [string]$Method = "GET",

        [object]$Body = $null
    )

    if ([string]::IsNullOrWhiteSpace($RabbitManagementUrl)) {
        throw "RabbitManagementUrl was not configured."
    }

    if ([string]::IsNullOrWhiteSpace($RabbitUser)) {
        throw "RabbitUser was not configured."
    }

    if ([string]::IsNullOrWhiteSpace($RabbitPassword)) {
        throw "RabbitPassword was not configured."
    }

    $base = $RabbitManagementUrl.TrimEnd("/")

    $uri = "$base$Path"

    $headers = ConvertTo-BasicAuthHeader `
        -Username $RabbitUser `
        -Password $RabbitPassword

    $params = @{
        Uri         = $uri
        Method      = $Method
        Headers     = $headers
        TimeoutSec  = 30
        ErrorAction = "Stop"
    }

    if ($null -ne $Body) {

        $params.ContentType = "application/json"

        $params.Body = (
            $Body | ConvertTo-Json -Depth 20 -Compress
        )
    }

    return Invoke-RestMethod @params
}

# ============================================================
# HEADER
# ============================================================

Write-Host ""
Write-Host "============================================================" -ForegroundColor Magenta
Write-Host "       PRODUCT -> RABBITMQ -> INVENTORY DIAGNOSTIC AGENT" -ForegroundColor Magenta
Write-Host "============================================================" -ForegroundColor Magenta
Write-Host ""

Write-Host "Profile          : $Profile"
Write-Host "Region           : $Region"
Write-Host "Cluster          : $Cluster"
Write-Host "Product Service  : $Service"
Write-Host "Inventory Service: $InventoryService"
Write-Host "Broker           : $BrokerName"
Write-Host "Exchange         : $Exchange"
Write-Host "Routing Key      : $RoutingKey"

# ============================================================
# CORRELATION ID
# ============================================================

if ([string]::IsNullOrWhiteSpace($CorrelationId)) {
    $script:CorrelationIdValue = New-CorrelationId
}
else {
    $script:CorrelationIdValue = $CorrelationId
}

Write-Host "Correlation ID    : $script:CorrelationIdValue" -ForegroundColor Yellow

# ============================================================
# 1. AWS CLI
# ============================================================

Write-Step "1. CHECK AWS CLI"

try {

    $null = & aws --version 2>&1

    if ($LASTEXITCODE -ne 0) {
        throw "AWS CLI is not available."
    }

    Write-OK `
        "AWS CLI available" `
        "AWS CLI"

}
catch {

    Write-Fail `
        "AWS CLI is not available." `
        "AWS CLI"

    exit 1
}

# ============================================================
# 2. AWS CREDENTIALS
# ============================================================

Write-Step "2. CHECK AWS CREDENTIALS"

try {

    $identity = Invoke-AwsJson @(
        "sts",
        "get-caller-identity",
        "--profile", $Profile
    )

    Write-Host "Account : $($identity.Account)"
    Write-Host "ARN     : $($identity.Arn)"

    Write-OK `
        "AWS authentication successful" `
        "AWS credentials"

}
catch {

    Write-Fail `
        "AWS authentication failed." `
        "AWS credentials"

    Write-Host $_.Exception.Message -ForegroundColor Red

    exit 1
}

# ============================================================
# 3. AMAZON MQ BROKER
# ============================================================

Write-Step "3. FIND AMAZON MQ RABBITMQ"

try {

    $brokerId = Invoke-AwsText @(
        "mq",
        "list-brokers",
        "--profile", $Profile,
        "--region", $Region,
        "--query",
        "BrokerSummaries[?BrokerName=='$BrokerName'].BrokerId | [0]",
        "--output",
        "text"
    )

    if (
        [string]::IsNullOrWhiteSpace($brokerId) -or
        $brokerId -eq "None"
    ) {
        throw "Broker '$BrokerName' was not found."
    }

    Write-Host "Broker ID: $brokerId"

    Write-OK `
        "RabbitMQ broker found" `
        "RabbitMQ broker"

}
catch {

    Write-Fail `
        "RabbitMQ broker '$BrokerName' was not found." `
        "RabbitMQ broker"

    Write-Host $_.Exception.Message -ForegroundColor Red

    exit 1
}

# ============================================================
# 4. BROKER DETAILS
# ============================================================

Write-Step "4. READ RABBITMQ BROKER"

try {

    $broker = Invoke-AwsJson @(
        "mq",
        "describe-broker",
        "--broker-id", $brokerId,
        "--profile", $Profile,
        "--region", $Region
    )

    Write-Host "Broker Name : $($broker.BrokerName)"
    Write-Host "Broker ID   : $($broker.BrokerId)"
    Write-Host "State       : $($broker.BrokerState)"
    Write-Host "Engine      : $($broker.EngineType)"
    Write-Host "Version     : $($broker.EngineVersion)"

    if ($broker.BrokerState -eq "RUNNING") {

        Write-OK `
            "RabbitMQ broker is RUNNING" `
            "RabbitMQ broker state"

    }
    else {

        Write-Fail `
            "RabbitMQ broker state is $($broker.BrokerState)." `
            "RabbitMQ broker state"
    }

    $rabbitEndpoint = $null

    foreach ($instance in $broker.BrokerInstances) {

        if ($instance.Endpoints) {

            foreach ($endpoint in $instance.Endpoints) {

                if ($endpoint -like "amqps://*") {

                    $rabbitEndpoint = $endpoint

                    break
                }
            }
        }

        if ($rabbitEndpoint) {
            break
        }
    }

    if (-not $rabbitEndpoint) {
        throw "No AMQPS endpoint found."
    }

    $script:RabbitEndpoint = $rabbitEndpoint

    $script:RabbitHost = $rabbitEndpoint

    $script:RabbitHost = `
        $script:RabbitHost -replace "^amqps://", ""

    $script:RabbitHost = `
        $script:RabbitHost -replace "^amqp://", ""

    $script:RabbitHost = `
        $script:RabbitHost.Split(":")[0]

    Write-Host ""
    Write-Host "AMQPS Endpoint : $rabbitEndpoint"
    Write-Host "Rabbit Host    : $script:RabbitHost"
    Write-Host "Rabbit Port    : 5671"

    Write-OK `
        "RabbitMQ endpoint discovered" `
        "RabbitMQ endpoint"

}
catch {

    Write-Fail `
        "Could not read RabbitMQ broker." `
        "RabbitMQ endpoint"

    Write-Host $_.Exception.Message -ForegroundColor Red

    exit 1
}

# ============================================================
# 5. PRODUCT ECS SERVICE
# ============================================================

Write-Step "5. CHECK PRODUCT ECS SERVICE"

try {

    $serviceResult = Invoke-AwsJson @(
        "ecs",
        "describe-services",
        "--cluster", $Cluster,
        "--services", $Service,
        "--profile", $Profile,
        "--region", $Region
    )

    if (
        -not $serviceResult.services -or
        $serviceResult.services.Count -eq 0
    ) {
        throw "Product ECS service '$Service' was not found."
    }

    $ecsService = $serviceResult.services[0]

    Write-Host "Service          : $($ecsService.serviceName)"
    Write-Host "Desired count    : $($ecsService.desiredCount)"
    Write-Host "Running count    : $($ecsService.runningCount)"
    Write-Host "Pending count    : $($ecsService.pendingCount)"
    Write-Host "ECS Exec enabled : $($ecsService.enableExecuteCommand)"

    if ($ecsService.runningCount -gt 0) {

        Write-OK `
            "Product ECS service has running tasks" `
            "Product ECS"

    }
    else {

        Write-Fail `
            "Product ECS service has no running tasks." `
            "Product ECS"

        exit 1
    }

    if ($ecsService.enableExecuteCommand) {

        Write-OK `
            "Product ECS Exec is enabled" `
            "Product ECS Exec"

    }
    else {

        Write-Warn `
            "Product ECS Exec is disabled." `
            "Product ECS Exec"
    }

}
catch {

    Write-Fail `
        "Could not inspect Product ECS service." `
        "Product ECS"

    Write-Host $_.Exception.Message -ForegroundColor Red

    exit 1
}

# ============================================================
# 6. INVENTORY ECS SERVICE
# ============================================================

Write-Step "6. CHECK INVENTORY ECS SERVICE"

try {

    $inventoryServiceResult = Invoke-AwsJson @(
        "ecs",
        "describe-services",
        "--cluster", $Cluster,
        "--services", $InventoryService,
        "--profile", $Profile,
        "--region", $Region
    )

    if (
        -not $inventoryServiceResult.services -or
        $inventoryServiceResult.services.Count -eq 0
    ) {
        throw "Inventory ECS service '$InventoryService' was not found."
    }

    $inventoryEcsService = `
        $inventoryServiceResult.services[0]

    Write-Host "Service       : $($inventoryEcsService.serviceName)"
    Write-Host "Desired count : $($inventoryEcsService.desiredCount)"
    Write-Host "Running count : $($inventoryEcsService.runningCount)"

    if ($inventoryEcsService.runningCount -gt 0) {

        Write-OK `
            "Inventory ECS service has running tasks" `
            "Inventory ECS"

    }
    else {

        Write-Fail `
            "Inventory ECS service has no running tasks." `
            "Inventory ECS"
    }

}
catch {

    Write-Fail `
        "Could not inspect Inventory ECS service." `
        "Inventory ECS"

    Write-Host $_.Exception.Message -ForegroundColor Red
}

# ============================================================
# 7. FIND PRODUCT TASK
# ============================================================

Write-Step "7. FIND RUNNING PRODUCT ECS TASK"

try {

    $taskArn = Invoke-AwsText @(
        "ecs",
        "list-tasks",
        "--cluster", $Cluster,
        "--service-name", $Service,
        "--desired-status", "RUNNING",
        "--profile", $Profile,
        "--region", $Region,
        "--query", "taskArns[0]",
        "--output", "text"
    )

    if (
        [string]::IsNullOrWhiteSpace($taskArn) -or
        $taskArn -eq "None"
    ) {
        throw "No RUNNING Product task found."
    }

    $script:ProductTaskId = `
        $taskArn.Split("/")[-1]

    Write-Host "Task ID: $script:ProductTaskId"

    Write-OK `
        "Running Product task found" `
        "Product task"

}
catch {

    Write-Fail `
        "Could not find running Product task." `
        "Product task"

    Write-Host $_.Exception.Message -ForegroundColor Red

    exit 1
}

# ============================================================
# 8. PRODUCT TASK DETAILS
# ============================================================

Write-Step "8. READ PRODUCT ECS TASK"

try {

    $taskResult = Invoke-AwsJson @(
        "ecs",
        "describe-tasks",
        "--cluster", $Cluster,
        "--tasks", $taskArn,
        "--profile", $Profile,
        "--region", $Region
    )

    $task = $taskResult.tasks[0]

    Write-Host "Task status     : $($task.lastStatus)"
    Write-Host "Health status   : $($task.healthStatus)"
    Write-Host "Task definition : $($task.taskDefinitionArn)"

    $container = $task.containers |
        Where-Object {
            $_.name -eq $Service
        } |
        Select-Object -First 1

    if (-not $container) {
        $container = $task.containers |
            Select-Object -First 1
    }

    if (-not $container) {
        throw "Product container not found."
    }

    $script:ProductContainerName = $container.name

    Write-Host "Container       : $script:ProductContainerName"

    if ($task.lastStatus -eq "RUNNING") {

        Write-OK `
            "Product task is RUNNING" `
            "Product task state"

    }
    else {

        Write-Fail `
            "Product task is not RUNNING." `
            "Product task state"
    }

}
catch {

    Write-Fail `
        "Could not inspect Product ECS task." `
        "Product task state"

    Write-Host $_.Exception.Message -ForegroundColor Red

    exit 1
}

# ============================================================
# 9. FIND INVENTORY TASK
# ============================================================

Write-Step "9. FIND RUNNING INVENTORY ECS TASK"

try {

    $inventoryTaskArn = Invoke-AwsText @(
        "ecs",
        "list-tasks",
        "--cluster", $Cluster,
        "--service-name", $InventoryService,
        "--desired-status", "RUNNING",
        "--profile", $Profile,
        "--region", $Region,
        "--query", "taskArns[0]",
        "--output", "text"
    )

    if (
        [string]::IsNullOrWhiteSpace($inventoryTaskArn) -or
        $inventoryTaskArn -eq "None"
    ) {
        throw "No RUNNING Inventory task found."
    }

    $script:InventoryTaskId = `
        $inventoryTaskArn.Split("/")[-1]

    $inventoryTaskResult = Invoke-AwsJson @(
        "ecs",
        "describe-tasks",
        "--cluster", $Cluster,
        "--tasks", $inventoryTaskArn,
        "--profile", $Profile,
        "--region", $Region
    )

    $inventoryTask = $inventoryTaskResult.tasks[0]

    $inventoryContainer = $inventoryTask.containers |
        Where-Object {
            $_.name -eq $InventoryService
        } |
        Select-Object -First 1

    if (-not $inventoryContainer) {
        $inventoryContainer = `
            $inventoryTask.containers |
            Select-Object -First 1
    }

    if (-not $inventoryContainer) {
        throw "Inventory container not found."
    }

    $script:InventoryContainerName = `
        $inventoryContainer.name

    Write-Host "Task ID  : $script:InventoryTaskId"
    Write-Host "Container: $script:InventoryContainerName"

    Write-OK `
        "Running Inventory task found" `
        "Inventory task"

}
catch {

    Write-Fail `
        "Could not find running Inventory task." `
        "Inventory task"

    Write-Host $_.Exception.Message -ForegroundColor Red
}

# ============================================================
# 10. PRODUCT RABBITMQ CONFIGURATION
# ============================================================

Write-Step "10. CHECK PRODUCT RABBITMQ ENVIRONMENT"

try {

    $taskDefinitionResult = Invoke-AwsJson @(
        "ecs",
        "describe-task-definition",
        "--task-definition", $task.taskDefinitionArn,
        "--profile", $Profile,
        "--region", $Region
    )

    $containerDefinition = `
        $taskDefinitionResult.taskDefinition.containerDefinitions |
        Where-Object {
            $_.name -eq $script:ProductContainerName
        } |
        Select-Object -First 1

    if (-not $containerDefinition) {
        throw "Product container definition not found."
    }

    $environment = @{}

    foreach ($item in $containerDefinition.environment) {

        $environment[$item.name] = $item.value
    }

    $hostValue = $environment["RABBITMQ_HOST"]
    $portValue = $environment["RABBITMQ_PORT"]
    $tlsValue  = $environment["RABBITMQ_TLS"]

    Write-Host ""
    Write-Host "RABBITMQ_HOST = $hostValue"
    Write-Host "RABBITMQ_PORT = $portValue"
    Write-Host "RABBITMQ_TLS  = $tlsValue"

    if ([string]::IsNullOrWhiteSpace($hostValue)) {

        Write-Fail `
            "RABBITMQ_HOST is missing." `
            "Product RabbitMQ host"

    }
    elseif ($hostValue -eq $script:RabbitHost) {

        Write-OK `
            "RABBITMQ_HOST matches Amazon MQ endpoint" `
            "Product RabbitMQ host"

    }
    else {

        Write-Fail `
            "RABBITMQ_HOST does not match Amazon MQ endpoint." `
            "Product RabbitMQ host"

        Write-Host "Expected: $script:RabbitHost"
        Write-Host "Actual  : $hostValue"
    }

    if ($portValue -eq "5671") {

        Write-OK `
            "RABBITMQ_PORT is 5671" `
            "Product RabbitMQ port"

    }
    else {

        Write-Fail `
            "RABBITMQ_PORT is '$portValue'." `
            "Product RabbitMQ port"
    }

    if ($tlsValue -eq "true") {

        Write-OK `
            "RABBITMQ_TLS is true" `
            "Product RabbitMQ TLS"

    }
    else {

        Write-Fail `
            "RABBITMQ_TLS is '$tlsValue'." `
            "Product RabbitMQ TLS"
    }

}
catch {

    Write-Fail `
        "Could not inspect Product RabbitMQ environment." `
        "Product RabbitMQ configuration"

    Write-Host $_.Exception.Message -ForegroundColor Red
}

# ============================================================
# 11. PRODUCT ECS NETWORK
# ============================================================

Write-Step "11. CHECK PRODUCT ECS NETWORK"

try {

    $eniAttachment = $task.attachments |
        Where-Object {
            $_.type -eq "ElasticNetworkInterface"
        } |
        Select-Object -First 1

    if (-not $eniAttachment) {
        throw "Product ECS ENI not found."
    }

    $eniId = (
        $eniAttachment.details |
        Where-Object {
            $_.name -eq "networkInterfaceId"
        }
    ).value

    $eniResult = Invoke-AwsJson @(
        "ec2",
        "describe-network-interfaces",
        "--network-interface-ids", $eniId,
        "--profile", $Profile,
        "--region", $Region
    )

    $eni = $eniResult.NetworkInterfaces[0]

    Write-Host "ENI       : $eniId"
    Write-Host "Private IP: $($eni.PrivateIpAddress)"

    foreach ($group in $eni.Groups) {

        Write-Host "Product SG : $($group.GroupId) ($($group.GroupName))"
    }

    Write-OK `
        "Product ECS network information found" `
        "Product ECS network"

}
catch {

    Write-Fail `
        "Could not inspect Product ECS network." `
        "Product ECS network"

    Write-Host $_.Exception.Message -ForegroundColor Red
}

# ============================================================
# 12. RABBITMQ SECURITY GROUP
# ============================================================

Write-Step "12. CHECK RABBITMQ SECURITY GROUP"

try {

    if (-not $broker.SecurityGroups) {

        Write-Warn `
            "No RabbitMQ security groups returned." `
            "RabbitMQ security group"

    }
    else {

        foreach ($rabbitSgId in $broker.SecurityGroups) {

            Write-Host ""
            Write-Host "RabbitMQ SG: $rabbitSgId"

            $sgResult = Invoke-AwsJson @(
                "ec2",
                "describe-security-groups",
                "--group-ids", $rabbitSgId,
                "--profile", $Profile,
                "--region", $Region
            )

            $sg = $sgResult.SecurityGroups[0]

            $rabbitRules = $sg.IpPermissions |
                Where-Object {
                    $_.IpProtocol -eq "tcp" -and
                    $_.FromPort -le 5671 -and
                    $_.ToPort -ge 5671
                }

            if ($rabbitRules) {

                Write-OK `
                    "TCP 5671 ingress rule exists" `
                    "RabbitMQ TCP 5671"

                foreach ($rule in $rabbitRules) {

                    foreach ($pair in $rule.UserIdGroupPairs) {

                        Write-Host `
                            "  Source SG : $($pair.GroupId)"
                    }

                    foreach ($range in $rule.IpRanges) {

                        Write-Host `
                            "  Source CIDR: $($range.CidrIp)"
                    }
                }
            }
            else {

                Write-Fail `
                    "No TCP 5671 ingress rule." `
                    "RabbitMQ TCP 5671"
            }
        }
    }
}
catch {

    Write-Fail `
        "Could not inspect RabbitMQ security group." `
        "RabbitMQ security group"

    Write-Host $_.Exception.Message -ForegroundColor Red
}
# ============================================================
# 13. ECS EXEC - DNS / TCP / TLS
# ============================================================

Write-Step "13. TEST PRODUCT ECS -> RABBITMQ"

if (-not $ecsService.enableExecuteCommand) {

    Write-Skip `
        "ECS Exec is disabled. Network test cannot be executed inside Product." `
        "ECS network execution"

}
else {

    Write-Host ""
    Write-Host "Testing from inside Product container..." -ForegroundColor Yellow
    Write-Host ""

    # IMPORTANT:
    # Use a single-line shell command.
    # Do NOT allow PowerShell to expand $RABBITMQ_HOST etc.
    # inside the local PowerShell process.
    #
    # The backslash before $ prevents PowerShell interpolation.

    $remoteCommand = 'sh -c ''echo "=== ENVIRONMENT ==="; echo "RABBITMQ_HOST=$RABBITMQ_HOST"; echo "RABBITMQ_PORT=$RABBITMQ_PORT"; echo "RABBITMQ_TLS=$RABBITMQ_TLS"; echo ""; echo "=== DNS ==="; if command -v getent >/dev/null 2>&1; then getent hosts "$RABBITMQ_HOST"; elif command -v nslookup >/dev/null 2>&1; then nslookup "$RABBITMQ_HOST"; else echo "DNS_TOOLS_NOT_AVAILABLE"; fi; echo ""; echo "=== TCP 5671 ==="; if command -v nc >/dev/null 2>&1; then nc -zvw5 "$RABBITMQ_HOST" 5671; elif command -v timeout >/dev/null 2>&1; then timeout 5 sh -c "cat < /dev/null > /dev/tcp/$RABBITMQ_HOST/5671"; else echo "TCP_TOOLS_NOT_AVAILABLE"; fi; echo ""; echo "=== TLS ==="; if command -v openssl >/dev/null 2>&1; then echo | timeout 10 openssl s_client -connect "$RABBITMQ_HOST:5671" -servername "$RABBITMQ_HOST" -brief 2>&1; else echo "OPENSSL_NOT_AVAILABLE"; fi;'' '

    try {

        Write-Host "Executing remote diagnostic..." -ForegroundColor DarkGray
        Write-Host ""

        & aws ecs execute-command `
            --cluster $Cluster `
            --task $script:ProductTaskId `
            --container $script:ProductContainerName `
            --interactive `
            --command $remoteCommand `
            --profile $Profile `
            --region $Region

        if ($LASTEXITCODE -eq 0) {

            Write-OK `
                "ECS Exec RabbitMQ network test completed" `
                "Product to RabbitMQ network"

        }
        else {

            Write-Fail `
                "ECS Exec returned exit code $LASTEXITCODE." `
                "Product to RabbitMQ network"
        }

    }
    catch {

        Write-Fail `
            "ECS Exec RabbitMQ test failed." `
            "Product to RabbitMQ network"

        Write-Host $_.Exception.Message -ForegroundColor Red
    }
}
# ============================================================
# 14. RABBITMQ MANAGEMENT API
# ============================================================

Write-Step "14. CHECK RABBITMQ MANAGEMENT API"

if ([string]::IsNullOrWhiteSpace($RabbitManagementUrl)) {

    Write-Skip `
        "RabbitManagementUrl was not supplied." `
        "RabbitMQ Management API"

}
else {

    Write-Host "Management URL: $RabbitManagementUrl"

    try {

        $overview = Invoke-RabbitManagement `
            -Path "/api/overview"

        Write-Host "RabbitMQ version: $($overview.rabbitmq_version)"

        Write-OK `
            "RabbitMQ Management API is reachable" `
            "RabbitMQ Management API"

    }
    catch {

        Write-Fail `
            "RabbitMQ Management API is not reachable or credentials are invalid." `
            "RabbitMQ Management API"

        Write-Host $_.Exception.Message -ForegroundColor Red
    }
}

# ============================================================
# 15. EXCHANGE VALIDATION
# ============================================================

Write-Step "15. CHECK RABBITMQ EXCHANGE"

if ([string]::IsNullOrWhiteSpace($RabbitManagementUrl)) {

    Write-Skip `
        "Cannot inspect exchange automatically without RabbitMQ Management API." `
        "RabbitMQ exchange"

}
else {

    try {

        $exchangePath = `
            "/api/exchanges/%2F/$([uri]::EscapeDataString($Exchange))"

        $exchangeInfo = Invoke-RabbitManagement `
            -Path $exchangePath

        Write-Host "Exchange : $Exchange"
        Write-Host "Type     : $($exchangeInfo.type)"
        Write-Host "Durable  : $($exchangeInfo.durable)"

        Write-OK `
            "Exchange '$Exchange' exists" `
            "RabbitMQ exchange"

    }
    catch {

        Write-Fail `
            "Exchange '$Exchange' was not found." `
            "RabbitMQ exchange"

        Write-Host $_.Exception.Message -ForegroundColor Red
    }
}

# ============================================================
# 16. QUEUE DISCOVERY / VALIDATION
# ============================================================

Write-Step "16. CHECK RABBITMQ QUEUE"

if ([string]::IsNullOrWhiteSpace($RabbitManagementUrl)) {

    Write-Skip `
        "Cannot inspect queue automatically without RabbitMQ Management API." `
        "RabbitMQ queue"

}
else {

    try {

        if ([string]::IsNullOrWhiteSpace($Queue)) {

            Write-Host "Queue parameter was not supplied."
            Write-Host "Discovering queues..." -ForegroundColor Yellow

            $queues = Invoke-RabbitManagement `
                -Path "/api/queues/%2F"

            if (-not $queues) {

                throw "No RabbitMQ queues were returned."
            }

            Write-Host ""
            Write-Host "Available queues:" -ForegroundColor Yellow

            foreach ($q in $queues) {

                Write-Host `
                    "  $($q.name) | messages=$($q.messages) | consumers=$($q.consumers)"
            }

            Write-Skip `
                "Queue name was not supplied. Queue discovery completed." `
                "RabbitMQ queue"

        }
        else {

            $queuePath = `
                "/api/queues/%2F/$([uri]::EscapeDataString($Queue))"

            $queueInfo = Invoke-RabbitManagement `
                -Path $queuePath

            Write-Host "Queue      : $Queue"
            Write-Host "Messages   : $($queueInfo.messages)"
            Write-Host "Consumers  : $($queueInfo.consumers)"
            Write-Host "Ready      : $($queueInfo.messages_ready)"
            Write-Host "Unacked    : $($queueInfo.messages_unacknowledged)"

            Write-OK `
                "Queue '$Queue' exists" `
                "RabbitMQ queue"
        }
    }
    catch {

        Write-Fail `
            "Could not validate RabbitMQ queue." `
            "RabbitMQ queue"

        Write-Host $_.Exception.Message -ForegroundColor Red
    }
}

# ============================================================
# 17. ROUTING / BINDING VALIDATION
# ============================================================

Write-Step "17. CHECK EXCHANGE -> QUEUE BINDING"

if (
    [string]::IsNullOrWhiteSpace($RabbitManagementUrl) -or
    [string]::IsNullOrWhiteSpace($Queue)
) {

    Write-Skip `
        "Management API and Queue are required to validate bindings." `
        "RabbitMQ binding"

}
else {

    try {

        $bindingsPath = `
            "/api/bindings/%2F/e/$([uri]::EscapeDataString($Exchange))/q/$([uri]::EscapeDataString($Queue))"

        $bindings = Invoke-RabbitManagement `
            -Path $bindingsPath

        $matchingBinding = $bindings |
            Where-Object {
                $_.routing_key -eq $RoutingKey
            } |
            Select-Object -First 1

        if ($matchingBinding) {

            Write-Host "Exchange   : $Exchange"
            Write-Host "Queue      : $Queue"
            Write-Host "Routing key: $RoutingKey"

            Write-OK `
                "Exchange -> Queue binding exists" `
                "RabbitMQ binding"

        }
        else {

            Write-Fail `
                "No binding found for routing key '$RoutingKey'." `
                "RabbitMQ binding"
        }

    }
    catch {

        Write-Fail `
            "Could not inspect RabbitMQ bindings." `
            "RabbitMQ binding"

        Write-Host $_.Exception.Message -ForegroundColor Red
    }
}

# ============================================================
# 18. PUBLISH DIAGNOSTIC MESSAGE
# ============================================================

Write-Step "18. PUBLISH DIAGNOSTIC MESSAGE"

$diagnosticPayload = [ordered]@{
    eventType     = "diagnostic.rabbitmq.test"
    correlationId = $script:CorrelationIdValue
    source        = "rabbitmq-agent"
    timestamp     = (Get-Date).ToUniversalTime().ToString("o")
}

if (-not [string]::IsNullOrWhiteSpace($PublishUrl)) {

    Write-Host ""
    Write-Host "Publishing through Product API..." -ForegroundColor Yellow
    Write-Host "URL         : $PublishUrl"
    Write-Host "Method      : $PublishMethod"
    Write-Host "Correlation : $script:CorrelationIdValue"

    try {

        $body = $PublishBody

        if ([string]::IsNullOrWhiteSpace($body)) {

            $body = `
                $diagnosticPayload |
                ConvertTo-Json -Depth 10
        }

        $response = Invoke-RestMethod `
            -Uri $PublishUrl `
            -Method $PublishMethod `
            -ContentType "application/json" `
            -Body $body `
            -TimeoutSec $PublishTimeoutSeconds `
            -ErrorAction Stop

        Write-Host ""
        Write-Host "Product API response:" -ForegroundColor DarkGray

        if ($response) {
            $response | ConvertTo-Json -Depth 10
        }

        Write-OK `
            "Product API accepted diagnostic request" `
            "Product publish request"

    }
    catch {

        Write-Fail `
            "Product API publish request failed." `
            "Product publish request"

        Write-Host $_.Exception.Message -ForegroundColor Red
    }

}
elseif (
    -not [string]::IsNullOrWhiteSpace($RabbitManagementUrl) -and
    -not [string]::IsNullOrWhiteSpace($RabbitUser) -and
    -not [string]::IsNullOrWhiteSpace($RabbitPassword)
) {

    Write-Host ""
    Write-Host "Publishing directly through RabbitMQ Management API..." `
        -ForegroundColor Yellow

    try {

        $publishPath = `
            "/api/exchanges/%2F/$([uri]::EscapeDataString($Exchange))/publish"

        $publishBody = [ordered]@{
            properties       = @{}
            routing_key      = $RoutingKey
            payload          = (
                $diagnosticPayload |
                ConvertTo-Json -Depth 10 -Compress
            )
            payload_encoding = "string"
        }

        $publishResult = Invoke-RabbitManagement `
            -Path $publishPath `
            -Method "POST" `
            -Body $publishBody

        Write-Host "RabbitMQ publish response:"
        $publishResult | ConvertTo-Json -Depth 10

        if ($publishResult.routed -eq $true) {

            Write-OK `
                "RabbitMQ accepted and routed diagnostic message" `
                "RabbitMQ publish"

        }
        else {

            Write-Fail `
                "RabbitMQ accepted the publish request but did not route the message." `
                "RabbitMQ publish"
        }

    }
    catch {

        Write-Fail `
            "RabbitMQ diagnostic publish failed." `
            "RabbitMQ publish"

        Write-Host $_.Exception.Message -ForegroundColor Red
    }

}
else {

    Write-Skip `
        "No PublishUrl or RabbitMQ Management API credentials were supplied. Publish was not tested." `
        "RabbitMQ publish"
}

# ============================================================
# 19. CLOUDWATCH LOG GROUP DISCOVERY
# ============================================================

Write-Step "19. DISCOVER CLOUDWATCH LOG GROUPS"

try {

    $taskDefinition = `
        $taskDefinitionResult.taskDefinition

    $logConfiguration = `
        $containerDefinition.logConfiguration

    if ($logConfiguration -and
        $logConfiguration.options) {

        $script:ProductLogGroup = `
            $logConfiguration.options."awslogs-group"
    }

    if ([string]::IsNullOrWhiteSpace($script:ProductLogGroup)) {

        Write-Warn `
            "Product CloudWatch log group could not be determined." `
            "Product CloudWatch log group"

    }
    else {

        Write-Host "Product log group: $script:ProductLogGroup"

        Write-OK `
            "Product CloudWatch log group discovered" `
            "Product CloudWatch log group"
    }

}
catch {

    Write-Warn `
        "Could not determine Product CloudWatch log group." `
        "Product CloudWatch log group"
}

# ============================================================
# 20. INVENTORY CLOUDWATCH LOG GROUP
# ============================================================

try {

    if ($inventoryTask) {

        $inventoryTaskDefinitionResult = Invoke-AwsJson @(
            "ecs",
            "describe-task-definition",
            "--task-definition",
            $inventoryTask.taskDefinitionArn,
            "--profile",
            $Profile,
            "--region",
            $Region
        )

        $inventoryContainerDefinition = `
            $inventoryTaskDefinitionResult.taskDefinition.containerDefinitions |
            Where-Object {
                $_.name -eq $script:InventoryContainerName
            } |
            Select-Object -First 1

        if ($inventoryContainerDefinition.logConfiguration) {

            $script:InventoryLogGroup = `
                $inventoryContainerDefinition.logConfiguration.options."awslogs-group"
        }

        if ($script:InventoryLogGroup) {

            Write-Host "Inventory log group: $script:InventoryLogGroup"

            Write-OK `
                "Inventory CloudWatch log group discovered" `
                "Inventory CloudWatch log group"

        }
        else {

            Write-Warn `
                "Inventory CloudWatch log group could not be determined." `
                "Inventory CloudWatch log group"
        }
    }

}
catch {

    Write-Warn `
        "Could not determine Inventory CloudWatch log group." `
        "Inventory CloudWatch log group"
}

# ============================================================
# 21. PRODUCT CLOUDWATCH CORRELATION
# ============================================================

Write-Step "21. SEARCH PRODUCT CLOUDWATCH FOR CORRELATION ID"

if ([string]::IsNullOrWhiteSpace($script:ProductLogGroup)) {

    Write-Skip `
        "Product CloudWatch log group is unavailable." `
        "Product correlation"

}
else {

    try {

        $productLogs = & aws logs filter-log-events `
            --log-group-name $script:ProductLogGroup `
            --filter-pattern $script:CorrelationIdValue `
            --start-time (
                [DateTimeOffset]::UtcNow.AddMinutes(
                    -$LogMinutes
                ).ToUnixTimeMilliseconds()
            ) `
            --profile $Profile `
            --region $Region `
            --output json 2>&1

        if ($LASTEXITCODE -ne 0) {

            throw ($productLogs -join "`n")
        }

        $productLogResult = `
            ($productLogs -join "`n") |
            ConvertFrom-Json

        $productEvents = $productLogResult.events

        if ($productEvents -and
            $productEvents.Count -gt 0) {

            Write-Host ""
            Write-Host "Correlation ID found in Product logs:" `
                -ForegroundColor Green

            foreach ($event in $productEvents) {

                Write-Host $event.message
            }

            Write-OK `
                "Correlation ID found in Product CloudWatch logs" `
                "Product correlation"

        }
        else {

            Write-Warn `
                "Correlation ID was not found in Product logs." `
                "Product correlation"
        }

    }
    catch {

        Write-Warn `
            "Could not search Product CloudWatch logs." `
            "Product correlation"

        Write-Host $_.Exception.Message -ForegroundColor Yellow
    }
}

# ============================================================
# 22. INVENTORY CLOUDWATCH CORRELATION
# ============================================================

Write-Step "22. SEARCH INVENTORY CLOUDWATCH FOR CORRELATION ID"

if ([string]::IsNullOrWhiteSpace($script:InventoryLogGroup)) {

    Write-Skip `
        "Inventory CloudWatch log group is unavailable." `
        "Inventory correlation"

}
else {

    $deadline = `
        (Get-Date).ToUniversalTime().AddSeconds(
            $CorrelationTimeoutSeconds
        )

    $foundInventoryCorrelation = $false

    Write-Host ""
    Write-Host "Waiting for Inventory correlation..." `
        -ForegroundColor Yellow

    while (
        (Get-Date).ToUniversalTime() -lt $deadline
    ) {

        try {

            $inventoryLogs = & aws logs filter-log-events `
                --log-group-name $script:InventoryLogGroup `
                --filter-pattern $script:CorrelationIdValue `
                --start-time (
                    [DateTimeOffset]::UtcNow.AddMinutes(
                        -$LogMinutes
                    ).ToUnixTimeMilliseconds()
                ) `
                --profile $Profile `
                --region $Region `
                --output json 2>&1

            if ($LASTEXITCODE -eq 0) {

                $inventoryLogResult = `
                    ($inventoryLogs -join "`n") |
                    ConvertFrom-Json

                $inventoryEvents = `
                    $inventoryLogResult.events

                if (
                    $inventoryEvents -and
                    $inventoryEvents.Count -gt 0
                ) {

                    Write-Host ""
                    Write-Host "Correlation ID found in Inventory logs:" `
                        -ForegroundColor Green

                    foreach ($event in $inventoryEvents) {

                        Write-Host $event.message
                    }

                    $foundInventoryCorrelation = $true

                    break
                }
            }

        }
        catch {
            # Continue polling.
        }

        Start-Sleep -Seconds 5
    }

    if ($foundInventoryCorrelation) {

        Write-OK `
            "Inventory received the diagnostic correlation ID" `
            "Inventory correlation"

    }
    else {

        Write-Fail `
            "Inventory did not receive correlation ID within $CorrelationTimeoutSeconds seconds." `
            "Inventory correlation"
    }
}

# ============================================================
# 23. FULL CLOUDWATCH OUTPUT
# ============================================================

if ($ShowLogs) {

    Write-Step "23. CLOUDWATCH LOGS"

    if ($script:ProductLogGroup) {

        Write-Host ""
        Write-Host "PRODUCT LOGS: $script:ProductLogGroup" `
            -ForegroundColor Yellow

        Write-Host ""

        try {

            & aws logs tail `
                $script:ProductLogGroup `
                --since "${LogMinutes}m" `
                --profile $Profile `
                --region $Region

        }
        catch {

            Write-Warn `
                "Could not retrieve Product logs."
        }
    }

    if ($script:InventoryLogGroup) {

        Write-Host ""
        Write-Host "INVENTORY LOGS: $script:InventoryLogGroup" `
            -ForegroundColor Yellow

        Write-Host ""

        try {

            & aws logs tail `
                $script:InventoryLogGroup `
                --since "${LogMinutes}m" `
                --profile $Profile `
                --region $Region

        }
        catch {

            Write-Warn `
                "Could not retrieve Inventory logs."
        }
    }
}

# ============================================================
# 24. FINAL DIAGNOSTIC SUMMARY
# ============================================================

Write-Step "24. FINAL DIAGNOSTIC SUMMARY"

Write-Host ""

Write-Host "AWS Account        : $($identity.Account)"
Write-Host "Cluster            : $Cluster"
Write-Host "Product Service    : $Service"
Write-Host "Product Task       : $script:ProductTaskId"
Write-Host "Inventory Service  : $InventoryService"
Write-Host "Inventory Task     : $script:InventoryTaskId"
Write-Host "RabbitMQ Broker    : $BrokerName"
Write-Host "RabbitMQ Broker ID : $brokerId"
Write-Host "RabbitMQ State     : $($broker.BrokerState)"
Write-Host "RabbitMQ Host      : $script:RabbitHost"
Write-Host "RabbitMQ Port      : 5671"
Write-Host "Exchange           : $Exchange"
Write-Host "Routing Key        : $RoutingKey"
Write-Host "Queue              : $Queue"
Write-Host "Correlation ID     : $script:CorrelationIdValue"

Write-Host ""

Write-Host "------------------------------------------------------------"
Write-Host "CHECK RESULTS"
Write-Host "------------------------------------------------------------"

foreach ($check in $script:Checks) {

    $status = $check.Status.PadRight(4)

    switch ($check.Status) {

        "PASS" {
            Write-Host `
                "[PASS] $($check.Name)" `
                -ForegroundColor Green
        }

        "FAIL" {
            Write-Host `
                "[FAIL] $($check.Name)" `
                -ForegroundColor Red
        }

        "WARN" {
            Write-Host `
                "[WARN] $($check.Name)" `
                -ForegroundColor Yellow
        }

        "SKIP" {
            Write-Host `
                "[SKIP] $($check.Name)" `
                -ForegroundColor DarkYellow
        }
    }
}

$failedChecks = @(
    $script:Checks |
    Where-Object {
        $_.Status -eq "FAIL"
    }
)

$passedChecks = @(
    $script:Checks |
    Where-Object {
        $_.Status -eq "PASS"
    }
)

$skippedChecks = @(
    $script:Checks |
    Where-Object {
        $_.Status -eq "SKIP"
    }
)

Write-Host ""
Write-Host "------------------------------------------------------------"
Write-Host "COUNTS"
Write-Host "------------------------------------------------------------"

Write-Host "PASS : $($passedChecks.Count)"
Write-Host "FAIL : $($failedChecks.Count)"
Write-Host "SKIP : $($skippedChecks.Count)"

Write-Host ""

# ============================================================
# END-TO-END RESULT
# ============================================================

$publishCheck = $script:Checks |
    Where-Object {
        $_.Name -eq "RabbitMQ publish" -or
        $_.Name -eq "Product publish request"
    } |
    Select-Object -Last 1

$inventoryCheck = $script:Checks |
    Where-Object {
        $_.Name -eq "Inventory correlation"
    } |
    Select-Object -Last 1

$endToEndPassed = (
    $null -ne $publishCheck -and
    $publishCheck.Status -eq "PASS" -and
    $null -ne $inventoryCheck -and
    $inventoryCheck.Status -eq "PASS"
)

Write-Host ""
Write-Host "============================================================"

if ($endToEndPassed) {

    Write-Host `
        "       END-TO-END RESULT: PASS" `
        -ForegroundColor Green

    Write-Host ""
    Write-Host "Product -> RabbitMQ -> Inventory is working." `
        -ForegroundColor Green

}
elseif (
    $null -ne $publishCheck -and
    $publishCheck.Status -eq "FAIL"
) {

    Write-Host `
        "       END-TO-END RESULT: FAIL - PUBLISH" `
        -ForegroundColor Red

    Write-Host ""
    Write-Host `
        "Product did not successfully publish the diagnostic event." `
        -ForegroundColor Red
}
elseif (
    $null -ne $publishCheck -and
    $publishCheck.Status -eq "PASS" -and
    $null -ne $inventoryCheck -and
    $inventoryCheck.Status -eq "FAIL"
) {

    Write-Host `
        "       END-TO-END RESULT: FAIL - CONSUMPTION" `
        -ForegroundColor Red

    Write-Host ""
    Write-Host `
        "RabbitMQ accepted the message, but Inventory did not show the correlation ID." `
        -ForegroundColor Red
}
else {

    Write-Host `
        "       END-TO-END RESULT: INCOMPLETE" `
        -ForegroundColor Yellow

    Write-Host ""
    Write-Host `
        "Publish validation was not executed or required configuration was missing." `
        -ForegroundColor Yellow
}

Write-Host "============================================================"

# ============================================================
# ROOT-CAUSE HINTS
# ============================================================

Write-Host ""
Write-Host "DIAGNOSTIC INTERPRETATION" -ForegroundColor Cyan
Write-Host ""

if (
    $script:Checks |
    Where-Object {
        $_.Name -eq "Product RabbitMQ host" -and
        $_.Status -eq "FAIL"
    }
) {

    Write-Host `
        "[!] Product is probably using the wrong RabbitMQ hostname." `
        -ForegroundColor Yellow

    Write-Host `
        "    Check RABBITMQ_HOST in the ECS task definition."
}

if (
    $script:Checks |
    Where-Object {
        $_.Name -eq "RabbitMQ TCP 5671" -and
        $_.Status -eq "FAIL"
    }
) {

    Write-Host `
        "[!] RabbitMQ security-group connectivity is incorrect." `
        -ForegroundColor Yellow

    Write-Host `
        "    Verify TCP 5671 from the Product ECS security group."
}

if (
    $script:Checks |
    Where-Object {
        $_.Name -eq "RabbitMQ binding" -and
        $_.Status -eq "FAIL"
    }
) {

    Write-Host `
        "[!] Exchange/routing-key/queue topology is incorrect." `
        -ForegroundColor Yellow

    Write-Host `
        "    Check exchange, queue and routing-key configuration."
}

if (
    $null -ne $publishCheck -and
    $publishCheck.Status -eq "PASS" -and
    $null -ne $inventoryCheck -and
    $inventoryCheck.Status -eq "FAIL"
) {

    Write-Host ""
    Write-Host `
        "[!] RabbitMQ publish succeeded but Inventory did not consume the event." `
        -ForegroundColor Yellow

    Write-Host `
        "    Check Inventory consumer configuration, queue binding,"
    Write-Host `
        "    SmallRye Reactive Messaging and Inventory CloudWatch logs."
}

Write-Host ""

if ($ShowLogs) {

    Write-Host `
        "CloudWatch logs were included." `
        -ForegroundColor Cyan
}
else {

    Write-Host `
        "For full CloudWatch logs use:" `
        -ForegroundColor Cyan

    Write-Host ""
    Write-Host `
        ".\rabbitmq-agent.ps1 -ShowLogs"
}

Write-Host ""
Write-Host "Agent finished."
Write-Host ""

