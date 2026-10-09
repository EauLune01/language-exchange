FROM gradle:9-jdk25 AS builder
WORKDIR /app
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
COPY src src
RUN ./gradlew bootJar --no-daemon

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8080
# 프로파일은 SPRING_PROFILES_ACTIVE 환경 변수로 받는다
ENTRYPOINT ["java", "-jar", "app.jar"]
