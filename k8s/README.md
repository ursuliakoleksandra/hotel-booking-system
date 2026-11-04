# Лабораторна робота №3. Розгортання hotel-booking-system у Kubernetes

Застосунок складається з трьох мікросервісів на Spring Boot (Java 17, Maven):
`user-service` (порт 8082), `hotel-management` (порт 8081), `booking-service` (порт 8080).
Стан зберігається в PostgreSQL (по одній базі на сервіс), події бронювань передаються через Kafka.
Усі об'єкти Kubernetes створюються в namespace `hotel-booking`.

## Архітектура в кластері

```
Ingress (hotel.local) ──► user-service ───────┐
                      ──► hotel-management ───┼──► postgres (PVC) : userdb / hoteldb / bookingdb
                      ──► booking-service ────┘
                              │  └─► hotel-management (Feign, http://hotel-management:8081)
                              └────► kafka (PVC) : топік booking-events
```

Кожен сервіс має 2 репліки. Репліки не можуть використовувати власну in-memory H2 (кожна мала б свої
дані, і запити розподілялися б між «різними базами»), тому в кластері всі репліки працюють із
спільним PostgreSQL. Локально (без Kubernetes) застосунок, як і раніше, працює на H2.

## Склад каталогу k8s/

| Файл | Об'єкт |
|---|---|
| `00-namespace.yaml` | Namespace `hotel-booking` |
| `<сервіс>-configmap.yaml` | ConfigMap із параметрами сервісу (`user-service`, `hotel-management`, `booking-service`) |
| `<сервіс>-secret.yaml` | Secret із паролем до БД |
| `<сервіс>-deployment.yaml` | Deployment: 2 репліки, RollingUpdate, requests/limits, startup/liveness/readiness-проби |
| `<сервіс>-service.yaml` | Service типу ClusterIP |
| `postgres-*.yaml` | ConfigMap (налаштування і init-скрипт), Secret, PVC 1Gi, Deployment, Service PostgreSQL |
| `kafka-*.yaml` | ConfigMap, PVC 1Gi, Deployment, Service Kafka (KRaft, один вузол) |
| `ingress.yaml` | Ingress (nginx), хост `hotel.local` |

Скрипти збірки образів лежать у `scripts/`. Звіт з командами для пунктів 5–7 — `docs/lab3-report.md`.

## Передумови

- Docker, minikube, kubectl.
- Кластеру потрібно приблизно 4 ГБ пам'яті (6 подів Spring Boot + PostgreSQL + Kafka).

## Порядок розгортання

1. Запустити кластер і доповнення:

```
minikube start --driver=docker --cpus=4 --memory=4096
minikube addons enable metrics-server
minikube addons enable ingress
kubectl wait --namespace ingress-nginx --for=condition=ready pod --selector=app.kubernetes.io/component=controller --timeout=180s
```

2. Зібрати образи версій 1.0.0 і 1.1.0 та завантажити їх у minikube (з кореня репозиторію):

```
bash scripts/build-and-load-images.sh
```

Для Windows PowerShell: `powershell -ExecutionPolicy Bypass -File .\scripts\build-and-load-images.ps1`.

3. Застосувати всі маніфести однією командою:

```
kubectl apply -f k8s/
```

4. Дочекатися готовності (PostgreSQL і Kafka стартують першими, сервіси чекають на БД в init-контейнері):

```
kubectl get pods -n hotel-booking -w
kubectl rollout status deployment/postgres -n hotel-booking
kubectl rollout status deployment/user-service -n hotel-booking
kubectl rollout status deployment/hotel-management -n hotel-booking
kubectl rollout status deployment/booking-service -n hotel-booking
```

5. Доступ із хоста. Додати у файл hosts рядок `127.0.0.1 hotel.local`
(Windows: `C:\Windows\System32\drivers\etc\hosts`, Linux/macOS: `/etc/hosts`; потрібні права адміністратора)
і в окремому терміналі запустити `minikube tunnel`.

Без правки hosts: `kubectl port-forward -n ingress-nginx svc/ingress-nginx-controller 8080:80`, після чого
додавати до запитів заголовок `Host: hotel.local` і звертатися до `localhost:8080`.

## Перевірка роботи

```
curl http://hotel.local/api/v1/hotels
curl -X POST http://hotel.local/api/v1/hotels -H "Content-Type: application/json" \
  -d '{"name":"Grand Hotel","address":"Kyiv, Main st. 1","rating":5}'
curl -X POST http://hotel.local/api/v1/users -H "Content-Type: application/json" \
  -d '{"fullName":"Ivan Petrenko","email":"ivan@example.com"}'
curl -X POST http://hotel.local/api/v1/bookings -H "Content-Type: application/json" \
  -d '{"userId":1,"hotelId":1,"checkIn":"2030-01-10","checkOut":"2030-01-15"}'
curl http://hotel.local/api/v1/bookings
```

Створений запис видно незалежно від того, яка репліка обробила запит (спільна БД). Подію бронювання
можна побачити в логах `booking-service` ("відправлена в Kafka").

Версію образу і ім'я пода, що відповів, показує actuator:

```
kubectl exec -n hotel-booking deploy/hotel-management -- wget -qO- http://localhost:8081/actuator/info
```

Swagger UI окремого сервісу (наприклад, booking-service):

```
kubectl port-forward svc/booking-service 8080:8080 -n hotel-booking
```

Після цього відкрити http://localhost:8080/swagger-ui.html.

## Параметри ConfigMap

Спільні для трьох сервісів (`user-service-config`, `hotel-management-config`, `booking-service-config`):

| Параметр | Значення | Призначення |
|---|---|---|
| `APP_ENV` | `k8s` | Назва середовища |
| `LOGGING_LEVEL_ROOT` | `INFO` | Рівень логування |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://postgres:5432/<userdb\|hoteldb\|bookingdb>` | Адреса бази сервісу |
| `SPRING_DATASOURCE_USERNAME` | `hotel` | Користувач БД |
| `SPRING_H2_CONSOLE_ENABLED` | `false` | Консоль H2 не потрібна з PostgreSQL |
| `MANAGEMENT_ENDPOINT_HEALTH_PROBES_ENABLED` | `true` | Ендпоінти `/actuator/health/liveness` і `/readiness` для проб |

Додатково:

| ConfigMap | Параметр | Значення | Призначення |
|---|---|---|---|
| `booking-service-config` | `HOTEL_SERVICE_URL` | `http://hotel-management:8081/api/v1/hotels` | Адреса hotel-management усередині кластера |
| | `SPRING_KAFKA_BOOTSTRAP_SERVERS` | `kafka:9092` | Адреса Kafka |
| `hotel-management-config` | `SPRING_JPA_SHOW_SQL` | `true` | SQL у логах (видно, яка репліка обробила запит) |
| `user-service-config` | `SPRING_JPA_SHOW_SQL` | `true` | SQL у логах |
| `postgres-config` | `POSTGRES_USER`, `POSTGRES_DB`, `PGDATA` | `hotel`, `hotel`, `/var/lib/postgresql/data/pgdata` | Налаштування PostgreSQL |
| `postgres-init` | `init.sql` | `CREATE DATABASE userdb/hoteldb/bookingdb` | Створення баз при першому запуску |
| `kafka-config` | `KAFKA_*` | див. файл | Одновузловий Kafka у режимі KRaft |

## Параметри Secret

| Secret | Параметр | Призначення |
|---|---|---|
| `user-service-secret` | `SPRING_DATASOURCE_PASSWORD` | Пароль до БД (демонстраційне значення) |
| `hotel-management-secret` | `SPRING_DATASOURCE_PASSWORD` | Пароль до БД (демонстраційне значення) |
| `booking-service-secret` | `SPRING_DATASOURCE_PASSWORD` | Пароль до БД (демонстраційне значення) |
| `postgres-secret` | `POSTGRES_PASSWORD` | Пароль користувача PostgreSQL (збігається з паролями сервісів) |

Значення в Secret навчальні і не є паролями реальних систем.

## Версії образів

Образи `hotel-booking-system/<сервіс>:1.0.0` і `:1.1.0` збираються з одного коду й відрізняються
аргументом збірки `APP_VERSION` (змінна оточення в контейнері, мітка образу та поле `app.version` в
`/actuator/info`). Тег `latest` не використовується.

## Відомі обмеження

- Паролі в Secret демонстраційні; для реального середовища потрібні зовнішні сховища секретів.
- Outbox-релей працює за принципом at-least-once: при кількох репліках `booking-service` подія
  може бути відправлена в Kafka більше одного разу, тому споживачі мають бути ідемпотентними.
- PostgreSQL і Kafka запущені в одному екземплярі (достатньо для навчального кластера).

## Видалення

```
kubectl delete namespace hotel-booking
```
