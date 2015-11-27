/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.calibration;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.index.IborIndices.EUR_EURIBOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_3M;
import static com.opengamma.strata.basics.index.OvernightIndices.USD_FED_FUND;
import static com.opengamma.strata.product.deposit.type.TermDepositConventions.USD_DEPOSIT_T0;
import static com.opengamma.strata.product.deposit.type.TermDepositConventions.USD_DEPOSIT_T1;
import static com.opengamma.strata.product.fx.type.FxSwapConventions.EUR_USD;
import static com.opengamma.strata.product.swap.type.FixedIborSwapConventions.EUR_FIXED_1Y_EURIBOR_3M;
import static com.opengamma.strata.product.swap.type.FixedIborSwapConventions.USD_FIXED_6M_LIBOR_3M;
import static com.opengamma.strata.product.swap.type.FixedOvernightSwapConventions.USD_FIXED_1Y_FED_FUND_OIS;
import static com.opengamma.strata.product.swap.type.XCcyIborIborSwapConventions.EUR_EURIBOR_3M_USD_LIBOR_3M;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.BuySell;
import com.opengamma.strata.basics.Trade;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.market.FxRateKey;
import com.opengamma.strata.basics.market.ImmutableMarketData;
import com.opengamma.strata.basics.market.MarketDataKey;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.CurveCurrencyParameterSensitivities;
import com.opengamma.strata.market.curve.CurveGroupDefinition;
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.CurveNode;
import com.opengamma.strata.market.curve.InterpolatedNodalCurveDefinition;
import com.opengamma.strata.market.curve.node.FixedIborSwapCurveNode;
import com.opengamma.strata.market.curve.node.FixedOvernightSwapCurveNode;
import com.opengamma.strata.market.curve.node.FraCurveNode;
import com.opengamma.strata.market.curve.node.FxSwapCurveNode;
import com.opengamma.strata.market.curve.node.IborFixingDepositCurveNode;
import com.opengamma.strata.market.curve.node.TermDepositCurveNode;
import com.opengamma.strata.market.curve.node.XCcyIborIborSwapCurveNode;
import com.opengamma.strata.market.interpolator.CurveExtrapolator;
import com.opengamma.strata.market.interpolator.CurveExtrapolators;
import com.opengamma.strata.market.interpolator.CurveInterpolator;
import com.opengamma.strata.market.interpolator.CurveInterpolators;
import com.opengamma.strata.market.key.QuoteKey;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.deposit.DiscountingIborFixingDepositProductPricer;
import com.opengamma.strata.pricer.deposit.DiscountingTermDepositProductPricer;
import com.opengamma.strata.pricer.fra.DiscountingFraTradePricer;
import com.opengamma.strata.pricer.fx.DiscountingFxSwapProductPricer;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.sensitivity.MarketQuoteSensitivityCalculator;
import com.opengamma.strata.pricer.swap.DiscountingSwapProductPricer;
import com.opengamma.strata.product.deposit.IborFixingDepositTrade;
import com.opengamma.strata.product.deposit.TermDepositTrade;
import com.opengamma.strata.product.deposit.type.IborFixingDepositTemplate;
import com.opengamma.strata.product.deposit.type.TermDepositTemplate;
import com.opengamma.strata.product.fra.FraTrade;
import com.opengamma.strata.product.fra.type.FraTemplate;
import com.opengamma.strata.product.fx.FxSwapTrade;
import com.opengamma.strata.product.fx.type.FxSwapTemplate;
import com.opengamma.strata.product.swap.SwapTrade;
import com.opengamma.strata.product.swap.type.FixedIborSwapTemplate;
import com.opengamma.strata.product.swap.type.FixedOvernightSwapTemplate;
import com.opengamma.strata.product.swap.type.XCcyIborIborSwapTemplate;

/**
 * Test for curve calibration in USD and EUR.
 * The USD curve is obtained by OIS and the EUR one by FX Swaps from USD.
 */
@Test
public class CalibrationZeroRateUsdOisIrsEurFxXCcyIrsTest {

  private static final LocalDate VALUATION_DATE = LocalDate.of(2015, 11, 2);

  private static final CurveInterpolator INTERPOLATOR_LINEAR = CurveInterpolators.LINEAR;
  private static final CurveExtrapolator EXTRAPOLATOR_FLAT = CurveExtrapolators.FLAT;
  private static final DayCount CURVE_DC = ACT_365F;

  private static final String SCHEME = "CALIBRATION";

  /** Curve names */
  private static final String USD_DSCON_STR = "USD-DSCON-OIS";
  private static final CurveName USD_DSCON_CURVE_NAME = CurveName.of(USD_DSCON_STR);
  private static final String USD_FWD3_NAME = "USD-LIBOR3M-FRAIRS";
  private static final CurveName USD_FWD3_CURVE_NAME = CurveName.of(USD_FWD3_NAME);
  private static final String EUR_DSC_STR = "EUR-DSC-FXXCCY";
  private static final CurveName EUR_DSC_CURVE_NAME = CurveName.of(EUR_DSC_STR);
  private static final String EUR_FWD3_NAME = "EUR-EURIBOR3M-FRAIRS";
  public static final CurveName EUR_FWD3_CURVE_NAME = CurveName.of(EUR_FWD3_NAME);
  private static final Map<Index, LocalDateDoubleTimeSeries> TS = new HashMap<>();

  /** Data FX **/
  private static final double FX_RATE_EUR_USD = 1.10;
  private static final String EUR_USD_ID_VALUE = "EUR-USD";
  /** Data for USD-DSCON curve */
  /* Market values */
  private static final double[] USD_DSC_MARKET_QUOTES = new double[] {
      0.0016, 0.0022,
      0.0013, 0.0016, 0.0020, 0.0026, 0.0033,
      0.0039, 0.0053, 0.0066, 0.0090, 0.0111,
      0.0128, 0.0143, 0.0156, 0.0167, 0.0175,
      0.0183};
  private static final int USD_DSC_NB_NODES = USD_DSC_MARKET_QUOTES.length;
  private static final String[] USD_DSC_ID_VALUE = new String[] {
      "USD-ON", "USD-TN",
      "USD-OIS-1M", "USD-OIS-2M", "USD-OIS-3M", "USD-OIS-6M", "USD-OIS-9M",
      "USD-OIS-1Y", "USD-OIS-18M", "USD-OIS-2Y", "USD-OIS-3Y", "USD-OIS-4Y",
      "USD-OIS-5Y", "USD-OIS-6Y", "USD-OIS-7Y", "USD-OIS-8Y", "USD-OIS-9Y",
      "USD-OIS-10Y"};
  /* Nodes */
  private static final CurveNode[] USD_DSC_NODES = new CurveNode[USD_DSC_NB_NODES];
  /* Tenors */
  private static final Period[] USD_DSC_OIS_TENORS = new Period[] {
      Period.ofMonths(1), Period.ofMonths(2), Period.ofMonths(3), Period.ofMonths(6), Period.ofMonths(9),
      Period.ofYears(1), Period.ofMonths(18), Period.ofYears(2), Period.ofYears(3), Period.ofYears(4),
      Period.ofYears(5), Period.ofYears(6), Period.ofYears(7), Period.ofYears(8), Period.ofYears(9),
      Period.ofYears(10)};
  private static final int USD_DSC_NB_OIS_NODES = USD_DSC_OIS_TENORS.length;
  static {
    USD_DSC_NODES[0] = TermDepositCurveNode.of(
        TermDepositTemplate.of(Period.ofDays(1), USD_DEPOSIT_T0),
        QuoteKey.of(StandardId.of(SCHEME, USD_DSC_ID_VALUE[0])));
    USD_DSC_NODES[1] = TermDepositCurveNode.of(TermDepositTemplate.of(Period.ofDays(1), USD_DEPOSIT_T1),
        QuoteKey.of(StandardId.of(SCHEME, USD_DSC_ID_VALUE[1])));
    for (int i = 0; i < USD_DSC_NB_OIS_NODES; i++) {
      USD_DSC_NODES[2 + i] = FixedOvernightSwapCurveNode.of(
          FixedOvernightSwapTemplate.of(Period.ZERO, Tenor.of(USD_DSC_OIS_TENORS[i]), USD_FIXED_1Y_FED_FUND_OIS),
          QuoteKey.of(StandardId.of(SCHEME, USD_DSC_ID_VALUE[2 + i])));
    }
  }
  /** Data for USD-LIBOR3M curve */
  /* Market values */
  private static final double[] USD_FWD3_MARKET_QUOTES = new double[] {
      0.003341,
      0.0049, 0.0063,
      0.0057, 0.0087, 0.0112, 0.0134, 0.0152,
      0.0181, 0.0209};
  private static final int USD_FWD3_NB_NODES = USD_FWD3_MARKET_QUOTES.length;
  private static final String[] USD_FWD3_ID_VALUE = new String[] {
      "USD-Fixing-3M",
      "USD-FRA3Mx6M", "USD-FRA6Mx9M",
      "USD-IRS3M-1Y", "USD-IRS3M-2Y", "USD-IRS3M-3Y", "USD-IRS3M-4Y", "USD-IRS3M-5Y",
      "USD-IRS3M-7Y", "USD-IRS3M-10Y"};
  /* Nodes */
  private static final CurveNode[] USD_FWD3_NODES = new CurveNode[USD_FWD3_NB_NODES];
  /* Tenors */
  private static final Period[] USD_FWD3_FRA_TENORS = new Period[] { // Period to start
      Period.ofMonths(3), Period.ofMonths(6)};
  private static final int USD_FWD3_NB_FRA_NODES = USD_FWD3_FRA_TENORS.length;
  private static final Period[] USD_FWD3_IRS_TENORS = new Period[] {
      Period.ofYears(1), Period.ofYears(2), Period.ofYears(3), Period.ofYears(4), Period.ofYears(5),
      Period.ofYears(7), Period.ofYears(10)};
  private static final int USD_FWD3_NB_IRS_NODES = USD_FWD3_IRS_TENORS.length;
  static {
    USD_FWD3_NODES[0] = IborFixingDepositCurveNode.of(
        IborFixingDepositTemplate.of(USD_LIBOR_3M),
        QuoteKey.of(StandardId.of(SCHEME, USD_FWD3_ID_VALUE[0])));
    for (int i = 0; i < USD_FWD3_NB_FRA_NODES; i++) {
      USD_FWD3_NODES[i + 1] = FraCurveNode.of(
          FraTemplate.of(USD_FWD3_FRA_TENORS[i], USD_LIBOR_3M),
          QuoteKey.of(StandardId.of(SCHEME, USD_FWD3_ID_VALUE[i + 1])));
    }
    for (int i = 0; i < USD_FWD3_NB_IRS_NODES; i++) {
      USD_FWD3_NODES[i + 1 + USD_FWD3_NB_FRA_NODES] = FixedIborSwapCurveNode.of(
          FixedIborSwapTemplate.of(Period.ZERO, Tenor.of(USD_FWD3_IRS_TENORS[i]), USD_FIXED_6M_LIBOR_3M),
          QuoteKey.of(StandardId.of(SCHEME, USD_FWD3_ID_VALUE[i + 1 + USD_FWD3_NB_FRA_NODES])));
    }
  }
  /** Data for EUR-DSC curve */
  /* Market values */
  private static final double[] EUR_DSC_MARKET_QUOTES = new double[] {
      0.0004, 0.0012, 0.0019, 0.0043, 0.0074,
      0.0109, -0.0034, -0.0036, -0.0038, -0.0039,
      -0.0040, -0.0039};
  private static final int EUR_DSC_NB_NODES = EUR_DSC_MARKET_QUOTES.length;
  private static final String[] EUR_DSC_ID_VALUE = new String[] {
      "EUR-USD-FX-1M", "EUR-USD-FX-2M", "EUR-USD-FX-3M", "EUR-USD-FX-6M", "EUR-USD-FX-9M",
      "EUR-USD-FX-1Y", "EUR-USD-XCCY-2Y", "EUR-USD-XCCY-3Y", "EUR-USD-XCCY-4Y", "EUR-USD-XCCY-5Y",
      "EUR-USD-XCCY-7Y", "EUR-USD-XCCY-10Y"};
  /* Nodes */
  private static final CurveNode[] EUR_DSC_NODES = new CurveNode[EUR_DSC_NB_NODES];
  /* Tenors */
  private static final Period[] EUR_DSC_FX_TENORS = new Period[] {
      Period.ofMonths(1), Period.ofMonths(2), Period.ofMonths(3), Period.ofMonths(6), Period.ofMonths(9),
      Period.ofYears(1)};
  private static final int EUR_DSC_NB_FX_NODES = EUR_DSC_FX_TENORS.length;
  private static final Period[] EUR_DSC_XCCY_TENORS = new Period[] {
      Period.ofYears(2), Period.ofYears(3), Period.ofYears(4), Period.ofYears(5), Period.ofYears(7),
      Period.ofYears(10)};
  private static final int EUR_DSC_NB_XCCY_NODES = EUR_DSC_XCCY_TENORS.length;
  static {
    for (int i = 0; i < EUR_DSC_NB_FX_NODES; i++) {
      EUR_DSC_NODES[i] = FxSwapCurveNode.of(
          FxSwapTemplate.of(EUR_DSC_FX_TENORS[i], EUR_USD),
          QuoteKey.of(StandardId.of(SCHEME, EUR_DSC_ID_VALUE[i])));
    }
    for (int i = 0; i < EUR_DSC_NB_XCCY_NODES; i++) {
      EUR_DSC_NODES[EUR_DSC_NB_FX_NODES + i] = XCcyIborIborSwapCurveNode.of(
          XCcyIborIborSwapTemplate.of(
              Tenor.of(EUR_DSC_XCCY_TENORS[i]), EUR_EURIBOR_3M_USD_LIBOR_3M),
          QuoteKey.of(StandardId.of(SCHEME, EUR_DSC_ID_VALUE[EUR_DSC_NB_FX_NODES + i])));
    }
  }

  /** Data for EUR-EURIBOR3M curve */
  /* Market values */
  private static final double[] EUR_FWD3_MARKET_QUOTES = new double[] {
      -0.00066,
      -0.0010, -0.0006,
      -0.0012, -0.0010, -0.0004, 0.0006, 0.0019,
      0.0047, 0.0085};
  private static final int EUR_FWD3_NB_NODES = EUR_FWD3_MARKET_QUOTES.length;
  private static final String[] EUR_FWD3_ID_VALUE = new String[] {
      "EUR-Fixing-3M",
      "EUR-FRA3Mx6M", "EUR-FRA6Mx9M",
      "EUR-IRS3M-1Y", "EUR-IRS3M-2Y", "EUR-IRS3M-3Y", "EUR-IRS3M-4Y", "EUR-IRS3M-5Y",
      "EUR-IRS3M-7Y", "EUR-IRS3M-10Y"};
  /* Nodes */
  private static final CurveNode[] EUR_FWD3_NODES = new CurveNode[EUR_FWD3_NB_NODES];
  /* Tenors */
  private static final Period[] EUR_FWD3_FRA_TENORS = new Period[] { // Period to start
      Period.ofMonths(3), Period.ofMonths(6)};
  private static final int EUR_FWD3_NB_FRA_NODES = EUR_FWD3_FRA_TENORS.length;
  private static final Period[] EUR_FWD3_IRS_TENORS = new Period[] {
      Period.ofYears(1), Period.ofYears(2), Period.ofYears(3), Period.ofYears(4), Period.ofYears(5),
      Period.ofYears(7), Period.ofYears(10)};
  private static final int EUR_FWD3_NB_IRS_NODES = EUR_FWD3_IRS_TENORS.length;
  static {
    EUR_FWD3_NODES[0] = IborFixingDepositCurveNode.of(IborFixingDepositTemplate.of(EUR_EURIBOR_3M),
        QuoteKey.of(StandardId.of(SCHEME, EUR_FWD3_ID_VALUE[0])));
    for (int i = 0; i < EUR_FWD3_NB_FRA_NODES; i++) {
      EUR_FWD3_NODES[i + 1] = FraCurveNode.of(FraTemplate.of(EUR_FWD3_FRA_TENORS[i], EUR_EURIBOR_3M),
          QuoteKey.of(StandardId.of(SCHEME, EUR_FWD3_ID_VALUE[i + 1])));
    }
    for (int i = 0; i < EUR_FWD3_NB_IRS_NODES; i++) {
      EUR_FWD3_NODES[i + 1 + EUR_FWD3_NB_FRA_NODES] = FixedIborSwapCurveNode.of(
          FixedIborSwapTemplate.of(Period.ZERO, Tenor.of(EUR_FWD3_IRS_TENORS[i]), EUR_FIXED_1Y_EURIBOR_3M),
          QuoteKey.of(StandardId.of(SCHEME, EUR_FWD3_ID_VALUE[i + 1 + EUR_FWD3_NB_FRA_NODES])));
    }
  }

  /** All quotes for the curve calibration */
  private static final ImmutableMarketData ALL_QUOTES;
  static {
    Map<MarketDataKey<?>, Object> map = new HashMap<>();
    for (int i = 0; i < USD_DSC_NB_NODES; i++) {
      map.put(QuoteKey.of(StandardId.of(SCHEME, USD_DSC_ID_VALUE[i])), USD_DSC_MARKET_QUOTES[i]);
    }
    for (int i = 0; i < USD_FWD3_NB_NODES; i++) {
      map.put(QuoteKey.of(StandardId.of(SCHEME, USD_FWD3_ID_VALUE[i])), USD_FWD3_MARKET_QUOTES[i]);
    }
    for (int i = 0; i < EUR_DSC_NB_NODES; i++) {
      map.put(QuoteKey.of(StandardId.of(SCHEME, EUR_DSC_ID_VALUE[i])), EUR_DSC_MARKET_QUOTES[i]);
    }
    for (int i = 0; i < EUR_FWD3_NB_NODES; i++) {
      map.put(QuoteKey.of(StandardId.of(SCHEME, EUR_FWD3_ID_VALUE[i])), EUR_FWD3_MARKET_QUOTES[i]);
    }
    map.put(QuoteKey.of(StandardId.of(SCHEME, EUR_USD_ID_VALUE)), FX_RATE_EUR_USD);
    map.put(FxRateKey.of(EUR, USD), FxRate.of(EUR, USD, FX_RATE_EUR_USD));
    ALL_QUOTES = ImmutableMarketData.of(map);
  }

  private static final DiscountingIborFixingDepositProductPricer FIXING_PRICER =
      DiscountingIborFixingDepositProductPricer.DEFAULT;
  private static final DiscountingFraTradePricer FRA_PRICER =
      DiscountingFraTradePricer.DEFAULT;
  private static final DiscountingSwapProductPricer SWAP_PRICER =
      DiscountingSwapProductPricer.DEFAULT;
  private static final DiscountingTermDepositProductPricer DEPO_PRICER =
      DiscountingTermDepositProductPricer.DEFAULT;
  private static final DiscountingFxSwapProductPricer FX_PRICER =
      DiscountingFxSwapProductPricer.DEFAULT;
  private static final MarketQuoteSensitivityCalculator MQC = MarketQuoteSensitivityCalculator.DEFAULT;

  private static final CalibrationMeasures CALIBRATION_MEASURES = CalibrationMeasures.DEFAULT;
  private static final CurveCalibrator CALIBRATOR = CurveCalibrator.of(1e-9, 1e-9, 100, CALIBRATION_MEASURES);

  // Constants
  private static final double TOLERANCE_PV = 1.0E-6;
  private static final double TOLERANCE_PV_DELTA = 1.0E+3;

  private static final CurveGroupName CURVE_GROUP_NAME = CurveGroupName.of("USD-DSCON-EUR-DSC");
  private static final InterpolatedNodalCurveDefinition USD_DSC_CURVE_DEFN =
      InterpolatedNodalCurveDefinition.builder()
          .name(USD_DSCON_CURVE_NAME)
          .xValueType(ValueType.YEAR_FRACTION)
          .yValueType(ValueType.ZERO_RATE)
          .dayCount(CURVE_DC)
          .interpolator(INTERPOLATOR_LINEAR)
          .extrapolatorLeft(EXTRAPOLATOR_FLAT)
          .extrapolatorRight(EXTRAPOLATOR_FLAT)
          .nodes(USD_DSC_NODES).build();
  private static final InterpolatedNodalCurveDefinition USD_FWD3_CURVE_DEFN =
      InterpolatedNodalCurveDefinition.builder()
          .name(USD_FWD3_CURVE_NAME)
          .xValueType(ValueType.YEAR_FRACTION)
          .yValueType(ValueType.ZERO_RATE)
          .dayCount(CURVE_DC)
          .interpolator(INTERPOLATOR_LINEAR)
          .extrapolatorLeft(EXTRAPOLATOR_FLAT)
          .extrapolatorRight(EXTRAPOLATOR_FLAT)
          .nodes(USD_FWD3_NODES).build();
  private static final InterpolatedNodalCurveDefinition EUR_DSC_CURVE_DEFN =
      InterpolatedNodalCurveDefinition.builder()
          .name(EUR_DSC_CURVE_NAME)
          .xValueType(ValueType.YEAR_FRACTION)
          .yValueType(ValueType.ZERO_RATE)
          .dayCount(CURVE_DC)
          .interpolator(INTERPOLATOR_LINEAR)
          .extrapolatorLeft(EXTRAPOLATOR_FLAT)
          .extrapolatorRight(EXTRAPOLATOR_FLAT)
          .nodes(EUR_DSC_NODES).build();
  private static final InterpolatedNodalCurveDefinition EUR_FWD3_CURVE_DEFN =
      InterpolatedNodalCurveDefinition.builder()
          .name(EUR_FWD3_CURVE_NAME)
          .xValueType(ValueType.YEAR_FRACTION)
          .yValueType(ValueType.ZERO_RATE)
          .dayCount(CURVE_DC)
          .interpolator(INTERPOLATOR_LINEAR)
          .extrapolatorLeft(EXTRAPOLATOR_FLAT)
          .extrapolatorRight(EXTRAPOLATOR_FLAT)
          .nodes(EUR_FWD3_NODES).build();
  private static final CurveGroupDefinition CURVE_GROUP_CONFIG =
      CurveGroupDefinition.builder()
          .name(CURVE_GROUP_NAME)
          .addCurve(USD_DSC_CURVE_DEFN, USD, USD_FED_FUND)
          .addForwardCurve(USD_FWD3_CURVE_DEFN, USD_LIBOR_3M)
          .addDiscountCurve(EUR_DSC_CURVE_DEFN, EUR)
          .addForwardCurve(EUR_FWD3_CURVE_DEFN, EUR_EURIBOR_3M).build();

  private static final CurveGroupDefinition GROUP_1 =
      CurveGroupDefinition.builder()
          .name(CurveGroupName.of("USD-DSCON"))
          .addCurve(USD_DSC_CURVE_DEFN, USD, USD_FED_FUND)
          .build();
  private static final CurveGroupDefinition GROUP_2 =
      CurveGroupDefinition.builder()
          .name(CurveGroupName.of("USD-LIBOR3M"))
          .addForwardCurve(USD_FWD3_CURVE_DEFN, USD_LIBOR_3M)
          .build();
  private static final CurveGroupDefinition GROUP_3 =
      CurveGroupDefinition.builder()
          .name(CurveGroupName.of("EUR-DSC-EURIBOR3M"))
          .addDiscountCurve(EUR_DSC_CURVE_DEFN, EUR)
          .addForwardCurve(EUR_FWD3_CURVE_DEFN, EUR_EURIBOR_3M).build();
  private static final ImmutableRatesProvider KNOWN_DATA = ImmutableRatesProvider.builder()
      .valuationDate(VALUATION_DATE)
      .fxRateProvider(new MarketDataFxRateProvider(ALL_QUOTES))
      .timeSeries(TS)
      .build();

  //-------------------------------------------------------------------------
  public void calibration_present_value_oneGroup() {
    ImmutableRatesProvider result = CALIBRATOR.calibrate(CURVE_GROUP_CONFIG, VALUATION_DATE, ALL_QUOTES, TS);
    assertPresentValue(result);
  }

  public void calibration_present_value_threeGroups() {
    ImmutableRatesProvider result =
        CALIBRATOR.calibrate(ImmutableList.of(GROUP_1, GROUP_2, GROUP_3), KNOWN_DATA, ALL_QUOTES);
    assertPresentValue(result);
  }

  private void assertPresentValue(ImmutableRatesProvider result) {
    // Test PV USD;
    List<Trade> usdTrades = new ArrayList<>();
    for (CurveNode USD_DSC_NODE : USD_DSC_NODES) {
      usdTrades.add(USD_DSC_NODE.trade(VALUATION_DATE, ALL_QUOTES));
    }
    // Depo
    for (int i = 0; i < 2; i++) {
      CurrencyAmount pvDep = DEPO_PRICER
          .presentValue(((TermDepositTrade) usdTrades.get(i)).getProduct(), result);
      assertEquals(pvDep.getAmount(), 0.0, TOLERANCE_PV);
    }
    // OIS
    for (int i = 0; i < USD_DSC_NB_OIS_NODES; i++) {
      MultiCurrencyAmount pvOis = SWAP_PRICER
          .presentValue(((SwapTrade) usdTrades.get(2 + i)).getProduct(), result);
      assertEquals(pvOis.getAmount(USD).getAmount(), 0.0, TOLERANCE_PV);
    }
    // Test PV USD Fwd3
    List<Trade> fwd3Trades = new ArrayList<>();
    for (int i = 0; i < USD_FWD3_NB_NODES; i++) {
      fwd3Trades.add(USD_FWD3_NODES[i].trade(VALUATION_DATE, ALL_QUOTES));
    }
    // Fixing 
    CurrencyAmount pvFixing =
        FIXING_PRICER.presentValue(((IborFixingDepositTrade) fwd3Trades.get(0)).getProduct(), result);
    assertEquals(pvFixing.getAmount(), 0.0, TOLERANCE_PV);
    // FRA
    for (int i = 0; i < USD_FWD3_NB_FRA_NODES; i++) {
      CurrencyAmount pvFra =
          FRA_PRICER.presentValue(((FraTrade) fwd3Trades.get(i + 1)), result);
      assertEquals(pvFra.getAmount(), 0.0, TOLERANCE_PV);
    }
    // IRS
    for (int i = 0; i < USD_FWD3_NB_IRS_NODES; i++) {
      MultiCurrencyAmount pvIrs = SWAP_PRICER
          .presentValue(((SwapTrade) fwd3Trades.get(i + 1 + USD_FWD3_NB_FRA_NODES)).getProduct(), result);
      assertEquals(pvIrs.getAmount(USD).getAmount(), 0.0, TOLERANCE_PV);
    }
    // Test DSC EUR;
    List<Trade> eurTrades = new ArrayList<>();
    for (CurveNode EUR_DSC_NODE : EUR_DSC_NODES) {
      eurTrades.add(EUR_DSC_NODE.trade(VALUATION_DATE, ALL_QUOTES));
    }
    // FX
    for (int i = 0; i < EUR_DSC_NB_FX_NODES; i++) {
      MultiCurrencyAmount pvFx = FX_PRICER
          .presentValue(((FxSwapTrade) eurTrades.get(i)).getProduct(), result);
      assertEquals(pvFx.convertedTo(USD, result).getAmount(), 0.0, TOLERANCE_PV);
    }
    // XCCY
    for (int i = 0; i < EUR_DSC_NB_XCCY_NODES; i++) {
      MultiCurrencyAmount pvFx = SWAP_PRICER
          .presentValue(((SwapTrade) eurTrades.get(EUR_DSC_NB_FX_NODES + i)).getProduct(), result);
      assertEquals(pvFx.convertedTo(USD, result).getAmount(), 0.0, TOLERANCE_PV);
    }
    // Test PV EUR Fwd3
    List<Trade> eurFwd3Trades = new ArrayList<>();
    for (int i = 0; i < EUR_FWD3_NB_NODES; i++) {
      eurFwd3Trades.add(EUR_FWD3_NODES[i].trade(VALUATION_DATE, ALL_QUOTES));
    }
    // Fixing 
    CurrencyAmount eurPvFixing =
        FIXING_PRICER.presentValue(((IborFixingDepositTrade) eurFwd3Trades.get(0)).getProduct(), result);
    assertEquals(eurPvFixing.getAmount(), 0.0, TOLERANCE_PV);
    // FRA
    for (int i = 0; i < EUR_FWD3_NB_FRA_NODES; i++) {
      CurrencyAmount pvFra =
          FRA_PRICER.presentValue(((FraTrade) eurFwd3Trades.get(i + 1)), result);
      assertEquals(pvFra.getAmount(), 0.0, TOLERANCE_PV);
    }
    // IRS
    for (int i = 0; i < EUR_FWD3_NB_IRS_NODES; i++) {
      MultiCurrencyAmount pvIrs = SWAP_PRICER
          .presentValue(((SwapTrade) eurFwd3Trades.get(i + 1 + EUR_FWD3_NB_FRA_NODES)).getProduct(), result);
      assertEquals(pvIrs.getAmount(EUR).getAmount(), 0.0, TOLERANCE_PV);
    }
  }

  public void calibration_market_quote_sensitivity_one_group() {
    double shift = 1.0E-6;
    Function<ImmutableMarketData, ImmutableRatesProvider> f =
        marketData -> CALIBRATOR.calibrate(CURVE_GROUP_CONFIG, VALUATION_DATE, marketData, TS);
    calibration_market_quote_sensitivity_check(f, shift);
  }

  private void calibration_market_quote_sensitivity_check(
      Function<ImmutableMarketData, ImmutableRatesProvider> calibrator,
      double shift) {
    double notional = 100_000_000.0;
    double fx = 1.1111;
    double fxPts = 0.0012;
    FxSwapTrade trade = EUR_USD
        .toTrade(VALUATION_DATE, Period.ofWeeks(6), Period.ofMonths(5), BuySell.BUY, notional, fx, fxPts);
    ImmutableRatesProvider result = CALIBRATOR.calibrate(CURVE_GROUP_CONFIG, VALUATION_DATE, ALL_QUOTES, TS);
    PointSensitivities pts = FX_PRICER.presentValueSensitivity(trade.getProduct(), result);
    CurveCurrencyParameterSensitivities ps = result.curveParameterSensitivity(pts);
    CurveCurrencyParameterSensitivities mqs = MQC.sensitivity(ps, result);
    double pvUsd = FX_PRICER.presentValue(trade.getProduct(), result).getAmount(USD).getAmount();
    double pvEur = FX_PRICER.presentValue(trade.getProduct(), result).getAmount(EUR).getAmount();
    double[] mqsUsd1Computed = mqs.getSensitivity(USD_DSCON_CURVE_NAME, USD).getSensitivity().toArray();
    for (int i = 0; i < USD_DSC_NB_NODES; i++) {
      Map<MarketDataKey<?>, Object> map = new HashMap<>(ALL_QUOTES.getValues());
      map.put(QuoteKey.of(StandardId.of(SCHEME, USD_DSC_ID_VALUE[i])), USD_DSC_MARKET_QUOTES[i] + shift);
      ImmutableMarketData marketData = ImmutableMarketData.of(map);
      ImmutableRatesProvider rpShifted = calibrator.apply(marketData);
      double pvS = FX_PRICER.presentValue(trade.getProduct(), rpShifted).getAmount(USD).getAmount();
      assertEquals(mqsUsd1Computed[i], (pvS - pvUsd) / shift, TOLERANCE_PV_DELTA);
    }
    double[] mqsUsd2Computed = mqs.getSensitivity(USD_DSCON_CURVE_NAME, EUR).getSensitivity().toArray();
    for (int i = 0; i < USD_DSC_NB_NODES; i++) {
      Map<MarketDataKey<?>, Object> map = new HashMap<>(ALL_QUOTES.getValues());
      map.put(QuoteKey.of(StandardId.of(SCHEME, USD_DSC_ID_VALUE[i])), USD_DSC_MARKET_QUOTES[i] + shift);
      ImmutableMarketData marketData = ImmutableMarketData.of(map);
      ImmutableRatesProvider rpShifted = calibrator.apply(marketData);
      double pvS = FX_PRICER.presentValue(trade.getProduct(), rpShifted).getAmount(EUR).getAmount();
      assertEquals(mqsUsd2Computed[i], (pvS - pvEur) / shift, TOLERANCE_PV_DELTA);
    }
    double[] mqsEur1Computed = mqs.getSensitivity(EUR_DSC_CURVE_NAME, USD).getSensitivity().toArray();
    for (int i = 0; i < EUR_DSC_NB_NODES; i++) {
      assertEquals(mqsEur1Computed[i], 0.0, TOLERANCE_PV_DELTA);
    }
    double[] mqsEur2Computed = mqs.getSensitivity(EUR_DSC_CURVE_NAME, EUR).getSensitivity().toArray();
    for (int i = 0; i < EUR_DSC_NB_NODES; i++) {
      Map<MarketDataKey<?>, Object> map = new HashMap<>(ALL_QUOTES.getValues());
      map.put(QuoteKey.of(StandardId.of(SCHEME, EUR_DSC_ID_VALUE[i])), EUR_DSC_MARKET_QUOTES[i] + shift);
      ImmutableMarketData marketData = ImmutableMarketData.of(map);
      ImmutableRatesProvider rpShifted = calibrator.apply(marketData);
      double pvS = FX_PRICER.presentValue(trade.getProduct(), rpShifted).getAmount(EUR).getAmount();
      assertEquals(mqsEur2Computed[i], (pvS - pvEur) / shift, TOLERANCE_PV_DELTA, "Node " + i);
    }
  }

}
