# DVO-L3-7F2A91
# Образ hotel-management: збірка з кореня репозиторію (docker build -f hotel-management/Dockerfile .)

# ---------- Етап збірки ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

# Спочатку pom-файли (шар із залежностями кешується)
COPY pom.xml .
COPY booking-service/pom.xml booking-service/pom.xml
COPY user-service/pom.xml user-service/pom.xml
COPY hotel-management/pom.xml hotel-management/pom.xml

# Потім вихідний код тільки цього сервісу
COPY hotel-management/src hotel-management/src

RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -ntp -pl hotel-management package -DskipTests

# ---------- Фінальний образ ----------
FROM eclipse-temurin:17-jre-alpine
ARG APP_VERSION=1.0.0
ENV APP_VERSION=${APP_VERSION}
LABEL org.opencontainers.image.version="${APP_VERSION}"
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=build /workspace/hotel-management/target/*.jar app.jar
USER app
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
