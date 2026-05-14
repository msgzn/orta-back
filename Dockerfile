# syntax=docker/dockerfile:1
#
# ORTA backend — multi-stage Docker build.
#
# Stage 1 (build): официальный Maven-образ собирает fat-jar.
#   Зависимости резолвятся ОТДЕЛЬНОЙ инструкцией ДО копирования src/,
#   чтобы при изменении только Java-кода Docker переиспользовал слой
#   с .m2 кэшем — пересборка ~30 сек вместо ~3 мин.
#
# Stage 2 (runtime): тонкий JRE-образ, только jar.
#   Запускается под non-root пользователем для базовой безопасности.

# ------------------- BUILD --------------------------------------------------
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /build

# 1) Сначала только pom.xml — это даёт кэш-слой с зависимостями.
COPY pom.xml ./
RUN mvn -B -q dependency:go-offline

# 2) Теперь источники. Любая правка кода инвалидирует только этот слой.
COPY src ./src

RUN mvn -B -q -DskipTests package \
    && mv target/orta-back-*.jar /build/app.jar

# ------------------- RUNTIME ------------------------------------------------
FROM eclipse-temurin:17-jre

# non-root user
RUN groupadd --system --gid 1001 orta \
    && useradd  --system --uid 1001 --gid orta --create-home orta

WORKDIR /app
COPY --from=build /build/app.jar /app/app.jar
RUN chown -R orta:orta /app
USER orta

EXPOSE 8080

# JVM-аргументы можно переопределить через JAVA_OPTS env (compose это делает).
ENV JAVA_OPTS=""

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
