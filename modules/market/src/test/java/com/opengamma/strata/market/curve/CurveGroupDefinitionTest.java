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
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.market.curve.CurveNodeClashAction.DROP_THIS;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.time.Period;
import java.util.Optional;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.data.ImmutableMarketData;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.data.ObservableId;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.interpolator.CurveExtrapolators;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolators;
import com.opengamma.strata.market.observable.QuoteId;
import com.opengamma.strata.product.Trade;

/**
 * Test {@link CurveGroupDefinition}.
 */
@Test
public class CurveGroupDefinitionTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final ObservableId GBP_LIBOR_1M_ID = QuoteId.of(StandardId.of("OG", "Ticker1"));
  private static final ObservableId GBP_LIBOR_3M_ID = QuoteId.of(StandardId.of("OG", "Ticker3"));
  private static final DummyFraCurveNode NODE1 = DummyFraCurveNode.of(Period.ofMonths(1), GBP_LIBOR_1M, GBP_LIBOR_1M_ID);
  private static final DummyFraCurveNode NODE2 = DummyFraCurveNode.of(Period.ofMonths(3), GBP_LIBOR_3M, GBP_LIBOR_3M_ID);
  private static final CurveNodeDateOrder DROP_THIS_2D = CurveNodeDateOrder.of(2, DROP_THIS);
  private static final CurveName CURVE_NAME1 = CurveName.of("Test");
  private static final CurveName CURVE_NAME2 = CurveName.of("Test2");
  private static final InterpolatedNodalCurveDefinition CURVE_DEFN1 = InterpolatedNodalCurveDefinition.builder()
      .name(CURVE_NAME1)
      .xValueType(ValueType.YEAR_FRACTION)
      .yValueType(ValueType.ZERO_RATE)
      .dayCount(ACT_365F)
      .nodes(ImmutableList.of(NODE1, NODE2))
      .interpolator(CurveInterpolators.LINEAR)
      .extrapolatorLeft(CurveExtrapolators.FLAT)
      .extrapolatorRight(CurveExtrapolators.FLAT)
      .build();
  private static final InterpolatedNodalCurveDefinition CURVE_DEFN2 = CURVE_DEFN1.toBuilder()
      .name(CURVE_NAME2)
      .build();
  private static final CurveGroupEntry ENTRY1 = CurveGroupEntry.builder()
      .curveName(CURVE_NAME1)
      .discountCurrencies(GBP)
      .indices(GBP_LIBOR_1W, GBP_SONIA)
      .build();
  private static final CurveGroupEntry ENTRY2 = CurveGroupEntry.builder()
      .curveName(CURVE_NAME2)
      .indices(GBP_LIBOR_1M, GBP_LIBOR_3M)
      .build();
  private static final CurveGroupEntry ENTRY3 = CurveGroupEntry.builder()
      .curveName(CURVE_NAME1)
      .discountCurrencies(GBP)
      .indices(GBP_LIBOR_1M, GBP_LIBOR_3M)
      .build();

  public void test_builder1() {
    CurveGroupDefinition test = CurveGroupDefinition.builder()
        .name(CurveGroupName.of("Test"))
        .addDiscountCurve(CURVE_DEFN1, GBP)
        .addForwardCurve(CURVE_DEFN1, GBP_SONIA)
        .addForwardCurve(CURVE_DEFN1, GBP_LIBOR_1W)
        .addForwardCurve(CURVE_DEFN2, GBP_LIBOR_1M, GBP_LIBOR_3M)
        .build();
    assertEquals(test.getName(), CurveGroupName.of("Test"));
    assertEquals(test.getEntries(), ImmutableList.of(ENTRY1, ENTRY2));
    assertEquals(test.findEntry(CurveName.of("Test")), Optional.of(ENTRY1));
    assertEquals(test.findEntry(CurveName.of("Test2")), Optional.of(ENTRY2));
    assertEquals(test.findEntry(CurveName.of("Rubbish")), Optional.empty());
    assertEquals(test.findCurveDefinition(CurveName.of("Test")), Optional.of(CURVE_DEFN1));
    assertEquals(test.findCurveDefinition(CurveName.of("Test2")), Optional.of(CURVE_DEFN2));
    assertEquals(test.findCurveDefinition(CurveName.of("Rubbish")), Optional.empty());
  }

  public void test_builder2() {
    CurveGroupDefinition test = CurveGroupDefinition.builder()
        .name(CurveGroupName.of("Test"))
        .addCurve(CURVE_DEFN1, GBP, GBP_LIBOR_1M, GBP_LIBOR_3M)
        .build();
    assertEquals(test.getName(), CurveGroupName.of("Test"));
    assertEquals(test.getEntries(), ImmutableList.of(ENTRY3));
    assertEquals(test.findEntry(CurveName.of("Test")), Optional.of(ENTRY3));
    assertEquals(test.findEntry(CurveName.of("Test2")), Optional.empty());
    assertEquals(test.findEntry(CurveName.of("Rubbish")), Optional.empty());
    assertEquals(test.findCurveDefinition(CurveName.of("Test")), Optional.of(CURVE_DEFN1));
    assertEquals(test.findCurveDefinition(CurveName.of("Test2")), Optional.empty());
    assertEquals(test.findCurveDefinition(CurveName.of("Rubbish")), Optional.empty());
  }

  public void test_builder3() {
    CurveGroupDefinition test = CurveGroupDefinition.builder()
        .name(CurveGroupName.of("Test"))
        .addDiscountCurve(CURVE_NAME1, GBP)
        .addForwardCurve(CURVE_NAME1, GBP_SONIA)
        .addForwardCurve(CURVE_NAME1, GBP_LIBOR_1W)
        .addForwardCurve(CURVE_NAME2, GBP_LIBOR_1M, GBP_LIBOR_3M)
        .build();
    assertEquals(test.getName(), CurveGroupName.of("Test"));
    assertEquals(test.getEntries(), ImmutableList.of(ENTRY1, ENTRY2));
    assertEquals(test.findEntry(CurveName.of("Test")), Optional.of(ENTRY1));
    assertEquals(test.findEntry(CurveName.of("Test2")), Optional.of(ENTRY2));
    assertEquals(test.findEntry(CurveName.of("Rubbish")), Optional.empty());
  }

  public void test_builder4() {
    CurveGroupDefinition test = CurveGroupDefinition.builder()
        .name(CurveGroupName.of("Test"))
        .addCurve(CURVE_NAME1, GBP, GBP_LIBOR_1M, GBP_LIBOR_3M)
        .build();
    assertEquals(test.getName(), CurveGroupName.of("Test"));
    assertEquals(test.getEntries(), ImmutableList.of(ENTRY3));
    assertEquals(test.findEntry(CurveName.of("Test")), Optional.of(ENTRY3));
    assertEquals(test.findEntry(CurveName.of("Test2")), Optional.empty());
    assertEquals(test.findEntry(CurveName.of("Rubbish")), Optional.empty());
  }

  public void test_missingEntries() {
    assertThrowsIllegalArg(() -> CurveGroupDefinition.of(
        CurveGroupName.of("group"),
        ImmutableList.of(ENTRY1),
        ImmutableList.of(CURVE_DEFN1, CURVE_DEFN2)),
        "An entry must be provided .* \\[Test2\\]");
  }

  //-------------------------------------------------------------------------
  public void test_filtered() {
    DummyFraCurveNode node1 = DummyFraCurveNode.of(Period.ofDays(5), GBP_LIBOR_1M, GBP_LIBOR_1M_ID);
    DummyFraCurveNode node2 = DummyFraCurveNode.of(Period.ofDays(10), GBP_LIBOR_1M, GBP_LIBOR_1M_ID);
    DummyFraCurveNode node3 = DummyFraCurveNode.of(Period.ofDays(11), GBP_LIBOR_1M, GBP_LIBOR_1M_ID, DROP_THIS_2D);
    ImmutableList<DummyFraCurveNode> nodes = ImmutableList.of(node1, node2, node3);
    LocalDate valuationDate = date(2015, 6, 30);

    InterpolatedNodalCurveDefinition curveDefn = InterpolatedNodalCurveDefinition.builder()
        .name(CURVE_NAME1)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.ZERO_RATE)
        .dayCount(ACT_365F)
        .nodes(nodes)
        .interpolator(CurveInterpolators.LINEAR)
        .extrapolatorLeft(CurveExtrapolators.FLAT)
        .extrapolatorRight(CurveExtrapolators.FLAT)
        .build();
    CurveGroupDefinition test = CurveGroupDefinition.builder()
        .name(CurveGroupName.of("Test"))
        .addCurve(curveDefn, GBP, GBP_LIBOR_1M, GBP_LIBOR_3M)
        .build();
    CurveGroupDefinition expected = CurveGroupDefinition.builder()
        .name(CurveGroupName.of("Test"))
        .addCurve(curveDefn.filtered(valuationDate, REF_DATA), GBP, GBP_LIBOR_1M, GBP_LIBOR_3M)
        .build();

    assertEquals(test.filtered(valuationDate, REF_DATA), expected);
  }

  //-------------------------------------------------------------------------
  public void test_metadata() {
    CurveGroupDefinition test = CurveGroupDefinition.builder()
        .name(CurveGroupName.of("Test"))
        .addCurve(CURVE_DEFN1, GBP, GBP_LIBOR_1M, GBP_LIBOR_3M)
        .build();

    LocalDate valuationDate = date(2015, 6, 30);
    CurveMetadata meta = CURVE_DEFN1.metadata(valuationDate, REF_DATA);
    assertEquals(test.metadata(valuationDate, REF_DATA), ImmutableList.of(meta));
  }

  //-------------------------------------------------------------------------
  public void test_tradesInitialGuesses() {
    CurveGroupDefinition test = CurveGroupDefinition.builder()
        .name(CurveGroupName.of("Test"))
        .addCurve(CURVE_DEFN1, GBP, GBP_LIBOR_1M, GBP_LIBOR_3M)
        .build();

    MarketData marketData = ImmutableMarketData.of(
        date(2015, 6, 30), ImmutableMap.of(GBP_LIBOR_1M_ID, 0.5d, GBP_LIBOR_3M_ID, 1.5d));
    Trade trade1 = NODE1.trade(1d, marketData, REF_DATA);
    Trade trade2 = NODE2.trade(1d, marketData, REF_DATA);
    assertEquals(test.getTotalParameterCount(), 2);
    assertEquals(test.resolvedTrades(marketData, REF_DATA), ImmutableList.of(trade1, trade2));
    assertEquals(test.initialGuesses(marketData), ImmutableList.of(0.5d, 1.5d));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    CurveGroupDefinition test = CurveGroupDefinition.builder()
        .name(CurveGroupName.of("Test"))
        .addDiscountCurve(CURVE_DEFN1, GBP)
        .build();
    coverImmutableBean(test);
    CurveGroupDefinition test2 = CurveGroupDefinition.builder()
        .name(CurveGroupName.of("Test2"))
        .addForwardCurve(CURVE_DEFN2, GBP_LIBOR_1M)
        .build();
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    CurveGroupDefinition test = CurveGroupDefinition.builder()
        .name(CurveGroupName.of("Test"))
        .addDiscountCurve(CURVE_DEFN1, GBP)
        .build();
    assertSerialization(test);
  }

  public void test_withName() {
    CurveGroupDefinition test = CurveGroupDefinition.builder()
        .name(CurveGroupName.of("Test"))
        .addDiscountCurve(CURVE_DEFN1, GBP)
        .build();
    CurveGroupDefinition expected = CurveGroupDefinition.builder()
        .name(CurveGroupName.of("NewName"))
        .addDiscountCurve(CURVE_DEFN1, GBP)
        .build();
    CurveGroupDefinition withNewName = test.withName(CurveGroupName.of("NewName"));
    assertEquals(withNewName, expected);
  }
}
