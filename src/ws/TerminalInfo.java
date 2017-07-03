
package ws;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for terminalInfo complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="terminalInfo">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="dealCount" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="dealSum" type="{http://www.w3.org/2001/XMLSchema}float"/>
 *         &lt;element name="firstDealTime" type="{http://www.w3.org/2001/XMLSchema}long"/>
 *         &lt;element name="IPAddress" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="id" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="memo" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="name" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="openDealCount" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="openDealSum" type="{http://www.w3.org/2001/XMLSchema}float"/>
 *         &lt;element name="status" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="statusTime" type="{http://www.w3.org/2001/XMLSchema}long"/>
 *         &lt;element name="userId" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="userName" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "terminalInfo", propOrder = {
    "dealCount",
    "dealSum",
    "firstDealTime",
    "ipAddress",
    "id",
    "memo",
    "name",
    "openDealCount",
    "openDealSum",
    "status",
    "statusTime",
    "userId",
    "userName"
})
public class TerminalInfo {

    protected int dealCount;
    protected float dealSum;
    protected long firstDealTime;
    @XmlElement(name = "IPAddress")
    protected String ipAddress;
    protected int id;
    protected String memo;
    protected String name;
    protected int openDealCount;
    protected float openDealSum;
    protected int status;
    protected long statusTime;
    protected int userId;
    protected String userName;

    /**
     * Gets the value of the dealCount property.
     * 
     */
    public int getDealCount() {
        return dealCount;
    }

    /**
     * Sets the value of the dealCount property.
     * 
     */
    public void setDealCount(int value) {
        this.dealCount = value;
    }

    /**
     * Gets the value of the dealSum property.
     * 
     */
    public float getDealSum() {
        return dealSum;
    }

    /**
     * Sets the value of the dealSum property.
     * 
     */
    public void setDealSum(float value) {
        this.dealSum = value;
    }

    /**
     * Gets the value of the firstDealTime property.
     * 
     */
    public long getFirstDealTime() {
        return firstDealTime;
    }

    /**
     * Sets the value of the firstDealTime property.
     * 
     */
    public void setFirstDealTime(long value) {
        this.firstDealTime = value;
    }

    /**
     * Gets the value of the ipAddress property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getIPAddress() {
        return ipAddress;
    }

    /**
     * Sets the value of the ipAddress property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setIPAddress(String value) {
        this.ipAddress = value;
    }

    /**
     * Gets the value of the id property.
     * 
     */
    public int getId() {
        return id;
    }

    /**
     * Sets the value of the id property.
     * 
     */
    public void setId(int value) {
        this.id = value;
    }

    /**
     * Gets the value of the memo property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getMemo() {
        return memo;
    }

    /**
     * Sets the value of the memo property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setMemo(String value) {
        this.memo = value;
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
     * Gets the value of the openDealCount property.
     * 
     */
    public int getOpenDealCount() {
        return openDealCount;
    }

    /**
     * Sets the value of the openDealCount property.
     * 
     */
    public void setOpenDealCount(int value) {
        this.openDealCount = value;
    }

    /**
     * Gets the value of the openDealSum property.
     * 
     */
    public float getOpenDealSum() {
        return openDealSum;
    }

    /**
     * Sets the value of the openDealSum property.
     * 
     */
    public void setOpenDealSum(float value) {
        this.openDealSum = value;
    }

    /**
     * Gets the value of the status property.
     * 
     */
    public int getStatus() {
        return status;
    }

    /**
     * Sets the value of the status property.
     * 
     */
    public void setStatus(int value) {
        this.status = value;
    }

    /**
     * Gets the value of the statusTime property.
     * 
     */
    public long getStatusTime() {
        return statusTime;
    }

    /**
     * Sets the value of the statusTime property.
     * 
     */
    public void setStatusTime(long value) {
        this.statusTime = value;
    }

    /**
     * Gets the value of the userId property.
     * 
     */
    public int getUserId() {
        return userId;
    }

    /**
     * Sets the value of the userId property.
     * 
     */
    public void setUserId(int value) {
        this.userId = value;
    }

    /**
     * Gets the value of the userName property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getUserName() {
        return userName;
    }

    /**
     * Sets the value of the userName property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setUserName(String value) {
        this.userName = value;
    }

}
