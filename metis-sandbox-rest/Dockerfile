FROM eclipse-temurin:21-jre
RUN apt-get update && \
    apt-get install imagemagick -y && \
    apt-get install ffmpeg -y && \
    apt-get install ghostscript -y && \
    apt-get clean
COPY target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
