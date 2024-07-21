# JVM Object Counter
This is a tool, that reads some stats from a remote JVM via JMX protocol. It starts a HSQLDB and writes object count per class. 

## How to build
This app uses Maven to build and package. Run following commands in repo's base folder.
```bash
    mvn clean package
    java -jar target/jvmcounter.jar
```

## How to run
TODO

## How to configure
If application detects application.properties file in same directory, this will loaded and overrides any internal config. Shipped property file can be found [here](src/main/resources/application.properties).

# License
This software is published under Apache 2.0 license and file with license agreement can be found [here](LICENSE). 