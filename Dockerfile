FROM eclipse-temurin:21-jre
LABEL authors="Carbonara Nicolas, Surbeck Léon"
WORKDIR /app
COPY target/java-tcp-programming-1.0-SNAPSHOT.jar /app/app.jar
EXPOSE 4242/tcp
EXPOSE 4343/udp
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
CMD ["--help"]

