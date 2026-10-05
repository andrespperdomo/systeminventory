param(
    [Parameter(Mandatory=$true)]
    [string]$Repository,

    [Parameter(Mandatory=$false)]
    [string]$ImageTag = "1.0",

    [Parameter(Mandatory=$false)]
    [string]$Profile = "terraform-dev",

    [Parameter(Mandatory=$false)]
    [string]$Region = "us-east-1"
)

$ErrorActionPreference = "Stop"

Write-Host "Collecting ECR information..." -ForegroundColor Cyan
Write-Host "Repository: $Repository"
Write-Host "Tag: $ImageTag"
Write-Host ""

try {

    # ---------------------------------------------------------
    # Repository information
    # ---------------------------------------------------------

    $repoRaw = aws ecr describe-repositories `
        --repository-names $Repository `
        --profile $Profile `
        --region $Region `
        --output json 2>&1

    if ($LASTEXITCODE -ne 0) {
        throw "ECR repository does not exist or cannot be accessed."
    }

    $repoData = $repoRaw | ConvertFrom-Json
    $repo = $repoData.repositories[0]

    # ---------------------------------------------------------
    # Image information
    # ---------------------------------------------------------

    $imageRaw = aws ecr describe-images `
        --repository-name $Repository `
        --image-ids imageTag=$ImageTag `
        --profile $Profile `
        --region $Region `
        --output json 2>&1

    $imageExists = $false
    $image = $null

    if ($LASTEXITCODE -eq 0) {

        $imageData = $imageRaw | ConvertFrom-Json

        if ($imageData.imageDetails.Count -gt 0) {
            $imageExists = $true
            $image = $imageData.imageDetails[0]
        }
    }

    # ---------------------------------------------------------
    # Latest images
    # ---------------------------------------------------------

    $latestRaw = aws ecr describe-images `
        --repository-name $Repository `
        --profile $Profile `
        --region $Region `
        --query "sort_by(imageDetails,&imagePushedAt)[-10:]" `
        --output json

    $latestImages = $latestRaw | ConvertFrom-Json

    # ---------------------------------------------------------
    # Build result
    # ---------------------------------------------------------

    $result = [ordered]@{

        timestamp = (Get-Date).ToUniversalTime().ToString("o")

        repository = [ordered]@{
            name = $repo.repositoryName
            uri = $repo.repositoryUri
            registryId = $repo.registryId
            createdAt = $repo.createdAt
            imageTagMutability = $repo.imageTagMutability
            scanOnPush = $repo.imageScanningConfiguration.scanOnPush
        }

        requestedImage = [ordered]@{
            tag = $ImageTag
            exists = $imageExists
        }

        image = if ($imageExists) {
            [ordered]@{
                digest = $image.imageDigest
                tags = $image.imageTags
                pushedAt = $image.imagePushedAt
                sizeBytes = $image.imageSizeInBytes
                mediaType = $image.imageManifestMediaType
            }
        }
        else {
            $null
        }

        latestImages = $latestImages | ForEach-Object {
            [ordered]@{
                digest = $_.imageDigest
                tags = $_.imageTags
                pushedAt = $_.imagePushedAt
                sizeBytes = $_.imageSizeInBytes
            }
        }
    }

    $result | ConvertTo-Json -Depth 20

}
catch {

    $errorResult = [ordered]@{
        timestamp = (Get-Date).ToUniversalTime().ToString("o")
        success = $false
        repository = $Repository
        requestedTag = $ImageTag
        error = $_.Exception.Message
    }

    $errorResult | ConvertTo-Json -Depth 20

    exit 1
}