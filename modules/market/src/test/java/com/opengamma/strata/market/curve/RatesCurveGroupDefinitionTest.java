/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.DayCounts.ACT_364;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.index.FloatingRateNames.GBP_LIBOR;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_1M;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_1W;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_6M;
import static com.opengamma.strata.basics.index.OvernightIndices.GBP_SONIA;
import static com.opengamma.strata.basics.index.PriceIndices.GB_RPI;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.market.curve.CurveNodeClashAction.DROP_THIS;
import static com.opengamma.strata.product.swap.type.FixedInflationSwapConventions.GBP_FIXED_ZC_GB_RPI;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDate;
import java.time.Period;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

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
 * Test {@link RatesCurveGroupDefinition}.
 */
public class RatesCurveGroupDefinitionTest {

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
  private static final InterpolatedNodalCurveDefinition CURVE_DEFN1B = CURVE_DEFN1.toBuilder()
      .dayCount(ACT_364)
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
  private static final RatesCurveGroupEntry ENTRY1 = RatesCurveGroupEntry.builder()
      .curveName(CURVE_NAME1)
      .discountCurrencies(GBP)
      .indices(GBP_LIBOR_1W, GBP_SONIA)
      .build();
  private static final RatesCurveGroupEntry ENTRY2 = RatesCurveGroupEntry.builder()
      .curveName(CURVE_NAME2)
      .indices(GBP_LIBOR_1M, GBP_LIBOR_3M)
      .build();
  private static final RatesCurveGroupEntry ENTRY3 = RatesCurveGroupEntry.builder()
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

  @Test
  public void test_builder1() {
    RatesCurveGroupDefinition test = RatesCurveGroupDefinition.builder()
        .name(CurveGroupName.of("Test"))
        .addDiscountCurve(CURVE_DEFN1, GBP)
        .addForwardCurve(CURVE_DEFN1, GBP_SONIA)
        .addForwardCurve(CURVE_DEFN1, GBP_LIBOR_1W)
        .addForwardCurve(CURVE_DEFN2, GBP_LIBOR_1M, GBP_LIBOR_3M)
        .build();
    assertThat(test.getName()).isEqualTo(CurveGroupName.of("Test"));
    assertThat(test.getEntries()).containsExactly(ENTRY1, ENTRY2);
    assertThat(test.findDiscountCurveName(GBP)).isEqualTo(Optional.of(CURVE_NAME1));
    assertThat(test.findDiscountCurveName(USD)).isEqualTo(Optional.empty());
    assertThat(test.findForwardCurveName(GBP_LIBOR_1W)).isEqualTo(Optional.of(CURVE_NAME1));
    assertThat(test.findForwardCurveName(GBP_LIBOR_1M)).isEqualTo(Optional.of(CURVE_NAME2));
    assertThat(test.findForwardCurveName(GBP_LIBOR_6M)).isEqualTo(Optional.empty());
    assertThat(test.findForwardCurveNames(GBP_LIBOR)).containsOnly(CURVE_NAME1, CURVE_NAME2);
    assertThat(test.findEntry(CurveName.of("Test"))).isEqualTo(Optional.of(ENTRY1));
    assertThat(test.findEntry(CurveName.of("Test2"))).isEqualTo(Optional.of(ENTRY2));
    assertThat(test.findEntry(CurveName.of("Rubbish"))).isEqualTo(Optional.empty());
    assertThat(test.findCurveDefinition(CurveName.of("Test"))).isEqualTo(Optional.of(CURVE_DEFN1));
    assertThat(test.findCurveDefinition(CurveName.of("Test2"))).isEqualTo(Optional.of(CURVE_DEFN2));
    assertThat(test.findCurveDefinition(CurveName.of("Rubbish"))).isEqualTo(Optional.empty());
  }

  @Test
  public void test_builder2() {
    RatesCurveGroupDefinition test = RatesCurveGroupDefinition.builder()
        .name(CurveGroupName.of("Test"))
        .addCurve(CURVE_DEFN1, GBP, GBP_LIBOR_1M, GBP_LIBOR_3M)
        .build();
    assertThat(test.getName()).isEqualTo(CurveGroupName.of("Test"));
    assertThat(test.getEntries()).containsExactly(ENTRY3);
    assertThat(test.findEntry(CurveName.of("Test"))).isEqualTo(Optional.of(ENTRY3));
    assertThat(test.findEntry(CurveName.of("Test2"))).isEqualTo(Optional.empty());
    assertThat(test.findEntry(CurveName.of("Rubbish"))).isEqualTo(Optional.empty());
    assertThat(test.findCurveDefinition(CurveName.of("Test"))).isEqualTo(Optional.of(CURVE_DEFN1));
    assertThat(test.findCurveDefinition(CurveName.of("Test2"))).isEqualTo(Optional.empty());
    assertThat(test.findCurveDefinition(CurveName.of("Rubbish"))).isEqualTo(Optional.empty());
  }

  @Test
  public void test_builder_seasonality() {
    RatesCurveGroupDefinition test = RatesCurveGroupDefinition.builder()
        .name(CurveGroupName.of("Test"))
        .addCurve(CURVE_DEFN1, GBP, GBP_LIBOR_1M, GBP_LIBOR_3M)
        .addSeasonality(CURVE_NAME_I, SEASONALITY_ADDITIVE_DEF)
        .build();
    assertThat(test.getName()).isEqualTo(CurveGroupName.of("Test"));
    assertThat(test.getEntries()).containsExactly(ENTRY3);
    assertThat(test.findEntry(CurveName.of("Test"))).isEqualTo(Optional.of(ENTRY3));
    assertThat(test.findEntry(CurveName.of("Test2"))).isEqualTo(Optional.empty());
    assertThat(test.findEntry(CurveName.of("Rubbish"))).isEqualTo(Optional.empty());
    assertThat(test.findCurveDefinition(CurveName.of("Test"))).isEqualTo(Optional.of(CURVE_DEFN1));
    assertThat(test.findCurveDefinition(CurveName.of("Test2"))).isEqualTo(Optional.empty());
    assertThat(test.findCurveDefinition(CurveName.of("Rubbish"))).isEqualTo(Optional.empty());
    ImmutableMap<CurveName, SeasonalityDefinition> seasonMap = test.getSeasonalityDefinitions();
    assertThat(seasonMap.size() == 1).isTrue();
    assertThat(seasonMap.get(CURVE_NAME_I)).isEqualTo(SEASONALITY_ADDITIVE_DEF);
  }

  @Test
  public void test_builder3() {
    RatesCurveGroupDefinition test = RatesCurveGroupDefinition.builder()
        .name(CurveGroupName.of("Test"))
        .addDiscountCurve(CURVE_NAME1, GBP)
        .addForwardCurve(CURVE_NAME1, GBP_SONIA)
        .addForwardCurve(CURVE_NAME1, GBP_LIBOR_1W)
        .addForwardCurve(CURVE_NAME2, GBP_LIBOR_1M, GBP_LIBOR_3M)
        .build();
    assertThat(test.getName()).isEqualTo(CurveGroupName.of("Test"));
    assertThat(test.getEntries()).containsExactly(ENTRY1, ENTRY2);
    assertThat(test.findEntry(CurveName.of("Test"))).isEqualTo(Optional.of(ENTRY1));
    assertThat(test.findEntry(CurveName.of("Test2"))).isEqualTo(Optional.of(ENTRY2));
    assertThat(test.findEntry(CurveName.of("Rubbish"))).isEqualTo(Optional.empty());
  }

  @Test
  public void test_builder4() {
    RatesCurveGroupDefinition test = RatesCurveGroupDefinition.builder()
        .name(CurveGroupName.of("Test"))
        .addCurve(CURVE_NAME1, GBP, GBP_LIBOR_1M, GBP_LIBOR_3M)
        .build();
    assertThat(test.getName()).isEqualTo(CurveGroupName.of("Test"));
    assertThat(test.getEntries()).containsExactly(ENTRY3);
    assertThat(test.findEntry(CurveName.of("Test"))).isEqualTo(Optional.of(ENTRY3));
    assertThat(test.findEntry(CurveName.of("Test2"))).isEqualTo(Optional.empty());
    assertThat(test.findEntry(CurveName.of("Rubbish"))).isEqualTo(Optional.empty());
  }

  @Test
  public void test_missingEntries() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> RatesCurveGroupDefinition.of(
            CurveGroupName.of("group"),
            ImmutableList.of(ENTRY1),
            ImmutableList.of(CURVE_DEFN1, CURVE_DEFN2)))
        .withMessageMatching("An entry must be provided .* \\[Test2\\]");
  }

  //-------------------------------------------------------------------------
  @Test
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
    RatesCurveGroupDefinition test = RatesCurveGroupDefinition.builder()
        .name(CurveGroupName.of("Test"))
        .addCurve(curveDefn, GBP, GBP_LIBOR_1M, GBP_LIBOR_3M)
        .build();
    RatesCurveGroupDefinition expected = RatesCurveGroupDefinition.builder()
        .name(CurveGroupName.of("Test"))
        .addCurve(curveDefn.filtered(valuationDate, REF_DATA), GBP, GBP_LIBOR_1M, GBP_LIBOR_3M)
        .build();

    assertThat(test.filtered(valuationDate, REF_DATA)).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_metadata() {
    RatesCurveGroupDefinition test = RatesCurveGroupDefinition.builder()
        .name(CurveGroupName.of("Test"))
        .addCurve(CURVE_DEFN1, GBP, GBP_LIBOR_1M, GBP_LIBOR_3M)
        .build();

    LocalDate valuationDate = date(2015, 6, 30);
    CurveMetadata meta = CURVE_DEFN1.metadata(valuationDate, REF_DATA);
    assertThat(test.metadata(valuationDate, REF_DATA)).containsExactly(meta);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_tradesInitialGuesses() {
    RatesCurveGroupDefinition test = RatesCurveGroupDefinition.builder()
        .name(CurveGroupName.of("Test"))
        .addCurve(CURVE_DEFN1, GBP, GBP_LIBOR_1M, GBP_LIBOR_3M)
        .build();

    MarketData marketData = ImmutableMarketData.of(
        date(2015, 6, 30), ImmutableMap.of(GBP_LIBOR_1M_ID, 0.5d, GBP_LIBOR_3M_ID, 1.5d));
    Trade trade1 = NODE1.trade(1d, marketData, REF_DATA);
    Trade trade2 = NODE2.trade(1d, marketData, REF_DATA);
    assertThat(test.getTotalParameterCount()).isEqualTo(2);
    assertThat(test.resolvedTrades(marketData, REF_DATA)).isEqualTo(ImmutableList.of(trade1, trade2));
    assertThat(test.initialGuesses(marketData)).containsExactly(0.5d, 1.5d);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_bind() {
    RatesCurveGroupDefinition test = RatesCurveGroupDefinition.builder()
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
    RatesCurveGroupDefinition testBound = test.bindTimeSeries(valuationDate, map);
    List<CurveDefinition> list = testBound.getCurveDefinitions();
    assertThat(list).hasSize(2);
    assertThat(list.get(0) instanceof InterpolatedNodalCurveDefinition).isTrue();
    assertThat(list.get(1) instanceof InflationNodalCurveDefinition).isTrue();
    InflationNodalCurveDefinition seasonDef = (InflationNodalCurveDefinition) list.get(1);
    assertThat(seasonDef.getCurveWithoutFixingDefinition()).isEqualTo(CURVE_DEFN_I);
    assertThat(seasonDef.getLastFixingMonth()).isEqualTo(YearMonth.from(lastFixingDate));
    assertThat(seasonDef.getLastFixingValue()).isEqualTo(lastFixingValue);
    assertThat(seasonDef.getName()).isEqualTo(CURVE_NAME_I);
    assertThat(seasonDef.getSeasonalityDefinition()).isEqualTo(SEASONALITY_ADDITIVE_DEF);
    assertThat(seasonDef.getYValueType()).isEqualTo(ValueType.PRICE_INDEX);
  }

  @Test
  public void test_bind_after_last_fixing() {
    RatesCurveGroupDefinition test = RatesCurveGroupDefinition.builder()
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
    RatesCurveGroupDefinition testBound = test.bindTimeSeries(valuationDate, map);
    List<CurveDefinition> list = testBound.getCurveDefinitions();
    assertThat(list).hasSize(2);
    assertThat(list.get(0) instanceof InterpolatedNodalCurveDefinition).isTrue();
    assertThat(list.get(1) instanceof InflationNodalCurveDefinition).isTrue();
    InflationNodalCurveDefinition seasonDef = (InflationNodalCurveDefinition) list.get(1);
    assertThat(seasonDef.getCurveWithoutFixingDefinition()).isEqualTo(CURVE_DEFN_I);
    assertThat(seasonDef.getLastFixingMonth()).isEqualTo(YearMonth.from(otherFixingDate));
    assertThat(seasonDef.getLastFixingValue()).isEqualTo(lastFixingValue - 1.0);
    assertThat(seasonDef.getName()).isEqualTo(CURVE_NAME_I);
    assertThat(seasonDef.getSeasonalityDefinition()).isEqualTo(SEASONALITY_ADDITIVE_DEF);
    assertThat(seasonDef.getYValueType()).isEqualTo(ValueType.PRICE_INDEX);
  }

  @Test
  public void test_bind_no_seasonality() {
    RatesCurveGroupDefinition test = RatesCurveGroupDefinition.builder()
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
    RatesCurveGroupDefinition testBound = test.bindTimeSeries(valuationDate, map);
    List<CurveDefinition> list = testBound.getCurveDefinitions();
    assertThat(list).hasSize(2);
    assertThat(list.get(0) instanceof InterpolatedNodalCurveDefinition).isTrue();
    assertThat(list.get(1) instanceof InflationNodalCurveDefinition).isTrue();
    InflationNodalCurveDefinition seasonDef = (InflationNodalCurveDefinition) list.get(1);
    assertThat(seasonDef.getCurveWithoutFixingDefinition()).isEqualTo(CURVE_DEFN_I);
    assertThat(seasonDef.getLastFixingMonth()).isEqualTo(YearMonth.from(lastFixingDate));
    assertThat(seasonDef.getLastFixingValue()).isEqualTo(lastFixingValue);
    assertThat(seasonDef.getName()).isEqualTo(CURVE_NAME_I);
    assertThat(seasonDef.getYValueType()).isEqualTo(ValueType.PRICE_INDEX);
    // Check the default
    assertThat(seasonDef.getSeasonalityDefinition().getSeasonalityMonthOnMonth()
        .equalWithTolerance(DoubleArray.filled(12, 1d), 1.0E-10)).isTrue();
    assertThat(seasonDef.getSeasonalityDefinition().getAdjustmentType()).isEqualTo(ShiftType.SCALED);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_combinedWith_sameCurveNames() {
    RatesCurveGroupDefinition base1 = RatesCurveGroupDefinition.builder()
        .name(CurveGroupName.of("Test"))
        .addCurve(CURVE_DEFN1, GBP, GBP_LIBOR_1M, GBP_LIBOR_3M)
        .addForwardCurve(CURVE_DEFN_I, GB_RPI)
        .build();
    RatesCurveGroupDefinition base2 = RatesCurveGroupDefinition.builder()
        .name(CurveGroupName.of("TestX"))
        .addCurve(CURVE_DEFN1, GBP, GBP_LIBOR_6M)
        .build();
    RatesCurveGroupDefinition expected = RatesCurveGroupDefinition.builder()
        .name(CurveGroupName.of("Test"))
        .addCurve(CURVE_DEFN1, GBP, GBP_LIBOR_1M, GBP_LIBOR_3M, GBP_LIBOR_6M)
        .addForwardCurve(CURVE_DEFN_I, GB_RPI)
        .build();
    assertThat(base1.combinedWith(base2)).isEqualTo(expected);
  }

  @Test
  public void test_combinedWith_differentCurveNames() {
    RatesCurveGroupDefinition base1 = RatesCurveGroupDefinition.builder()
        .name(CurveGroupName.of("Test"))
        .addCurve(CURVE_DEFN1, GBP, GBP_LIBOR_1M, GBP_LIBOR_3M)
        .addForwardCurve(CURVE_DEFN_I, GB_RPI)
        .build();
    RatesCurveGroupDefinition base2 = RatesCurveGroupDefinition.builder()
        .name(CurveGroupName.of("TestX"))
        .addForwardCurve(CURVE_DEFN2, GBP_LIBOR_6M)
        .build();
    RatesCurveGroupDefinition expected = RatesCurveGroupDefinition.builder()
        .name(CurveGroupName.of("Test"))
        .addCurve(CURVE_DEFN1, GBP, GBP_LIBOR_1M, GBP_LIBOR_3M)
        .addForwardCurve(CURVE_DEFN_I, GB_RPI)
        .addForwardCurve(CURVE_DEFN2, GBP_LIBOR_6M)
        .build();
    assertThat(base1.combinedWith(base2)).isEqualTo(expected);
  }

  @Test
  public void test_combinedWith_sameCurveNamesClash() {
    RatesCurveGroupDefinition base1 = RatesCurveGroupDefinition.builder()
        .name(CurveGroupName.of("Test"))
        .addCurve(CURVE_DEFN1, GBP, GBP_LIBOR_1M, GBP_LIBOR_3M)
        .addForwardCurve(CURVE_DEFN_I, GB_RPI)
        .build();
    RatesCurveGroupDefinition base2 = RatesCurveGroupDefinition.builder()
        .name(CurveGroupName.of("TestX"))
        .addCurve(CURVE_DEFN1B, GBP, GBP_LIBOR_6M)
        .build();
    assertThatIllegalArgumentException()
        .isThrownBy(() -> base1.combinedWith(base2));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    RatesCurveGroupDefinition test = RatesCurveGroupDefinition.builder()
        .name(CurveGroupName.of("Test"))
        .addDiscountCurve(CURVE_DEFN1, GBP)
        .build();
    coverImmutableBean(test);
    RatesCurveGroupDefinition test2 = RatesCurveGroupDefinition.builder()
        .name(CurveGroupName.of("Test2"))
        .addForwardCurve(CURVE_DEFN2, GBP_LIBOR_1M)
        .build();
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    RatesCurveGroupDefinition test = RatesCurveGroupDefinition.builder()
        .name(CurveGroupName.of("Test"))
        .addDiscountCurve(CURVE_DEFN1, GBP)
        .build();
    assertSerialization(test);
  }

  @Test
  public void test_withName() {
    RatesCurveGroupDefinition test = RatesCurveGroupDefinition.builder()
        .name(CurveGroupName.of("Test"))
        .addDiscountCurve(CURVE_DEFN1, GBP)
        .build();
    RatesCurveGroupDefinition expected = RatesCurveGroupDefinition.builder()
        .name(CurveGroupName.of("NewName"))
        .addDiscountCurve(CURVE_DEFN1, GBP)
        .build();
    RatesCurveGroupDefinition withNewName = test.withName(CurveGroupName.of("NewName"));
    assertThat(withNewName).isEqualTo(expected);
  }
}
