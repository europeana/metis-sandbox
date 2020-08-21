# Metis Sandbox

## Purpose
Give a client (GLAM or aggregator) an environment to test their dataset before sending it to Europeana

## Dependencies
- [RabbitMQ](https://www.rabbitmq.com/) server available (check spring.rabbitmq in application.yml for config example)
- Storage like [H2](https://www.h2database.com/html/main.html) or [PostgreSQL](https://www.google.com/search?q=posgresql&rlz=1C5CHFA_enCR881NL888&oq=posgresql&aqs=chrome..69i57j69i59j0l6.1535j0j7&sourceid=chrome&ie=UTF-8) (check sandbox.datasource in application.yml for config example)
- S3 bucket available (check sandbox.s3 in application.yml for config example)

## Run Tests

> mvn clean test

## Run Application

> mvn clean package

That will generate a WAR file that you can deploy to a tomcat instance

## API
Composed by 2 endpoints

`POST metis-sandbox/dataset/{name}/process`

This endpoint let you provide a name to your dataset and specify a zip file containing xml files, each xml file should represent a record.
When you call this endpoint it will trigger the process of the dataset and give you back a dataset id to be able to monitor the progress of the process.

`GET metis-sandbox/dataset/{id}`

This endpoint let you provide a dataset id (the one you get from calling the POST endpoint) to get information about the dataset, and the progress of the process.

### How to use
You can check how the endpoints work using Swagger-UI in path 

`{your-host}/metis-sandbox/swagger-ui.html#/dataset-controller/processDatasetUsingPOST`

## Technologies
Project created with:

* Java 11
* [Spring Boot](https://spring.io/projects/spring-boot)
* [RabbitMQ](https://www.rabbitmq.com/)
* [Postgresql](https://www.postgresql.org/)
* [AWS S3](https://aws.amazon.com/s3/)
* [Spring Fox](https://springfox.github.io/springfox/)

# Funding

The first version of the Metis Sandbox was developed in 2020 as an MVP for the Common Culture Generic 
Services project, originally proposed as a Feedback Loop pilot. The Feedback Loop software was 
financed by Deutsche Digitale Bibliothek - Kultur und Wissen online (German Digital Library).