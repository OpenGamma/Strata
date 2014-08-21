/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.collect.named;

import static com.opengamma.collect.TestHelper.assertThrows;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;

/**
 * Test {@link ExtendedEnum}.
 */
@Test
public class ExtendedEnumTest {

  public void test_enum_MockNamed() {
    ExtendedEnum<MockNamed> test = ExtendedEnum.of(MockNamed.class);
    assertEquals(test.lookupAll(),
        ImmutableMap.of("Standard", MockNameds.STANDARD, "More", MoreMockNameds.MORE, "Other", OtherMockNameds.OTHER));
    assertEquals(test.alternateNames(), ImmutableMap.of("Alternate", "Standard"));
    assertEquals(test.lookup("Standard"), MockNameds.STANDARD);
    assertEquals(test.lookup("Alternate"), MockNameds.STANDARD);
    assertEquals(test.lookup("More"), MoreMockNameds.MORE);
    assertEquals(test.lookup("Other"), OtherMockNameds.OTHER);
    assertThrows(() -> test.lookup("Rubbish"), IllegalArgumentException.class);
    assertThrows(() -> test.lookup(null), IllegalArgumentException.class);
    assertEquals(test.toString(), "ExtendedEnum[MockNamed]");
  }

  public void test_enum_MockOther() {
    ExtendedEnum<MockOther> test = ExtendedEnum.of(MockOther.class);
    assertEquals(test.lookupAll(), ImmutableMap.of());
    assertEquals(test.alternateNames(), ImmutableMap.of());
    assertThrows(() -> test.lookup("Rubbish"), IllegalArgumentException.class);
    assertThrows(() -> test.lookup(null), IllegalArgumentException.class);
    assertEquals(test.toString(), "ExtendedEnum[MockOther]");
  }

  public void test_enum_invalid() {
    assertThrows(() -> ExtendedEnum.of(MockInvalid1.class), IllegalArgumentException.class);
    assertThrows(() -> ExtendedEnum.of(MockInvalid2.class), IllegalArgumentException.class);
    assertThrows(() -> ExtendedEnum.of(MockInvalid3.class), IllegalArgumentException.class);
  }

}
