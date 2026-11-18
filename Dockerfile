FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
COPY app app
COPY wiremock-extension wiremock-extension
RUN ./gradlew --no-daemon :app:bootJar

FROM eclipse-temurin:25-jre
COPY --from=build /workspace/app/build/libs/app.jar /app/app.jar
EXPOSE 8080
# Das JRE-Image enthält weder curl noch wget
HEALTHCHECK --interval=2s --retries=30 \
  CMD ["bash", "-c", "exec 3<>/dev/tcp/127.0.0.1/8080"]
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
