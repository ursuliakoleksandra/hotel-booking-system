# DVO-L3-7F2A91
# Збірка образів 1.0.0 та 1.1.0 і завантаження їх у minikube (тег latest не використовується).
# Запуск з кореня репозиторію: powershell -ExecutionPolicy Bypass -File .\scripts\build-and-load-images.ps1
$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $Root

$services = @("user-service", "hotel-management", "booking-service")
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
