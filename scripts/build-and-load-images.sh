# DVO-L3-7F2A91
# Збірка образів 1.0.0 та 1.1.0 і завантаження їх у minikube (тег latest не використовується).
# Запуск з кореня репозиторію: bash scripts/build-and-load-images.sh
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

services="user-service hotel-management booking-service"
versions="1.0.0 1.1.0"

for svc in $services; do
  image="hotel-booking-system/${svc}"
  for ver in $versions; do
    echo "==> Building ${image}:${ver}"
    docker build -f "${svc}/Dockerfile" --build-arg APP_VERSION="${ver}" -t "${image}:${ver}" .

    echo "==> Loading ${image}:${ver} into minikube"
    minikube image load "${image}:${ver}"
  done
done

echo "Done. Images loaded into minikube:"
minikube image ls | grep hotel-booking-system || true
