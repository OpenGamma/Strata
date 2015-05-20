package com.opengamma.strata.examples.fpml;

import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;

import static com.opengamma.strata.examples.fpml.XmlTools.inputStreamFromClasspath;
import static org.testng.AssertJUnit.assertTrue;

@Test
public class TestHarness {

  // these live in etc and are accessed through classpath
  protected final static String goodXsdFile = "fpml/test/xsd/note.xsd";
  protected final static String goodXmlFile = "fpml/test/xml/note.xml";

  protected final static String badXmlFile = "fpml/test/xml/bad.xml";

  @Test
  public void canReadGoodSchemaFile() {
    thereIsDataOnTheFile(goodXsdFile);
  }

  @Test
  public void canReadGoodXmlFile() {
    thereIsDataOnTheFile(goodXmlFile);
  }

  private void thereIsDataOnTheFile(final String xmlFile) {
    InputStream is = inputStreamFromClasspath(xmlFile);
    assertTrue("", is != null);
    try {
      assertTrue(is.read() != -1);
      is.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}
