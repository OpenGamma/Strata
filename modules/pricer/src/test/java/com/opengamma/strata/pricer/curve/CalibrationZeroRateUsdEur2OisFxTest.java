/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.curve;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.index.OvernightIndices.USD_FED_FUND;
import static com.opengamma.strata.product.deposit.type.TermDepositConventions.USD_SHORT_DEPOSIT_T0;
import static com.opengamma.strata.product.deposit.type.TermDepositConventions.USD_SHORT_DEPOSIT_T1;
import static com.opengamma.strata.product.fx.type.FxSwapConventions.EUR_USD;
import static com.opengamma.strata.product.swap.type.FixedOvernightSwapConventions.USD_FIXED_1Y_FED_FUND_OIS;
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

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.data.FxRateId;
import com.opengamma.strata.data.ImmutableMarketData;
import com.opengamma.strata.data.ImmutableMarketDataBuilder;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.data.MarketDataId;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.CurveGroupDefinition;
import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.CurveNode;
import com.opengamma.strata.market.curve.InterpolatedNodalCurveDefinition;
import com.opengamma.strata.market.curve.interpolator.CurveExtrapolator;
import com.opengamma.strata.market.curve.interpolator.CurveExtrapolators;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolator;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolators;
import com.opengamma.strata.market.curve.node.FixedOvernightSwapCurveNode;
import com.opengamma.strata.market.curve.node.FxSwapCurveNode;
import com.opengamma.strata.market.curve.node.TermDepositCurveNode;
import com.opengamma.strata.market.observable.QuoteId;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.deposit.DiscountingTermDepositProductPricer;
import com.opengamma.strata.pricer.fx.DiscountingFxSwapProductPricer;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.sensitivity.MarketQuoteSensitivityCalculator;
import com.opengamma.strata.pricer.swap.DiscountingSwapProductPricer;
import com.opengamma.strata.product.ResolvedTrade;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.deposit.ResolvedTermDepositTrade;
import com.opengamma.strata.product.deposit.type.TermDepositTemplate;
import com.opengamma.strata.product.fx.ResolvedFxSwapTrade;
import com.opengamma.strata.product.fx.type.FxSwapTemplate;
import com.opengamma.strata.product.swap.ResolvedSwapTrade;
import com.opengamma.strata.product.swap.type.FixedOvernightSwapTemplate;

/**
 * Test for curve calibration in USD and EUR.
 * The USD curve is obtained by OIS and the EUR one by FX Swaps from USD.
 */
@Test
public class CalibrationZeroRateUsdEur2OisFxTest {

  private static final LocalDate VAL_DATE = LocalDate.of(2015, 11, 2);

  private static final CurveInterpolator INTERPOLATOR_LINEAR = CurveInterpolators.LINEAR;
  private static final CurveExtrapolator EXTRAPOLATOR_FLAT = CurveExtrapolators.FLAT;
  private static final DayCount CURVE_DC = ACT_365F;

  // reference data
  private static final ReferenceData REF_DATA = ReferenceData.standard();

  private static final String SCHEME = "CALIBRATION";

  /** Curve names */
  private static final String USD_DSCON_STR = "USD-DSCON-OIS";
  private static final CurveName USD_DSCON_CURVE_NAME = CurveName.of(USD_DSCON_STR);
  private static final String EUR_DSC_STR = "EUR-DSC-FX";
  private static final CurveName EUR_DSC_CURVE_NAME = CurveName.of(EUR_DSC_STR);
  /** Curves associations to currencies and indices. */
  private static final Map<CurveName, Currency> DSC_NAMES = new HashMap<>();
  private static final Map<CurveName, Set<Index>> IDX_NAMES = new HashMap<>();
  static {
    DSC_NAMES.put(USD_DSCON_CURVE_NAME, USD);
    Set<Index> usdFedFundSet = new HashSet<>();
    usdFedFundSet.add(USD_FED_FUND);
    IDX_NAMES.put(USD_DSCON_CURVE_NAME, usdFedFundSet);
  }

  /** Data FX **/
  private static final FxRate FX_RATE_EUR_USD = FxRate.of(EUR, USD, 1.10);
  /** Data for USD-DSCON curve */
  /* Market values */
  private static final double[] USD_DSC_MARKET_QUOTES = new double[] {
    0.0016, 0.0022,
    0.0013, 0.0016, 0.0020, 0.0026, 0.0033,
    0.0039, 0.0053, 0.0066, 0.0090, 0.0111};
  private static final int USD_DSC_NB_NODES = USD_DSC_MARKET_QUOTES.length;
  private static final String[] USD_DSC_ID_VALUE = new String[] {
    "USD-ON", "USD-TN",
    "USD-OIS-1M", "USD-OIS-2M", "USD-OIS-3M", "USD-OIS-6M", "USD-OIS-9M",
    "USD-OIS-1Y", "USD-OIS-18M", "USD-OIS-2Y", "USD-OIS-3Y", "USD-OIS-4Y"};
  /* Nodes */
  private static final CurveNode[] USD_DSC_NODES = new CurveNode[USD_DSC_NB_NODES];
  /* Tenors */
  private static final int[] USD_DSC_DEPO_OFFSET = new int[] {0, 1 };
  private static final int USD_DSC_NB_DEPO_NODES = USD_DSC_DEPO_OFFSET.length;
  private static final Period[] USD_DSC_OIS_TENORS = new Period[] {
    Period.ofMonths(1), Period.ofMonths(2), Period.ofMonths(3), Period.ofMonths(6), Period.ofMonths(9),
    Period.ofYears(1), Period.ofMonths(18), Period.ofYears(2), Period.ofYears(3), Period.ofYears(4) };
  private static final int USD_DSC_NB_OIS_NODES = USD_DSC_OIS_TENORS.length;
  static {
    USD_DSC_NODES[0] = TermDepositCurveNode.of(TermDepositTemplate.of(Period.ofDays(1), USD_SHORT_DEPOSIT_T0),
        QuoteId.of(StandardId.of(SCHEME, USD_DSC_ID_VALUE[0])));
    USD_DSC_NODES[1] = TermDepositCurveNode.of(TermDepositTemplate.of(Period.ofDays(1), USD_SHORT_DEPOSIT_T1),
        QuoteId.of(StandardId.of(SCHEME, USD_DSC_ID_VALUE[1])));
    for (int i = 0; i < USD_DSC_NB_OIS_NODES; i++) {
      USD_DSC_NODES[USD_DSC_NB_DEPO_NODES + i] = FixedOvernightSwapCurveNode.of(
          FixedOvernightSwapTemplate.of(Period.ZERO, Tenor.of(USD_DSC_OIS_TENORS[i]), USD_FIXED_1Y_FED_FUND_OIS),
          QuoteId.of(StandardId.of(SCHEME, USD_DSC_ID_VALUE[USD_DSC_NB_DEPO_NODES + i])));
    }
  }
  /** Data for EUR-DSC curve */
  /* Market values */
  private static final double[] EUR_DSC_MARKET_QUOTES = new double[] {
    0.0004, 0.0012, 0.0019, 0.0043, 0.0074,
    0.0109, 0.0193, 0.0294, 0.0519, 0.0757};
  private static final int EUR_DSC_NB_NODES = EUR_DSC_MARKET_QUOTES.length;
  private static final String[] EUR_DSC_ID_VALUE = new String[] {
    "EUR-USD-FX-1M", "EUR-USD-FX-2M", "EUR-USD-FX-3M", "EUR-USD-FX-6M", "EUR-USD-FX-9M",
    "EUR-USD-FX-1Y", "EUR-USD-FX-18M", "EUR-USD-FX-2Y", "EUR-USD-FX-3Y", "EUR-USD-FX-4Y"};
  /* Nodes */
  private static final CurveNode[] EUR_DSC_NODES = new CurveNode[EUR_DSC_NB_NODES];
  /* Tenors */
  private static final Period[] EUR_DSC_FX_TENORS = new Period[] {
      Period.ofMonths(1), Period.ofMonths(2), Period.ofMonths(3), Period.ofMonths(6), Period.ofMonths(9),
      Period.ofYears(1), Period.ofMonths(18), Period.ofYears(2), Period.ofYears(3), Period.ofYears(4)};
  private static final int EUR_DSC_NB_FX_NODES = EUR_DSC_FX_TENORS.length;
  static {
    for (int i = 0; i < EUR_DSC_NB_FX_NODES; i++) {
      EUR_DSC_NODES[i] = FxSwapCurveNode.of(
          FxSwapTemplate.of(EUR_DSC_FX_TENORS[i], EUR_USD),
          QuoteId.of(StandardId.of(SCHEME, EUR_DSC_ID_VALUE[i])));
    }
  }

  /** All quotes for the curve calibration */
  private static final ImmutableMarketData ALL_QUOTES;
  static {
    ImmutableMarketDataBuilder builder = ImmutableMarketData.builder(VAL_DATE);
    for (int i = 0; i < USD_DSC_NB_NODES; i++) {
      builder.addValue(QuoteId.of(StandardId.of(SCHEME, USD_DSC_ID_VALUE[i])), USD_DSC_MARKET_QUOTES[i]);
    }
    for (int i = 0; i < EUR_DSC_NB_NODES; i++) {
      builder.addValue(QuoteId.of(StandardId.of(SCHEME, EUR_DSC_ID_VALUE[i])), EUR_DSC_MARKET_QUOTES[i]);
    }
    builder.addValue(FxRateId.of(EUR, USD), FX_RATE_EUR_USD);
    ALL_QUOTES = builder.build();
  }

  private static final DiscountingSwapProductPricer SWAP_PRICER =
      DiscountingSwapProductPricer.DEFAULT;
  private static final DiscountingTermDepositProductPricer DEPO_PRICER =
      DiscountingTermDepositProductPricer.DEFAULT;
  private static final DiscountingFxSwapProductPricer FX_PRICER =
      DiscountingFxSwapProductPricer.DEFAULT;
  private static final MarketQuoteSensitivityCalculator MQC = MarketQuoteSensitivityCalculator.DEFAULT;
  
  private static final CurveCalibrator CALIBRATOR = CurveCalibrator.of(1e-9, 1e-9, 100);

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
  private static final CurveGroupDefinition CURVE_GROUP_CONFIG =
      CurveGroupDefinition.builder()
          .name(CURVE_GROUP_NAME)
          .addCurve(USD_DSC_CURVE_DEFN, USD, USD_FED_FUND)
          .addDiscountCurve(EUR_DSC_CURVE_DEFN, EUR).build();
  
  //-------------------------------------------------------------------------
  public void calibration_present_value_oneGroup() {
    RatesProvider result =
        CALIBRATOR.calibrate(CURVE_GROUP_CONFIG, ALL_QUOTES, REF_DATA);
    assertPresentValue(result);
  }
  
  private void assertPresentValue(RatesProvider result) {
    // Test PV USD;
    List<ResolvedTrade> usdTrades = new ArrayList<>();
    for (int i = 0; i < USD_DSC_NODES.length; i++) {
      usdTrades.add(USD_DSC_NODES[i].resolvedTrade(1d, ALL_QUOTES, REF_DATA));
    }
    // Depo
    for (int i = 0; i < USD_DSC_NB_DEPO_NODES; i++) {
      CurrencyAmount pvDep = DEPO_PRICER.presentValue(
          ((ResolvedTermDepositTrade) usdTrades.get(i)).getProduct(), result);
      assertEquals(pvDep.getAmount(), 0.0, TOLERANCE_PV);
    }
    // OIS
    for (int i = 0; i < USD_DSC_NB_OIS_NODES; i++) {
      MultiCurrencyAmount pvOis = SWAP_PRICER.presentValue(
          ((ResolvedSwapTrade) usdTrades.get(USD_DSC_NB_DEPO_NODES + i)).getProduct(), result);
      assertEquals(pvOis.getAmount(USD).getAmount(), 0.0, TOLERANCE_PV);
    }
    // Test PV EUR;
    List<ResolvedTrade> eurTrades = new ArrayList<>();
    for (int i = 0; i < EUR_DSC_NODES.length; i++) {
      eurTrades.add(EUR_DSC_NODES[i].resolvedTrade(1d, ALL_QUOTES, REF_DATA));
    }
    // Depo
    for (int i = 0; i < EUR_DSC_NB_FX_NODES; i++) {
      MultiCurrencyAmount pvFx = FX_PRICER.presentValue(
          ((ResolvedFxSwapTrade) eurTrades.get(i)).getProduct(), result);
      assertEquals(pvFx.convertedTo(USD, result).getAmount(), 0.0, TOLERANCE_PV);
    }
  }
  
  public void calibration_market_quote_sensitivity_one_group() {
    double shift = 1.0E-6;
    Function<MarketData, RatesProvider> f =
        ov -> CALIBRATOR.calibrate(CURVE_GROUP_CONFIG, ov, REF_DATA);
    calibration_market_quote_sensitivity_check(f, shift);
  }

  private void calibration_market_quote_sensitivity_check(
      Function<MarketData, RatesProvider> calibrator,
      double shift) {

    double notional = 100_000_000.0;
    double fx = 1.1111;
    double fxPts = 0.0012;
    ResolvedFxSwapTrade trade = EUR_USD
        .createTrade(VAL_DATE, Period.ofWeeks(6), Period.ofMonths(5), BuySell.BUY, notional, fx, fxPts, REF_DATA)
        .resolve(REF_DATA);
    RatesProvider result =
        CALIBRATOR.calibrate(CURVE_GROUP_CONFIG, ALL_QUOTES, REF_DATA);
    PointSensitivities pts = FX_PRICER.presentValueSensitivity(trade.getProduct(), result);
    CurrencyParameterSensitivities ps = result.parameterSensitivity(pts);
    CurrencyParameterSensitivities mqs = MQC.sensitivity(ps, result);
    double pvUsd = FX_PRICER.presentValue(trade.getProduct(), result).getAmount(USD).getAmount();
    double pvEur = FX_PRICER.presentValue(trade.getProduct(), result).getAmount(EUR).getAmount();
    double[] mqsUsd1Computed = mqs.getSensitivity(USD_DSCON_CURVE_NAME, USD).getSensitivity().toArray();
    for (int i = 0; i < USD_DSC_NB_NODES; i++) {
      Map<MarketDataId<?>, Object> map = new HashMap<>(ALL_QUOTES.getValues());
      map.put(QuoteId.of(StandardId.of(SCHEME, USD_DSC_ID_VALUE[i])), USD_DSC_MARKET_QUOTES[i] + shift);
      ImmutableMarketData marketData = ImmutableMarketData.of(VAL_DATE, map);
      RatesProvider rpShifted = calibrator.apply(marketData);
      double pvS = FX_PRICER.presentValue(trade.getProduct(), rpShifted).getAmount(USD).getAmount();
      assertEquals(mqsUsd1Computed[i], (pvS - pvUsd) / shift, TOLERANCE_PV_DELTA);
    }
    double[] mqsUsd2Computed = mqs.getSensitivity(USD_DSCON_CURVE_NAME, EUR).getSensitivity().toArray();
    for (int i = 0; i < USD_DSC_NB_NODES; i++) {
      Map<MarketDataId<?>, Object> map = new HashMap<>(ALL_QUOTES.getValues());
      map.put(QuoteId.of(StandardId.of(SCHEME, USD_DSC_ID_VALUE[i])), USD_DSC_MARKET_QUOTES[i] + shift);
      ImmutableMarketData ov = ImmutableMarketData.of(VAL_DATE, map);
      RatesProvider rpShifted = calibrator.apply(ov);
      double pvS = FX_PRICER.presentValue(trade.getProduct(), rpShifted).getAmount(EUR).getAmount();
      assertEquals(mqsUsd2Computed[i], (pvS - pvEur) / shift, TOLERANCE_PV_DELTA);
    }
    double[] mqsEur1Computed = mqs.getSensitivity(EUR_DSC_CURVE_NAME, USD).getSensitivity().toArray();
    for (int i = 0; i < EUR_DSC_NB_NODES; i++) {
      assertEquals(mqsEur1Computed[i], 0.0 , TOLERANCE_PV_DELTA);      
    }
    double[] mqsEur2Computed = mqs.getSensitivity(EUR_DSC_CURVE_NAME, EUR).getSensitivity().toArray();
    for (int i = 0; i < EUR_DSC_NB_NODES; i++) {
      Map<MarketDataId<?>, Object> map = new HashMap<>(ALL_QUOTES.getValues());
      map.put(QuoteId.of(StandardId.of(SCHEME, EUR_DSC_ID_VALUE[i])), EUR_DSC_MARKET_QUOTES[i] + shift);
      ImmutableMarketData marketData = ImmutableMarketData.of(VAL_DATE, map);
      RatesProvider rpShifted = calibrator.apply(marketData);
      double pvS = FX_PRICER.presentValue(trade.getProduct(), rpShifted).getAmount(EUR).getAmount();
      assertEquals(mqsEur2Computed[i], (pvS - pvEur) / shift, TOLERANCE_PV_DELTA, "Node " + i);
    }
  }

}
