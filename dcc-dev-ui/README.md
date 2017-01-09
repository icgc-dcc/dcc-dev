# ICGC DCC - Dev - UI

UI module that packages static resources/

## Setup

Install the following:
- [nvm](http://docs.icgc.org/software/development/setup/#nvm)

Then execute:

```shell
nvm install 5
nvm use 5
```

## Build

To compile, test and package the module, execute the following from the root of the repository:

```shell
mvn -am -pl dcc-dev/dcc-dev-ui
```

Using angular2-webpack-starter (most starred ng2 seed on github)
https://github.com/AngularClass/angular2-webpack-starter/tree/material2

## Run

```shell
npm run start:hmr
```

It will be on port 3000
