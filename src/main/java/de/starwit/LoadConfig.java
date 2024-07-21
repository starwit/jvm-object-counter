package de.starwit;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LoadConfig {
    static Logger log = LogManager.getLogger(LoadConfig.class.getName());
    
    public static Properties loadProperties() {
        Properties config = new Properties();

        // test if app props in start directory
        File propsFile = new File("application.properties");
        if(propsFile.exists() && !propsFile.isDirectory()) {
            //load external app props file
            try {
                InputStream in = new FileInputStream(propsFile);
                try {
                    if(in != null) {
                        config.load(in);
                        in.close();
                    }
                } catch (IOException e) {
                    log.error("Can't load property file, exiting " + e.getMessage());
                    System.exit(1); // exit with error status
                }                
            } catch (FileNotFoundException e) {
                log.error("Can't find external app property file " + e.getMessage());
                System.exit(1);
            }
        } else {
            // load shipped app props
            try (InputStream in = App.class.getClassLoader().getResourceAsStream("application.properties")) {
                if(in != null) {
                    config.load(in);
                    in.close();
                } else {
                    log.error("Can't find property file");
                    System.exit(1);
                }
            } catch (IOException e) {
                log.error("Can't load property file, exiting " + e.getMessage());
                System.exit(1); // exit with error status
            }            
        }

        return config;
    }
}
