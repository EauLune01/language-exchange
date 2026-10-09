# jar 는 이미지 밖에서 만든다 (배포: GitHub Actions, 로컬: ./gradlew bootJar).
# 서버 메모리가 작아 컨테이너 안에서 Gradle 컴파일을 돌리면 끝나지 않는다.
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY build/libs/*.jar app.jar
EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=prod
ENV TZ=Asia/Seoul
ENTRYPOINT ["java", "-jar", "app.jar"]
