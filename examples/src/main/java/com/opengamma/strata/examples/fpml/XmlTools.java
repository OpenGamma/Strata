package com.opengamma.strata.examples.fpml;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class XmlTools {

  final static Logger logger = LoggerFactory.getLogger(XmlTools.class);

  final static String xml2001Schema = XMLConstants.W3C_XML_SCHEMA_NS_URI;
  final static SchemaFactory defaultSchemaFactory = SchemaFactory.newInstance(xml2001Schema);

  @SuppressWarnings("unchecked")
  public static <T> T deserializeFromXml(final Class<T> clazz, final String filename) {
    try {
      JAXBContext jaxbContext = JAXBContext.newInstance(clazz);
      Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
      Object o = jaxbUnmarshaller.unmarshal(sourceFromXml(filename));
      return (T) o;
    } catch (JAXBException e) {
      throw new RuntimeException(e);
    }
  }

  private static Schema schemaFromXsd(final String filename) {
    try {
      return defaultSchemaFactory.newSchema(sourceFromXml(filename));
    } catch (SAXException e) {
      throw new RuntimeException("Error getting schema", e);
    }
  }

  private static Source sourceFromXml(final String filename) {
    return new StreamSource(inputStreamFromXml(filename));
  }

  protected static InputStream inputStreamFromXml(final String filename) {
    InputStream result = inputStreamFromFilesystem(filename);
    if (result == null) {
      result = inputStreamFromClasspath(filename);
    }
    return result;
  }

  protected static InputStream inputStreamFromClasspath(final String filename) {
    return ClassLoader.getSystemClassLoader().getResourceAsStream(filename);
  }

  protected static InputStream inputStreamFromFilesystem(final String filename) {
    InputStream result;
    final String xmlFile = "/tmp/" + filename;
    try {
      result = new FileInputStream(xmlFile);
    } catch (FileNotFoundException e) {
      result = null;
    }
    if (result != null) {
      System.out.println("Found an override file, using: " + xmlFile);
      logger.warn("Found an override file, using: " + xmlFile);
    }
    return result;
  }

  private static void validateASourceAgainstASchema(final Schema schema, final Source source) {
    Validator validator = schema.newValidator();
    try {
      validator.validate(source);
    } catch (SAXException | IOException e) {
      throw new RuntimeException("Error validating", e);
    }
  }

  public static void validateASourceAgainstASchema(final String xsdFile, final String xmlFile) {
    validateASourceAgainstASchema(schemaFromXsd(xsdFile), sourceFromXml(xmlFile));
  }

  public static void thereIsDataInXml(final String xmlFile) {
    InputStream is = inputStreamFromClasspath(xmlFile);
    assert (is != null);
    try {
      assert (is.read() != -1);
      is.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}