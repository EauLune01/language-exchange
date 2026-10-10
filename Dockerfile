# jar 는 이미지 밖에서 만든다 (배포: GitHub Actions, 로컬: ./gradlew bootJar).
# 서버 메모리가 작아 컨테이너 안에서 Gradle 컴파일을 돌리면 끝나지 않는다.
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY build/libs/*.jar app.jar
EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=prod
ENV TZ=Asia/Seoul
# 서버 CPU 가 작아서 JIT 최적화 컴파일(C2)이 시작 시간을 다 잡아먹는다. C1 까지만 쓰면 시작이 절반 이하로 줄고 메모리도 덜 쓴다.
# (CPU 0.25개로 제한해 잰 값: 75~80초 → 32~35초. 대신 오래 돌 때의 최고 처리 속도는 낮아진다)
ENV JAVA_TOOL_OPTIONS="-XX:TieredStopAtLevel=1"
ENTRYPOINT ["java", "-jar", "app.jar"]
