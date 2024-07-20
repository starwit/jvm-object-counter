package de.starwit;

import java.util.Date;

public class ObjectStats {
    int count;
    int bytes;
    Date measurementTime;

    public Date getMeasurementTime() {
        return measurementTime;
    }

    public void setMeasurementTime(Date measurementTime) {
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
        return "ObjectStats [measurementTime=" + measurementTime + ", count=" + count + ", bytes=" + bytes + "]";
    }
}
