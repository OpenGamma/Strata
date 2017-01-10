/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.named;

import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static org.testng.Assert.assertEquals;

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.collect.named.ExtendedEnum.ExternalEnumNames;

/**
 * Test {@link ExtendedEnum}.
 */
@Test
public class ExtendedEnumTest {

  public void test_enum_SampleNamed() {
    ExtendedEnum<SampleNamed> test = ExtendedEnum.of(SampleNamed.class);
    assertEquals(test.lookupAll(),
        ImmutableMap.builder()
            .put("Standard", SampleNameds.STANDARD)
            .put("STANDARD", SampleNameds.STANDARD)
            .put("More", MoreSampleNameds.MORE)
            .put("MORE", MoreSampleNameds.MORE)
            .put("Other", OtherSampleNameds.OTHER)
            .put("Another1", SampleNamedInstanceLookup1.ANOTHER1)
            .put("ANOTHER1", SampleNamedInstanceLookup1.ANOTHER1)
            .put("Another2", SampleNamedInstanceLookup2.ANOTHER2)
            .put("ANOTHER2", SampleNamedInstanceLookup2.ANOTHER2)
            .build());
    assertEquals(test.lookupAllNormalized(),
        ImmutableMap.builder()
            .put("Standard", SampleNameds.STANDARD)
            .put("More", MoreSampleNameds.MORE)
            .put("Other", OtherSampleNameds.OTHER)
            .put("Another1", SampleNamedInstanceLookup1.ANOTHER1)
            .put("Another2", SampleNamedInstanceLookup2.ANOTHER2)
            .build());
    assertEquals(test.alternateNames(), ImmutableMap.of("Alternate", "Standard", "ALTERNATE", "Standard"));
    assertEquals(test.find("Standard"), Optional.of(SampleNameds.STANDARD));
    assertEquals(test.find("STANDARD"), Optional.of(SampleNameds.STANDARD));
    assertEquals(test.find("Rubbish"), Optional.empty());
    assertEquals(test.lookup("Standard"), SampleNameds.STANDARD);
    assertEquals(test.lookup("Alternate"), SampleNameds.STANDARD);
    assertEquals(test.lookup("ALTERNATE"), SampleNameds.STANDARD);
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

  public void test_enum_SampleNamed_externals() {
    ExtendedEnum<SampleNamed> test = ExtendedEnum.of(SampleNamed.class);
    assertEquals(test.externalNameGroups(), ImmutableSet.of("Foo", "Bar"));
    assertThrowsIllegalArg(() -> test.externalNames("Rubbish"));
    ExternalEnumNames<SampleNamed> fooExternals = test.externalNames("Foo");
    assertEquals(fooExternals.lookup("Foo1"), SampleNameds.STANDARD);
    assertEquals(fooExternals.lookup("Foo1", SampleNamed.class), SampleNameds.STANDARD);
    assertEquals(fooExternals.lookup("Foo1", SampleNamed.class), SampleNameds.STANDARD);
    assertEquals(fooExternals.externalNames(), ImmutableMap.of("Foo1", "Standard"));
    assertThrowsIllegalArg(() -> fooExternals.lookup("Rubbish"));
    assertThrowsIllegalArg(() -> fooExternals.lookup(null));
    assertThrowsIllegalArg(() -> fooExternals.lookup("Other", MoreSampleNameds.class));
    assertEquals(fooExternals.toString(), "ExternalEnumNames[SampleNamed:Foo]");

    ExternalEnumNames<SampleNamed> barExternals = test.externalNames("Bar");
    assertEquals(barExternals.lookup("Foo1"), MoreSampleNameds.MORE);
    assertEquals(barExternals.lookup("Foo2"), SampleNameds.STANDARD);
    assertEquals(barExternals.reverseLookup(MoreSampleNameds.MORE), "Foo1");
    assertEquals(barExternals.reverseLookup(SampleNameds.STANDARD), "Foo2");
    assertThrowsIllegalArg(() -> barExternals.reverseLookup(OtherSampleNameds.OTHER));
    assertEquals(barExternals.externalNames(), ImmutableMap.of("Foo1", "More", "Foo2", "Standard"));
    assertEquals(barExternals.toString(), "ExternalEnumNames[SampleNamed:Bar]");
  }

  public void test_enum_SampleOther() {
    ExtendedEnum<SampleOther> test = ExtendedEnum.of(SampleOther.class);
    assertEquals(test.lookupAll(), ImmutableMap.of());
    assertEquals(test.alternateNames(), ImmutableMap.of());
    assertEquals(test.externalNameGroups(), ImmutableSet.of());
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
