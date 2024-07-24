package de.starwit.analysis;

public class CountEntity {
    String classId;
    String timestamp;
    int count;
    int memsize;
    
    public String getClassId() {
        return classId;
    }
    public void setClassId(String classId) {
        this.classId = classId;
    }
    public String getTimestamp() {
        return timestamp;
    }
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
    public int getCount() {
        return count;
    }
    public void setCount(int count) {
        this.count = count;
    }
    public int getMemsize() {
        return memsize;
    }
    public void setMemsize(int memsize) {
        this.memsize = memsize;
    }

    @Override
    public String toString() {
        return "CountEntity [classId=" + classId + ", timestamp=" + timestamp + ", count=" + count + ", memsize="
                + memsize + "]";
    }

    public String toCSV() {
        return classId + "," + timestamp + "," + count + ","+ memsize;
    }    
}
