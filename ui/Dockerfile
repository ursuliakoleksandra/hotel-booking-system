# Образ UI: статичний вебінтерфейс на nginx (збірка з кореня репозиторію: docker build -f ui/Dockerfile .)
FROM nginx:1.27-alpine
ARG APP_VERSION=1.0.0
ENV APP_VERSION=${APP_VERSION}
LABEL org.opencontainers.image.version="${APP_VERSION}"
COPY ui/html/ /usr/share/nginx/html/
# Версію образу показує інтерфейс (див. ui/html/app.js)
RUN printf '{"name":"hotel-booking-ui","version":"%s"}\n' "${APP_VERSION}" > /usr/share/nginx/html/version.json
EXPOSE 80
