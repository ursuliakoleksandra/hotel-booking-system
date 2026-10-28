$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $Root

$services = @("user-service", "hotel-management", "booking-service", "ui")
$versions = @("1.0.0", "1.1.0")

foreach ($svc in $services) {
    $image = "hotel-booking-system/${svc}"
    foreach ($ver in $versions) {
        Write-Host "==> Building ${image}:${ver}"
        docker build -f "$svc/Dockerfile" --build-arg "APP_VERSION=$ver" -t "${image}:${ver}" .
        if ($LASTEXITCODE -ne 0) { throw "docker build failed for ${image}:${ver}" }

        Write-Host "==> Loading ${image}:${ver} into minikube"
        minikube image load "${image}:${ver}"
        if ($LASTEXITCODE -ne 0) { throw "minikube image load failed for ${image}:${ver}" }
    }
}

Write-Host "Done. Images loaded into minikube:"
minikube image ls | Select-String "hotel-booking-system"
