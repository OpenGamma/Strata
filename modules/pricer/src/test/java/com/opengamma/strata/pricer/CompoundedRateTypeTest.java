/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer;

import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverEnum;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test {@link CompoundedRateType}.
 */
@Test
public class CompoundedRateTypeTest {

  //-------------------------------------------------------------------------
  @DataProvider(name = "name")
  public static Object[][] data_name() {
    return new Object[][] {
        {CompoundedRateType.PERIODIC, "Periodic"},
        {CompoundedRateType.CONTINUOUS, "Continuous"},
    };
  }

  @Test(dataProvider = "name")
  public void test_toString(CompoundedRateType convention, String name) {
    assertEquals(convention.toString(), name);
  }

  @Test(dataProvider = "name")
  public void test_of_lookup(CompoundedRateType convention, String name) {
    assertEquals(CompoundedRateType.of(name), convention);
  }

  public void test_of_lookup_notFound() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> CompoundedRateType.of("Rubbish"));
  }

  public void test_of_lookup_null() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> CompoundedRateType.of(null));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverEnum(CompoundedRateType.class);
  }

  public void test_serialization() {
    assertSerialization(CompoundedRateType.CONTINUOUS);
  }

  public void test_jodaConvert() {
    assertJodaConvert(CompoundedRateType.class, CompoundedRateType.CONTINUOUS);
  }

}
