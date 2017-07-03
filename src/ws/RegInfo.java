
package ws;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for regInfo complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="regInfo">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="address" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="appVer" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="avivId" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="branch" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="city" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="clientType" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="DBName" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="dataSize" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="dbVer" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="employeeCount" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="employeeCurrentCount" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="firstWorkHourEntry" type="{http://www.w3.org/2001/XMLSchema}long"/>
 *         &lt;element name="freeSize" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="javaVer" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="mhmlk" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="name" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="osVer" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="paydeskCount" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="phone" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="replicateSystem" type="{http://www.w3.org/2001/XMLSchema}boolean"/>
 *         &lt;element name="terminalId" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="totalSize" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="version" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="workhourTotal" type="{http://www.w3.org/2001/XMLSchema}long"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "regInfo", propOrder = {
    "address",
    "appVer",
    "avivId",
    "branch",
    "city",
    "clientType",
    "dbName",
    "dataSize",
    "dbVer",
    "employeeCount",
    "employeeCurrentCount",
    "firstWorkHourEntry",
    "freeSize",
    "javaVer",
    "mhmlk",
    "name",
    "osVer",
    "paydeskCount",
    "phone",
    "replicateSystem",
    "terminalId",
    "totalSize",
    "version",
    "workhourTotal"
})
public class RegInfo {

    protected String address;
    protected String appVer;
    protected String avivId;
    protected String branch;
    protected String city;
    protected int clientType;
    @XmlElement(name = "DBName")
    protected String dbName;
    protected String dataSize;
    protected String dbVer;
    protected int employeeCount;
    protected int employeeCurrentCount;
    protected long firstWorkHourEntry;
    protected String freeSize;
    protected String javaVer;
    protected String mhmlk;
    protected String name;
    protected String osVer;
    protected int paydeskCount;
    protected String phone;
    protected boolean replicateSystem;
    protected String terminalId;
    protected String totalSize;
    protected String version;
    protected long workhourTotal;

    /**
     * Gets the value of the address property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getAddress() {
        return address;
    }

    /**
     * Sets the value of the address property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setAddress(String value) {
        this.address = value;
    }

    /**
     * Gets the value of the appVer property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getAppVer() {
        return appVer;
    }

    /**
     * Sets the value of the appVer property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setAppVer(String value) {
        this.appVer = value;
    }

    /**
     * Gets the value of the avivId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getAvivId() {
        return avivId;
    }

    /**
     * Sets the value of the avivId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setAvivId(String value) {
        this.avivId = value;
    }

    /**
     * Gets the value of the branch property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getBranch() {
        return branch;
    }

    /**
     * Sets the value of the branch property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setBranch(String value) {
        this.branch = value;
    }

    /**
     * Gets the value of the city property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCity() {
        return city;
    }

    /**
     * Sets the value of the city property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCity(String value) {
        this.city = value;
    }

    /**
     * Gets the value of the clientType property.
     * 
     */
    public int getClientType() {
        return clientType;
    }

    /**
     * Sets the value of the clientType property.
     * 
     */
    public void setClientType(int value) {
        this.clientType = value;
    }

    /**
     * Gets the value of the dbName property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDBName() {
        return dbName;
    }

    /**
     * Sets the value of the dbName property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDBName(String value) {
        this.dbName = value;
    }

    /**
     * Gets the value of the dataSize property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDataSize() {
        return dataSize;
    }

    /**
     * Sets the value of the dataSize property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDataSize(String value) {
        this.dataSize = value;
    }

    /**
     * Gets the value of the dbVer property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDbVer() {
        return dbVer;
    }

    /**
     * Sets the value of the dbVer property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDbVer(String value) {
        this.dbVer = value;
    }

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
     * Gets the value of the firstWorkHourEntry property.
     * 
     */
    public long getFirstWorkHourEntry() {
        return firstWorkHourEntry;
    }

    /**
     * Sets the value of the firstWorkHourEntry property.
     * 
     */
    public void setFirstWorkHourEntry(long value) {
        this.firstWorkHourEntry = value;
    }

    /**
     * Gets the value of the freeSize property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getFreeSize() {
        return freeSize;
    }

    /**
     * Sets the value of the freeSize property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setFreeSize(String value) {
        this.freeSize = value;
    }

    /**
     * Gets the value of the javaVer property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getJavaVer() {
        return javaVer;
    }

    /**
     * Sets the value of the javaVer property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setJavaVer(String value) {
        this.javaVer = value;
    }

    /**
     * Gets the value of the mhmlk property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getMhmlk() {
        return mhmlk;
    }

    /**
     * Sets the value of the mhmlk property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setMhmlk(String value) {
        this.mhmlk = value;
    }

    /**
     * Gets the value of the name property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the value of the name property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setName(String value) {
        this.name = value;
    }

    /**
     * Gets the value of the osVer property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getOsVer() {
        return osVer;
    }

    /**
     * Sets the value of the osVer property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setOsVer(String value) {
        this.osVer = value;
    }

    /**
     * Gets the value of the paydeskCount property.
     * 
     */
    public int getPaydeskCount() {
        return paydeskCount;
    }

    /**
     * Sets the value of the paydeskCount property.
     * 
     */
    public void setPaydeskCount(int value) {
        this.paydeskCount = value;
    }

    /**
     * Gets the value of the phone property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPhone() {
        return phone;
    }

    /**
     * Sets the value of the phone property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPhone(String value) {
        this.phone = value;
    }

    /**
     * Gets the value of the replicateSystem property.
     * 
     */
    public boolean isReplicateSystem() {
        return replicateSystem;
    }

    /**
     * Sets the value of the replicateSystem property.
     * 
     */
    public void setReplicateSystem(boolean value) {
        this.replicateSystem = value;
    }

    /**
     * Gets the value of the terminalId property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTerminalId() {
        return terminalId;
    }

    /**
     * Sets the value of the terminalId property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTerminalId(String value) {
        this.terminalId = value;
    }

    /**
     * Gets the value of the totalSize property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTotalSize() {
        return totalSize;
    }

    /**
     * Sets the value of the totalSize property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTotalSize(String value) {
        this.totalSize = value;
    }

    /**
     * Gets the value of the version property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getVersion() {
        return version;
    }

    /**
     * Sets the value of the version property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setVersion(String value) {
        this.version = value;
    }

    /**
     * Gets the value of the workhourTotal property.
     * 
     */
    public long getWorkhourTotal() {
        return workhourTotal;
    }

    /**
     * Sets the value of the workhourTotal property.
     * 
     */
    public void setWorkhourTotal(long value) {
        this.workhourTotal = value;
    }

}
