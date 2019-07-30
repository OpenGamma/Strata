/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.credit.type;

import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverEnum;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test {@link AccrualStart}.
 */
@Test
public class AccrualStartTest {

  //-------------------------------------------------------------------------
  @DataProvider(name = "name")
  public static Object[][] data_name() {
    return new Object[][] {
        {AccrualStart.NEXT_DAY, "NextDay"},
        {AccrualStart.IMM_DATE, "ImmDate"},
    };
  }

  @Test(dataProvider = "name")
  public void test_toString(AccrualStart convention, String name) {
    assertEquals(convention.toString(), name);
  }

  @Test(dataProvider = "name")
  public void test_of_lookup(AccrualStart convention, String name) {
    assertEquals(AccrualStart.of(name), convention);
  }

  public void test_of_lookup_notFound() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> AccrualStart.of("Rubbish"));
  }

  public void test_of_lookup_null() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> AccrualStart.of(null));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverEnum(AccrualStart.class);
  }

  public void test_serialization() {
    assertSerialization(AccrualStart.IMM_DATE);
  }

  public void test_jodaConvert() {
    assertJodaConvert(AccrualStart.class, AccrualStart.IMM_DATE);
  }

}
