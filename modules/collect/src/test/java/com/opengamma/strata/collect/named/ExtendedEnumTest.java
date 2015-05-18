/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.named;

import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static org.testng.Assert.assertEquals;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;

/**
 * Test {@link ExtendedEnum}.
 */
@Test
public class ExtendedEnumTest {

  public void test_enum_SampleNamed() {
    ExtendedEnum<SampleNamed> test = ExtendedEnum.of(SampleNamed.class);
    assertEquals(test.lookupAll(),
        ImmutableMap.of(
            "Standard", SampleNameds.STANDARD,
            "More", MoreSampleNameds.MORE,
            "Other", OtherSampleNameds.OTHER,
            "Another1", SampleNamedInstanceLookup1.ANOTHER1,
            "Another2", SampleNamedInstanceLookup2.ANOTHER2));
    assertEquals(test.alternateNames(), ImmutableMap.of("Alternate", "Standard"));
    assertEquals(test.lookup("Standard"), SampleNameds.STANDARD);
    assertEquals(test.lookup("Alternate"), SampleNameds.STANDARD);
    assertEquals(test.lookup("More"), MoreSampleNameds.MORE);
    assertEquals(test.lookup("More", MoreSampleNameds.class), MoreSampleNameds.MORE);
    assertEquals(test.lookup("Other"), OtherSampleNameds.OTHER);
    assertEquals(test.lookup("Other", OtherSampleNameds.class), OtherSampleNameds.OTHER);
    assertEquals(test.lookup("Another1"), SampleNamedInstanceLookup1.ANOTHER1);
    assertEquals(test.lookup("Another2"), SampleNamedInstanceLookup2.ANOTHER2);
    assertThrowsIllegalArg(() -> test.lookup("Rubbish"));
    assertThrowsIllegalArg(() -> test.lookup(null));
    assertThrowsIllegalArg(() -> test.lookup("Other", MoreSampleNameds.class));
    assertEquals(test.toString(), "ExtendedEnum[SampleNamed]");
  }

  public void test_enum_SampleOther() {
    ExtendedEnum<SampleOther> test = ExtendedEnum.of(SampleOther.class);
    assertEquals(test.lookupAll(), ImmutableMap.of());
    assertEquals(test.alternateNames(), ImmutableMap.of());
    assertThrowsIllegalArg(() -> test.lookup("Rubbish"));
    assertThrowsIllegalArg(() -> test.lookup(null));
    assertEquals(test.toString(), "ExtendedEnum[SampleOther]");
  }

  public void test_enum_invalid() {
    Logger logger = Logger.getLogger(ExtendedEnum.class.getName());
    Level level = logger.getLevel();
    try {
      logger.setLevel(Level.OFF);
      // these return empty instances to avoid ExceptionInInitializerError
      assertEquals(ExtendedEnum.of(SampleInvalid1.class).lookupAll().isEmpty(), true);
      assertEquals(ExtendedEnum.of(SampleInvalid2.class).lookupAll().isEmpty(), true);
      assertEquals(ExtendedEnum.of(SampleInvalid3.class).lookupAll().isEmpty(), true);
      assertEquals(ExtendedEnum.of(SampleInvalid4.class).lookupAll().isEmpty(), true);
      assertEquals(ExtendedEnum.of(SampleInvalid5.class).lookupAll().isEmpty(), true);
      assertEquals(ExtendedEnum.of(SampleInvalid6.class).lookupAll().isEmpty(), true);
      assertEquals(ExtendedEnum.of(SampleInvalid7.class).lookupAll().isEmpty(), true);
    } finally {
      logger.setLevel(level);
    }
  }

}
