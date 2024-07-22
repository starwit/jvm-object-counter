package de.starwit.persistence;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hsqldb.Server;
import org.hsqldb.persist.HsqlProperties;
import org.hsqldb.server.ServerAcl.AclFormatException;

import de.starwit.ObjectStats;

public class EmbeddedDB {
    static Logger log = LogManager.getLogger(EmbeddedDB.class.getName());

    Connection conn;
    Server server;

    HashMap<String, Integer> classIdentifiersSoFar = new HashMap<>();

    public boolean startHSQLDB(Properties config) {
        HsqlProperties props = new HsqlProperties();
        props.setProperty("server.database.0", "file:gclogs;hsqldb.lock_file=false;hsqldb.default_table_type=cached;hsqldb.script_format=3");
        props.setProperty("server.dbname.0", "gclogs");
        props.setProperty("server.port", "9001");

        server = new Server();
        try {
            server.setProperties(props);
            server.start();
        } catch (IOException | AclFormatException e) {
            log.error("Can't start integrated HSQLDB server " + e.getMessage());
            return false;
        }

        if(!makeConnection()) {
            return false;
        }

        return setupDB();
    }

    private boolean makeConnection() {
        boolean result = false;
        String url="jdbc:hsqldb:hsql://localhost:9001/gclogs";
        try {
            Class.forName("org.hsqldb.jdbc.JDBCDriver");
            conn = DriverManager.getConnection(url, "SA", "");
            result = true;
        } catch (ClassNotFoundException e) {
            log.error("Can't load driver for HSQLDB " + e.getMessage());
        } catch (SQLException e) {
            log.error("Can't establish connection to DB " + e.getMessage());
        }
        return result;   
    }

    private boolean setupDB() {
        boolean result = false;
        boolean init = true;
        // check if tables are already present
        try {
            DatabaseMetaData dbm = conn.getMetaData();
            ResultSet rs = dbm.getTables(null, null, "GCLOGSCLASSID", new String[] {"TABLE"});
            while (rs.next()) {
                log.debug(rs.getString("Table_NAME"));
                init = false;
            }
            if(init) {
                return createTables();
            }
            result = true;
        } catch (SQLException e) {
            log.error("Can't load meta data for db " + e.getMessage());
            result = false;
        }
        //check if data is already present
        updateIdentifierList();
        return result;
    }

    private boolean createTables() {
        boolean result = false;
        String createStatement = """
                CREATE TABLE gclogsclassid (
                    id INTEGER IDENTITY PRIMARY KEY,
                    identifier VARCHAR(255) NOT NULL
                );

                CREATE TABLE gclogscount (
                    id INTEGER IDENTITY PRIMARY KEY,
                    count INT NOT NULL,
                    memsize INT NOT NULL,
                    measuretime TIMESTAMP,
                    classid INT,
                    FOREIGN KEY (classid) REFERENCES gclogsclassid(id)
                );
           """;
        try {
            Statement stmt = conn.createStatement();
            stmt.executeUpdate(createStatement);
            result = true;
        } catch (SQLException e) {
            log.error("Can't create tables " + e.getMessage());
        }

        return result;
    }

    public void insertMeasurementData(List<ObjectStats> data) {
        List<String> newIdentifiers = new ArrayList<>();
        for(ObjectStats os : data) {
            if(!classIdentifiersSoFar.containsKey(os.getClassIdentifier())) {
                newIdentifiers.add(os.getClassIdentifier());
            }
        }
        insertNewIdentifier(newIdentifiers);

        String insertQueryMeasurement = "INSERT INTO gclogscount (count, memsize, measuretime, classid) VALUES ";
        for(ObjectStats os : data) {
            String datetime = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(os.getMeasurementTime());
            insertQueryMeasurement += "(" + os.getCount() + ", " 
                                    + os.getBytes() + "," 
                                    + "'" + datetime + "'," 
                                    + classIdentifiersSoFar.get(os.getClassIdentifier()) + "),";
        }
        insertQueryMeasurement = insertQueryMeasurement.substring(0, insertQueryMeasurement.length() - 1);
        insertQueryMeasurement += ";";

        try {
            Statement stmt = conn.createStatement();
            stmt.executeQuery(insertQueryMeasurement);
            updateIdentifierList();
        } catch (SQLException e) {
            log.error("Can't add new measurement data " + e.getMessage());
        }
    }

    private void insertNewIdentifier(List<String> identifiers) {
        if(identifiers.size() == 0) {
            return;
        }

        String insertQueryIdentifier = "INSERT INTO gclogsclassid (identifier) VALUES ";

        for (String s : identifiers) {
            insertQueryIdentifier += "('" + s + "'),";
        }
        insertQueryIdentifier = insertQueryIdentifier.substring(0, insertQueryIdentifier.length() - 1);
        insertQueryIdentifier += ";";

        try {
            Statement stmt = conn.createStatement();
            stmt.executeQuery(insertQueryIdentifier);
            updateIdentifierList();
        } catch (SQLException e) {
            log.error("Can't add new identifiers " + e.getMessage());
        }
    }

    private void updateIdentifierList() {
        String query = "SELECT * FROM gclogsclassid;";
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            classIdentifiersSoFar = new HashMap<>();
            while (rs.next()) {
                Integer id = rs.getInt("id");
                String classID = rs.getString("identifier");
                classIdentifiersSoFar.put(classID, id);
            }
        } catch (SQLException e) {
            log.error("Can't load identifiers from db " + e.getMessage());
        }
    }    

    public void stop() {
        if(conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                log.error("Can't close connection to DB " + e.getMessage());
            }
        }
        if (server != null) {
            server.stop();
        }
    }
}
