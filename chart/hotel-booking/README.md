# Helm-чарт hotel-booking

Чарт описує той самий застосунок, що й маніфести з `k8s/` (лабораторна №3): три Spring Boot сервіси
(`user-service`, `hotel-management`, `booking-service`), вебінтерфейс `ui`, PostgreSQL і Kafka.
Параметризація дозволяє розгортати його в різних середовищах (`values-dev.yaml`, `values-prod.yaml`).

## Що створює чарт

| Компонент | Об'єкти |
|---|---|
| user-service, hotel-management, booking-service | ConfigMap, Secret, Service, Deployment (файли `app-*.yaml`; один шаблон на вид об'єкта, що перебирає `services` у values) |
| ui | ConfigMap (nginx), Service, Deployment |
| postgres | ConfigMap, ConfigMap з init-скриптом, Secret, PVC, Service, Deployment |
| kafka | ConfigMap, PVC, Service, Deployment |
| ingress | Ingress (лише при `ingress.enabled=true`) |
| хук і тест | Job `*-preupgrade-check` (pre-upgrade), Pod `*-test` (`helm test`) |

Namespace чартом не створюється: його створює `helm install --create-namespace -n <namespace>`.
У лабораторній №3 це був окремий об'єкт `00-namespace.yaml`.

## Різниця між version і appVersion

- `version` (зараз `0.1.0`) є версією **чарта**: шаблонів і значень. Вона зростає, коли змінюється спосіб розгортання,
  наприклад з'явився новий шаблон, прапорець або змінилися ресурси.
- `appVersion` (зараз `1.0.0`) є версією **застосунку**: тегом образів `hotel-booking-system/*`. Вона зростає, коли
  виходить нова версія коду. Якщо `image.tag` порожній, використовується саме `appVersion`.

Приклад: образ збудовано з виправленням коду, чарт не змінювався, тож змінюється лише `appVersion` (і тег). Додано нове
поле в values, тож змінюється лише `version`.

## Підготовка образів

Образи збираються тим самим скриптом, що й у лабораторній №3 (разом з UI), з кореня репозиторію:

```
bash scripts/build-and-load-images.sh
```

Тегів `1.0.0` і `1.1.0` достатньо: dev використовує `1.0.0`, prod використовує `1.1.0`.

## Встановлення

Dev (одна репліка, без Ingress):

```
helm install hotel-booking chart/hotel-booking -n hotel-helm --create-namespace \
  -f chart/hotel-booking/values-dev.yaml --wait --timeout 10m
```

Prod (2 репліки, образи 1.1.0, Ingress):

```
helm install hotel-booking-prod chart/hotel-booking -n hotel-helm-prod --create-namespace \
  -f chart/hotel-booking/values-prod.yaml --set secrets.dbPassword=<ВАШ_ПАРОЛЬ> --wait --timeout 10m
```

Пароль БД у файлах значень не зберігається. У `values.yaml` лежить лише демонстраційне значення `lab-demo-password`.
Пароль PostgreSQL застосовується під час першого створення тому, тож змінювати його при `helm upgrade` на вже створеній
БД не можна.

Відкрити застосунок:

- dev: `kubectl port-forward -n hotel-helm svc/hotel-booking-ui 8088:80`, далі http://localhost:8088/
- prod: у hosts додати `127.0.0.1 hotel.local`, виконати
  `kubectl port-forward -n ingress-nginx svc/ingress-nginx-controller 8080:80`, далі http://hotel.local:8080/

Для встановлення того самого чарта вдруге в один кластер достатньо іншого імені релізу (і, якщо вмикаєте Ingress,
іншого `ingress.host`): імена об'єктів будуються з імені релізу.

## Пріоритет значень

Від найнижчого до найвищого:

1. `values.yaml` чарта (значення за замовчуванням);
2. файли `-f` / `--values` (якщо їх кілька, пізніший перекриває попередній);
3. прапорці `--set`, `--set-string`, `--set-file` (найвищий пріоритет, пізніші перекривають попередні).

Мапи зливаються по ключах (наприклад, у `values-prod.yaml` достатньо вказати лише `replicas` і `resources`), а списки
замінюються цілком.

## Параметри values

| Параметр | За замовчуванням (dev) | Опис |
|---|---|---|
| `nameOverride` / `fullnameOverride` | `""` | Перевизначення імені чарта чи повного імені об'єктів |
| `image.tag` | `1.0.0` | Тег усіх образів застосунку (порожній: `appVersion`) |
| `image.pullPolicy` | `IfNotPresent` | Політика завантаження образів |
| `revisionHistoryLimit` | `5` | Кількість збережених ревізій Deployment |
| `javaOpts` | `-XX:MaxRAMPercentage=55.0` | `JAVA_TOOL_OPTIONS` для Spring-сервісів |
| `config.*` | див. файл | Спільні змінні оточення Spring-сервісів (ConfigMap) |
| `secrets.dbPassword` | `lab-demo-password` | Пароль БД (Secret), демонстраційний |
| `services.<сервіс>.image.repository` | `hotel-booking-system/<сервіс>` | Репозиторій образу |
| `services.<сервіс>.image.tag` | `""` | Власний тег сервісу (порожній: `image.tag`) |
| `services.<сервіс>.replicas` | `1` | Кількість реплік |
| `services.<сервіс>.port` | `8082` / `8081` / `8080` | Порт контейнера |
| `services.<сервіс>.apiPath` | `/api/v1/...` | Шлях API, який проксує UI |
| `services.<сервіс>.database` | `userdb` / `hoteldb` / `bookingdb` | База сервісу в PostgreSQL |
| `services.<сервіс>.service.type` / `.port` | `ClusterIP` / порт сервісу | Тип і порт Service |
| `services.<сервіс>.resources` | requests 50m/192Mi, limits 500m/512Mi | Ресурси |
| `services.<сервіс>.config` | `{}` | Додаткові змінні (у `booking-service` адреси hotel-management і Kafka) |
| `ui.image.repository` / `ui.image.tag` | `hotel-booking-system/ui` / `""` | Образ UI |
| `ui.replicas` | `1` | Кількість реплік UI |
| `ui.service.type` / `ui.service.port` | `ClusterIP` / `80` | Service UI |
| `ui.resources` | requests 10m/16Mi, limits 100m/64Mi | Ресурси UI |
| `postgres.image.repository` / `.tag` | `postgres` / `16-alpine` | Образ PostgreSQL |
| `postgres.replicas` | `1` | Кількість екземплярів PostgreSQL (один, з RWO-томом) |
| `postgres.port`, `.user`, `.database` | `5432`, `hotel`, `hotel` | Параметри PostgreSQL |
| `postgres.persistence.enabled` | `true` | Прапорець PVC (`false`: emptyDir) |
| `postgres.persistence.size` / `.storageClass` | `1Gi` / `""` | Розмір і клас сховища |
| `postgres.resources` | requests 100m/128Mi, limits 500m/384Mi | Ресурси |
| `kafka.image.repository` / `.tag` | `apache/kafka` / `3.7.0` | Образ Kafka |
| `kafka.replicas` | `1` | Кількість брокерів (одновузловий KRaft) |
| `kafka.port`, `kafka.heapOpts` | `9092`, `-Xms256m -Xmx512m` | Порт і heap брокера |
| `kafka.persistence.enabled` / `.size` / `.storageClass` | `true` / `1Gi` / `""` | PVC Kafka |
| `kafka.resources` | requests 100m/512Mi, limits 1000m/1Gi | Ресурси |
| `ingress.enabled` | `false` | Створювати Ingress |
| `ingress.className` / `ingress.host` / `ingress.annotations` | `nginx` / `hotel.local` / `{}` | Параметри Ingress |
| `testImage.repository` / `.tag` | `busybox` / `1.36` | Образ для `helm test` |

Відмінності середовищ:

| Параметр | dev | prod |
|---|---|---|
| Репліки Spring-сервісів і UI | 1 | 2 |
| Ресурси сервісів | requests 50m/192Mi, limits 500m/512Mi | requests 100m/256Mi, limits 1000m/768Mi |
| Тег образів | `1.0.0` | `1.1.0` |
| Ingress | вимкнено | увімкнено |
| Логування | `INFO`, SQL у логах | `WARN`, без SQL |

## Життєвий цикл

```
helm upgrade hotel-booking chart/hotel-booking -n hotel-helm -f chart/hotel-booking/values-dev.yaml --set image.tag=1.1.0 --wait
helm upgrade hotel-booking chart/hotel-booking -n hotel-helm -f chart/hotel-booking/values-prod.yaml --wait
helm history hotel-booking -n hotel-helm
helm rollback hotel-booking 2 -n hotel-helm --wait
helm uninstall hotel-booking -n hotel-helm
```

## Хук і тест релізу (завдання 9b)

- `templates/hooks/pre-upgrade-job.yaml`: перед кожним `helm upgrade` запускається Job, що перевіряє доступність PostgreSQL
  (`pg_isready`). Якщо база недоступна, оновлення не починається. Після успіху Job видаляється.
- `templates/tests/test-connection.yaml`: `helm test <реліз> -n <namespace>` запускає Pod, що перевіряє `/healthz`,
  `/version.json` і три API через UI.

## Перевірка чарта без кластера

```
helm lint chart/hotel-booking
helm lint chart/hotel-booking -f chart/hotel-booking/values-prod.yaml
helm template hotel-booking chart/hotel-booking -n hotel-helm
```
