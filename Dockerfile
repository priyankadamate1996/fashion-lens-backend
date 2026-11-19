FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /workspace
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B dependency:go-offline -DskipTests
COPY src src
RUN ./mvnw -B package -DskipTests

FROM eclipse-temurin:17-jre

WORKDIR /work
COPY --from=build /workspace/target/quarkus-app/ ./
EXPOSE 8080
CMD ["java", "-Dquarkus.http.host=0.0.0.0", "-jar", "quarkus-run.jar"]
