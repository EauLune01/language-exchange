FROM gradle:9-jdk25 AS builder
WORKDIR /build
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
RUN ./gradlew dependencies --no-daemon -q || true
COPY src src
RUN ./gradlew bootJar --no-daemon -x test

FROM eclipse-temurin:25-jre-alpine AS runtime
WORKDIR /app
COPY --from=builder /build/build/libs/*.jar app.jar
EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=prod
ENV TZ=Asia/Seoul
ENTRYPOINT ["java", "-jar", "app.jar"]
