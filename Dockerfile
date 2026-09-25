FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
COPY eldercare.db ./eldercare.db
RUN mvn compile -DskipTests

EXPOSE 8080
CMD ["mvn", "exec:java", "-Pweb"]
