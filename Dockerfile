FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY src src

RUN chmod +x mvnw && ./mvnw clean package -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /app

RUN useradd --create-home --shell /usr/sbin/nologin agromag

COPY --from=build /app/target/agromag-0.0.1-SNAPSHOT.jar app.jar

RUN chown agromag:agromag app.jar

USER agromag

EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
