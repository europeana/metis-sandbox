FROM openjdk:11-jre-slim
RUN apt update -y &&  \
    apt install imagemagick -y && \
    apt install ffmpeg -y && \
    apt install ghostscript -y
COPY target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
