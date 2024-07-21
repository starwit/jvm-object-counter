package de.starwit;

import java.time.LocalDateTime;

public class ObjectStats {
    String classIdentifier;
    int count;
    int bytes;
    LocalDateTime measurementTime;

    public String getClassIdentifier() {
        return classIdentifier;
    }

    public void setClassIdentifier(String classIdentifier) {
        this.classIdentifier = classIdentifier;
    }    

    public LocalDateTime getMeasurementTime() {
        return measurementTime;
    }

    public void setMeasurementTime(LocalDateTime measurementTime) {
        this.measurementTime = measurementTime;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public int getBytes() {
        return bytes;
    }

    public void setBytes(int bytes) {
        this.bytes = bytes;
    }

    @Override
    public String toString() {
        return "ObjectStats [classIdentifier=" + classIdentifier + ", count=" + count + ", bytes=" + bytes
                + ", measurementTime=" + measurementTime + "]";
    }
}
