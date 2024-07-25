package de.starwit;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
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

import de.starwit.persistence.EmbeddedDB;

public class App {

    static Logger log = LogManager.getLogger(App.class.getName());

    private EmbeddedDB db;
    private ThreadAnalysis ta;

    private Properties config = new Properties();
    private JMXConnector jmxc;
    private MBeanServerConnection mbsc;

    private String remoteJVMUrl = "service:jmx:rmi:///jndi/rmi://192.168.100.14:5433/jmxrmi";
    private int minimumObjectCount = 10;
    private int minimumObjectMemsize = 1024;
    private int sampleTime = 1000;

    HashMap<String, List<ObjectStats>> collectedStats = new HashMap<>();

    boolean isRunning = true;
    boolean printAllMbeans = false;
    boolean collectObjects = false;
    boolean collectThread = false;

    public static void main(String[] args) throws Exception {
        App a = new App();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down measurement");
            a.shutdown();
        }));

        a.setup();
        a.collectData();
    }

    App() {
        config = LoadConfig.loadProperties();
        remoteJVMUrl = config.getProperty("remotejvm.url");
        try {
            minimumObjectCount = Integer.parseInt(config.getProperty("instrumenting.minimumObjectCount"));
        } catch (NumberFormatException e) {
            log.info("Can't read minimum object count from app props, using default value " + minimumObjectCount);
        }
        try {
            minimumObjectMemsize = Integer.parseInt(config.getProperty("instrumenting.minimumObjectSize"));
        } catch (NumberFormatException e) {
            log.info("Can't read minimum object count from app props, using default value " + minimumObjectMemsize);
        }
        try {
            sampleTime = Integer.parseInt(config.getProperty("instrumenting.sampleTime"));
        } catch (NumberFormatException e) {
            log.info("Can't read minimum object count from app props, using default value " + sampleTime);
        }
        try {
            printAllMbeans = Boolean.parseBoolean(config.getProperty("instrumenting.printAllMBeans"));
        } catch (NumberFormatException e) {
            log.info("Can't read all MBean info switch, default to false");
        }
        try {
            collectObjects = Boolean.parseBoolean(config.getProperty("instrumenting.collectObjects"));
        } catch (NumberFormatException e) {
            log.info("Can't read object info switch, default to false");
        } 
        try {
            collectThread = Boolean.parseBoolean(config.getProperty("instrumenting.collectThreads"));
        } catch (NumberFormatException e) {
            log.info("Can't read thread info switch, default to false");
        }        

        db = new EmbeddedDB();
        if (!db.startHSQLDB(config)) {
            log.error("Can't start embedded DB, exiting");
            System.exit(2);
        }
    }

    public void setup() {
        try {
            JMXServiceURL url = new JMXServiceURL(remoteJVMUrl);
            jmxc = JMXConnectorFactory.connect(url, null);
            mbsc = jmxc.getMBeanServerConnection();
        } catch (Exception e) {
            log.error("can't make connection to remote JVM " + remoteJVMUrl + " " + e.getMessage());
            System.exit(1);
        }

        if(printAllMbeans) {
            printAllMbeans();
        }
        if(collectThread) {
            ta = new ThreadAnalysis(config);
            log.info("Storing thread info with format: timestamp, threadname, threadid, threadstate, lockname, issuspended, isnative, lockowner, lockownerid, trace");
        }

    }

    public void collectData() {
        while (isRunning) {
            if(collectObjects) {
                collectObjectCount();
            }
            if(collectThread) {
                ta.getThreadInfo(mbsc);
            }
            try {
                Thread.sleep(sampleTime);
            } catch (InterruptedException e) {
                log.error("Can't sleep properly " + e.getMessage());
                shutdown();
            }
        }
    }

    private void collectObjectCount() {
        String histogram;
        String[] params = new String[] { "-all" };
        String[] signature = new String[] { "[Ljava.lang.String;" };
        try {
            ObjectName diagCommandMBean = new ObjectName("com.sun.management:type=DiagnosticCommand");
            histogram = (String) mbsc.invoke(
                    diagCommandMBean,
                    "gcClassHistogram",
                    new Object[] { params },
                    signature);
            parseHistogram(histogram);
        } catch (InstanceNotFoundException | MalformedObjectNameException | MBeanException | ReflectionException
                | IOException e) {
            log.error("Can't invoke DiagnosticCommand MBean " + e.getMessage());
        }
    }

    public void parseHistogram(String histogram) {
        // Split the data into lines
        String[] lines = histogram.trim().split("\n");
        LocalDateTime now = LocalDateTime.now();
        int amountCollectedClasses = 0;
        List<ObjectStats> stats = new ArrayList<>();
        // Parse each line and extract the relevant information
        for (int i = 2; i < lines.length - 1; i++) {
            String line = lines[i].trim();

            String[] parts = line.split("\\s+");
            int instances = Integer.parseInt(parts[1]);
            int bytes = Integer.parseInt(parts[2]);
            String className = parts[3];
            if (parts.length == 5) {
                className += " " + parts[4];
            }

            if (instances >= minimumObjectCount & bytes > minimumObjectMemsize) {
                ObjectStats os = new ObjectStats();
                os.setClassIdentifier(className);
                os.setCount(instances);
                os.setBytes(bytes);
                os.setMeasurementTime(now);
                if (collectedStats.get(className) == null) {
                    collectedStats.put(className, new ArrayList<>());
                }
                collectedStats.get(className).add(os);
                amountCollectedClasses++;
                stats.add(os);
            }
        }
        log.info("added object count for " + amountCollectedClasses + " classes");
        db.insertMeasurementData(stats);
        if (Boolean.parseBoolean(config.getProperty("instrumenting.printAll"))) {
            printMeasurement();
        }
    }

    public void printAllMbeans() {
        try {
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
                    log.info("|__" + opsInfo.getDescription());
                    MBeanParameterInfo[] paramInfos = opsInfo.getSignature();
                    for (MBeanParameterInfo paramInfo : paramInfos) {
                        log.info("|__" + paramInfo.getName() +": " + paramInfo.getType());    
                    }
                    
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
            if (jmxc != null) {
                jmxc.close();
            }
        } catch (IOException e) {
            log.error("Can't close connection to remote JVM " + e.getMessage());
            System.exit(1);
        }
        if (db != null) {
            db.stop();
        }
    }

    private void printMeasurement() {
        for (String objectName : collectedStats.keySet()) {
            List<ObjectStats> stats = collectedStats.get(objectName);
            System.out.println(objectName);
            for (ObjectStats objectStats : stats) {
                log.debug(objectStats);
            }
        }
    }

    public HashMap<String, List<ObjectStats>> getCollectedStats() {
        return collectedStats;
    }

}
