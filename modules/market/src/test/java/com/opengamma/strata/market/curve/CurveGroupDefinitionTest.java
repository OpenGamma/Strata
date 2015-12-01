/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_1M;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_1W;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.basics.index.OvernightIndices.GBP_SONIA;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.time.Period;
import java.util.Optional;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.Trade;
import com.opengamma.strata.basics.market.MarketData;
import com.opengamma.strata.basics.market.ObservableKey;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.node.DummyFraCurveNode;
import com.opengamma.strata.market.interpolator.CurveExtrapolators;
import com.opengamma.strata.market.interpolator.CurveInterpolators;
import com.opengamma.strata.market.key.QuoteKey;

/**
 * Test {@link CurveGroupDefinition}.
 */
@Test
public class CurveGroupDefinitionTest {

  private static final ObservableKey GBP_LIBOR_1M_ID = QuoteKey.of(StandardId.of("OG", "Ticker1"));
  private static final ObservableKey GBP_LIBOR_3M_ID = QuoteKey.of(StandardId.of("OG", "Ticker3"));
  private static final DummyFraCurveNode NODE1 = DummyFraCurveNode.of(Period.ofMonths(1), GBP_LIBOR_1M, GBP_LIBOR_1M_ID);
  private static final DummyFraCurveNode NODE2 = DummyFraCurveNode.of(Period.ofMonths(3), GBP_LIBOR_3M, GBP_LIBOR_3M_ID);
  private static final InterpolatedNodalCurveDefinition CURVE_DEFN = InterpolatedNodalCurveDefinition.builder()
      .name(CurveName.of("Test"))
      .xValueType(ValueType.YEAR_FRACTION)
      .yValueType(ValueType.ZERO_RATE)
      .dayCount(ACT_365F)
      .nodes(ImmutableList.of(NODE1, NODE2))
      .interpolator(CurveInterpolators.LINEAR)
      .extrapolatorLeft(CurveExtrapolators.FLAT)
      .extrapolatorRight(CurveExtrapolators.FLAT)
      .build();
  private static final InterpolatedNodalCurveDefinition CURVE_CONFIG2 = CURVE_DEFN.toBuilder()
      .name(CurveName.of("Test2"))
      .build();
  private static final CurveGroupEntry ENTRY1 = CurveGroupEntry.builder()
      .curveDefinition(CURVE_DEFN)
      .discountCurrencies(GBP)
      .iborIndices(GBP_LIBOR_1W)
      .overnightIndices(GBP_SONIA)
      .build();
  private static final CurveGroupEntry ENTRY2 = CurveGroupEntry.builder()
      .curveDefinition(CURVE_CONFIG2)
      .iborIndices(GBP_LIBOR_1M, GBP_LIBOR_3M)
      .build();
  private static final CurveGroupEntry ENTRY3 = CurveGroupEntry.builder()
      .curveDefinition(CURVE_DEFN)
      .discountCurrencies(GBP)
      .iborIndices(GBP_LIBOR_1M, GBP_LIBOR_3M)
      .build();

  public void test_builder1() {
    CurveGroupDefinition test = CurveGroupDefinition.builder()
        .name(CurveGroupName.of("Test"))
        .addDiscountCurve(CURVE_DEFN, GBP)
        .addForwardCurve(CURVE_DEFN, GBP_SONIA)
        .addForwardCurve(CURVE_DEFN, GBP_LIBOR_1W)
        .addForwardCurve(CURVE_CONFIG2, GBP_LIBOR_1M, GBP_LIBOR_3M)
        .build();
    assertEquals(test.getName(), CurveGroupName.of("Test"));
    assertEquals(test.getEntries(), ImmutableList.of(ENTRY1, ENTRY2));
    assertEquals(test.findEntry(CurveName.of("Test")), Optional.of(ENTRY1));
    assertEquals(test.findEntry(CurveName.of("Test2")), Optional.of(ENTRY2));
    assertEquals(test.findEntry(CurveName.of("Rubbish")), Optional.empty());
  }

  public void test_builder2() {
    CurveGroupDefinition test = CurveGroupDefinition.builder()
        .name(CurveGroupName.of("Test"))
        .addCurve(CURVE_DEFN, GBP, GBP_LIBOR_1M, GBP_LIBOR_3M)
        .build();
    assertEquals(test.getName(), CurveGroupName.of("Test"));
    assertEquals(test.getEntries(), ImmutableList.of(ENTRY3));
    assertEquals(test.findEntry(CurveName.of("Test")), Optional.of(ENTRY3));
    assertEquals(test.findEntry(CurveName.of("Test2")), Optional.empty());
    assertEquals(test.findEntry(CurveName.of("Rubbish")), Optional.empty());
  }

  //-------------------------------------------------------------------------
  public void test_tradesInitialGuesses() {
    CurveGroupDefinition test = CurveGroupDefinition.builder()
        .name(CurveGroupName.of("Test"))
        .addCurve(CURVE_DEFN, GBP, GBP_LIBOR_1M, GBP_LIBOR_3M)
        .build();

    MarketData marketData = MarketData.builder()
        .addValues(ImmutableMap.of(GBP_LIBOR_1M_ID, 0.5d, GBP_LIBOR_3M_ID, 1.5d))
        .build();
    Trade trade1 = NODE1.trade(date(2015, 6, 30), marketData);
    Trade trade2 = NODE2.trade(date(2015, 6, 30), marketData);
    assertEquals(test.getTotalParameterCount(), 2);
    assertEquals(test.trades(date(2015, 6, 30), marketData), ImmutableList.of(trade1, trade2));
    assertEquals(test.initialGuesses(date(2015, 6, 30), marketData), ImmutableList.of(0.5d, 1.5d));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    CurveGroupDefinition test = CurveGroupDefinition.builder()
        .name(CurveGroupName.of("Test"))
        .addDiscountCurve(CURVE_DEFN, GBP)
        .build();
    coverImmutableBean(test);
    CurveGroupDefinition test2 = CurveGroupDefinition.builder()
        .name(CurveGroupName.of("Test2"))
        .addForwardCurve(CURVE_CONFIG2, GBP_LIBOR_1M)
        .build();
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    CurveGroupDefinition test = CurveGroupDefinition.builder()
        .name(CurveGroupName.of("Test"))
        .addDiscountCurve(CURVE_DEFN, GBP)
        .build();
    assertSerialization(test);
  }

}
