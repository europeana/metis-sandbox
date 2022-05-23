[![CI](https://github.com/europeana/metis-sandbox/actions/workflows/ci.yml/badge.svg)](https://github.com/europeana/metis-sandbox/actions/workflows/ci.yml)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=europeana_metis-sandbox&metric=coverage)](https://sonarcloud.io/summary/new_code?id=europeana_metis-sandbox)

# Metis Sandbox

## Purpose
Give a client (GLAM or aggregator) an environment to test their dataset before sending it to Europeana

## Docker installation for testcontainers

The project uses testcontainers for integration tests.   
For that reason docker has to be part of the system where the integration tests are run and the files should have the correct permissions.  
If docker is not present the integration tests could be skipped, check below.  
Configuring docker:

Example installing docker:
> sudo apt-get update  
> sudo apt install apt-transport-https ca-certificates curl software-properties-common  
> curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo apt-key add -  
> sudo add-apt-repository "deb [arch=amd64] https://download.docker.com/linux/ubuntu focal stable"   
> sudo apt install docker-ce

Check docker installation:
> sudo systemctl status docker

Check user of intellij(or any other application that runs the tests)
> ps aux | grep intellij

Add user that runs intellij to the docker group(replace _<user>_ accordingly)
> sudo usermod -aG docker <user>  

If there is an error when running in intellij, the permissions of the socket file might be the problem. Update them:
> sudo chmod a+rw /var/run/docker.sock   

## Run/Build Application(Runs both unit and integration tests)

> mvn clean verify

## Skip integration tests

> mvn clean verify -DskipITs

## Skip unit tests and integration tests

> mvn clean verify -DskipTests -DskipITs

## Only run integration tests(compile all tests, skip unit tests)

> mvn clean test-compile failsafe:integration-test

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
* [Testcontainers](https://www.testcontainers.org/) 
* [Docker](https://www.docker.com/)

# Funding

The first version of the Metis Sandbox was developed in 2020 as an MVP for the Common Culture Generic 
Services project, originally proposed as a Feedback Loop pilot. The Feedback Loop software was 
financed by Deutsche Digitale Bibliothek - Kultur und Wissen online (German Digital Library).