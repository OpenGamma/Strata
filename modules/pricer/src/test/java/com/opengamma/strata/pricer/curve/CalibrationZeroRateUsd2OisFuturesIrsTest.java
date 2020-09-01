/*
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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.SequenceDate;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.basics.index.Index;
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
import com.opengamma.strata.market.curve.node.FixedIborSwapCurveNode;
import com.opengamma.strata.market.curve.node.FixedOvernightSwapCurveNode;
import com.opengamma.strata.market.curve.node.IborFixingDepositCurveNode;
import com.opengamma.strata.market.curve.node.IborFutureCurveNode;
import com.opengamma.strata.market.curve.node.TermDepositCurveNode;
import com.opengamma.strata.market.observable.QuoteId;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.deposit.DiscountingIborFixingDepositProductPricer;
import com.opengamma.strata.pricer.deposit.DiscountingTermDepositProductPricer;
import com.opengamma.strata.pricer.index.DiscountingIborFutureTradePricer;
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
import com.opengamma.strata.product.index.ResolvedIborFutureTrade;
import com.opengamma.strata.product.index.type.IborFutureContractSpec;
import com.opengamma.strata.product.index.type.IborFutureContractSpecs;
import com.opengamma.strata.product.index.type.IborFutureTemplate;
import com.opengamma.strata.product.swap.ResolvedSwap;
import com.opengamma.strata.product.swap.ResolvedSwapTrade;
import com.opengamma.strata.product.swap.SwapTrade;
import com.opengamma.strata.product.swap.type.FixedIborSwapConventions;
import com.opengamma.strata.product.swap.type.FixedIborSwapTemplate;
import com.opengamma.strata.product.swap.type.FixedOvernightSwapTemplate;

/**
 * Test for curve calibration with 2 curves in USD.
 * One curve is Discounting and Fed Fund forward and the other one is Libor 3M forward.
 * The Forward 3M curve is calibrated in part to Ibor futures without convexity adjustment.
 */
public class CalibrationZeroRateUsd2OisFuturesIrsTest {
  
  private static final ReferenceData REF_DATA = ReferenceData.standard();

  private static final LocalDate VAL_DATE = LocalDate.of(2015, 7, 21);

  private static final CurveInterpolator INTERPOLATOR_LINEAR = CurveInterpolators.LINEAR;
  private static final CurveExtrapolator EXTRAPOLATOR_FLAT = CurveExtrapolators.FLAT;
  private static final DayCount CURVE_DC = ACT_365F;

  private static final String SCHEME = "CALIBRATION";

  /** Curve names */
  private static final String DSCON_NAME = "USD-DSCON-OIS";
  private static final CurveName DSCON_CURVE_NAME = CurveName.of(DSCON_NAME);
  private static final String FWD3_NAME = "USD-LIBOR3M-FUTIRS";
  private static final CurveName FWD3_CURVE_NAME = CurveName.of(FWD3_NAME);
  /** Curves associations to currencies and indices. */
  private static final Map<CurveName, Currency> DSC_NAMES = new HashMap<>();
  private static final Map<CurveName, Set<Index>> IDX_NAMES = new HashMap<>();
  static {
    DSC_NAMES.put(DSCON_CURVE_NAME, USD);
    Set<Index> usdFedFundSet = new HashSet<>();
    usdFedFundSet.add(USD_FED_FUND);
    IDX_NAMES.put(DSCON_CURVE_NAME, usdFedFundSet);
    Set<Index> usdLibor3Set = new HashSet<>();
    usdLibor3Set.add(USD_LIBOR_3M);
    IDX_NAMES.put(FWD3_CURVE_NAME, usdLibor3Set);
  }

  /** Data for USD-DSCON curve */
  /* Market values */
  private static final double[] DSC_MARKET_QUOTES = new double[] {
      0.0005, 0.0005,
      0.00072000, 0.00082000, 0.00093000, 0.00090000, 0.00105000,
      0.00118500, 0.00318650, 0.00318650, 0.00704000, 0.01121500, 0.01515000,
      0.01845500, 0.02111000, 0.02332000, 0.02513500, 0.02668500};
  private static final int DSC_NB_NODES = DSC_MARKET_QUOTES.length;
  private static final String[] DSC_ID_VALUE = new String[] {
      "USD-ON", "USD-TN",
      "USD-OIS-1M", "USD-OIS-2M", "USD-OIS-3M", "USD-OIS-6M", "USD-OIS-9M",
      "USD-OIS-1Y", "USD-OIS-18M", "USD-OIS-2Y", "USD-OIS-3Y", "USD-OIS-4Y", "USD-OIS-5Y",
      "USD-OIS-6Y", "USD-OIS-7Y", "USD-OIS-8Y", "USD-OIS-9Y", "USD-OIS-10Y"};
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
    for (int i = 0; i < DSC_NB_DEPO_NODES; i++) {
      BusinessDayAdjustment bda = BusinessDayAdjustment.of(FOLLOWING, USNY);
      TermDepositConvention convention = 
          ImmutableTermDepositConvention.of(
              "USD-Dep", USD, bda, ACT_360, DaysAdjustment.ofBusinessDays(DSC_DEPO_OFFSET[i], USNY));
      DSC_NODES[i] = TermDepositCurveNode.of(TermDepositTemplate.of(Period.ofDays(1), convention), 
          QuoteId.of(StandardId.of(SCHEME, DSC_ID_VALUE[i])));
    }
    for (int i = 0; i < DSC_NB_OIS_NODES; i++) {
      DSC_NODES[DSC_NB_DEPO_NODES + i] = FixedOvernightSwapCurveNode.of(
          FixedOvernightSwapTemplate.of(Period.ZERO, Tenor.of(DSC_OIS_TENORS[i]), USD_FIXED_1Y_FED_FUND_OIS),
          QuoteId.of(StandardId.of(SCHEME, DSC_ID_VALUE[DSC_NB_DEPO_NODES + i])));
    }
  }

  /** Data for USD-LIBOR3M curve */
  /* Market values */
  private static final double[] FWD3_MARKET_QUOTES = new double[] {
      0.00236600,
      0.9975, 0.9975, 0.9950, 0.9950, 0.9940, 0.9930, 0.9920, 0.9910,
      0.00939150, 0.01380800, 0.01732000,
      0.02000000, 0.02396200, 0.02500000, 0.02700000, 0.02930000};
  private static final int FWD3_NB_NODES = FWD3_MARKET_QUOTES.length;
  private static final String[] FWD3_ID_VALUE = new String[] {
      "USD-Fixing-3M",
      "USD-ED1", "USD-ED2", "USD-ED3", "USD-ED4", "USD-ED5", "USD-ED6", "USD-ED7", "USD-ED8",
      "USD-IRS3M-3Y", "USD-IRS3M-4Y", "USD-IRS3M-5Y",
      "USD-IRS3M-6Y", "USD-IRS3M-7Y", "USD-IRS3M-8Y", "USD-IRS3M-9Y", "USD-IRS3M-10Y"};
  /* Nodes */
  private static final CurveNode[] FWD3_NODES = new CurveNode[FWD3_NB_NODES];
  /* Tenors */
  private static final int[] FWD3_FUT_SEQ = new int[] {1, 2, 3, 4, 5, 6, 7, 8};
  private static final int FWD3_NB_FUT_NODES = FWD3_FUT_SEQ.length;
  private static final Period[] FWD3_IRS_TENORS = new Period[] {
    Period.ofYears(3), Period.ofYears(4), Period.ofYears(5),
    Period.ofYears(6), Period.ofYears(7), Period.ofYears(8), Period.ofYears(9), Period.ofYears(10)};
  private static final int FWD3_NB_IRS_NODES = FWD3_IRS_TENORS.length;
  static {
    FWD3_NODES[0] = IborFixingDepositCurveNode.of(IborFixingDepositTemplate.of(USD_LIBOR_3M),
        QuoteId.of(StandardId.of(SCHEME, FWD3_ID_VALUE[0])));
    IborFutureContractSpec spec = IborFutureContractSpecs.USD_LIBOR_3M_IMM_CME;
    for (int i = 0; i < FWD3_NB_FUT_NODES; i++) {
      IborFutureTemplate template = IborFutureTemplate.of(SequenceDate.base(Period.ofDays(7), FWD3_FUT_SEQ[i]), spec);
      FWD3_NODES[i + 1] = IborFutureCurveNode.of(template, 
          QuoteId.of(StandardId.of(SCHEME, FWD3_ID_VALUE[i + 1])));
    }
    for (int i = 0; i < FWD3_NB_IRS_NODES; i++) {
      FWD3_NODES[i + 1 + FWD3_NB_FUT_NODES] = FixedIborSwapCurveNode.of(
          FixedIborSwapTemplate.of(Period.ZERO, Tenor.of(FWD3_IRS_TENORS[i]), USD_FIXED_6M_LIBOR_3M),
          QuoteId.of(StandardId.of(SCHEME, FWD3_ID_VALUE[i + 1 + FWD3_NB_FUT_NODES])));
    }
  }

  /** All quotes for the curve calibration */
  private static final ImmutableMarketData ALL_QUOTES;
  static {
    ImmutableMarketDataBuilder builder = ImmutableMarketData.builder(VAL_DATE);
    for (int i = 0; i < DSC_NB_NODES; i++) {
      builder.addValue(QuoteId.of(StandardId.of(SCHEME, DSC_ID_VALUE[i])), DSC_MARKET_QUOTES[i]);
    }
    for (int i = 0; i < FWD3_NB_NODES; i++) {
      builder.addValue(QuoteId.of(StandardId.of(SCHEME, FWD3_ID_VALUE[i])), FWD3_MARKET_QUOTES[i]);
    }
    ALL_QUOTES = builder.build();
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
  private static final DiscountingIborFutureTradePricer FUT_PRICER =
      DiscountingIborFutureTradePricer.DEFAULT;
  private static final DiscountingSwapProductPricer SWAP_PRICER =
      DiscountingSwapProductPricer.DEFAULT;
  private static final DiscountingTermDepositProductPricer DEPO_PRICER =
      DiscountingTermDepositProductPricer.DEFAULT;
  private static final MarketQuoteSensitivityCalculator MQC = MarketQuoteSensitivityCalculator.DEFAULT;

  private static final RatesCurveCalibrator CALIBRATOR = RatesCurveCalibrator.of(1e-9, 1e-9, 100);

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
  private static final RatesCurveGroupDefinition CURVE_GROUP_CONFIG =
      RatesCurveGroupDefinition.builder()
          .name(CURVE_GROUP_NAME)
          .addCurve(DSC_CURVE_DEFN, USD, USD_FED_FUND)
          .addForwardCurve(FWD3_CURVE_DEFN, USD_LIBOR_3M).build();

  //-------------------------------------------------------------------------
  @Test
  public void calibration_present_value_oneGroup() {
    RatesProvider result = CALIBRATOR.calibrate(CURVE_GROUP_CONFIG, ALL_QUOTES, REF_DATA);
    assertPresentValue(result);
  }
  
  @Test
  public void calibration_market_quote_sensitivity_one_group() {
    double shift = 1.0E-6;
    Function<MarketData, RatesProvider> f =
        marketData -> CALIBRATOR.calibrate(CURVE_GROUP_CONFIG, marketData, REF_DATA);
    calibration_market_quote_sensitivity_check(f, shift);
  }

  private void calibration_market_quote_sensitivity_check(
      Function<MarketData, RatesProvider> calibrator,
      double shift) {
    double notional = 100_000_000.0;
    double spread = 0.0050;
    SwapTrade trade = FixedIborSwapConventions.USD_FIXED_1Y_LIBOR_3M
        .createTrade(VAL_DATE, Period.ofMonths(8), Tenor.TENOR_7Y, BuySell.BUY, notional, spread, REF_DATA);
    RatesProvider result = calibrator.apply(ALL_QUOTES);
    ResolvedSwap product = trade.getProduct().resolve(REF_DATA);
    PointSensitivityBuilder pts = SWAP_PRICER.presentValueSensitivity(product, result);
    CurrencyParameterSensitivities ps = result.parameterSensitivity(pts.build());
    CurrencyParameterSensitivities mqs = MQC.sensitivity(ps, result);
    double pv0 = SWAP_PRICER.presentValue(product, result).getAmount(USD).getAmount();
    double[] mqsDscComputed = mqs.getSensitivity(DSCON_CURVE_NAME, USD).getSensitivity().toArray();
    for (int i = 0; i < DSC_NB_NODES; i++) {
      Map<MarketDataId<?>, Object> map = new HashMap<>(ALL_QUOTES.getValues());
      map.put(QuoteId.of(StandardId.of(SCHEME, DSC_ID_VALUE[i])), DSC_MARKET_QUOTES[i] + shift);
      ImmutableMarketData marketData = ImmutableMarketData.of(VAL_DATE, map);
      RatesProvider rpShifted = calibrator.apply(marketData);
      double pvS = SWAP_PRICER.presentValue(product, rpShifted).getAmount(USD).getAmount();
      assertThat(mqsDscComputed[i]).as("DSC - node " + i).isCloseTo((pvS - pv0) / shift, offset(TOLERANCE_PV_DELTA));
    }
    double[] mqsFwd3Computed = mqs.getSensitivity(FWD3_CURVE_NAME, USD).getSensitivity().toArray();
    for (int i = 0; i < FWD3_NB_NODES; i++) {
      Map<MarketDataId<?>, Object> map = new HashMap<>(ALL_QUOTES.getValues());
      map.put(QuoteId.of(StandardId.of(SCHEME, FWD3_ID_VALUE[i])), FWD3_MARKET_QUOTES[i] + shift);
      ImmutableMarketData marketData = ImmutableMarketData.of(VAL_DATE, map);
      RatesProvider rpShifted = calibrator.apply(marketData);
      double pvS = SWAP_PRICER.presentValue(product, rpShifted).getAmount(USD).getAmount();
      assertThat(mqsFwd3Computed[i]).as("FWD3 - node " + i).isCloseTo((pvS - pv0) / shift, offset(TOLERANCE_PV_DELTA));
    }
  }

  private void assertPresentValue(RatesProvider result) {
    // Test PV Dsc
    CurveNode[] dscNodes = CURVES_NODES.get(0).get(0);
    List<ResolvedTrade> dscTrades = new ArrayList<>();
    for (int i = 0; i < dscNodes.length; i++) {
      dscTrades.add(dscNodes[i].resolvedTrade(1d, ALL_QUOTES, REF_DATA));
    }
    // Depo
    for (int i = 0; i < DSC_NB_DEPO_NODES; i++) {
      CurrencyAmount pvIrs = DEPO_PRICER.presentValue(
          ((ResolvedTermDepositTrade) dscTrades.get(i)).getProduct(), result);
      assertThat(pvIrs.getAmount()).isCloseTo(0.0, offset(TOLERANCE_PV));
    }
    // OIS
    for (int i = 0; i < DSC_NB_OIS_NODES; i++) {
      MultiCurrencyAmount pvIrs = SWAP_PRICER.presentValue(
          ((ResolvedSwapTrade) dscTrades.get(DSC_NB_DEPO_NODES + i)).getProduct(), result);
      assertThat(pvIrs.getAmount(USD).getAmount()).isCloseTo(0.0, offset(TOLERANCE_PV));
    }
    // Test PV Fwd3
    CurveNode[] fwd3Nodes = CURVES_NODES.get(1).get(0);
    List<ResolvedTrade> fwd3Trades = new ArrayList<>();
    for (int i = 0; i < fwd3Nodes.length; i++) {
      fwd3Trades.add(fwd3Nodes[i].resolvedTrade(1d, ALL_QUOTES, REF_DATA));
    }
    // Fixing 
    CurrencyAmount pvFixing3 = FIXING_PRICER.presentValue(
        ((ResolvedIborFixingDepositTrade) fwd3Trades.get(0)).getProduct(), result);
    assertThat(pvFixing3.getAmount()).isCloseTo(0.0, offset(TOLERANCE_PV));
    // Futures
    for (int i = 0; i < FWD3_NB_FUT_NODES; i++) {
      CurrencyAmount pvFut = FUT_PRICER.presentValue(
          ((ResolvedIborFutureTrade) fwd3Trades.get(i + 1)), result, 0.0);
      assertThat(pvFut.getAmount()).isCloseTo(0.0, offset(TOLERANCE_PV));
    }
    // IRS
    for (int i = 0; i < FWD3_NB_IRS_NODES; i++) {
      MultiCurrencyAmount pvIrs = SWAP_PRICER.presentValue(
          ((ResolvedSwapTrade) fwd3Trades.get(i + 1 + FWD3_NB_FUT_NODES)).getProduct(), result);
      assertThat(pvIrs.getAmount(USD).getAmount()).isCloseTo(0.0, offset(TOLERANCE_PV));
    }
  }

  //-------------------------------------------------------------------------
  @SuppressWarnings("unused")
  @Disabled
  void performance() {
    long startTime, endTime;
    int nbTests = 100;
    int nbRep = 3;
    int count = 0;

    for (int i = 0; i < nbRep; i++) {
      startTime = System.currentTimeMillis();
      for (int looprep = 0; looprep < nbTests; looprep++) {
        RatesProvider result = CALIBRATOR.calibrate(CURVE_GROUP_CONFIG, ALL_QUOTES, REF_DATA);
        count += result.getValuationDate().getDayOfMonth();
      }
      endTime = System.currentTimeMillis();
      System.out.println("Performance: " + nbTests + " calibrations for 2 curves with 35 nodes in "
          + (endTime - startTime) + " ms.");
    }
    System.out.println("Avoiding hotspot: " + count);
    // Previous run: 665 ms for 100 calibrations (2 curves simultaneous - 35 nodes)
  }

}
