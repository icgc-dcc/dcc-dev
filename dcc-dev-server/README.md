# ICGC DCC - Dev - Server

Server module that is the execution entry point into the system.

## Build

To compile, test and package the module, execute the following from the root of the repository:

```shell
mvn -am -pl dcc-dev/dcc-dev-server
```

## Running 

### Command-line

To run against  setup, create an `application-production.yml` in `src/main/resources/` and fill in the commented out properties from `application.yml`. When finished, execute the following:

```shell
mvn clean install
mvn -pl dcc-dev-server spring-boot:run -Drun.profiles=production
```

## Configuration

See `application-production.yml`
