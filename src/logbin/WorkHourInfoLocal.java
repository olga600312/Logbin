package logbin;

/**
 * Created by olgats on 15/09/2014.
 */
public class WorkHourInfoLocal {
    private int employeeCount;
    private int employeeCurrentCount;
    private long workhourSum;
    private long firstWorkhourEntry;

    public int getEmployeeCount() {
        return employeeCount;
    }

    public void setEmployeeCount(int employeeCount) {
        this.employeeCount = employeeCount;
    }

    public int getEmployeeCurrentCount() {
        return employeeCurrentCount;
    }

    public void setEmployeeCurrentCount(int employeeCurrentCount) {
        this.employeeCurrentCount = employeeCurrentCount;
    }

    public long getWorkhourSum() {
        return workhourSum;
    }

    public void setWorkhourSum(long workhourSum) {
        this.workhourSum = workhourSum;
    }

    public long getFirstWorkhourEntry() {
        return firstWorkhourEntry;
    }

    public void setFirstWorkhourEntry(long firstWorkhourEntry) {
        this.firstWorkhourEntry = firstWorkhourEntry;
    }
}
