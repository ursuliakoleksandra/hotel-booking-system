# syntax=docker/dockerfile:1

# ---------- Етап збірки ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml .
COPY booking-service/pom.xml booking-service/pom.xml
COPY user-service/pom.xml user-service/pom.xml
COPY hotel-management/pom.xml hotel-management/pom.xml

COPY hotel-management/src hotel-management/src

RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -ntp -pl hotel-management package -DskipTests

# ---------- Фінальний образ ----------
FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=build /workspace/hotel-management/target/*.jar app.jar
USER app
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]