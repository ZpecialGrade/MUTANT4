FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY . .
RUN chmod +x mvnw && ./mvnw -q -DskipTests -pl services/generation-service -am package
RUN cp services/generation-service/target/*.jar /app/app.jar

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/app.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

