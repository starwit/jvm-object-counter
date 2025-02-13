# JVM Object Counter
This is a tool, that reads some stats from a remote JVM via JMX protocol. It starts a HSQLDB and writes object count per class. 

## General idea
In Java software at times too many objects or too large objects are created. If that happens, a full garbage collection can be triggered and that will stop your application. With garbage collection logging activated, you can get an idea how much data is moved on Java's heap. However you don't know, which objects are created and how much memory they use. This tool aims at collecting a more detailed statistic, to break down number and size of objects on heap. 

Data source is object histogram that is offered from MBean DiagnosticCommand via JMX. For more details see [JavaDoc] (https://docs.oracle.com/javase/8/docs/jre/api/management/extension/com/sun/management/DiagnosticCommandMBean.html)

__Note__ Operation _gcClassHistogram_ appears to cause full GC. If that's the case, this will slow down your app significantly.

## How to build
This app uses Maven to build and package. Run following commands in repo's base folder.
```bash
    mvn clean package
    java -jar target/jvmcounter.jar
```

## How to run
Simply run jar file with Java (version >= 21). App will start an embedded HSQLDB to which you can connect on port 9001, credentials can be set via application properties - see next section. Collected data can be analyzed from HSQLDB using other tools.

## How to configure
If application detects application.properties file in same directory, this will be loaded and overrides any internal config. Shipped property file can be found [here](src/main/resources/application.properties).
```properties
    remotejvm.url=service:jmx:rmi:///jndi/rmi://host:port/jmxrmi
    remotejvm.enableAuth=false
    remotejvm.username=
    remotejvm.password=

    storage.hsqldb.pw=agoodpassword
    storage.hsqldb.path=

    # be careful - lower is faster. milliseconds
    instrumenting.sampleTime=5000
    instrumenting.collectObjects=false
    instrumenting.minimumObjectCount=1
    instrumenting.minimumObjectSize=1024
    instrumenting.printAllMBeans=false
    instrumenting.printObjectCount=true
    instrumenting.printAll=false
    instrumenting.collectThreads=true
    instrumenting.threadOutputFile=threadlog.csv
```

## How to analyze
If you run tool, you'll end up with a HSQL database, that you can use for further analysis. Just start HSQLDB like so:
```bash
    java -cp ./hsqldb.jar org.hsqldb.Server --database.0 file:path/gclogs --dbname.0 gclogs
```
Necessary jar can be downloaded on HSQLDB's web page: https://hsqldb.org/

Project also has a tool, that extracts database into a flat CSV file. This can then be used with tools like Python/Pandas for further analysis. TODO more details on that.

# License
This software is published under Apache 2.0 license and file with license agreement can be found [here](LICENSE). 