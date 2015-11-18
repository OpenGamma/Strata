/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_1M;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.basics.index.OvernightIndices.GBP_SONIA;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import java.time.Period;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.node.DummyFraCurveNode;
import com.opengamma.strata.market.interpolator.CurveExtrapolators;
import com.opengamma.strata.market.interpolator.CurveInterpolators;
import com.opengamma.strata.market.key.QuoteKey;

/**
 * Test {@link CurveGroupEntry}.
 */
@Test
public class CurveGroupEntryTest {

  private static final InterpolatedNodalCurveDefinition CURVE_DEFN =
      InterpolatedNodalCurveDefinition.builder()
          .name(CurveName.of("Test"))
          .xValueType(ValueType.YEAR_FRACTION)
          .yValueType(ValueType.ZERO_RATE)
          .dayCount(ACT_365F)
          .nodes(ImmutableList.of(
              DummyFraCurveNode.of(Period.ofMonths(1), GBP_LIBOR_1M, QuoteKey.of(StandardId.of("OG", "Ticker")))))
          .interpolator(CurveInterpolators.LINEAR)
          .extrapolatorLeft(CurveExtrapolators.FLAT)
          .extrapolatorRight(CurveExtrapolators.FLAT)
          .build();
  private static final InterpolatedNodalCurveDefinition CURVE_DEFN2 = CURVE_DEFN.toBuilder()
      .name(CurveName.of("Test2"))
      .build();

  public void test_builder() {
    CurveGroupEntry test = CurveGroupEntry.builder()
        .curveDefinition(CURVE_DEFN)
        .discountCurrencies(GBP)
        .iborIndices(GBP_LIBOR_1M, GBP_LIBOR_3M)
        .overnightIndices(GBP_SONIA)
        .build();
    assertEquals(test.getCurveDefinition(), CURVE_DEFN);
    assertEquals(test.getDiscountCurrencies(), ImmutableSet.of(GBP));
    assertEquals(test.getIborIndices(), ImmutableSet.of(GBP_LIBOR_1M, GBP_LIBOR_3M));
    assertEquals(test.getOvernightIndices(), ImmutableSet.of(GBP_SONIA));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    CurveGroupEntry test = CurveGroupEntry.builder()
        .curveDefinition(CURVE_DEFN)
        .discountCurrencies(GBP)
        .build();
    coverImmutableBean(test);
    CurveGroupEntry test2 = CurveGroupEntry.builder()
        .curveDefinition(CURVE_DEFN2)
        .iborIndices(GBP_LIBOR_1M)
        .overnightIndices(GBP_SONIA)
        .build();
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    CurveGroupEntry test = CurveGroupEntry.builder()
        .curveDefinition(CURVE_DEFN)
        .discountCurrencies(GBP)
        .build();
    assertSerialization(test);
  }

}
