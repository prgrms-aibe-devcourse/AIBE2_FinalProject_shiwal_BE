# ===== 1) Build Stage =====
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

# Gradle 캐시 최적화: 먼저 gradle wrapper & 설정만 복사
COPY gradlew gradlew
COPY gradle gradle
COPY build.gradle settings.gradle ./

# 의존성만 먼저 받아 캐시
RUN ./gradlew --version
RUN ./gradlew dependencies -q || true

# 나머지 소스 복사 후 빌드
COPY . .
RUN ./gradlew clean bootJar --no-daemon

# ===== 2) Runtime Stage =====
FROM eclipse-temurin:17-jre
WORKDIR /app

# 시간대(한국) 설정이 필요하면 주석 해제
# ENV TZ=Asia/Seoul

# 빌드 산출물 복사 (이름이 SNAPSHOT이든 아니든 단일 jar로 매칭)
COPY --from=build /app/build/libs/*jar app.jar

# JVM 옵션 (필요 시 조정)
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75 -XX:InitialRAMPercentage=50 -XX:+ExitOnOutOfMemoryError"

EXPOSE 8080
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]