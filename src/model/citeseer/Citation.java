//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.0-b52-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2013.05.12 at 01:42:56 AM EEST 
//


package model.citeseer;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for citation complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="citation">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="raw" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="contexts" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="clusterid" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "citation", propOrder = {
    "raw",
    "contexts",
    "clusterid"
})
public class Citation {

    protected String raw;
    protected String contexts;
    protected String clusterid;

    /**
     * Gets the value of the raw property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getRaw() {
        return raw;
    }

    /**
     * Sets the value of the raw property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setRaw(String value) {
        this.raw = value;
    }

    /**
     * Gets the value of the contexts property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getContexts() {
        return contexts;
    }

    /**
     * Sets the value of the contexts property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setContexts(String value) {
        this.contexts = value;
    }

    /**
     * Gets the value of the clusterid property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getClusterid() {
        return clusterid;
    }

    /**
     * Sets the value of the clusterid property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setClusterid(String value) {
        this.clusterid = value;
    }

}
