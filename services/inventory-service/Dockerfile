FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /workspace

COPY services/common services/common
RUN cd services/common && mvn install -DskipTests -q

COPY services/inventory-service services/inventory-service
RUN cd services/inventory-service && mvn package -DskipTests -q && \
    mv target/inventory-service-*.jar /app.jar

FROM eclipse-temurin:21-jre-alpine
COPY --from=build /app.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
