# Etapa 1: compila el .jar con Maven (no depende de lo que tenga instalado cada uno)
FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /workspace
# Primero solo el pom, para que Docker reutilice las dependencias si no cambió
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

# Etapa 2: imagen liviana que solo ejecuta el .jar
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S uniride && adduser -S uniride -G uniride
COPY --from=build /workspace/target/uniride-backend.jar app.jar
USER uniride
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
