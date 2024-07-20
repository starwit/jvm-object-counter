package de.starwit;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class App {

    static Logger log = LogManager.getLogger(App.class.getName());

    private Properties config = new Properties();
    private JMXConnector jmxc;
    private MBeanServerConnection mbsc;

    private String remoteJVMUrl = "service:jmx:rmi:///jndi/rmi://192.168.100.14:5433/jmxrmi";

    HashMap<String, List<ObjectStats>> collectedStats = new HashMap<>();
    public HashMap<String, List<ObjectStats>> getCollectedStats() {
        return collectedStats;
    }

    boolean isRunning = true;

    public static void main(String[] args) throws Exception {
        App a = new App();

        Runtime.getRuntime().addShutdownHook(new Thread(){
            public void run() {
                try {
                    Thread.sleep(100);
                    log.info("Shutting down measurement");
                    a.shutdown();
                } catch (InterruptedException e) {
                    log.error("Can't shutdown properly " + e.getMessage());
                }
            }
        });        

        a.setup();
        a.collectObjectCount();
    }

    App() {
        try (InputStream in = App.class.getClassLoader().getResourceAsStream("application.properties")) {
            if(in != null) {
                config.load(in);
                remoteJVMUrl = config.getProperty("remotejvm.url");
            } else {
                log.error("Can't find property file");
                System.exit(1);
            }
        } catch (IOException e) {
            log.error("Can't load property file, exiting " + e.getMessage());
            System.exit(1); // exit with error status
        }
    }

    public void setup() {
        try {
            JMXServiceURL url = new JMXServiceURL(remoteJVMUrl);
            jmxc = JMXConnectorFactory.connect(url, null);
            mbsc = jmxc.getMBeanServerConnection();
        } catch (Exception e) {
            log.error("can't make connection to remote JVM " + e.getMessage());
            System.exit(1);
        }

        if(Boolean.parseBoolean(config.getProperty("instrumenting.printAllMBeans"))) {
            printAllMbeans();
        }
    }

    public void collectObjectCount() {
        while(isRunning) {
            String histogram;
            try {
                histogram = (String) mbsc.invoke(
                        new ObjectName("com.sun.management:type=DiagnosticCommand"),
                        "gcClassHistogram",
                        new Object[] { null },
                        new String[] { "[Ljava.lang.String;" });
                parseHistogram(histogram);
            } catch (InstanceNotFoundException | MalformedObjectNameException | MBeanException | ReflectionException
                    | IOException e) {
                log.error("Can't invoke DiagnosticCommand MBean " + e.getMessage());
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                log.error("Can't sleep properly " + e.getMessage());
                shutdown();
            }            
        }
    }

    public void parseHistogram(String histogram) {
        // Split the data into lines
        String[] lines = histogram.strip().split("\n");
        Date now = new Date();
        int amountCollectedClasses = 0;
        // Parse each line and extract the relevant information
        for (int i = 2; i < lines.length-1; i++) {
            String line = lines[i].strip();

            String[] parts = line.split("\\s+");
            int instances = Integer.parseInt(parts[1]);
            String bytes = parts[2];
            String className = parts[3];
            if (parts.length == 5) {
                className += " " + parts[4];   
            }

            if(instances >= 10) {
                ObjectStats os = new ObjectStats();
                os.setCount(instances);
                os.setBytes(Integer.parseInt(bytes));
                os.setMeasurementTime(now);
                if (collectedStats.get(className) == null) {
                    collectedStats.put(className, new ArrayList<>());
                }
                collectedStats.get(className).add(os);
                amountCollectedClasses++;
            }
        }
        log.info("added object count for " + amountCollectedClasses + " classes");
        if(Boolean.parseBoolean(config.getProperty("instrumenting.printAll"))) {
            printMeasurement();
        }        
    }

    public void printAllMbeans() {
        try{
            Set<ObjectInstance> mbeans = mbsc.queryMBeans(null, null);
            for (ObjectInstance ob : mbeans) {
                ObjectName on = ob.getObjectName();
                log.info(on.getCanonicalName());
                MBeanInfo info = mbsc.getMBeanInfo(on);
                MBeanAttributeInfo[] attrs = info.getAttributes();
                for (MBeanAttributeInfo attrInfo : attrs) {
                    log.info("|_" + attrInfo.getName() + ": " + attrInfo.getDescription());
                }
                MBeanOperationInfo[] opsInfos = info.getOperations();
                for (MBeanOperationInfo opsInfo : opsInfos) {
                    log.info("|_*" + opsInfo.getName());
                }
            }
        } catch (IOException | InstanceNotFoundException | IntrospectionException | ReflectionException e) {
            log.error("Can't invoke MBean list " + e.getMessage());
        }
    }

    public void shutdown() {
        log.info("Captured SIGTERM, shutting down");
        isRunning = false;
        try {
            jmxc.close();
        } catch (IOException e) {
            log.error("Can't close connection to remote JVM " + e.getMessage());
            System.exit(1);
        }
    }

    private void printMeasurement() {
        for (String objectName: collectedStats.keySet()) {
            List<ObjectStats> stats = collectedStats.get(objectName);
            System.out.println(objectName);
            for (ObjectStats objectStats : stats) {
                log.debug(objectStats);
            }
        }
    }
}
