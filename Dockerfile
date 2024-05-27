FROM openjdk:8-jre-slim
COPY build/libs/homeyhutsMicroService-0.1.0.jar /
WORKDIR /
CMD ["java", "-jar", "homeyhutsMicroService-0.1.0.jar"]
