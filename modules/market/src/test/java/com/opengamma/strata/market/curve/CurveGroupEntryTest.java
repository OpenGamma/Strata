/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_1M;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.basics.index.OvernightIndices.GBP_SONIA;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableSet;

/**
 * Test {@link CurveGroupEntry}.
 */
@Test
public class CurveGroupEntryTest {

  private static final CurveName CURVE_NAME1 = CurveName.of("Test");
  private static final CurveName CURVE_NAME2 = CurveName.of("Test2");

  public void test_builder() {
    CurveGroupEntry test = CurveGroupEntry.builder()
        .curveName(CURVE_NAME1)
        .discountCurrencies(GBP)
        .indices(GBP_LIBOR_1M, GBP_LIBOR_3M, GBP_SONIA)
        .build();
    assertEquals(test.getCurveName(), CURVE_NAME1);
    assertEquals(test.getDiscountCurrencies(), ImmutableSet.of(GBP));
    assertEquals(test.getIndices(), ImmutableSet.of(GBP_LIBOR_1M, GBP_LIBOR_3M, GBP_SONIA));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    CurveGroupEntry test = CurveGroupEntry.builder()
        .curveName(CURVE_NAME1)
        .discountCurrencies(GBP)
        .build();
    coverImmutableBean(test);
    CurveGroupEntry test2 = CurveGroupEntry.builder()
        .curveName(CURVE_NAME2)
        .indices(GBP_LIBOR_1M, GBP_SONIA)
        .build();
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    CurveGroupEntry test = CurveGroupEntry.builder()
        .curveName(CURVE_NAME1)
        .discountCurrencies(GBP)
        .build();
    assertSerialization(test);
  }

}
