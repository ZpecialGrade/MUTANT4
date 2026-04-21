FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY . .
RUN chmod +x mvnw && ./mvnw -q -DskipTests -pl services/wardrobe-service -am package
RUN cp services/wardrobe-service/target/*.jar /app/app.jar

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN apt-get update \
	&& apt-get install -y --no-install-recommends curl \
	&& rm -rf /var/lib/apt/lists/*
COPY --from=build /app/app.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

