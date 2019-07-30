/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverEnum;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test {@link CurveNodeClashAction}.
 */
@Test
public class CurveNodeClashActionTest {

  //-------------------------------------------------------------------------
  @DataProvider(name = "name")
  public static Object[][] data_name() {
    return new Object[][] {
        {CurveNodeClashAction.DROP_THIS, "DropThis"},
        {CurveNodeClashAction.DROP_OTHER, "DropOther"},
        {CurveNodeClashAction.EXCEPTION, "Exception"},
    };
  }

  @Test(dataProvider = "name")
  public void test_toString(CurveNodeClashAction convention, String name) {
    assertEquals(convention.toString(), name);
  }

  @Test(dataProvider = "name")
  public void test_of_lookup(CurveNodeClashAction convention, String name) {
    assertEquals(CurveNodeClashAction.of(name), convention);
  }

  public void test_of_lookup_notFound() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> CurveNodeClashAction.of("Rubbish"));
  }

  public void test_of_lookup_null() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> CurveNodeClashAction.of(null));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverEnum(CurveNodeClashAction.class);
  }

  public void test_serialization() {
    assertSerialization(CurveNodeClashAction.DROP_THIS);
  }

  public void test_jodaConvert() {
    assertJodaConvert(CurveNodeClashAction.class, CurveNodeClashAction.DROP_THIS);
  }

}
