
package ws;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for workhourInfo complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="workhourInfo">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="employeeCount" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="employeeCurrentCount" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="firstWorkhourEntry" type="{http://www.w3.org/2001/XMLSchema}long"/>
 *         &lt;element name="workhourSum" type="{http://www.w3.org/2001/XMLSchema}long"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "workhourInfo", propOrder = {
    "employeeCount",
    "employeeCurrentCount",
    "firstWorkhourEntry",
    "workhourSum"
})
public class WorkhourInfo {

    protected int employeeCount;
    protected int employeeCurrentCount;
    protected long firstWorkhourEntry;
    protected long workhourSum;

    /**
     * Gets the value of the employeeCount property.
     * 
     */
    public int getEmployeeCount() {
        return employeeCount;
    }

    /**
     * Sets the value of the employeeCount property.
     * 
     */
    public void setEmployeeCount(int value) {
        this.employeeCount = value;
    }

    /**
     * Gets the value of the employeeCurrentCount property.
     * 
     */
    public int getEmployeeCurrentCount() {
        return employeeCurrentCount;
    }

    /**
     * Sets the value of the employeeCurrentCount property.
     * 
     */
    public void setEmployeeCurrentCount(int value) {
        this.employeeCurrentCount = value;
    }

    /**
     * Gets the value of the firstWorkhourEntry property.
     * 
     */
    public long getFirstWorkhourEntry() {
        return firstWorkhourEntry;
    }

    /**
     * Sets the value of the firstWorkhourEntry property.
     * 
     */
    public void setFirstWorkhourEntry(long value) {
        this.firstWorkhourEntry = value;
    }

    /**
     * Gets the value of the workhourSum property.
     * 
     */
    public long getWorkhourSum() {
        return workhourSum;
    }

    /**
     * Sets the value of the workhourSum property.
     * 
     */
    public void setWorkhourSum(long value) {
        this.workhourSum = value;
    }

}
