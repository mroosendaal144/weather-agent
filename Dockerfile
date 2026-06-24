# ---- Bouwfase: compileren met Maven + Java 21 ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---- Draaifase: alleen een slanke JRE ----
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
# Cloud Run levert PORT aan (standaard 8080); de app leest die uit.
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
