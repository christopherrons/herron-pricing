# Deploy Scripts

Once the application is deployed, script are used for starting, monitoring and stopping the application.

## Table of Content

* [Scripts](#scripts): Deploy scripts.

## Scripts

### Variables

Common variables used by the scripts are set in:

* [setenv-server.sh](../pricing-engine-deploy/src/main/java/com/herron/exchange/pricingengine/deploy/scripts/setenv-server.sh)
* [setenv-kafka.sh](../pricing-engine-deploy/src/main/java/com/herron/exchange/pricingengine/deploy/scripts/setenv-kafka.sh)

### Bootstrap script

The [bootstrap.sh](../pricing-engine-deploy/src/main/java/com/herron/exchange/pricingengine/deploy/scripts/bootstrap.sh)
is used to init and maintain the deploy directory.

### Start Script

Start the application by running:

1. [startup-kafka.sh](../pricing-engine-deploy/src/main/java/com/herron/exchange/pricingengine/deploy/scripts/startup-kafka.sh).
2. [startup-server.sh](../pricing-engine-deploy/src/main/java/com/herron/exchange/pricingengine/deploy/scripts/startup-server.sh).

### Shutdown Script

1. [shutdown-kafka.sh](../pricing-engine-deploy/src/main/java/com/herron/exchange/pricingengine/deploy/scripts/shutdown-kafka.sh).
2. [shutdown-server.sh](../pricing-engine-deploy/src/main/java/com/herron/exchange/pricingengine/deploy/scripts/shutdown-server.sh).

## Crontab

Crontab is used for
scheduled [task](../pricing-engine-deploy/src/main/java/com/herron/exchange/pricingengine/deploy/cron/pricing-engine.crontab).
