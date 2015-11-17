/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.calibration;

import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.BusinessDayConventions.FOLLOWING;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.date.HolidayCalendars.USNY;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_6M;
import static com.opengamma.strata.basics.index.OvernightIndices.USD_FED_FUND;
import static com.opengamma.strata.product.rate.swap.type.FixedIborSwapConventions.USD_FIXED_6M_LIBOR_3M;
import static com.opengamma.strata.product.rate.swap.type.FixedOvernightSwapConventions.USD_FIXED_1Y_FED_FUND_OIS;
import static com.opengamma.strata.product.rate.swap.type.IborIborSwapConventions.USD_LIBOR_3M_LIBOR_6M;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.BuySell;
import com.opengamma.strata.basics.Trade;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.FxMatrix;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.interpolator.CurveExtrapolator;
import com.opengamma.strata.basics.interpolator.CurveInterpolator;
import com.opengamma.strata.basics.market.ImmutableMarketData;
import com.opengamma.strata.basics.market.MarketData;
import com.opengamma.strata.basics.market.MarketDataKey;
import com.opengamma.strata.basics.market.ObservableKey;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.DefaultCurveMetadata;
import com.opengamma.strata.market.curve.definition.CurveGroupDefinition;
import com.opengamma.strata.market.curve.definition.CurveNode;
import com.opengamma.strata.market.curve.definition.FixedIborSwapCurveNode;
import com.opengamma.strata.market.curve.definition.FixedOvernightSwapCurveNode;
import com.opengamma.strata.market.curve.definition.FraCurveNode;
import com.opengamma.strata.market.curve.definition.IborFixingDepositCurveNode;
import com.opengamma.strata.market.curve.definition.IborIborSwapCurveNode;
import com.opengamma.strata.market.curve.definition.InterpolatedNodalCurveDefinition;
import com.opengamma.strata.market.curve.definition.TermDepositCurveNode;
import com.opengamma.strata.market.key.QuoteKey;
import com.opengamma.strata.market.sensitivity.CurveCurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.market.value.ValueType;
import com.opengamma.strata.math.impl.interpolation.FlatExtrapolator1D;
import com.opengamma.strata.math.impl.interpolation.LinearInterpolator1D;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.rate.deposit.DiscountingIborFixingDepositProductPricer;
import com.opengamma.strata.pricer.rate.deposit.DiscountingTermDepositProductPricer;
import com.opengamma.strata.pricer.rate.fra.DiscountingFraTradePricer;
import com.opengamma.strata.pricer.rate.swap.DiscountingSwapProductPricer;
import com.opengamma.strata.pricer.sensitivity.MarketQuoteSensitivityCalculator;
import com.opengamma.strata.product.rate.deposit.IborFixingDepositTrade;
import com.opengamma.strata.product.rate.deposit.TermDepositTrade;
import com.opengamma.strata.product.rate.deposit.type.IborFixingDepositTemplate;
import com.opengamma.strata.product.rate.deposit.type.ImmutableTermDepositConvention;
import com.opengamma.strata.product.rate.deposit.type.TermDepositConvention;
import com.opengamma.strata.product.rate.deposit.type.TermDepositTemplate;
import com.opengamma.strata.product.rate.fra.FraTrade;
import com.opengamma.strata.product.rate.fra.type.FraTemplate;
import com.opengamma.strata.product.rate.swap.SwapTrade;
import com.opengamma.strata.product.rate.swap.type.FixedIborSwapTemplate;
import com.opengamma.strata.product.rate.swap.type.FixedOvernightSwapTemplate;
import com.opengamma.strata.product.rate.swap.type.IborIborSwapConventions;
import com.opengamma.strata.product.rate.swap.type.IborIborSwapTemplate;

/**
 * Test for curve calibration with 2 curves in USD.
 * One curve is Discounting and Fed Fund forward and the other one is Libor 3M forward.
 */
@Test
public class CalibrationZeroRateUsd3OisIrsBsTest {

  private static final LocalDate VALUATION_DATE = LocalDate.of(2015, 7, 21);

  private static final CurveInterpolator INTERPOLATOR_LINEAR = new LinearInterpolator1D();
  private static final CurveExtrapolator EXTRAPOLATOR_FLAT = new FlatExtrapolator1D();
  private static final DayCount CURVE_DC = ACT_365F;
  private static final LocalDateDoubleTimeSeries TS_EMTPY = LocalDateDoubleTimeSeries.empty();

  private static final String SCHEME = "CALIBRATION";

  /** Curve names */
  private static final String DSCON_NAME = "USD-DSCON-OIS";
  private static final CurveName DSCON_CURVE_NAME = CurveName.of(DSCON_NAME);
  private static final String FWD3_NAME = "USD-LIBOR3M-FRAIRS";
  private static final CurveName FWD3_CURVE_NAME = CurveName.of(FWD3_NAME);
  private static final String FWD6_NAME = "USD-LIBOR6M-FRABS";
  private static final CurveName FWD6_CURVE_NAME = CurveName.of(FWD6_NAME);
  /** Curves associations to currencies and indices. */
  private static final Map<CurveName, Currency> DSC_NAMES = new HashMap<>();
  private static final Map<CurveName, Set<Index>> IDX_NAMES = new HashMap<>();
  private static final Map<Index, LocalDateDoubleTimeSeries> TS = new HashMap<>();
  static {
    DSC_NAMES.put(DSCON_CURVE_NAME, USD);
    Set<Index> usdFedFundSet = new HashSet<>();
    usdFedFundSet.add(USD_FED_FUND);
    IDX_NAMES.put(DSCON_CURVE_NAME, usdFedFundSet);
    Set<Index> usdLibor3Set = new HashSet<>();
    usdLibor3Set.add(USD_LIBOR_3M);
    IDX_NAMES.put(FWD3_CURVE_NAME, usdLibor3Set);
    Set<Index> usdLibor6Set = new HashSet<>();
    usdLibor6Set.add(USD_LIBOR_6M);
    IDX_NAMES.put(FWD6_CURVE_NAME, usdLibor6Set);
    TS.put(USD_FED_FUND, TS_EMTPY);
    TS.put(USD_LIBOR_3M, TS_EMTPY);
    TS.put(USD_LIBOR_6M, TS_EMTPY);
  }

  /** Data for USD-DSCON curve */
  /* Market values */
  private static final double[] DSC_MARKET_QUOTES = new double[] {
    0.0005, 0.0005,
    0.00072000, 0.00082000, 0.00093000, 0.00090000, 0.00105000,
    0.00118500, 0.00318650, 0.00318650, 0.00704000, 0.01121500, 0.01515000,
    0.01845500, 0.02111000, 0.02332000, 0.02513500, 0.02668500 };
  private static final int DSC_NB_NODES = DSC_MARKET_QUOTES.length;
  private static final String[] DSC_ID_VALUE = new String[] {
    "USD-ON", "USD-TN",
    "USD-OIS-1M", "USD-OIS-2M", "USD-OIS-3M", "USD-OIS-6M", "USD-OIS-9M",
    "USD-OIS-1Y", "USD-OIS-18M", "USD-OIS-2Y", "USD-OIS-3Y", "USD-OIS-4Y", "USD-OIS-5Y",
    "USD-OIS-6Y", "USD-OIS-7Y", "USD-OIS-8Y", "USD-OIS-9Y", "USD-OIS-10Y" };
  /* Nodes */
  private static final CurveNode[] DSC_NODES = new CurveNode[DSC_NB_NODES];
  /* Tenors */
  private static final int[] DSC_DEPO_OFFSET = new int[] {0, 1};
  private static final int DSC_NB_DEPO_NODES = DSC_DEPO_OFFSET.length;  
  private static final Period[] DSC_OIS_TENORS = new Period[] {
      Period.ofMonths(1), Period.ofMonths(2), Period.ofMonths(3), Period.ofMonths(6), Period.ofMonths(9),
      Period.ofYears(1), Period.ofMonths(18), Period.ofYears(2), Period.ofYears(3), Period.ofYears(4), Period.ofYears(5),
      Period.ofYears(6), Period.ofYears(7), Period.ofYears(8), Period.ofYears(9), Period.ofYears(10)};
  private static final int DSC_NB_OIS_NODES = DSC_OIS_TENORS.length;
  static {
    for(int i = 0; i < DSC_NB_DEPO_NODES; i++) {
      BusinessDayAdjustment bda = BusinessDayAdjustment.of(FOLLOWING, USNY);
      TermDepositConvention convention = 
          ImmutableTermDepositConvention.of(USD, bda, ACT_360, DaysAdjustment.ofBusinessDays(DSC_DEPO_OFFSET[i], USNY));
      DSC_NODES[i] = TermDepositCurveNode.of(TermDepositTemplate.of(Period.ofDays(1), convention), 
          QuoteKey.of(StandardId.of(SCHEME, DSC_ID_VALUE[i])));
    }
    for (int i = 0; i < DSC_NB_OIS_NODES; i++) {
      DSC_NODES[DSC_NB_DEPO_NODES + i] = FixedOvernightSwapCurveNode.of(
          FixedOvernightSwapTemplate.of(Period.ZERO, Tenor.of(DSC_OIS_TENORS[i]), USD_FIXED_1Y_FED_FUND_OIS),
          QuoteKey.of(StandardId.of(SCHEME, DSC_ID_VALUE[DSC_NB_DEPO_NODES + i])));
    }
  }

  /** Data for USD-LIBOR3M curve */
  /* Market values */
  private static final double[] FWD3_MARKET_QUOTES = new double[] {
    0.00236600,
    0.00258250, 0.00296050,
    0.00294300, 0.00503000, 0.00939150, 0.01380800, 0.01732000,
    0.02000000, 0.02396200, 0.02500000, 0.02700000, 0.02930000 };
  private static final int FWD3_NB_NODES = FWD3_MARKET_QUOTES.length;
  private static final String[] FWD3_ID_VALUE = new String[] {
    "USD-Fixing-3M",
    "USD-FRA3Mx6M", "USD-FRA6Mx9M",
    "USD-IRS3M-1Y", "USD-IRS3M-2Y", "USD-IRS3M-3Y", "USD-IRS3M-4Y", "USD-IRS3M-5Y",
    "USD-IRS3M-6Y", "USD-IRS3M-7Y", "USD-IRS3M-8Y", "USD-IRS3M-9Y", "USD-IRS3M-10Y" };
  /* Nodes */
  private static final CurveNode[] FWD3_NODES = new CurveNode[FWD3_NB_NODES];
  /* Tenors */
  private static final Period[] FWD3_FRA_TENORS = new Period[] { // Period to start
      Period.ofMonths(3), Period.ofMonths(6)};
  private static final int FWD3_NB_FRA_NODES = FWD3_FRA_TENORS.length;
  private static final Period[] FWD3_IRS_TENORS = new Period[] {
    Period.ofYears(1), Period.ofYears(2), Period.ofYears(3), Period.ofYears(4), Period.ofYears(5),
    Period.ofYears(6), Period.ofYears(7), Period.ofYears(8), Period.ofYears(9), Period.ofYears(10)};
  private static final int FWD3_NB_IRS_NODES = FWD3_IRS_TENORS.length;
  static {
    FWD3_NODES[0] = IborFixingDepositCurveNode.of(IborFixingDepositTemplate.of(USD_LIBOR_3M),
        QuoteKey.of(StandardId.of(SCHEME, FWD3_ID_VALUE[0])));
    for (int i = 0; i < FWD3_NB_FRA_NODES; i++) {
      FWD3_NODES[i + 1] = FraCurveNode.of(FraTemplate.of(FWD3_FRA_TENORS[i], USD_LIBOR_3M),
          QuoteKey.of(StandardId.of(SCHEME, FWD3_ID_VALUE[i + 1])));
    }
    for (int i = 0; i < FWD3_NB_IRS_NODES; i++) {
      FWD3_NODES[i + 1 + FWD3_NB_FRA_NODES] = FixedIborSwapCurveNode.of(
          FixedIborSwapTemplate.of(Period.ZERO, Tenor.of(FWD3_IRS_TENORS[i]), USD_FIXED_6M_LIBOR_3M),
          QuoteKey.of(StandardId.of(SCHEME, FWD3_ID_VALUE[i + 1 + FWD3_NB_FRA_NODES])));
    }
  }

  /** Data for USD-LIBOR6M curve */
  /* Market values */
  private static final double[] FWD6_MARKET_QUOTES = new double[] {
      0.00246600,
      0.00268250,
      0.00130000, 0.00120000, 0.00100000, 0.00100000, 0.00100000, 
      0.00100000, 0.00100000, 0.00090000, 0.00090000, 0.00080000, };
  private static final int FWD6_NB_NODES = FWD6_MARKET_QUOTES.length;
  private static final String[] FWD6_ID_VALUE = new String[] {
      "Fixing",
      "FRA3Mx9M", 
      "USD-BS36-1Y", "USD-BS36-2Y", "USD-BS36-3Y", "USD-BS36-4Y", "USD-BS36-5Y",
      "USD-BS36-6Y", "USD-BS36-7Y", "USD-BS36-8Y", "USD-BS36-9Y", "USD-BS36-10Y"};
  /* Nodes */
  private static final CurveNode[] FWD6_NODES = new CurveNode[FWD6_NB_NODES];
  /* Tenors */
  private static final Period[] FWD6_FRA_TENORS = new Period[] { // Period to start
      Period.ofMonths(3)};
  private static final int FWD6_NB_FRA_NODES = FWD6_FRA_TENORS.length;
  private static final Period[] FWD6_IRS_TENORS = new Period[] {
    Period.ofYears(1), Period.ofYears(2), Period.ofYears(3), Period.ofYears(4), Period.ofYears(5),
    Period.ofYears(6), Period.ofYears(7), Period.ofYears(8), Period.ofYears(9), Period.ofYears(10)};
  private static final int FWD6_NB_IRS_NODES = FWD6_IRS_TENORS.length;
  static {
    FWD6_NODES[0] = IborFixingDepositCurveNode.of(IborFixingDepositTemplate.of(USD_LIBOR_6M),
        QuoteKey.of(StandardId.of(SCHEME, FWD6_ID_VALUE[0])));
    for (int i = 0; i < FWD6_NB_FRA_NODES; i++) {
      FWD6_NODES[i + 1] = FraCurveNode.of(FraTemplate.of(FWD6_FRA_TENORS[i], USD_LIBOR_6M),
          QuoteKey.of(StandardId.of(SCHEME, FWD6_ID_VALUE[i + 1])));
    }
    for (int i = 0; i < FWD6_NB_IRS_NODES; i++) {
      FWD6_NODES[i + 1 + FWD6_NB_FRA_NODES] = IborIborSwapCurveNode.of(
          IborIborSwapTemplate.of(Period.ZERO, Tenor.of(FWD6_IRS_TENORS[i]), USD_LIBOR_3M_LIBOR_6M),
          QuoteKey.of(StandardId.of(SCHEME, FWD6_ID_VALUE[i + 1 + FWD6_NB_FRA_NODES])));
    }
  }

  /** All quotes for the curve calibration */
  private static final ImmutableMarketData ALL_QUOTES;
  static {
    Map<ObservableKey, Double> map = new HashMap<>();
    for (int i = 0; i < DSC_NB_NODES; i++) {
      map.put(QuoteKey.of(StandardId.of(SCHEME, DSC_ID_VALUE[i])), DSC_MARKET_QUOTES[i]);
    }
    for (int i = 0; i < FWD3_NB_NODES; i++) {
      map.put(QuoteKey.of(StandardId.of(SCHEME, FWD3_ID_VALUE[i])), FWD3_MARKET_QUOTES[i]);
    }
    for (int i = 0; i < FWD6_NB_NODES; i++) {
      map.put(QuoteKey.of(StandardId.of(SCHEME, FWD6_ID_VALUE[i])), FWD6_MARKET_QUOTES[i]);
    }
    ALL_QUOTES = ImmutableMarketData.of(map);
  }

  /** All nodes by groups. */
  private static final List<List<CurveNode[]>> CURVES_NODES = new ArrayList<>();
  static {
    List<CurveNode[]> groupDsc = new ArrayList<>();
    groupDsc.add(DSC_NODES);
    CURVES_NODES.add(groupDsc);
    List<CurveNode[]> groupFwd3 = new ArrayList<>();
    groupFwd3.add(FWD3_NODES);
    CURVES_NODES.add(groupFwd3);
    List<CurveNode[]> groupFwd6 = new ArrayList<>();
    groupFwd6.add(FWD6_NODES);
    CURVES_NODES.add(groupFwd6);
  }

  /** All metadata by groups */
  private static final List<List<CurveMetadata>> CURVES_METADATA = new ArrayList<>();
  static {
    List<CurveMetadata> groupDsc = new ArrayList<>();
    groupDsc.add(DefaultCurveMetadata.builder().curveName(DSCON_CURVE_NAME).xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.ZERO_RATE).dayCount(CURVE_DC).build());
    CURVES_METADATA.add(groupDsc);
    List<CurveMetadata> groupFwd3 = new ArrayList<>();
    groupFwd3.add(DefaultCurveMetadata.builder().curveName(FWD3_CURVE_NAME).xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.ZERO_RATE).dayCount(CURVE_DC).build());
    CURVES_METADATA.add(groupFwd3);
    List<CurveMetadata> groupFwd6 = new ArrayList<>();
    groupFwd6.add(DefaultCurveMetadata.builder().curveName(FWD6_CURVE_NAME).xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.ZERO_RATE).dayCount(CURVE_DC).build());
    CURVES_METADATA.add(groupFwd6);
  }

  private static final DiscountingIborFixingDepositProductPricer FIXING_PRICER =
      DiscountingIborFixingDepositProductPricer.DEFAULT;
  private static final DiscountingFraTradePricer FRA_PRICER =
      DiscountingFraTradePricer.DEFAULT;
  private static final DiscountingSwapProductPricer SWAP_PRICER =
      DiscountingSwapProductPricer.DEFAULT;
  private static final DiscountingTermDepositProductPricer DEPO_PRICER =
      DiscountingTermDepositProductPricer.DEFAULT;
  private static final MarketQuoteSensitivityCalculator MQC = MarketQuoteSensitivityCalculator.DEFAULT;

  private static final CalibrationMeasures CALIBRATION_MEASURES = CalibrationMeasures.DEFAULT;
  private static final CurveCalibrator CALIBRATOR = CurveCalibrator.of(1e-9, 1e-9, 100, CALIBRATION_MEASURES);

  // Constants
  private static final double TOLERANCE_PV = 1.0E-6;
  private static final double TOLERANCE_PV_DELTA = 1.0E+3;

  private static final CurveGroupName CURVE_GROUP_NAME = CurveGroupName.of("USD-DSCON-LIBOR3M");
  private static final InterpolatedNodalCurveDefinition DSC_CURVE_DEFN =
      InterpolatedNodalCurveDefinition.builder()
          .name(DSCON_CURVE_NAME)
          .xValueType(ValueType.YEAR_FRACTION)
          .yValueType(ValueType.ZERO_RATE)
          .dayCount(CURVE_DC)
          .interpolator(INTERPOLATOR_LINEAR)
          .extrapolatorLeft(EXTRAPOLATOR_FLAT)
          .extrapolatorRight(EXTRAPOLATOR_FLAT)
          .nodes(DSC_NODES).build();
  private static final InterpolatedNodalCurveDefinition FWD3_CURVE_DEFN =
      InterpolatedNodalCurveDefinition.builder()
          .name(FWD3_CURVE_NAME)
          .xValueType(ValueType.YEAR_FRACTION)
          .yValueType(ValueType.ZERO_RATE)
          .dayCount(CURVE_DC)
          .interpolator(INTERPOLATOR_LINEAR)
          .extrapolatorLeft(EXTRAPOLATOR_FLAT)
          .extrapolatorRight(EXTRAPOLATOR_FLAT)
          .nodes(FWD3_NODES).build();
  private static final InterpolatedNodalCurveDefinition FWD6_CURVE_DEFN =
      InterpolatedNodalCurveDefinition.builder()
          .name(FWD6_CURVE_NAME)
          .xValueType(ValueType.YEAR_FRACTION)
          .yValueType(ValueType.ZERO_RATE)
          .dayCount(CURVE_DC)
          .interpolator(INTERPOLATOR_LINEAR)
          .extrapolatorLeft(EXTRAPOLATOR_FLAT)
          .extrapolatorRight(EXTRAPOLATOR_FLAT)
          .nodes(FWD6_NODES).build();
  private static final CurveGroupDefinition CURVE_GROUP_CONFIG =
      CurveGroupDefinition.builder()
          .name(CURVE_GROUP_NAME)
          .addCurve(DSC_CURVE_DEFN, USD, USD_FED_FUND)
          .addForwardCurve(FWD3_CURVE_DEFN, USD_LIBOR_3M)
          .addForwardCurve(FWD6_CURVE_DEFN, USD_LIBOR_6M).build();

  private static final CurveGroupDefinition GROUP_1 =
      CurveGroupDefinition.builder()
          .name(CurveGroupName.of(DSCON_NAME))
          .addCurve(DSC_CURVE_DEFN, USD, USD_FED_FUND)
          .build();
  private static final CurveGroupDefinition GROUP_2 =
      CurveGroupDefinition.builder()
          .name(CurveGroupName.of(FWD3_NAME))
          .addForwardCurve(FWD3_CURVE_DEFN, USD_LIBOR_3M)
          .build();
  private static final CurveGroupDefinition GROUP_3 =
      CurveGroupDefinition.builder()
          .name(CurveGroupName.of(FWD6_NAME))
          .addForwardCurve(FWD6_CURVE_DEFN, USD_LIBOR_6M)
          .build();
  private static final ImmutableRatesProvider KNOWN_DATA = ImmutableRatesProvider.builder()
      .valuationDate(VALUATION_DATE)
      .timeSeries(TS)
      .build();

  //-------------------------------------------------------------------------
  public void calibration_present_value_oneGroup() {
    ImmutableRatesProvider result =
        CALIBRATOR.calibrate(CURVE_GROUP_CONFIG, VALUATION_DATE, ALL_QUOTES, TS, FxMatrix.empty());
    assertPresentValue(result);
  }

  public void calibration_present_value_threeGroups() {
    ImmutableRatesProvider result =
        CALIBRATOR.calibrate(ImmutableList.of(GROUP_1, GROUP_2, GROUP_3), KNOWN_DATA, ALL_QUOTES);
    assertPresentValue(result);
  }
  
  public void calibration_market_quote_sensitivity_one_group() {
    double shift = 1.0E-6;
    Function<MarketData, ImmutableRatesProvider> f =
        marketData -> CALIBRATOR.calibrate(CURVE_GROUP_CONFIG, VALUATION_DATE, marketData, TS, FxMatrix.empty());
    calibration_market_quote_sensitivity_check(f, shift);
  }
  
  public void calibration_market_quote_sensitivity_three_group() {
    double shift = 1.0E-6;
    Function<MarketData, ImmutableRatesProvider> calibrator =
        marketData -> CALIBRATOR.calibrate(ImmutableList.of(GROUP_1, GROUP_2, GROUP_3), KNOWN_DATA, marketData);
    calibration_market_quote_sensitivity_check(calibrator, shift);
  }

  private void calibration_market_quote_sensitivity_check(
      Function<MarketData, ImmutableRatesProvider> calibrator,
      double shift) {
    double notional = 100_000_000.0;
    double spread = 0.0050;
    SwapTrade trade = IborIborSwapConventions.USD_LIBOR_3M_LIBOR_6M.toTrade(VALUATION_DATE, Period.ofMonths(8),
        Tenor.TENOR_7Y, BuySell.BUY, notional, spread);
    ImmutableRatesProvider result = calibrator.apply(ALL_QUOTES);
    PointSensitivityBuilder pts = SWAP_PRICER.presentValueSensitivity(trade.getProduct(), result);
    CurveCurrencyParameterSensitivities ps = result.curveParameterSensitivity(pts.build());
    CurveCurrencyParameterSensitivities mqs = MQC.sensitivity(ps, result);
    double pv0 = SWAP_PRICER.presentValue(trade.getProduct(), result).getAmount(USD).getAmount();
    double[] mqsDscComputed = mqs.getSensitivity(DSCON_CURVE_NAME, USD).getSensitivity().toArray();
    for (int i = 0; i < DSC_NB_NODES; i++) {
      Map<MarketDataKey<?>, Object> map = new HashMap<>(ALL_QUOTES.getValues());
      map.put(QuoteKey.of(StandardId.of(SCHEME, DSC_ID_VALUE[i])), DSC_MARKET_QUOTES[i] + shift);
      ImmutableMarketData marketData = ImmutableMarketData.of(map);
      ImmutableRatesProvider rpShifted = calibrator.apply(marketData);
      double pvS = SWAP_PRICER.presentValue(trade.getProduct(), rpShifted).getAmount(USD).getAmount();
      assertEquals(mqsDscComputed[i], (pvS - pv0) / shift, TOLERANCE_PV_DELTA, "DSC - node " + i);
    }
    double[] mqsFwd3Computed = mqs.getSensitivity(FWD3_CURVE_NAME, USD).getSensitivity().toArray();
    for (int i = 0; i < FWD3_NB_NODES; i++) {
      Map<MarketDataKey<?>, Object> map = new HashMap<>(ALL_QUOTES.getValues());
      map.put(QuoteKey.of(StandardId.of(SCHEME, FWD3_ID_VALUE[i])), FWD3_MARKET_QUOTES[i] + shift);
      ImmutableMarketData marketData = ImmutableMarketData.of(map);
      ImmutableRatesProvider rpShifted = calibrator.apply(marketData);
      double pvS = SWAP_PRICER.presentValue(trade.getProduct(), rpShifted).getAmount(USD).getAmount();
      assertEquals(mqsFwd3Computed[i], (pvS - pv0) / shift, TOLERANCE_PV_DELTA, "FWD3 - node " + i);
    }
    double[] mqsFwd6Computed = mqs.getSensitivity(FWD6_CURVE_NAME, USD).getSensitivity().toArray();
    for (int i = 0; i < FWD6_NB_NODES; i++) {
      Map<MarketDataKey<?>, Object> map = new HashMap<>(ALL_QUOTES.getValues());
      map.put(QuoteKey.of(StandardId.of(SCHEME, FWD6_ID_VALUE[i])), FWD6_MARKET_QUOTES[i] + shift);
      ImmutableMarketData marketData = ImmutableMarketData.of(map);
      ImmutableRatesProvider rpShifted = calibrator.apply(marketData);
      double pvS = SWAP_PRICER.presentValue(trade.getProduct(), rpShifted).getAmount(USD).getAmount();
      assertEquals(mqsFwd6Computed[i], (pvS - pv0) / shift, TOLERANCE_PV_DELTA, "FWD6 - node " + i);
    }
  }

  private void assertPresentValue(ImmutableRatesProvider result) {
    // Test PV Dsc
    CurveNode[] dscNodes = CURVES_NODES.get(0).get(0);
    List<Trade> dscTrades = new ArrayList<>();
    for (int i = 0; i < dscNodes.length; i++) {
      dscTrades.add(dscNodes[i].trade(VALUATION_DATE, ALL_QUOTES));
    }
    // Depo
    for (int i = 0; i < DSC_NB_DEPO_NODES; i++) {
      CurrencyAmount pvIrs = DEPO_PRICER
          .presentValue(((TermDepositTrade) dscTrades.get(i)).getProduct(), result);
      assertEquals(pvIrs.getAmount(), 0.0, TOLERANCE_PV);
    }
    // OIS
    for (int i = 0; i < DSC_NB_OIS_NODES; i++) {
      MultiCurrencyAmount pvIrs = SWAP_PRICER
          .presentValue(((SwapTrade) dscTrades.get(DSC_NB_DEPO_NODES + i)).getProduct(), result);
      assertEquals(pvIrs.getAmount(USD).getAmount(), 0.0, TOLERANCE_PV);
    }
    // Test PV Fwd3
    CurveNode[] fwd3Nodes = CURVES_NODES.get(1).get(0);
    List<Trade> fwd3Trades = new ArrayList<>();
    for (int i = 0; i < fwd3Nodes.length; i++) {
      fwd3Trades.add(fwd3Nodes[i].trade(VALUATION_DATE, ALL_QUOTES));
    }
    // Fixing 
    CurrencyAmount pvFixing3 =
        FIXING_PRICER.presentValue(((IborFixingDepositTrade) fwd3Trades.get(0)).getProduct(), result);
    assertEquals(pvFixing3.getAmount(), 0.0, TOLERANCE_PV);
    // FRA
    for (int i = 0; i < FWD3_NB_FRA_NODES; i++) {
      CurrencyAmount pvFra =
          FRA_PRICER.presentValue(((FraTrade) fwd3Trades.get(i + 1)), result);
      assertEquals(pvFra.getAmount(), 0.0, TOLERANCE_PV);
    }
    // IRS
    for (int i = 0; i < FWD3_NB_IRS_NODES; i++) {
      MultiCurrencyAmount pvIrs = SWAP_PRICER
          .presentValue(((SwapTrade) fwd3Trades.get(i + 1 + FWD3_NB_FRA_NODES)).getProduct(), result);
      assertEquals(pvIrs.getAmount(USD).getAmount(), 0.0, TOLERANCE_PV);
    }
    // Test PV Fwd3
    CurveNode[] fwd6Nodes = CURVES_NODES.get(2).get(0);
    List<Trade> fwd6Trades = new ArrayList<>();
    for (int i = 0; i < fwd6Nodes.length; i++) {
      fwd6Trades.add(fwd6Nodes[i].trade(VALUATION_DATE, ALL_QUOTES));
    }
    // Fixing 
    CurrencyAmount pvFixing6 =
        FIXING_PRICER.presentValue(((IborFixingDepositTrade) fwd6Trades.get(0)).getProduct(), result);
    assertEquals(pvFixing6.getAmount(), 0.0, TOLERANCE_PV);
    // FRA
    for (int i = 0; i < FWD6_NB_FRA_NODES; i++) {
      CurrencyAmount pvFra =
          FRA_PRICER.presentValue(((FraTrade) fwd6Trades.get(i + 1)), result);
      assertEquals(pvFra.getAmount(), 0.0, TOLERANCE_PV);
    }
    // BS
    for (int i = 0; i < FWD6_NB_IRS_NODES; i++) {
      MultiCurrencyAmount pvIrs = SWAP_PRICER
          .presentValue(((SwapTrade) fwd6Trades.get(i + 1 + FWD6_NB_FRA_NODES)).getProduct(), result);
      assertEquals(pvIrs.getAmount(USD).getAmount(), 0.0, TOLERANCE_PV);
    }
  }

  //-------------------------------------------------------------------------
  @SuppressWarnings("unused")
  @Test(enabled = false)
  void performance() {
    long startTime, endTime;
    int nbTests = 100;
    int nbRep = 3;
    int count = 0;

    for (int i = 0; i < nbRep; i++) {
      startTime = System.currentTimeMillis();
      for (int looprep = 0; looprep < nbTests; looprep++) {
        ImmutableRatesProvider result =
            CALIBRATOR.calibrate(CURVE_GROUP_CONFIG, VALUATION_DATE, ALL_QUOTES, TS, FxMatrix.empty());
        count += result.getDiscountCurves().size() + result.getIndexCurves().size();
      }
      endTime = System.currentTimeMillis();
      System.out.println("Performance: " + nbTests + " calibrations for 3 curve with 43 nodes in "
          + (endTime - startTime) + " ms.");
    }
    System.out.println("Avoiding hotspot: " + count);
    // Previous run: 2325 ms for 100 calibrations (3 curve simultaneous - 43 nodes)
  }

}
