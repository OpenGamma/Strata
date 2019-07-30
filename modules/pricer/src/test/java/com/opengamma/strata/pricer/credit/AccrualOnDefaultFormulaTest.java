/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.credit;

import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverEnum;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.testng.Assert.assertEquals;

import java.util.Locale;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test {@link AccrualOnDefaultFormula}.
 */
@Test
public class AccrualOnDefaultFormulaTest {

  @DataProvider(name = "name")
  public static Object[][] data_name() {
    return new Object[][] {
        {AccrualOnDefaultFormula.ORIGINAL_ISDA, "OriginalISDA"},
        {AccrualOnDefaultFormula.MARKIT_FIX, "MarkitFix"},
        {AccrualOnDefaultFormula.CORRECT, "Correct"},
    };
  }

  @Test(dataProvider = "name")
  public void test_toString(AccrualOnDefaultFormula convention, String name) {
    assertEquals(convention.toString(), name);
  }

  @Test(dataProvider = "name")
  public void test_of_lookup(AccrualOnDefaultFormula convention, String name) {
    assertEquals(AccrualOnDefaultFormula.of(name), convention);
  }

  @Test(dataProvider = "name")
  public void test_of_lookupUpperCase(AccrualOnDefaultFormula convention, String name) {
    assertEquals(AccrualOnDefaultFormula.of(name.toUpperCase(Locale.ENGLISH)), convention);
  }

  @Test(dataProvider = "name")
  public void test_of_lookupLowerCase(AccrualOnDefaultFormula convention, String name) {
    assertEquals(AccrualOnDefaultFormula.of(name.toLowerCase(Locale.ENGLISH)), convention);
  }

  public void test_of_lookup_notFound() {
    assertThatIllegalArgumentException().isThrownBy(() -> AccrualOnDefaultFormula.of("Rubbish"));
  }

  public void test_of_lookup_null() {
    assertThatIllegalArgumentException().isThrownBy(() -> AccrualOnDefaultFormula.of(null));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverEnum(AccrualOnDefaultFormula.class);
  }

  public void test_serialization() {
    assertSerialization(AccrualOnDefaultFormula.ORIGINAL_ISDA);
  }

  public void test_jodaConvert() {
    assertJodaConvert(AccrualOnDefaultFormula.class, AccrualOnDefaultFormula.MARKIT_FIX);
  }

}
