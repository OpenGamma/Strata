/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.result;

import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverEnum;
import static org.testng.Assert.assertEquals;

import java.util.Locale;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test {@link FailureReason}.
 */
@Test
public class FailureReasonTest {

  //-------------------------------------------------------------------------
  @DataProvider(name = "name")
  public static Object[][] data_name() {
    return new Object[][] {
        {FailureReason.CALCULATION_FAILED, "CALCULATION_FAILED"},
        {FailureReason.CURRENCY_CONVERSION, "CURRENCY_CONVERSION"},
        {FailureReason.ERROR, "ERROR"},
        {FailureReason.INVALID, "INVALID"},
        {FailureReason.MISSING_DATA, "MISSING_DATA"},
        {FailureReason.MULTIPLE, "MULTIPLE"},
        {FailureReason.NOT_APPLICABLE, "NOT_APPLICABLE"},
        {FailureReason.OTHER, "OTHER"},
        {FailureReason.PARSING, "PARSING"},
        {FailureReason.UNSUPPORTED, "UNSUPPORTED"},
    };
  }

  @Test(dataProvider = "name")
  public void test_toString(FailureReason convention, String name) {
    assertEquals(convention.toString(), name);
  }

  @Test(dataProvider = "name")
  public void test_of_lookup(FailureReason convention, String name) {
    assertEquals(FailureReason.of(name), convention);
  }

  @Test(dataProvider = "name")
  public void test_of_lookupUpperCase(FailureReason convention, String name) {
    assertEquals(FailureReason.of(name.toUpperCase(Locale.ENGLISH)), convention);
  }

  @Test(dataProvider = "name")
  public void test_of_lookupLowerCase(FailureReason convention, String name) {
    assertEquals(FailureReason.of(name.toLowerCase(Locale.ENGLISH)), convention);
  }

  public void test_of_lookup_notFound() {
    assertThrowsIllegalArg(() -> FailureReason.of("Rubbish"));
  }

  public void test_of_lookup_null() {
    assertThrowsIllegalArg(() -> FailureReason.of(null));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverEnum(FailureReason.class);
  }

  public void test_serialization() {
    assertSerialization(FailureReason.CALCULATION_FAILED);
  }

  public void test_jodaConvert() {
    assertJodaConvert(FailureReason.class, FailureReason.CURRENCY_CONVERSION);
  }

}
