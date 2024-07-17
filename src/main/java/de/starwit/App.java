package de.starwit;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Set;

import javax.management.InstanceNotFoundException;
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

    private String remoteJVMUrl = "service:jmx:rmi:///jndi/rmi://localhost:5433/jmxrmi";

    public static void main(String[] args) throws Exception {
        App a = new App();
        a.setup();
        a.printObjectCount();
        // a.printAllMbeans(mbsc);
    }

    App() {
        try (InputStream in = App.class.getClassLoader().getResourceAsStream("application.properties")) {
            if(in != null) {
                config.load(in);
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
    }

    public void printObjectCount() {
        String histogram;
        try {
            histogram = (String) mbsc.invoke(
                    new ObjectName("com.sun.management:type=DiagnosticCommand"),
                    "gcClassHistogram",
                    new Object[] { null },
                    new String[] { "[Ljava.lang.String;" });
            // TODO parse properly
            System.out.println(histogram);
        } catch (InstanceNotFoundException | MalformedObjectNameException | MBeanException | ReflectionException
                | IOException e) {
            log.error("Can't invoke DiagnosticCommand MBean " + e.getMessage());
        }
    }

    public void printAllMbeans(MBeanServerConnection mbsc) throws Exception {
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
    }

    public void shutdown() {
        try {
            jmxc.close();
        } catch (IOException e) {
            log.error("Can't close connection to remote JVM " + e.getMessage());
            System.exit(1);
        }
    }
}
