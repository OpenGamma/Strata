/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.etd;

import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverEnum;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test {@link EtdType}.
 */
@Test
public class EtdTypeTest {

  //-------------------------------------------------------------------------
  @DataProvider(name = "name")
  public static Object[][] data_name() {
    return new Object[][] {
        {EtdType.FUTURE, "Future"},
        {EtdType.OPTION, "Option"},
    };
  }

  @Test(dataProvider = "name")
  public void test_toString(EtdType convention, String name) {
    assertEquals(convention.toString(), name);
  }

  @Test(dataProvider = "name")
  public void test_of_lookup(EtdType convention, String name) {
    assertEquals(EtdType.of(name), convention);
  }

  public void test_of_lookup_notFound() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> EtdType.of("Rubbish"));
  }

  public void test_of_lookup_null() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> EtdType.of(null));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverEnum(EtdType.class);
  }

  public void test_serialization() {
    assertSerialization(EtdType.FUTURE);
  }

  public void test_jodaConvert() {
    assertJodaConvert(EtdType.class, EtdType.OPTION);
  }

}
