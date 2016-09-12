/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.curve;

import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.BusinessDayConventions.FOLLOWING;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.USNY;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_3M;
import static com.opengamma.strata.basics.index.OvernightIndices.USD_FED_FUND;
import static com.opengamma.strata.product.swap.type.FixedIborSwapConventions.USD_FIXED_6M_LIBOR_3M;
import static com.opengamma.strata.product.swap.type.FixedOvernightSwapConventions.USD_FIXED_1Y_FED_FUND_OIS;
import static org.testng.Assert.assertEquals;

import java.io.IOException;
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
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.data.ImmutableMarketData;
import com.opengamma.strata.data.ImmutableMarketDataBuilder;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.data.MarketDataId;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.CurveGroupDefinition;
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.CurveNode;
import com.opengamma.strata.market.curve.CurveNodeDate;
import com.opengamma.strata.market.curve.DefaultCurveMetadata;
import com.opengamma.strata.market.curve.InterpolatedNodalCurveDefinition;
import com.opengamma.strata.market.curve.interpolator.CurveExtrapolator;
import com.opengamma.strata.market.curve.interpolator.CurveExtrapolators;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolator;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolators;
import com.opengamma.strata.market.curve.node.FixedIborSwapCurveNode;
import com.opengamma.strata.market.curve.node.FixedOvernightSwapCurveNode;
import com.opengamma.strata.market.curve.node.FraCurveNode;
import com.opengamma.strata.market.curve.node.IborFixingDepositCurveNode;
import com.opengamma.strata.market.curve.node.TermDepositCurveNode;
import com.opengamma.strata.market.observable.IndexQuoteId;
import com.opengamma.strata.market.observable.QuoteId;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.datasets.MeetingDatesDataSets;
import com.opengamma.strata.pricer.deposit.DiscountingIborFixingDepositProductPricer;
import com.opengamma.strata.pricer.deposit.DiscountingTermDepositProductPricer;
import com.opengamma.strata.pricer.fra.DiscountingFraTradePricer;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.sensitivity.MarketQuoteSensitivityCalculator;
import com.opengamma.strata.pricer.swap.DiscountingSwapProductPricer;
import com.opengamma.strata.product.ResolvedTrade;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.deposit.ResolvedIborFixingDepositTrade;
import com.opengamma.strata.product.deposit.ResolvedTermDepositTrade;
import com.opengamma.strata.product.deposit.type.IborFixingDepositTemplate;
import com.opengamma.strata.product.deposit.type.ImmutableTermDepositConvention;
import com.opengamma.strata.product.deposit.type.TermDepositConvention;
import com.opengamma.strata.product.deposit.type.TermDepositTemplate;
import com.opengamma.strata.product.fra.ResolvedFraTrade;
import com.opengamma.strata.product.fra.type.FraTemplate;
import com.opengamma.strata.product.swap.ResolvedSwap;
import com.opengamma.strata.product.swap.ResolvedSwapTrade;
import com.opengamma.strata.product.swap.SwapTrade;
import com.opengamma.strata.product.swap.type.FixedIborSwapConventions;
import com.opengamma.strata.product.swap.type.FixedIborSwapTemplate;
import com.opengamma.strata.product.swap.type.FixedOvernightSwapTemplate;

/**
 * Test for curve calibration with 2 curves in USD.
 * One curve is Discounting and Fed Fund forward and the other one is Libor 3M forward.
 * The discounting curve is calibrated with the dates of the next FOMC meetings are nodes.
 */
@Test
public class CalibrationDiscountFactorUsd2FomcDatesOisIrsTest {

  private static final LocalDate VAL_DATE_BD = LocalDate.of(2015, 7, 21);

  private static final CurveInterpolator INTERPOLATOR_LINEAR = CurveInterpolators.LINEAR;
  private static final CurveInterpolator INTERPOLATOR_LOGLINEAR = CurveInterpolators.LOG_LINEAR;
  private static final CurveExtrapolator EXTRAPOLATOR_FLAT = CurveExtrapolators.FLAT;
  private static final CurveExtrapolator EXTRAPOLATOR_EXP = CurveExtrapolators.EXPONENTIAL;
  private static final DayCount CURVE_DC = ACT_365F;

  // reference data
  private static final ReferenceData REF_DATA = ReferenceData.standard();

  private static final String SCHEME = "CALIBRATION";

  /** Curve names */
  private static final String DSCON_NAME = "USD-DSCON-OIS";
  private static final CurveName DSCON_CURVE_NAME = CurveName.of(DSCON_NAME);
  private static final String FWD3_NAME = "USD-LIBOR3M-FRAIRS";
  private static final CurveName FWD3_CURVE_NAME = CurveName.of(FWD3_NAME);
  /** Curves associations to currencies and indices. */
  private static final Map<CurveName, Currency> DSC_NAMES = new HashMap<>();
  private static final Map<CurveName, Set<Index>> IDX_NAMES = new HashMap<>();
  private static final MarketData TS_EMPTY = MarketData.empty(VAL_DATE_BD);
  private static final MarketData TS_BD_LIBOR3M;
  static {
    DSC_NAMES.put(DSCON_CURVE_NAME, USD);
    Set<Index> usdFedFundSet = new HashSet<>();
    usdFedFundSet.add(USD_FED_FUND);
    IDX_NAMES.put(DSCON_CURVE_NAME, usdFedFundSet);
    Set<Index> usdLibor3Set = new HashSet<>();
    usdLibor3Set.add(USD_LIBOR_3M);
    IDX_NAMES.put(FWD3_CURVE_NAME, usdLibor3Set);
    double fixingValue = 0.002345;
    LocalDateDoubleTimeSeries tsBdUsdLibor3M = 
        LocalDateDoubleTimeSeries.builder().put(VAL_DATE_BD, fixingValue).build();
    TS_BD_LIBOR3M = ImmutableMarketData.builder(VAL_DATE_BD)
        .addTimeSeries(IndexQuoteId.of(USD_LIBOR_3M), tsBdUsdLibor3M)
        .build();
  }

  /** Data for USD-DSCON curve */
  private static final Map<Integer, LocalDate> FOMC_NODES = fomcNodes(MeetingDatesDataSets.FOMC_MEETINGS,
      VAL_DATE_BD, 7); // select the next 7 FOMC dates as curve nodes
  /* Market values */
  private static final double[] DSC_MARKET_QUOTES = new double[] {
    0.001300, 0.001300,
    0.001435, 0.001680, 0.002105, 0.002385,
    0.002845, 0.003100, 0.003920, 0.005635, 0.007515,
    0.010550, 0.013225, 0.05450 };
  private static final int DSC_NB_NODES = DSC_MARKET_QUOTES.length;
  private static final String[] DSC_ID_VALUE = new String[] {
    "USD-ON", "USD-TN",
    "OIS2M", "OIS3M", "OIS5M", "OIS6M",
    "OIS8M", "OIS9M", "OIS1Y", "OIS18M", "OIS2Y",
    "OIS3Y", "OIS4Y", "OIS5Y" };
  /* Nodes */
  private static final CurveNode[] DSC_NODES = new CurveNode[DSC_NB_NODES];
  /* Tenors */
  private static final int[] DSC_DEPO_OFFSET = new int[] {0, 1};
  private static final int DSC_NB_DEPO_NODES = DSC_DEPO_OFFSET.length;  
  private static final Period[] DSC_OIS_TENORS = new Period[] {
      Period.ofMonths(2), Period.ofMonths(3), Period.ofMonths(5), Period.ofMonths(6),
      Period.ofMonths(8), Period.ofMonths(9), Period.ofYears(1), Period.ofMonths(18), Period.ofYears(2), 
      Period.ofYears(3), Period.ofYears(4), Period.ofYears(5)};
  private static final int DSC_NB_OIS_NODES = DSC_OIS_TENORS.length;
  static {
    for (int i = 0; i < DSC_NB_DEPO_NODES; i++) {
      BusinessDayAdjustment bda = BusinessDayAdjustment.of(FOLLOWING, USNY);
      TermDepositConvention convention = ImmutableTermDepositConvention.of(
          "USD-Dep", USD, bda, ACT_360, DaysAdjustment.ofBusinessDays(DSC_DEPO_OFFSET[i], USNY));
      LocalDate nodeDate = FOMC_NODES.get(i);
      if (nodeDate != null) {
        DSC_NODES[i] = TermDepositCurveNode.builder()
            .template(TermDepositTemplate.of(Period.ofDays(1), convention))
            .rateId(QuoteId.of(StandardId.of(SCHEME, DSC_ID_VALUE[i])))
            .date(CurveNodeDate.of(nodeDate))
            .build();
      } else {
        DSC_NODES[i] = TermDepositCurveNode.of(TermDepositTemplate.of(Period.ofDays(1), convention),
            QuoteId.of(StandardId.of(SCHEME, DSC_ID_VALUE[i])));
      }
    }
    for (int i = 0; i < DSC_NB_OIS_NODES; i++) {
      LocalDate nodeDate = FOMC_NODES.get(DSC_NB_DEPO_NODES + i);
      if (nodeDate != null) {
        DSC_NODES[DSC_NB_DEPO_NODES + i] = FixedOvernightSwapCurveNode.builder()
            .template(FixedOvernightSwapTemplate.of(Period.ZERO, Tenor.of(DSC_OIS_TENORS[i]), USD_FIXED_1Y_FED_FUND_OIS))
            .rateId(QuoteId.of(StandardId.of(SCHEME, DSC_ID_VALUE[DSC_NB_DEPO_NODES + i])))
            .date(CurveNodeDate.of(nodeDate))
            .build();
      } else {
        DSC_NODES[DSC_NB_DEPO_NODES + i] = FixedOvernightSwapCurveNode.of(
            FixedOvernightSwapTemplate.of(Period.ZERO, Tenor.of(DSC_OIS_TENORS[i]), USD_FIXED_1Y_FED_FUND_OIS),
            QuoteId.of(StandardId.of(SCHEME, DSC_ID_VALUE[DSC_NB_DEPO_NODES + i])));
      }
    }
  }

  /** Data for USD-LIBOR3M curve */
  /* Market values */
  private static final double[] FWD3_MARKET_QUOTES = new double[] {
      0.00236600,
      0.00258250, 0.00296050,
      0.00294300, 0.00503000, 0.00939150, 0.01380800, 0.01732000};
  private static final int FWD3_NB_NODES = FWD3_MARKET_QUOTES.length;
  private static final String[] FWD3_ID_VALUE = new String[] {
      "Fixing",
      "FRA3Mx6M", "FRA6Mx9M",
      "IRS1Y", "IRS2Y", "IRS3Y", "IRS4Y", "IRS5Y"};
  /* Nodes */
  private static final CurveNode[] FWD3_NODES = new CurveNode[FWD3_NB_NODES];
  /* Tenors */
  private static final Period[] FWD3_FRA_TENORS = new Period[] { // Period to start
      Period.ofMonths(3), Period.ofMonths(6)};
  private static final int FWD3_NB_FRA_NODES = FWD3_FRA_TENORS.length;
  private static final Period[] FWD3_IRS_TENORS = new Period[] {
      Period.ofYears(1), Period.ofYears(2), Period.ofYears(3), Period.ofYears(4), Period.ofYears(5)};
  private static final int FWD3_NB_IRS_NODES = FWD3_IRS_TENORS.length;
  static {
    FWD3_NODES[0] = IborFixingDepositCurveNode.of(IborFixingDepositTemplate.of(USD_LIBOR_3M),
        QuoteId.of(StandardId.of(SCHEME, FWD3_ID_VALUE[0])));
    for (int i = 0; i < FWD3_NB_FRA_NODES; i++) {
      FWD3_NODES[i + 1] = FraCurveNode.of(FraTemplate.of(FWD3_FRA_TENORS[i], USD_LIBOR_3M),
          QuoteId.of(StandardId.of(SCHEME, FWD3_ID_VALUE[i + 1])));
    }
    for (int i = 0; i < FWD3_NB_IRS_NODES; i++) {
      FWD3_NODES[i + 1 + FWD3_NB_FRA_NODES] = FixedIborSwapCurveNode.of(
          FixedIborSwapTemplate.of(Period.ZERO, Tenor.of(FWD3_IRS_TENORS[i]), USD_FIXED_6M_LIBOR_3M),
          QuoteId.of(StandardId.of(SCHEME, FWD3_ID_VALUE[i + 1 + FWD3_NB_FRA_NODES])));
    }
  }

  /** All quotes for the curve calibration on good business day. */
  private static final ImmutableMarketData ALL_QUOTES_BD;
  static {
    ImmutableMarketDataBuilder builder = ImmutableMarketData.builder(VAL_DATE_BD);
    for (int i = 0; i < FWD3_NB_NODES; i++) {
      builder.addValue(QuoteId.of(StandardId.of(SCHEME, FWD3_ID_VALUE[i])), FWD3_MARKET_QUOTES[i]);
    }
    for (int i = 0; i < DSC_NB_NODES; i++) {
      builder.addValue(QuoteId.of(StandardId.of(SCHEME, DSC_ID_VALUE[i])), DSC_MARKET_QUOTES[i]);
    }
    ALL_QUOTES_BD = builder.build();
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

  private static final CurveCalibrator CALIBRATOR = CurveCalibrator.of(1e-9, 1e-9, 100);

  // Constants
  private static final double TOLERANCE_PV = 1.0E-6;
  private static final double TOLERANCE_PV_DELTA = 2.0E+2;

  private static final CurveGroupName CURVE_GROUP_NAME = CurveGroupName.of("USD-DSCON-LIBOR3M");
  private static final InterpolatedNodalCurveDefinition DSC_CURVE_DEFN =
      InterpolatedNodalCurveDefinition.builder()
          .name(DSCON_CURVE_NAME)
          .xValueType(ValueType.YEAR_FRACTION)
          .yValueType(ValueType.DISCOUNT_FACTOR)
          .dayCount(CURVE_DC)
          .interpolator(INTERPOLATOR_LOGLINEAR)
          .extrapolatorLeft(EXTRAPOLATOR_EXP)
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
  private static final CurveGroupDefinition CURVE_GROUP_CONFIG =
      CurveGroupDefinition.builder()
          .name(CURVE_GROUP_NAME)
          .addCurve(DSC_CURVE_DEFN, USD, USD_FED_FUND)
          .addForwardCurve(FWD3_CURVE_DEFN, USD_LIBOR_3M).build();

  private static final CurveGroupDefinition GROUP_1 =
      CurveGroupDefinition.builder()
          .name(CurveGroupName.of("USD-DSCON"))
          .addCurve(DSC_CURVE_DEFN, USD, USD_FED_FUND)
          .build();
  private static final CurveGroupDefinition GROUP_2 =
      CurveGroupDefinition.builder()
          .name(CurveGroupName.of("USD-LIBOR3M"))
          .addForwardCurve(FWD3_CURVE_DEFN, USD_LIBOR_3M)
          .build();
  private static final ImmutableRatesProvider KNOWN_DATA = ImmutableRatesProvider.builder(VAL_DATE_BD).build();

  //-------------------------------------------------------------------------
  public void calibration_present_value_oneGroup_no_fixing() throws IOException {
    RatesProvider result =
        CALIBRATOR.calibrate(CURVE_GROUP_CONFIG, ALL_QUOTES_BD, REF_DATA);
    assertResult(result);
  }
  
  public void calibration_present_value_oneGroup_fixing() {
    RatesProvider result =
        CALIBRATOR.calibrate(CURVE_GROUP_CONFIG, ALL_QUOTES_BD.combinedWith(TS_BD_LIBOR3M), REF_DATA);
    assertResult(result);
  }

  public void calibration_present_value_twoGroups() {
    ImmutableRatesProvider result =
        CALIBRATOR.calibrate(ImmutableList.of(GROUP_1, GROUP_2), KNOWN_DATA, ALL_QUOTES_BD, REF_DATA);
    assertResult(result);
  }

  private void assertResult(RatesProvider result) {
    // Test PV Dsc
    CurveNode[] dscNodes = CURVES_NODES.get(0).get(0);
    List<ResolvedTrade> dscTrades = new ArrayList<>();
    for (int i = 0; i < dscNodes.length; i++) {
      dscTrades.add(dscNodes[i].resolvedTrade(1d, ALL_QUOTES_BD, REF_DATA));
    }
    // Depo
    for (int i = 0; i < DSC_NB_DEPO_NODES; i++) {
      CurrencyAmount pvIrs = DEPO_PRICER.presentValue(
          ((ResolvedTermDepositTrade) dscTrades.get(i)).getProduct(), result);
      assertEquals(pvIrs.getAmount(), 0.0, TOLERANCE_PV);
    }
    // OIS
    for (int i = 0; i < DSC_NB_OIS_NODES; i++) {
      MultiCurrencyAmount pvIrs = SWAP_PRICER.presentValue(
          ((ResolvedSwapTrade) dscTrades.get(DSC_NB_DEPO_NODES + i)).getProduct(), result);
      assertEquals(pvIrs.getAmount(USD).getAmount(), 0.0, TOLERANCE_PV);
    }
    // Test PV Fwd3
    CurveNode[] fwd3Nodes = CURVES_NODES.get(1).get(0);
    List<ResolvedTrade> fwd3Trades = new ArrayList<>();
    for (int i = 0; i < fwd3Nodes.length; i++) {
      fwd3Trades.add(fwd3Nodes[i].resolvedTrade(1d, ALL_QUOTES_BD, REF_DATA));
    }
    // Fixing 
    CurrencyAmount pvFixing = FIXING_PRICER.presentValue(
        ((ResolvedIborFixingDepositTrade) fwd3Trades.get(0)).getProduct(), result);
    assertEquals(pvFixing.getAmount(), 0.0, TOLERANCE_PV);
    // FRA
    for (int i = 0; i < FWD3_NB_FRA_NODES; i++) {
      CurrencyAmount pvFra = FRA_PRICER.presentValue(
          ((ResolvedFraTrade) fwd3Trades.get(i + 1)), result);
      assertEquals(pvFra.getAmount(), 0.0, TOLERANCE_PV);
    }
    // IRS
    for (int i = 0; i < FWD3_NB_IRS_NODES; i++) {
      MultiCurrencyAmount pvIrs = SWAP_PRICER.presentValue(
          ((ResolvedSwapTrade) fwd3Trades.get(i + 1 + FWD3_NB_FRA_NODES)).getProduct(), result);
      assertEquals(pvIrs.getAmount(USD).getAmount(), 0.0, TOLERANCE_PV);
    }
  }

  public void calibration_market_quote_sensitivity_one_group_no_fixing() {
    double shift = 1.0E-6;
    Function<MarketData, RatesProvider> f = marketData -> CALIBRATOR.calibrate(CURVE_GROUP_CONFIG, marketData, REF_DATA);
    calibration_market_quote_sensitivity_check(f, CURVE_GROUP_CONFIG, shift, TS_EMPTY);
  }

  public void calibration_market_quote_sensitivity_one_group_fixing() {
    double shift = 1.0E-6;
    Function<MarketData, RatesProvider> f = marketData -> CALIBRATOR.calibrate(CURVE_GROUP_CONFIG, marketData, REF_DATA);
    calibration_market_quote_sensitivity_check(f, CURVE_GROUP_CONFIG, shift, TS_BD_LIBOR3M);
  }

  private void calibration_market_quote_sensitivity_check(
      Function<MarketData, RatesProvider> calibrator,
      CurveGroupDefinition config,
      double shift,
      MarketData ts) {
    double notional = 100_000_000.0;
    double rate = 0.0400;
    SwapTrade trade = FixedIborSwapConventions.USD_FIXED_1Y_LIBOR_3M.createTrade(
        VAL_DATE_BD, Period.ofMonths(6), Tenor.TENOR_7Y, BuySell.BUY, notional, rate, REF_DATA);
    RatesProvider result = CALIBRATOR.calibrate(config, ALL_QUOTES_BD.combinedWith(ts), REF_DATA);
    ResolvedSwap product = trade.getProduct().resolve(REF_DATA);
    PointSensitivityBuilder pts = SWAP_PRICER.presentValueSensitivity(product, result);
    CurrencyParameterSensitivities ps = result.parameterSensitivity(pts.build());
    CurrencyParameterSensitivities mqs = MQC.sensitivity(ps, result);
    double pv0 = SWAP_PRICER.presentValue(product, result).getAmount(USD).getAmount();
    double[] mqsDscComputed = mqs.getSensitivity(DSCON_CURVE_NAME, USD).getSensitivity().toArray();
    for (int i = 0; i < DSC_NB_NODES; i++) {
      Map<MarketDataId<?>, Object> map = new HashMap<>(ALL_QUOTES_BD.getValues());
      map.put(QuoteId.of(StandardId.of(SCHEME, DSC_ID_VALUE[i])), DSC_MARKET_QUOTES[i] + shift);
      ImmutableMarketData marketData = ImmutableMarketData.of(VAL_DATE_BD, map);
      RatesProvider rpShifted = calibrator.apply(marketData.combinedWith(ts));
      double pvS = SWAP_PRICER.presentValue(product, rpShifted).getAmount(USD).getAmount();
      assertEquals(mqsDscComputed[i], (pvS - pv0) / shift, TOLERANCE_PV_DELTA);
    }
    double[] mqsFwd3Computed = mqs.getSensitivity(FWD3_CURVE_NAME, USD).getSensitivity().toArray();
    for (int i = 0; i < FWD3_NB_NODES; i++) {
      Map<MarketDataId<?>, Object> map = new HashMap<>(ALL_QUOTES_BD.getValues());
      map.put(QuoteId.of(StandardId.of(SCHEME, FWD3_ID_VALUE[i])), FWD3_MARKET_QUOTES[i] + shift);
      ImmutableMarketData marketData = ImmutableMarketData.of(VAL_DATE_BD, map);
      RatesProvider rpShifted = calibrator.apply(marketData.combinedWith(ts));
      double pvS = SWAP_PRICER.presentValue(product, rpShifted).getAmount(USD).getAmount();
      assertEquals(mqsFwd3Computed[i], (pvS - pv0) / shift, TOLERANCE_PV_DELTA);
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
        RatesProvider result =
            CALIBRATOR.calibrate(CURVE_GROUP_CONFIG, ALL_QUOTES_BD, REF_DATA);
        count += result.getValuationDate().getDayOfMonth();
      }
      endTime = System.currentTimeMillis();
      System.out.println("Performance: " + nbTests + " calibrations for 2 curve with 30 nodes in "
          + (endTime - startTime) + " ms.");
    }
    System.out.println("Avoiding hotspot: " + count);
    // Previous run: 375 ms for 100 calibrations (2 curve simultaneous - 30 nodes)
  }

  // Select the relevant FOMC meeting dates from a list of meetings
  private static Map<Integer, LocalDate> fomcNodes(List<LocalDate> meetings, LocalDate calibrationDate, int nbNodes) {
    Map<Integer, LocalDate> map = new HashMap<>();
    LocalDate currentDate = calibrationDate;
    Integer node = 0;
    int i = 0;
    while ((i < meetings.size()) && (node < nbNodes)) {
      if (meetings.get(i).isAfter(currentDate)) {
        map.put(node + 1, meetings.get(i)); // fixed dates periods start after ON
        node++;
        currentDate = meetings.get(i);
      }
      i++;
    }
    return map;
  }

}