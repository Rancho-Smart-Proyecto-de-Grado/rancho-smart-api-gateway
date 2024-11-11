FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
ARG GITHUB_TOKEN
# Establecer el token como variable de entorno dentro del contenedor
ENV GITHUB_TOKEN=${GITHUB_TOKEN}
COPY settings.xml /root/.m2/settings.xml
# Reemplaza el placeholder en settings.xml con la variable de entorno GITHUB_TOKEN
RUN sed -i 's|{{GITHUB_TOKEN}}|'$GITHUB_TOKEN'|g' /root/.m2/settings.xml

RUN mvn clean package -DskipTests

FROM openjdk:17-oracle
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]