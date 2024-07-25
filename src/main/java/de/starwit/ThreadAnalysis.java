package de.starwit;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import java.time.LocalDateTime;

import javax.management.MBeanServerConnection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static java.lang.management.ManagementFactory.THREAD_MXBEAN_NAME;
import static java.lang.management.ManagementFactory.newPlatformMXBeanProxy;

public class ThreadAnalysis {

    static Logger log = LogManager.getLogger(ThreadAnalysis.class.getName());

    String outputFileName = "threadlog.csv";
    PrintWriter logfileWriter;

    ThreadAnalysis(Properties config) {
        try {
            outputFileName = config.getProperty("instrumenting.threadOutputFile");
        } catch (NumberFormatException e) {
            log.info("Can't read file name for thread log, default value " + outputFileName);
        }

        File csvOutputFile = new File(outputFileName);
        try {
            logfileWriter = new PrintWriter(new FileOutputStream(csvOutputFile, true));
        } catch (FileNotFoundException e) {
            log.error("Can't open log file " + outputFileName + ". " + e.getMessage());
        }
        log.info("");
    }

    public void getThreadInfo(MBeanServerConnection mbsc) {
        String datetime = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now());
        try {
            ThreadMXBean tmbean = newPlatformMXBeanProxy(mbsc, THREAD_MXBEAN_NAME, ThreadMXBean.class);
            long[] tids = tmbean.getAllThreadIds();
            ThreadInfo[] tinfos = tmbean.getThreadInfo(tids, Integer.MAX_VALUE);
            for (ThreadInfo ti : tinfos) {
                logfileWriter.println(datetime +"," + formatThreadInfo(ti));
            }
        } catch (IOException e) {
            log.info("Can't access thread info " + e.getMessage());
        }
    }    

    private String formatThreadInfo(ThreadInfo ti) {
        String line = printThread(ti);

        StringBuffer traceInfo = new StringBuffer();
        // print stack trace with locks
        StackTraceElement[] stacktrace = ti.getStackTrace();
        for (int i = 0; i < stacktrace.length; i++) {
            StackTraceElement ste = stacktrace[i];
            traceInfo.append(ste.toString() + " ");
        }
        return line + ", " + traceInfo.toString();
    }

    private String printThread(ThreadInfo ti) {
        StringBuilder sb = new StringBuilder();
        sb.append(ti.getThreadName() + ", ");
        sb.append(ti.getThreadId() + ", ");
        sb.append(ti.getThreadState() + ", ");
        if (ti.getLockName() != null) {
            sb.append(ti.getLockName() + ", ");
        } else {
            sb.append(", ");
        }
        if (ti.isSuspended()) {
            sb.append("suspended, ");
        } else {
            sb.append(", ");
        }
        if (ti.isInNative()) {
            sb.append("native, ");
        } else {
            sb.append(", ");
        }
        if (ti.getLockOwnerName() != null) {
            sb.append(ti.getLockOwnerName() + ", " + ti.getLockOwnerId());
        } else {
            sb.append(", ");
        }
        return sb.toString();
    }
}
