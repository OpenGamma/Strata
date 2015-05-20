package com.opengamma.strata.examples.fpml;

import com.opengamma.strata.examples.fpml.generated.Note;
import org.testng.annotations.Test;

import static com.opengamma.strata.examples.fpml.TestHarness.badXmlFile;
import static com.opengamma.strata.examples.fpml.TestHarness.goodXmlFile;
import static com.opengamma.strata.examples.fpml.TestHarness.missingXmlFile;
import static com.opengamma.strata.examples.fpml.XmlTools.deserializeFromXml;
import static org.testng.Assert.assertEquals;

@Test
public class JaxbTests {

  @Test
  public void parseFromAFile() {

    Note note = deserializeFromXml(Note.class, goodXmlFile);

    assertEquals("F", note.getFrom());
    assertEquals("T", note.getTo());
    assertEquals("H", note.getHeading());
    assertEquals("B", note.getBody());
  }

  @Test(expectedExceptions = RuntimeException.class)
  public void badFileThrows() {
    deserializeFromXml(Note.class, badXmlFile);
  }

  @Test(expectedExceptions = RuntimeException.class)
  public void missingFileThrows() {
    deserializeFromXml(Note.class, missingXmlFile);
  }

}