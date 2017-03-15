/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.io;

import static com.opengamma.strata.collect.Guavate.ensureOnlyOne;
import static com.opengamma.strata.collect.Guavate.toImmutableList;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import javax.xml.namespace.QName;

import org.joda.beans.ImmutableBean;
import org.joda.beans.MetaBean;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.light.LightMetaBean;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.Messages;

/**
 * A single element in the tree structure of XML.
 * <p>
 * This class is a minimal, lightweight representation of an element in the XML tree.
 * The element has a name, attributes, and either content or children.
 * <p>
 * Note that this representation does not express all XML features.
 * No support is provided for processing instructions, comments or mixed content.
 * In addition, it is not possible to determine the difference between empty content and no children.
 * <p>
 * There is no explicit support for namespaces.
 * When creating instances, the caller may choose to use a convention to represent namespaces.
 * For example, element and attribute names may use prefixes or the standard {@link QName} format.
 */
public final class XmlElement
    implements ImmutableBean {

  /**
   * The meta-bean.
   * This is a manually coded bean.
   */
  private static MetaBean META_BEAN = LightMetaBean.of(XmlElement.class);

  /**
   * The element name.
   */
  @PropertyDefinition(validate = "notNull")
  private final String name;
  /**
   * The attributes.
   */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableMap<String, String> attributes;
  /**
   * The element content.
   */
  @PropertyDefinition(validate = "notNull")
  private final String content;
  /**
   * The child nodes.
   */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableList<XmlElement> children;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance with content and no attributes.
   * <p>
   * Returns an element representing XML with content, but no children.
   * 
   * @param name  the element name, not empty
   * @param content  the content, empty if the element has no content
   * @return the element
   */
  public static XmlElement ofContent(String name, String content) {
    return ofContent(name, ImmutableMap.of(), content);
  }

  /**
   * Obtains an instance with content and attributes.
   * <p>
   * Returns an element representing XML with content and attributes but no children.
   * 
   * @param name  the element name, not empty
   * @param attributes  the attributes, empty if the element has no attributes
   * @param content  the content, empty if the element has no content
   * @return the element
   */
  public static XmlElement ofContent(String name, Map<String, String> attributes, String content) {
    return new XmlElement(name, ImmutableMap.copyOf(attributes), content, ImmutableList.of());
  }

  /**
   * Obtains an instance with children and no attributes.
   * <p>
   * Returns an element representing XML with children, but no content.
   * 
   * @param name  the element name, not empty
   * @param children  the children, empty if the element has no children
   * @return the element
   */
  public static XmlElement ofChildren(String name, List<XmlElement> children) {
    return ofChildren(name, ImmutableMap.of(), children);
  }

  /**
   * Obtains an instance with children and attributes.
   * <p>
   * Returns an element representing XML with children and attributes, but no content.
   * 
   * @param name  the element name, not empty
   * @param attributes  the attributes, empty if the element has no attributes
   * @param children  the children, empty if the element has no children
   * @return the element
   */
  public static XmlElement ofChildren(String name, Map<String, String> attributes, List<XmlElement> children) {
    return new XmlElement(name, ImmutableMap.copyOf(attributes), "", ImmutableList.copyOf(children));
  }

  //-------------------------------------------------------------------------
  /**
   * Creates an instance.
   * 
   * @param name  the element name, not empty
   * @param attributes  the attributes, empty if the element has no attributes
   * @param content  the content, empty if the element has no content
   * @param children  the children, empty if the element has no children
   */
  private XmlElement(
      String name,
      ImmutableMap<String, String> attributes,
      String content,
      ImmutableList<XmlElement> children) {

    this.name = ArgChecker.notEmpty(name, "name");
    this.attributes = ArgChecker.notNull(attributes, "attributes");
    this.content = ArgChecker.notNull(content, "content");
    this.children = ArgChecker.notNull(children, "children");
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the element name.
   * 
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * Gets an attribute by name, throwing an exception if not found.
   * <p>
   * This returns the value of the attribute with the specified name.
   * An exception is thrown if the attribute does not exist.
   * 
   * @param attrName  the attribute name to find
   * @return the attribute value
   * @throws IllegalArgumentException if the attribute name does not exist
   */
  public String getAttribute(String attrName) {
    String attrValue = attributes.get(attrName);
    if (attrValue == null) {
      throw new IllegalArgumentException(Messages.format(
          "Unknown attribute '{}' on element '{}'", attrName, name));
    }
    return attrValue;
  }

  /**
   * Finds an attribute by name, or empty if not found.
   * <p>
   * This returns the value of the attribute with the specified name.
   * If the attribute is not found, optional empty is returned.
   * 
   * @param attrName  the attribute name to find
   * @return the attribute value, optional
   */
  public Optional<String> findAttribute(String attrName) {
    return Optional.ofNullable(attributes.get(attrName));
  }

  /**
   * Gets the attributes.
   * <p>
   * This returns all the attributes of this element.
   * 
   * @return the attributes
   */
  public ImmutableMap<String, String> getAttributes() {
    return attributes;
  }

  /**
   * Checks if the element has content.
   * <p>
   * Content exists if it is non-empty.
   * 
   * @return the content
   */
  public boolean hasContent() {
    return content.length() > 0;
  }

  /**
   * Gets the element content.
   * <p>
   * If this element has no content, the empty string is returned.
   * 
   * @return the content
   */
  public String getContent() {
    return content;
  }

  /**
   * Gets a child element by index.
   * 
   * @param index  the index to find
   * @return the child
   * @throws IndexOutOfBoundsException if the index is invalid
   */
  public XmlElement getChild(int index) {
    return children.get(index);
  }

  /**
   * Gets the child elements.
   * <p>
   * This returns all the children of this element.
   * 
   * @return the children
   */
  public ImmutableList<XmlElement> getChildren() {
    return children;
  }

  /**
   * Gets the child element with the specified name, throwing an exception if not found or more than one.
   * <p>
   * This returns the child element with the specified name.
   * An exception is thrown if there is more than one matching child or the child does not exist.
   * 
   * @param childName  the name to match
   * @return the child matching the name
   * @throws IllegalArgumentException if there is more than one match or no matches
   */
  public XmlElement getChild(String childName) {
    return findChild(childName)
        .orElseThrow(() -> new IllegalArgumentException(Messages.format(
            "Unknown element '{}' in element '{}'", childName, name)));
  }

  /**
   * Finds the child element with the specified name, or empty if not found,
   * throwing an exception if more than one.
   * <p>
   * This returns the child element with the specified name.
   * If the element is not found, optional empty is returned.
   * 
   * @param childName  the name to match
   * @return the child matching the name, optional
   * @throws IllegalArgumentException if there is more than one match
   */
  public Optional<XmlElement> findChild(String childName) {
    return streamChildren(childName).reduce(ensureOnlyOne());
  }

  /**
   * Gets the child elements matching the specified name.
   * <p>
   * This returns all the child elements with the specified name.
   * 
   * @param childName  the name to match
   * @return the children matching the name
   */
  public ImmutableList<XmlElement> getChildren(String childName) {
    return streamChildren(childName).collect(toImmutableList());
  }

  /**
   * Gets the child elements matching the specified name.
   * <p>
   * This returns all the child elements with the specified name.
   * 
   * @param childName  the name to match
   * @return the children matching the name
   */
  public Stream<XmlElement> streamChildren(String childName) {
    return children.stream().filter(child -> child.getName().equals(childName));
  }

  //-------------------------------------------------------------------------
  @Override
  public MetaBean metaBean() {
    return META_BEAN;
  }

  @Override
  public <R> Property<R> property(String propertyName) {
    return metaBean().<R>metaProperty(propertyName).createProperty(this);
  }

  @Override
  public Set<String> propertyNames() {
    return metaBean().metaPropertyMap().keySet();
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if this element equals another.
   * <p>
   * This compares the entire state of the element, including all children.
   * 
   * @param obj  the other element, null returns false
   * @return true if equal
   */
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj instanceof XmlElement) {
      XmlElement other = (XmlElement) obj;
      return name.equals(other.name) &&
          Objects.equals(content, other.content) &&
          attributes.equals(other.attributes) &&
          children.equals(other.children);
    }
    return false;
  }

  /**
   * Returns a suitable hash code.
   * <p>
   * This includes the entire state of the element, including all children.
   * 
   * @return the hash code
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + name.hashCode();
    result = prime * result + content.hashCode();
    result = prime * result + attributes.hashCode();
    result = prime * result + children.hashCode();
    return result;
  }

  /**
   * Returns a string summary of the element.
   * <p>
   * The string form includes the attributes and content, but summarizes the child elements.
   * 
   * @return the string form
   */
  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(512);
    buf.append('<').append(name);
    for (Entry<String, String> entry : attributes.entrySet()) {
      buf.append(' ').append(entry.getKey()).append('=').append('"').append(entry.getValue()).append('"');
    }
    buf.append('>');
    if (children.isEmpty()) {
      buf.append(content);
    } else {
      for (XmlElement child : children) {
        buf.append(System.lineSeparator()).append(" <").append(child.getName()).append(" ... />");
      }
      buf.append(System.lineSeparator());
    }
    buf.append("</").append(name).append('>');
    return buf.toString();
  }

}
