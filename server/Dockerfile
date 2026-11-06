FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
COPY . .
RUN ./gradlew bootJar --no-daemon

# 2단계: 실행 (JRE만 있으면 됨 → 이미지가 작아짐)
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/build/libs/server-0.0.1-SNAPSHOT.jar app.jar
# Render는 PORT 환경변수로 포트를 알려준다. 무료 인스턴스 메모리가 512MB라 힙 상한을 둔다
ENTRYPOINT ["sh", "-c", "java -XX:MaxRAMPercentage=75 -Dserver.port=${PORT:-8080} -jar app.jar"]