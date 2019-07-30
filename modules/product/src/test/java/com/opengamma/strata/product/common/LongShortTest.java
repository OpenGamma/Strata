/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.common;

import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverEnum;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.testng.Assert.assertEquals;

import java.util.Locale;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test {@link LongShort}.
 */
@Test
public class LongShortTest {

  //-------------------------------------------------------------------------
  public void test_ofLong() {
    assertEquals(LongShort.ofLong(true), LongShort.LONG);
    assertEquals(LongShort.ofLong(false), LongShort.SHORT);
  }

  public void test_isLong() {
    assertEquals(LongShort.LONG.isLong(), true);
    assertEquals(LongShort.SHORT.isLong(), false);
  }

  public void test_isShort() {
    assertEquals(LongShort.LONG.isShort(), false);
    assertEquals(LongShort.SHORT.isShort(), true);
  }

  public void test_sign() {
    assertEquals(LongShort.LONG.sign(), 1);
    assertEquals(LongShort.SHORT.sign(), -1);
  }

  public void test_opposite() {
    assertEquals(LongShort.LONG.opposite(), LongShort.SHORT);
    assertEquals(LongShort.SHORT.opposite(), LongShort.LONG);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "name")
  public static Object[][] data_name() {
    return new Object[][] {
        {LongShort.LONG, "Long"},
        {LongShort.SHORT, "Short"},
    };
  }

  @Test(dataProvider = "name")
  public void test_toString(LongShort convention, String name) {
    assertEquals(convention.toString(), name);
  }

  @Test(dataProvider = "name")
  public void test_of_lookup(LongShort convention, String name) {
    assertEquals(LongShort.of(name), convention);
  }

  @Test(dataProvider = "name")
  public void test_of_lookupUpperCase(LongShort convention, String name) {
    assertEquals(LongShort.of(name.toUpperCase(Locale.ENGLISH)), convention);
  }

  @Test(dataProvider = "name")
  public void test_of_lookupLowerCase(LongShort convention, String name) {
    assertEquals(LongShort.of(name.toLowerCase(Locale.ENGLISH)), convention);
  }

  public void test_of_lookup_notFound() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> LongShort.of("Rubbish"));
  }

  public void test_of_lookup_null() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> LongShort.of(null));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverEnum(LongShort.class);
  }

  public void test_serialization() {
    assertSerialization(LongShort.LONG);
  }

  public void test_jodaConvert() {
    assertJodaConvert(LongShort.class, LongShort.LONG);
  }

}
