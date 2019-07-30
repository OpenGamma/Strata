/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap;

import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverEnum;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test {@link IborRateResetMethod}.
 */
@Test
public class IborRateResetMethodTest {

  //-------------------------------------------------------------------------
  @DataProvider(name = "name")
  public static Object[][] data_name() {
    return new Object[][] {
        {IborRateResetMethod.WEIGHTED, "Weighted"},
        {IborRateResetMethod.UNWEIGHTED, "Unweighted"},
    };
  }

  @Test(dataProvider = "name")
  public void test_toString(IborRateResetMethod convention, String name) {
    assertEquals(convention.toString(), name);
  }

  @Test(dataProvider = "name")
  public void test_of_lookup(IborRateResetMethod convention, String name) {
    assertEquals(IborRateResetMethod.of(name), convention);
  }

  public void test_of_lookup_notFound() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> IborRateResetMethod.of("Rubbish"));
  }

  public void test_of_lookup_null() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> IborRateResetMethod.of(null));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverEnum(IborRateResetMethod.class);
  }

  public void test_serialization() {
    assertSerialization(IborRateResetMethod.WEIGHTED);
  }

  public void test_jodaConvert() {
    assertJodaConvert(IborRateResetMethod.class, IborRateResetMethod.WEIGHTED);
  }

}
