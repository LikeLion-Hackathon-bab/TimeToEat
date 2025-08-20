FROM --platform=linux/arm64 gradle:8.5-jdk17 AS builder

WORKDIR /build

COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle

RUN ./gradlew dependencies --no-daemon

COPY . .

RUN ./gradlew build -x test --no-daemon


FROM --platform=linux/arm64 openjdk:17-jdk-slim

WORKDIR /app

COPY --from=builder /build/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]