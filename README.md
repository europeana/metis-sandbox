# Metis Sandbox

## Purpose
Give a client (GLAM or aggregator) an environment to test their dataset before sending it to Europeana

## Dependencies
- [RabbitMQ](https://www.rabbitmq.com/) server available

## Run Tests

> mvn clean test

## Run Application

> mvn clean package spring-boot:run

## API
Composed by 2 endpoints

`POST sandbox/dataset/{name}/process`

This endpoint let you provide a name to your dataset and specify a zip file containing xml files, each xml file should represent a record.
When you call this endpoint it will trigger the process of the dataset and give you back a dataset id to be able to monitor the progress of the process.

`GET sandbox/dataset/{id}`

This endpoint let you provide a dataset id (the one you get from calling the POST endpoint) to get information about the dataset, and the progress of the process.

### How to use
You can check how the endpoints work using Swagger-UI in path 

`{your-host}/sandbox/swagger-ui.html#/dataset-controller/processDatasetUsingPOST`

## Technologies
Project is created with:

* Java 11
* [Spring Boot](https://spring.io/projects/spring-boot)
* [RabbitMQ](https://www.rabbitmq.com/)
* [Postgresql](https://www.postgresql.org/)
* [Spring Fox](https://springfox.github.io/springfox/)