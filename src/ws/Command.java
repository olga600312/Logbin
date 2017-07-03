
package ws;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for command complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="command">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="dependencyId" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="destType" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="destValue" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="executeTime" type="{http://www.w3.org/2001/XMLSchema}long"/>
 *         &lt;element name="id" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="interactive" type="{http://www.w3.org/2001/XMLSchema}boolean"/>
 *         &lt;element name="memo" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="propertyName" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="propertyValue" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="type" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "command", propOrder = {
    "dependencyId",
    "destType",
    "destValue",
    "executeTime",
    "id",
    "interactive",
    "memo",
    "propertyName",
    "propertyValue",
    "type"
})
public class Command {

    protected int dependencyId;
    protected int destType;
    protected String destValue;
    protected long executeTime;
    protected int id;
    protected boolean interactive;
    protected String memo;
    protected String propertyName;
    protected String propertyValue;
    protected int type;

    /**
     * Gets the value of the dependencyId property.
     * 
     */
    public int getDependencyId() {
        return dependencyId;
    }

    /**
     * Sets the value of the dependencyId property.
     * 
     */
    public void setDependencyId(int value) {
        this.dependencyId = value;
    }

    /**
     * Gets the value of the destType property.
     * 
     */
    public int getDestType() {
        return destType;
    }

    /**
     * Sets the value of the destType property.
     * 
     */
    public void setDestType(int value) {
        this.destType = value;
    }

    /**
     * Gets the value of the destValue property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDestValue() {
        return destValue;
    }

    /**
     * Sets the value of the destValue property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDestValue(String value) {
        this.destValue = value;
    }

    /**
     * Gets the value of the executeTime property.
     * 
     */
    public long getExecuteTime() {
        return executeTime;
    }

    /**
     * Sets the value of the executeTime property.
     * 
     */
    public void setExecuteTime(long value) {
        this.executeTime = value;
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
     * Gets the value of the interactive property.
     * 
     */
    public boolean isInteractive() {
        return interactive;
    }

    /**
     * Sets the value of the interactive property.
     * 
     */
    public void setInteractive(boolean value) {
        this.interactive = value;
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
     * Gets the value of the propertyName property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPropertyName() {
        return propertyName;
    }

    /**
     * Sets the value of the propertyName property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPropertyName(String value) {
        this.propertyName = value;
    }

    /**
     * Gets the value of the propertyValue property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPropertyValue() {
        return propertyValue;
    }

    /**
     * Sets the value of the propertyValue property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPropertyValue(String value) {
        this.propertyValue = value;
    }

    /**
     * Gets the value of the type property.
     * 
     */
    public int getType() {
        return type;
    }

    /**
     * Sets the value of the type property.
     * 
     */
    public void setType(int value) {
        this.type = value;
    }

}
