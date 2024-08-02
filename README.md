# Pricing Engine

The Pricing Engine provides real-time and historical prices for all types of instruments by using market data and
theoretical calculations.

## Table of Content

* [Requirements](#requirements): Application requirements.
* [Documentation](#documentation): Further documentation.
* [Configuration](#configuration): How to configure the application.
* [Application DevOps](#application-devops): How to deploy the application.

## Requirements

* Java 21
* [Kafka](https://www.apache.org/dyn/closer.cgi?path=/kafka/3.3.1/kafka_2.13-3.3.1.tgz)

## Documentation

* [Deploy Scripts](docs/deploy-scripts.md): Useful scripts once the application is deployed.
* [Data Flow](docs/data-flow.md): Visualization of the application data flow.

## Configuration

## Application DevOps

### Staring the Application

Start the application from the root folder by running`./gradlew bootRun`.

### Building the Application

Build the application from the root folder by running `/gradlew packageRelease -PreleaseVersion=<release-version>` to
build and bundle the application jar and relevant deploy
scripts.

### Deploying the Application

The application can be deployed to a remote machine using the `deploy` task.
