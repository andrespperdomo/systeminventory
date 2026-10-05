param(
    [Parameter(Mandatory=$true)]
    [string]$LogGroup,

    [Parameter(Mandatory=$false)]
    [string]$LogStreamPrefix = "",

    [Parameter(Mandatory=$false)]
    [int]$Minutes = 30,

    [Parameter(Mandatory=$false)]
    [int]$MaxEvents = 100,

    [Parameter(Mandatory=$false)]
    [string]$Profile = "terraform-dev",

    [Parameter(Mandatory=$false)]
    [string]$Region = "us-east-1"
)

$ErrorActionPreference = "Stop"

Write-Host "Collecting CloudWatch logs..." -ForegroundColor Cyan
Write-Host "Log group: $LogGroup"
Write-Host "Minutes: $Minutes"
Write-Host ""

try {

    # ---------------------------------------------------------
    # Calculate time window
    # ---------------------------------------------------------

    $endTime = [DateTimeOffset]::UtcNow
    $startTime = $endTime.AddMinutes(-$Minutes)

    $startMilliseconds = $startTime.ToUnixTimeMilliseconds()
    $endMilliseconds = $endTime.ToUnixTimeMilliseconds()

    # ---------------------------------------------------------
    # Build AWS command
    # ---------------------------------------------------------

    $arguments = @(
        "logs",
        "filter-log-events",
        "--log-group-name",
        $LogGroup,
        "--start-time",
        $startMilliseconds,
        "--end-time",
        $endMilliseconds,
        "--limit",
        $MaxEvents,
        "--profile",
        $Profile,
        "--region",
        $Region,
        "--output",
        "json"
    )

    if ($LogStreamPrefix -ne "") {

        $arguments += @(
            "--log-stream-name-prefix",
            $LogStreamPrefix
        )
    }

    # ---------------------------------------------------------
    # Execute AWS CLI
    # ---------------------------------------------------------

    $raw = & aws @arguments 2>&1

    if ($LASTEXITCODE -ne 0) {
        throw ($raw -join "`n")
    }

    $data = $raw | ConvertFrom-Json

    # ---------------------------------------------------------
    # Process events
    # ---------------------------------------------------------

    $events = @()

    foreach ($event in $data.events) {

        $message = $event.message.Trim()

        $severity = "INFO"

        if (
            $message -match "ERROR" -or
            $message -match "Exception" -or
            $message -match "Failed" -or
            $message -match "Caused by" -or
            $message -match "Connection refused" -or
            $message -match "UnknownHostException" -or
            $message -match "Timeout"
        ) {
            $severity = "ERROR"
        }
        elseif (
            $message -match "WARN" -or
            $message -match "WARNING"
        ) {
            $severity = "WARN"
        }

        $events += [ordered]@{
            timestamp = [DateTimeOffset]::FromUnixTimeMilliseconds(
                $event.timestamp
            ).ToString("o")

            logStream = $event.logStreamName

            severity = $severity

            message = $message
        }
    }

    # ---------------------------------------------------------
    # Extract important errors
    # ---------------------------------------------------------

    $errors = @(
        $events |
        Where-Object {
            $_.severity -eq "ERROR"
        }
    )

    # ---------------------------------------------------------
    # Result
    # ---------------------------------------------------------

    $result = [ordered]@{

        timestamp = (Get-Date).ToUniversalTime().ToString("o")

        logGroup = $LogGroup

        timeWindow = [ordered]@{
            start = $startTime.ToString("o")
            end = $endTime.ToString("o")
            minutes = $Minutes
        }

        totalEvents = $events.Count

        errorCount = $errors.Count

        warningCount = @(
            $events |
            Where-Object { $_.severity -eq "WARN" }
        ).Count

        events = $events

        importantErrors = $errors
    }

    $result | ConvertTo-Json -Depth 20

}
catch {

    $errorResult = [ordered]@{
        timestamp = (Get-Date).ToUniversalTime().ToString("o")
        success = $false
        logGroup = $LogGroup
        error = $_.Exception.Message
    }

    $errorResult | ConvertTo-Json -Depth 20

    exit 1
}