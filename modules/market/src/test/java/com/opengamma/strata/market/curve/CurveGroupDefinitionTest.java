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
import static com.opengamma.strata.basics.index.PriceIndices.GB_RPI;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.market.curve.CurveNodeClashAction.DROP_THIS;
import static com.opengamma.strata.product.swap.type.FixedInflationSwapConventions.GBP_FIXED_ZC_GB_RPI;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;
import java.time.Period;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.data.ImmutableMarketData;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.data.ObservableId;
import com.opengamma.strata.market.ShiftType;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.interpolator.CurveExtrapolators;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolators;
import com.opengamma.strata.market.curve.node.FixedInflationSwapCurveNode;
import com.opengamma.strata.market.observable.QuoteId;
import com.opengamma.strata.product.Trade;
import com.opengamma.strata.product.swap.type.FixedInflationSwapTemplate;

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
  private static final FixedInflationSwapCurveNode NODE_I1 =
      FixedInflationSwapCurveNode.of(FixedInflationSwapTemplate.of(Tenor.TENOR_5Y, GBP_FIXED_ZC_GB_RPI), GBP_LIBOR_1M_ID);
  private static final FixedInflationSwapCurveNode NODE_I2 =
      FixedInflationSwapCurveNode.of(FixedInflationSwapTemplate.of(Tenor.TENOR_10Y, GBP_FIXED_ZC_GB_RPI), GBP_LIBOR_1M_ID);
  private static final CurveNodeDateOrder DROP_THIS_2D = CurveNodeDateOrder.of(2, DROP_THIS);
  private static final CurveName CURVE_NAME1 = CurveName.of("Test");
  private static final CurveName CURVE_NAME2 = CurveName.of("Test2");
  private static final CurveName CURVE_NAME_I = CurveName.of("Test-CPI");
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
  private static final InterpolatedNodalCurveDefinition CURVE_DEFN_I = InterpolatedNodalCurveDefinition.builder()
      .name(CURVE_NAME_I)
      .xValueType(ValueType.YEAR_FRACTION)
      .yValueType(ValueType.PRICE_INDEX)
      .nodes(ImmutableList.of(NODE_I1, NODE_I2))
      .interpolator(CurveInterpolators.LOG_LINEAR)
      .extrapolatorLeft(CurveExtrapolators.FLAT)
      .extrapolatorRight(CurveExtrapolators.FLAT)
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
  private static final DoubleArray SEASONALITY_ADDITIVE = DoubleArray.of(
      1.0, 1.5, 1.0, -0.5,
      -0.5, -1.0, -1.5, 0.0,
      0.5, 1.0, 1.0, -2.5);
  private static final SeasonalityDefinition SEASONALITY_ADDITIVE_DEF =
      SeasonalityDefinition.of(SEASONALITY_ADDITIVE, ShiftType.SCALED);

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

  public void test_builder_seasonality() {
    CurveGroupDefinition test = CurveGroupDefinition.builder()
        .name(CurveGroupName.of("Test"))
        .addCurve(CURVE_DEFN1, GBP, GBP_LIBOR_1M, GBP_LIBOR_3M)
        .addSeasonality(CURVE_NAME_I, SEASONALITY_ADDITIVE_DEF)
        .build();
    assertEquals(test.getName(), CurveGroupName.of("Test"));
    assertEquals(test.getEntries(), ImmutableList.of(ENTRY3));
    assertEquals(test.findEntry(CurveName.of("Test")), Optional.of(ENTRY3));
    assertEquals(test.findEntry(CurveName.of("Test2")), Optional.empty());
    assertEquals(test.findEntry(CurveName.of("Rubbish")), Optional.empty());
    assertEquals(test.findCurveDefinition(CurveName.of("Test")), Optional.of(CURVE_DEFN1));
    assertEquals(test.findCurveDefinition(CurveName.of("Test2")), Optional.empty());
    assertEquals(test.findCurveDefinition(CurveName.of("Rubbish")), Optional.empty());
    ImmutableMap<CurveName, SeasonalityDefinition> seasonMap = test.getSeasonalityDefinitions();
    assertTrue(seasonMap.size() == 1);
    assertEquals(seasonMap.get(CURVE_NAME_I), SEASONALITY_ADDITIVE_DEF);
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
  public void test_bind() {
    CurveGroupDefinition test = CurveGroupDefinition.builder()
        .name(CurveGroupName.of("Test"))
        .addCurve(CURVE_DEFN1, GBP, GBP_LIBOR_1M, GBP_LIBOR_3M)
        .addForwardCurve(CURVE_DEFN_I, GB_RPI)
        .addSeasonality(CURVE_NAME_I, SEASONALITY_ADDITIVE_DEF)
        .build();
    LocalDate valuationDate = LocalDate.of(2015, 11, 10);
    LocalDate lastFixingDate = LocalDate.of(2015, 10, 31);
    LocalDate otherFixingDate = LocalDate.of(2015, 9, 30);
    double lastFixingValue = 234.56;
    Map<Index, LocalDateDoubleTimeSeries> map = ImmutableMap.of(GB_RPI,
        LocalDateDoubleTimeSeries.builder()
            .put(lastFixingDate, 234.56).put(otherFixingDate, lastFixingValue - 1).build());
    CurveGroupDefinition testBound = test.bindTimeSeries(valuationDate, map);
    List<NodalCurveDefinition> list = testBound.getCurveDefinitions();
    assertEquals(list.size(), 2);
    assertTrue(list.get(0) instanceof InterpolatedNodalCurveDefinition);
    assertTrue(list.get(1) instanceof InflationNodalCurveDefinition);
    InflationNodalCurveDefinition seasonDef = (InflationNodalCurveDefinition) list.get(1);
    assertEquals(seasonDef.getCurveWithoutFixingDefinition(), CURVE_DEFN_I);
    assertEquals(seasonDef.getLastFixingMonth(), YearMonth.from(lastFixingDate));
    assertEquals(seasonDef.getLastFixingValue(), lastFixingValue);
    assertEquals(seasonDef.getName(), CURVE_NAME_I);
    assertEquals(seasonDef.getSeasonalityDefinition(), SEASONALITY_ADDITIVE_DEF);
    assertEquals(seasonDef.getYValueType(), ValueType.PRICE_INDEX);
  }

  public void test_bind_after_last_fixing() {
    CurveGroupDefinition test = CurveGroupDefinition.builder()
        .name(CurveGroupName.of("Test"))
        .addCurve(CURVE_DEFN1, GBP, GBP_LIBOR_1M, GBP_LIBOR_3M)
        .addForwardCurve(CURVE_DEFN_I, GB_RPI)
        .addSeasonality(CURVE_NAME_I, SEASONALITY_ADDITIVE_DEF)
        .build();
    LocalDate valuationDate = LocalDate.of(2015, 10, 15);
    LocalDate lastFixingDate = LocalDate.of(2015, 10, 31);
    LocalDate otherFixingDate = LocalDate.of(2015, 9, 30);
    LocalDate other2FixingDate = LocalDate.of(2015, 8, 31);
    double lastFixingValue = 234.56;
    Map<Index, LocalDateDoubleTimeSeries> map = ImmutableMap.of(GB_RPI,
        LocalDateDoubleTimeSeries.builder()
            .put(lastFixingDate, lastFixingValue).put(otherFixingDate, lastFixingValue - 1.0)
            .put(other2FixingDate, lastFixingValue - 2.0).build());
    CurveGroupDefinition testBound = test.bindTimeSeries(valuationDate, map);
    List<NodalCurveDefinition> list = testBound.getCurveDefinitions();
    assertEquals(list.size(), 2);
    assertTrue(list.get(0) instanceof InterpolatedNodalCurveDefinition);
    assertTrue(list.get(1) instanceof InflationNodalCurveDefinition);
    InflationNodalCurveDefinition seasonDef = (InflationNodalCurveDefinition) list.get(1);
    assertEquals(seasonDef.getCurveWithoutFixingDefinition(), CURVE_DEFN_I);
    assertEquals(seasonDef.getLastFixingMonth(), YearMonth.from(otherFixingDate));
    assertEquals(seasonDef.getLastFixingValue(), lastFixingValue - 1.0);
    assertEquals(seasonDef.getName(), CURVE_NAME_I);
    assertEquals(seasonDef.getSeasonalityDefinition(), SEASONALITY_ADDITIVE_DEF);
    assertEquals(seasonDef.getYValueType(), ValueType.PRICE_INDEX);
  }

  public void test_bind_no_seasonality() {
    CurveGroupDefinition test = CurveGroupDefinition.builder()
        .name(CurveGroupName.of("Test"))
        .addCurve(CURVE_DEFN1, GBP, GBP_LIBOR_1M, GBP_LIBOR_3M)
        .addForwardCurve(CURVE_DEFN_I, GB_RPI)
        .build();
    LocalDate valuationDate = LocalDate.of(2015, 11, 10);
    LocalDate lastFixingDate = LocalDate.of(2015, 10, 31);
    LocalDate otherFixingDate = LocalDate.of(2015, 9, 30);
    double lastFixingValue = 234.56;
    Map<Index, LocalDateDoubleTimeSeries> map = ImmutableMap.of(GB_RPI,
        LocalDateDoubleTimeSeries.builder()
            .put(lastFixingDate, 234.56).put(otherFixingDate, lastFixingValue - 1).build());
    CurveGroupDefinition testBound = test.bindTimeSeries(valuationDate, map);
    List<NodalCurveDefinition> list = testBound.getCurveDefinitions();
    assertEquals(list.size(), 2);
    assertTrue(list.get(0) instanceof InterpolatedNodalCurveDefinition);
    assertTrue(list.get(1) instanceof InflationNodalCurveDefinition);
    InflationNodalCurveDefinition seasonDef = (InflationNodalCurveDefinition) list.get(1);
    assertEquals(seasonDef.getCurveWithoutFixingDefinition(), CURVE_DEFN_I);
    assertEquals(seasonDef.getLastFixingMonth(), YearMonth.from(lastFixingDate));
    assertEquals(seasonDef.getLastFixingValue(), lastFixingValue);
    assertEquals(seasonDef.getName(), CURVE_NAME_I);
    assertEquals(seasonDef.getYValueType(), ValueType.PRICE_INDEX);
    // Check the default
    assertTrue(seasonDef.getSeasonalityDefinition().getSeasonalityMonthOnMonth()
        .equalWithTolerance(DoubleArray.filled(12, 1d), 1.0E-10));
    assertEquals(seasonDef.getSeasonalityDefinition().getAdjustmentType(), ShiftType.SCALED);
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
