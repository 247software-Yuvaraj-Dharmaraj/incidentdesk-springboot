# --- build stage ---
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN ./mvnw -q -B dependency:go-offline
COPY src ./src
RUN ./mvnw -q -B clean package -DskipTests

# --- run stage ---
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
# 4000 = HTTP API, 9092 = Socket.IO
EXPOSE 4000 9092
ENTRYPOINT ["java", "-jar", "app.jar"]
