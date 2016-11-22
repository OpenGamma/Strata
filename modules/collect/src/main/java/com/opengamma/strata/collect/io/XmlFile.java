/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.io;

import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteSource;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.Unchecked;

/**
 * An XML file.
 * <p>
 * Represents an XML file together with the ability to parse it from a {@link ByteSource}.
 * <p>
 * This uses the standard StAX API to parse the file.
 * Once parsed, the XML is represented as a DOM-like structure, see {@link XmlElement}.
 * This approach is suitable for XML files where the size of the parsed XML file is
 * known to be manageable in memory.
 * <p>
 * Note that the {@link XmlElement} representation does not express all XML features.
 * No support is provided for processing instructions, comments or mixed content.
 * In addition, it is not possible to determine the difference between empty content and no children.
 * <p>
 * There is no support for namespaces.
 * All namespace prefixes are dropped.
 * There are cases where this can be a problem, but most of the time lenient parsing is helpful.
 */
public final class XmlFile {

  /**
   * The root element.
   */
  private final XmlElement root;
  /**
   * The map of references.
   */
  private final ImmutableMap<String, XmlElement> refs;

  //-----------------------------------------------------------------------
  /**
   * Parses the specified source as an XML file to an in-memory DOM-like structure.
   * <p>
   * This parses the specified byte source expecting an XML file format.
   * The resulting instance can be queried for the root element.
   * 
   * @param source  the XML source data
   * @return the parsed file
   * @throws UncheckedIOException if an IO exception occurs
   * @throws IllegalArgumentException if the file cannot be parsed
   */
  public static XmlFile of(ByteSource source) {
    return of(source, "");
  }

  /**
   * Parses the specified source as an XML file to an in-memory DOM-like structure.
   * <p>
   * This parses the specified byte source expecting an XML file format.
   * The resulting instance can be queried for the root element.
   * <p>
   * This supports capturing attribute references, such as an id/href pair.
   * Wherever the parser finds an attribute with the specified name, the element is added
   * to the internal map, accessible by calling {@link #getReferences()}.
   * <p>
   * For example, if one part of the XML has {@code <foo id="fooId">}, the references map will
   * contain an entry mapping "fooId" to the parsed element {@code <foo>}.
   * 
   * @param source  the XML source data
   * @param refAttrName  the attribute name that should be parsed as a reference
   * @return the parsed file
   * @throws UncheckedIOException if an IO exception occurs
   * @throws IllegalArgumentException if the file cannot be parsed
   */
  public static XmlFile of(ByteSource source, String refAttrName) {
    ArgChecker.notNull(source, "source");
    return Unchecked.wrap(() -> {
      try (InputStream in = source.openBufferedStream()) {
        XMLStreamReader xmlReader = xmlInputFactory().createXMLStreamReader(in);
        try {
          HashMap<String, XmlElement> refs = new HashMap<>();
          XmlElement root = parse(xmlReader, refAttrName, refs);
          return new XmlFile(root, refs);
        } finally {
          xmlReader.close();
        }
      }
    });
  }

  //-------------------------------------------------------------------------
  /**
   * Parses the tree from the StAX stream reader, capturing references.
   * <p>
   * The reader should be created using the factory returned from {@link #xmlInputFactory()}.
   * <p>
   * This method supports capturing attribute references, such as an id/href pair.
   * Wherever the parser finds an attribute with the specified name, the element is added
   * to the specified map. Note that the map is mutated.
   * 
   * @param reader  the StAX stream reader, positioned at or before the element to be parsed
   * @param refAttr  the attribute name that should be parsed as a reference, null if not applicable
   * @param refs  the mutable map of references to update, null if not applicable
   * @return the parsed element
   * @throws IllegalArgumentException if the input cannot be parsed
   */
  private static XmlElement parse(XMLStreamReader reader, String refAttr, Map<String, XmlElement> refs) {
    try {
      // parse start element
      String elementName = parseElementName(reader);
      ImmutableMap<String, String> attrs = parseAttributes(reader);

      // parse children or content
      ImmutableList.Builder<XmlElement> childBuilder = ImmutableList.builder();
      String content = "";
      int event = reader.next();
      while (event != XMLStreamConstants.END_ELEMENT) {
        switch (event) {
          // parse child when start element found
          case XMLStreamConstants.START_ELEMENT:
            childBuilder.add(parse(reader, refAttr, refs));
            break;
          // append content when characters found
          // since XMLStreamReader has IS_COALESCING=true means there should only be one content call
          case XMLStreamConstants.CHARACTERS:
          case XMLStreamConstants.CDATA:
            content += reader.getText();
            break;
          default:
            break;
        }
        event = reader.next();
      }
      ImmutableList<XmlElement> children = childBuilder.build();
      XmlElement parsed = children.isEmpty() ?
          XmlElement.ofContent(elementName, attrs, content) :
          XmlElement.ofChildren(elementName, attrs, children);
      String ref = attrs.get(refAttr);
      if (ref != null) {
        refs.put(ref, parsed);
      }
      return parsed;

    } catch (XMLStreamException ex) {
      throw new IllegalArgumentException(ex);
    }
  }

  // find the start element and parses the name
  private static String parseElementName(XMLStreamReader reader) throws XMLStreamException {
    int event = reader.getEventType();
    while (event != XMLStreamConstants.START_ELEMENT) {
      event = reader.next();
    }
    return reader.getLocalName();
  }

  // parses attributes into a map
  private static ImmutableMap<String, String> parseAttributes(XMLStreamReader reader) {
    ImmutableMap<String, String> attrs;
    int attributeCount = reader.getAttributeCount() + reader.getNamespaceCount();
    if (attributeCount == 0) {
      attrs = ImmutableMap.of();
    } else {
      ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
      for (int i = 0; i < reader.getAttributeCount(); i++) {
        builder.put(reader.getAttributeLocalName(i), reader.getAttributeValue(i));
      }
      attrs = builder.build();
    }
    return attrs;
  }

  //-------------------------------------------------------------------------
  // creates the XML input factory, recreated each time to avoid JDK-8028111
  // this also provides some protection against hackers attacking XML
  private static XMLInputFactory xmlInputFactory() {
    XMLInputFactory factory = XMLInputFactory.newFactory();
    factory.setProperty(XMLInputFactory.IS_COALESCING, true);
    factory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, true);
    factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
    factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
    return factory;
  }

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private XmlFile(XmlElement root, Map<String, XmlElement> refs) {
    this.root = ArgChecker.notNull(root, "root");
    this.refs = ImmutableMap.copyOf(refs);
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the root element of this file.
   * 
   * @return the root element
   */
  public XmlElement getRoot() {
    return root;
  }

  /**
   * Gets the reference map of id to element.
   * <p>
   * This is used to decode references, such as an id/href pair.
   * <p>
   * For example, if one part of the XML has {@code <foo id="fooId">}, the map will
   * contain an entry mapping "fooId" to the parsed element {@code <foo>}.
   * 
   * @return the map of id to element
   */
  public ImmutableMap<String, XmlElement> getReferences() {
    return refs;
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if this file equals another.
   * <p>
   * The comparison checks the content and reference map.
   * 
   * @param obj  the other section, null returns false
   * @return true if equal
   */
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj instanceof XmlFile) {
      XmlFile other = (XmlFile) obj;
      return root.equals(other.root) && refs.equals(other.refs);
    }
    return false;
  }

  /**
   * Returns a suitable hash code for the file.
   * 
   * @return the hash code
   */
  @Override
  public int hashCode() {
    return root.hashCode() ^ refs.hashCode();
  }

  /**
   * Returns a string describing the file.
   * 
   * @return the descriptive string
   */
  @Override
  public String toString() {
    return root.toString();
  }

}
