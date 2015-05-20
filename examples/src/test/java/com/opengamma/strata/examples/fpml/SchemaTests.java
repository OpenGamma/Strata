package com.opengamma.strata.examples.fpml;

import org.testng.annotations.Test;

import static com.opengamma.strata.examples.fpml.TestHarness.badXmlFile;
import static com.opengamma.strata.examples.fpml.TestHarness.goodXmlFile;
import static com.opengamma.strata.examples.fpml.TestHarness.goodXsdFile;
import static com.opengamma.strata.examples.fpml.TestHarness.missingXmlFile;
import static com.opengamma.strata.examples.fpml.XmlTools.validateASourceAgainstASchema;

@Test
public class SchemaTests {

  @Test
  public void canLoadAGoodXmlAndValidate() {
    validateASourceAgainstASchema(goodXsdFile, goodXmlFile);
  }

  @Test(expectedExceptions = RuntimeException.class)
  public void badXmlWillNotValidate() {
    validateASourceAgainstASchema(goodXsdFile, badXmlFile);
  }

  @Test(expectedExceptions = RuntimeException.class)
  public void missingXmlWillNotValidate() {
    validateASourceAgainstASchema(goodXsdFile, missingXmlFile);
  }

}
