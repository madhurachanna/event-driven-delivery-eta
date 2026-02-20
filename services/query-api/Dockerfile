FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /workspace

COPY services/common services/common
RUN cd services/common && mvn install -DskipTests -q

COPY services/query-api services/query-api
RUN cd services/query-api && mvn package -DskipTests -q && \
    mv target/query-api-*.jar /app.jar

FROM eclipse-temurin:21-jre-alpine
COPY --from=build /app.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
