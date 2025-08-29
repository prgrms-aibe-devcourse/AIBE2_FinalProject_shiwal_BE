# 실행 전용 (CI에서 이미 jar 빌드됨)
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Gradle 빌드 산출물 복사
COPY build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]