/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.curve;

import static com.opengamma.strata.basics.currency.Currency.BRL;
import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.BRBD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.basics.index.ImmutableOvernightIndex;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.index.OvernightIndex;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.data.ImmutableMarketData;
import com.opengamma.strata.data.ImmutableMarketDataBuilder;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.data.MarketDataId;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.CurveNode;
import com.opengamma.strata.market.curve.DefaultCurveMetadata;
import com.opengamma.strata.market.curve.InterpolatedNodalCurveDefinition;
import com.opengamma.strata.market.curve.RatesCurveGroupDefinition;
import com.opengamma.strata.market.curve.interpolator.CurveExtrapolator;
import com.opengamma.strata.market.curve.interpolator.CurveExtrapolators;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolator;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolators;
import com.opengamma.strata.market.curve.node.FixedOvernightSwapCurveNode;
import com.opengamma.strata.market.observable.QuoteId;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.param.CurrencyParameterSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.sensitivity.MarketQuoteSensitivityCalculator;
import com.opengamma.strata.pricer.swap.DiscountingSwapProductPricer;
import com.opengamma.strata.product.ResolvedTrade;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.swap.FixedAccrualMethod;
import com.opengamma.strata.product.swap.OvernightAccrualMethod;
import com.opengamma.strata.product.swap.ResolvedSwap;
import com.opengamma.strata.product.swap.ResolvedSwapTrade;
import com.opengamma.strata.product.swap.SwapTrade;
import com.opengamma.strata.product.swap.type.FixedOvernightSwapConvention;
import com.opengamma.strata.product.swap.type.FixedOvernightSwapTemplate;
import com.opengamma.strata.product.swap.type.FixedRateSwapLegConvention;
import com.opengamma.strata.product.swap.type.ImmutableFixedOvernightSwapConvention;
import com.opengamma.strata.product.swap.type.OvernightRateSwapLegConvention;

/**
 * Test for curve calibration with 1 curves in BRL.
 */
public class CalibrationDiscountingBrlTest {
  
  private static final LocalDate VAL_DATE = LocalDate.of(2015, 7, 21);
  
  private static final CurveInterpolator INTERPOLATOR_LINEAR = CurveInterpolators.LINEAR;
  private static final CurveExtrapolator EXTRAPOLATOR_FLAT = CurveExtrapolators.FLAT;
  private static final DayCount CURVE_DC = DayCount.ofBus252(BRBD);
  private static final BusinessDayAdjustment BDA_MF = BusinessDayAdjustment.of(MODIFIED_FOLLOWING, BRBD);
  
  // reference data
  private static final ReferenceData REF_DATA = ReferenceData.standard();
  
  private static final String SCHEME = "CALIBRATION";
  
  private static final OvernightIndex BRL_CDI =
      ImmutableOvernightIndex.builder()
          .currency(BRL)
          .dayCount(CURVE_DC)
          .effectiveDateOffset(0)
          .fixingCalendar(BRBD)
          .name("BRL_CDI").build();
  
  /** Curve name */
  private static final String ALL_NAME = "BRL-DSCON";
  private static final CurveName ALL_CURVE_NAME = CurveName.of(ALL_NAME);
  /** Curves associations to currencies and indices. */
  private static final Map<CurveName, Currency> DSC_NAMES = new HashMap<>();
  private static final Map<CurveName, Set<Index>> IDX_NAMES = new HashMap<>();
  private static final Set<Index> IBOR_INDICES = new HashSet<>();
  static {
    DSC_NAMES.put(ALL_CURVE_NAME, BRL);
    IDX_NAMES.put(ALL_CURVE_NAME, IBOR_INDICES);
  }
  
  public static final OvernightRateSwapLegConvention BRL_FLOATING_CONVENTION =
      OvernightRateSwapLegConvention.builder()
          .index(BRL_CDI)
          .accrualFrequency(Frequency.TERM)
          .paymentFrequency(Frequency.TERM)
          .accrualMethod(OvernightAccrualMethod.OVERNIGHT_COMPOUNDED_ANNUAL_RATE)
          .accrualBusinessDayAdjustment(BDA_MF)
          .paymentDateOffset(DaysAdjustment.ofBusinessDays(0, BRL_CDI.getFixingCalendar()))
          .build();
  
  public static final FixedRateSwapLegConvention BRL_FIXED_CONV =
      FixedRateSwapLegConvention.builder()
          .currency(BRL)
          .dayCount(CURVE_DC)
          .accrualFrequency(Frequency.TERM)
          .paymentFrequency(Frequency.TERM)
          .accrualBusinessDayAdjustment(BDA_MF)
          .accrualMethod(FixedAccrualMethod.OVERNIGHT_COMPOUNDED_ANNUAL_RATE)
          .paymentDateOffset(DaysAdjustment.ofBusinessDays(0, BRL_CDI.getFixingCalendar()))
          .build();
  
  public static final FixedOvernightSwapConvention BRL_OIS_CONVENTION = ImmutableFixedOvernightSwapConvention.of(
      "BRL-FIXED-TERM-CDI-OIS",
      BRL_FIXED_CONV,
      BRL_FLOATING_CONVENTION,
      DaysAdjustment.ofBusinessDays(0, BRL_CDI.getFixingCalendar()));
  
  /** Market values for the BRL curve */
  private static final double[] OIS_MARKET_QUOTES = new double[] {
      0.1380, 0.1340, 0.1250, 0.1175, 0.1150, 0.1150, 0.1160};
  private static final int OIS_NB_NODES = OIS_MARKET_QUOTES.length;
  private static final String[] OIS_ID_VALUE = new String[] {
      "OIS-3M", "OIS-6M", "OIS-1Y", "OIS-2Y", "OIS-3Y", "OIS-4Y", "OIS-5Y"};
  
  /** Nodes for the BRL curve */
  private static final CurveNode[] ALL_NODES = new CurveNode[OIS_NB_NODES];
  
  /** Tenors for the BRL swaps */
  private static final Period[] OIS_TENORS = new Period[] {
      Period.ofMonths(3), Period.ofMonths(6), Period.ofYears(1), Period.ofYears(2), Period.ofYears(3),
      Period.ofYears(4), Period.ofYears(5)};
  private static final int DSC_NB_OIS_NODES = OIS_TENORS.length;
  static {
    for (int i = 0; i < DSC_NB_OIS_NODES; i++) {
      FixedOvernightSwapTemplate template = FixedOvernightSwapTemplate.of(Tenor.of(OIS_TENORS[i]), BRL_OIS_CONVENTION);
      ALL_NODES[i] = FixedOvernightSwapCurveNode.of(template, QuoteId.of(StandardId.of(SCHEME, OIS_ID_VALUE[i])));
    }
  }
  
  /** All quotes for the curve calibration */
  private static final ImmutableMarketData ALL_QUOTES;
  static {
    ImmutableMarketDataBuilder builder = ImmutableMarketData.builder(VAL_DATE);
    for (int i = 0; i < OIS_NB_NODES; i++) {
      builder.addValue(QuoteId.of(StandardId.of(SCHEME, OIS_ID_VALUE[i])), OIS_MARKET_QUOTES[i]);
    }
    ALL_QUOTES = builder.build();
  }
  
  /** All nodes by groups. */
  private static final List<List<CurveNode[]>> CURVES_NODES = new ArrayList<>();
  static {
    List<CurveNode[]> groupNodes = new ArrayList<>();
    groupNodes.add(ALL_NODES);
    CURVES_NODES.add(groupNodes);
  }
  
  /** All metadata by groups */
  private static final List<List<CurveMetadata>> CURVES_METADATA = new ArrayList<>();
  static {
    List<CurveMetadata> groupMetadata = new ArrayList<>();
    groupMetadata.add(DefaultCurveMetadata.builder()
        .curveName(ALL_CURVE_NAME)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.ZERO_RATE)
        .dayCount(CURVE_DC)
        .build());
    CURVES_METADATA.add(groupMetadata);
  }
  
  private static final DiscountingSwapProductPricer SWAP_PRICER = DiscountingSwapProductPricer.DEFAULT;
  private static final MarketQuoteSensitivityCalculator MQC = MarketQuoteSensitivityCalculator.DEFAULT;
  
  private static final RatesCurveCalibrator CALIBRATOR = RatesCurveCalibrator.of(1e-9, 1e-9, 100);
  
  /** Test with CurveGroupDefinition */
  private static final String CURVE_GROUP_NAME_STR = "BRL-SINGLE-CURVE";
  private static final CurveGroupName CURVE_GROUP_NAME = CurveGroupName.of(CURVE_GROUP_NAME_STR);
  private static final InterpolatedNodalCurveDefinition CURVE_DEFN =
      InterpolatedNodalCurveDefinition.builder()
          .name(ALL_CURVE_NAME)
          .xValueType(ValueType.YEAR_FRACTION)
          .yValueType(ValueType.ZERO_RATE)
          .dayCount(CURVE_DC)
          .interpolator(INTERPOLATOR_LINEAR)
          .extrapolatorLeft(EXTRAPOLATOR_FLAT)
          .extrapolatorRight(EXTRAPOLATOR_FLAT)
          .nodes(ALL_NODES).build();
  private static final RatesCurveGroupDefinition CURVE_GROUP_DEFN =
      RatesCurveGroupDefinition.builder()
          .name(CURVE_GROUP_NAME)
          .addCurve(CURVE_DEFN, BRL, BRL_CDI).build();
  
  // Constants
  private static final double TOLERANCE_PV = 1.0E-6;
  private static final double TOLERANCE_DELTA = 1.0E3;
  
  //-------------------------------------------------------------------------
  @Test
  public void calibration_present_value() {
    RatesProvider result2 =
        CALIBRATOR.calibrate(CURVE_GROUP_DEFN, ALL_QUOTES, REF_DATA);
    // Test PV
    CurveNode[] dscNodes = CURVES_NODES.get(0).get(0);
    List<ResolvedTrade> dscTrades = new ArrayList<>();
    for (int i = 0; i < dscNodes.length; i++) {
      dscTrades.add(dscNodes[i].resolvedTrade(1d, ALL_QUOTES, REF_DATA));
    }
    // OIS
    for (int i = 0; i < OIS_NB_NODES; i++) {
      MultiCurrencyAmount pvIrs = SWAP_PRICER.presentValue(
          ((ResolvedSwapTrade) dscTrades.get(i)).getProduct(), result2);
      assertThat(pvIrs.getAmount(BRL).getAmount()).isCloseTo(0.0, offset(TOLERANCE_PV));
    }
  }
  
  //-------------------------------------------------------------------------
  @Test
  public void calibration_transition_coherence_par_rate() {
    RatesProvider provider =
        CALIBRATOR.calibrate(CURVE_GROUP_DEFN, ALL_QUOTES, REF_DATA);
    ImmutableList<ResolvedTrade> dscTrades = CURVE_GROUP_DEFN.resolvedTrades(ALL_QUOTES, REF_DATA);
    
    for (int loopnode = 0; loopnode < dscTrades.size(); loopnode++) {
      PointSensitivities pts = SWAP_PRICER.parRateSensitivity(
          ((ResolvedSwapTrade) dscTrades.get(loopnode)).getProduct(), provider).build();
      CurrencyParameterSensitivities ps = provider.parameterSensitivity(pts);
      CurrencyParameterSensitivities mqs = MQC.sensitivity(ps, provider);
      assertThat(mqs.size()).isEqualTo(1);
      CurrencyParameterSensitivity mqsDsc = mqs.getSensitivity(ALL_CURVE_NAME, BRL);
      assertThat(mqsDsc.getMarketDataName().equals(ALL_CURVE_NAME)).isTrue();
      assertThat(mqsDsc.getCurrency().equals(BRL)).isTrue();
      DoubleArray mqsData = mqsDsc.getSensitivity();
      assertThat(mqsData.size()).isEqualTo(dscTrades.size());
      for (int i = 0; i < mqsData.size(); i++) {
        assertThat(mqsData.get(i)).isCloseTo((i == loopnode) ? 1.0 : 0.0, offset(TOLERANCE_DELTA));
      }
    }
  }
  
  //-------------------------------------------------------------------------
  @Test
  public void calibration_market_quote_sensitivity() {
    double shift = 1.0E-6;
    Function<MarketData, RatesProvider> f =
        marketData -> CALIBRATOR.calibrate(CURVE_GROUP_DEFN, marketData, REF_DATA);
    calibration_market_quote_sensitivity_check(f, shift);
  }
  
  //-------------------------------------------------------------------------
  private void calibration_market_quote_sensitivity_check(
      Function<MarketData, RatesProvider> calibrator,
      double shift) {

    double notional = 100_000_000.0;
    double rate = 0.1723;
    SwapTrade trade = BRL_OIS_CONVENTION.createTrade(
        VAL_DATE, Period.ofMonths(8), Tenor.TENOR_3Y, BuySell.BUY, notional, rate, REF_DATA);
    RatesProvider result = calibrator.apply(ALL_QUOTES);
    ResolvedSwap product = trade.getProduct().resolve(REF_DATA);
    PointSensitivityBuilder pts = SWAP_PRICER.presentValueSensitivity(product, result);
    CurrencyParameterSensitivities ps = result.parameterSensitivity(pts.build());
    CurrencyParameterSensitivities mqs = MQC.sensitivity(ps, result);
    double pv0 = SWAP_PRICER.presentValue(product, result).getAmount(BRL).getAmount();
    double[] mqsDscComputed = mqs.getSensitivity(ALL_CURVE_NAME, BRL).getSensitivity().toArray();
    for (int i = 0; i < DSC_NB_OIS_NODES; i++) {
      Map<MarketDataId<?>, Object> map = new HashMap<>(ALL_QUOTES.getValues());
      map.put(QuoteId.of(StandardId.of(SCHEME, OIS_ID_VALUE[i])), OIS_MARKET_QUOTES[i] + shift);
      ImmutableMarketData marketData = ImmutableMarketData.of(VAL_DATE, map);
      RatesProvider rpShifted = calibrator.apply(marketData);
      double pvS = SWAP_PRICER.presentValue(product, rpShifted).getAmount(BRL).getAmount();
      assertThat(mqsDscComputed[i]).as("DSC - node " + i).isCloseTo((pvS - pv0) / shift, offset(TOLERANCE_DELTA));
    }
  }
}
