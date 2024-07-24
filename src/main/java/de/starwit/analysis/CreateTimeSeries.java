package de.starwit.analysis;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.starwit.LoadConfig;
import de.starwit.persistence.EmbeddedDB;

public class CreateTimeSeries {

    static Logger log = LogManager.getLogger(CreateTimeSeries.class.getName());

    Properties config = new Properties();
    private EmbeddedDB db;
    private HashMap<Integer, String> classIdentifiers;

    public static void main(String[] args) throws Exception {
        CreateTimeSeries cts = new CreateTimeSeries();
        cts.createTimeSeries();
        cts.stop();
    }

    CreateTimeSeries() {
        config = LoadConfig.loadProperties();
        db = new EmbeddedDB();
        db = new EmbeddedDB();
        if(!db.startHSQLDB(config)) {
            log.error("Can't start embedded DB, exiting");
            System.exit(2);
        }        
    }

    private void createTimeSeries() {
        log.info("select ids for objects with time series");
        Connection conn = db.getConn();
        classIdentifiers = getIdentifierList();

        String countOjbectQuery = "SELECT GCLOGSCOUNT.classid, count(GCLOGSCOUNT.classid) as entrycount FROM GCLOGSCOUNT, GCLOGSCLASSID\n"
                                + "    where GCLOGSCLASSID.id = GCLOGSCOUNT.classid group by GCLOGSCOUNT.classid \n"
                                + "   having count(GCLOGSCOUNT.classid) > 1";

        String getSeriesForIDQuery = "SELECT * from  GCLOGSCOUNT where classid = ";

        Statement stmt;
        try {
            stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(countOjbectQuery);
            while(rs.next()) {
                Integer classDbId = rs.getInt("CLASSID");
                String className = classIdentifiers.get(classDbId);
                String query = getSeriesForIDQuery + classDbId + " order by measuretime;";
                ResultSet rs2 = stmt.executeQuery(query);
                List<CountEntity> timeSeries = new ArrayList<>();
                while(rs2.next()) {
                    CountEntity c = new CountEntity();
                    c.setClassId(className);
                    c.setTimestamp(rs2.getString("MEASURETIME"));
                    c.setCount(rs2.getInt("COUNT"));
                    c.setMemsize(rs2.getInt("MEMSIZE"));
                    timeSeries.add(c);
                }
                writeListToCSV(timeSeries);
            }
            
            conn.close();

        } catch (SQLException e) {
            log.error("Can't fetch count data from database " + e.getMessage());
        }
        
    }

    private HashMap<Integer, String> getIdentifierList() {
        HashMap<Integer, String> result = new HashMap<>();
        String query = "SELECT * FROM gclogsclassid;";
        try {
            Statement stmt = db.getConn().createStatement();
            ResultSet rs = stmt.executeQuery(query);
            while (rs.next()) {
                Integer id = rs.getInt("id");
                String classID = rs.getString("identifier");
                classID = classID.replace("$", "dd");
                classID = classID.replace("@", "at");
                result.put(id,classID);
            }
        } catch (SQLException e) {
            log.error("Can't load identifiers from db " + e.getMessage());
        }

        return result;
    }
    
    private void writeListToCSV(List<CountEntity> timeSeries) {
        File csvOutputFile = new File("object_count_all.csv");
        try {
            csvOutputFile.createNewFile();
        } catch (IOException e) {
            log.error("Can't create output file object_count_all.csv" + " " + e.getMessage());
        }
        try (PrintWriter pw = new PrintWriter(new FileOutputStream(csvOutputFile, true))) {
            timeSeries.stream()
            .map((c) -> c.toCSV())
            .forEach(pw::println);
        } catch (FileNotFoundException e) {
            log.info("Can't write to file " + e.getMessage());
        }
    }

    public void stop() {
        db.stop();
    }
}
