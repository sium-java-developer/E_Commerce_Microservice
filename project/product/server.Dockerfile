FROM maven:3.8.1-openjdk-17-slim AS prebuild
RUN mkdir -p /workspace
WORKDIR /workspace
COPY ./kafkaChannel .
RUN mvn clean install

FROM maven:3.8.1-openjdk-17-slim AS build
COPY --from=prebuild /root/.m2 /root/.m2
COPY ./productServer .
RUN mvn clean package

FROM openjdk:17-jdk-alpine
COPY --from=build ./target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java","-jar","app.jar"]