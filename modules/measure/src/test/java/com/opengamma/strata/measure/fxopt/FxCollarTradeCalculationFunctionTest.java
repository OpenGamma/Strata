/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.fxopt;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.product.common.LongShort.LONG;
import static com.opengamma.strata.product.common.LongShort.SHORT;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.AdjustablePayment;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxMatrix;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.calc.Measure;
import com.opengamma.strata.calc.runner.CalculationParameters;
import com.opengamma.strata.calc.runner.FunctionRequirements;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.data.FxRateId;
import com.opengamma.strata.data.scenario.CurrencyScenarioArray;
import com.opengamma.strata.data.scenario.MultiCurrencyScenarioArray;
import com.opengamma.strata.data.scenario.ScenarioArray;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.market.curve.ConstantCurve;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveId;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.measure.Measures;
import com.opengamma.strata.measure.curve.TestMarketDataMap;
import com.opengamma.strata.measure.rate.RatesMarketDataLookup;
import com.opengamma.strata.pricer.datasets.RatesProviderDataSets;
import com.opengamma.strata.pricer.fx.RatesProviderFxDataSets;
import com.opengamma.strata.pricer.fxopt.BlackFxOptionSmileVolatilities;
import com.opengamma.strata.pricer.fxopt.DiscountingFxCollarTradePricer;
import com.opengamma.strata.pricer.fxopt.FxOptionVolatilitiesId;
import com.opengamma.strata.pricer.fxopt.FxOptionVolatilitiesName;
import com.opengamma.strata.pricer.fxopt.InterpolatedStrikeSmileDeltaTermStructure;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.fx.FxSingle;
import com.opengamma.strata.product.fxopt.FxCollar;
import com.opengamma.strata.product.fxopt.FxCollarTrade;
import com.opengamma.strata.product.fxopt.FxVanillaOption;
import com.opengamma.strata.product.fxopt.ResolvedFxCollarTrade;

/**
 * Test {@link FxCollarTradeCalculationFunction}.
 */
public class FxCollarTradeCalculationFunctionTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate VAL_DATE = RatesProviderDataSets.VAL_DATE_2014_01_22;
  private static final FxMatrix FX_MATRIX = RatesProviderFxDataSets.fxMatrix();
  private static final ZoneId ZONE = ZoneId.of("Z");

  private static final LocalDate PAYMENT_DATE = LocalDate.of(2014, 5, 13);
  private static final double NOTIONAL = 1.0e6;
  private static final CurrencyAmount EUR_AMOUNT = CurrencyAmount.of(EUR, NOTIONAL);
  private static final CurrencyAmount USD_AMOUNT = CurrencyAmount.of(USD, -NOTIONAL * FX_MATRIX.fxRate(EUR, USD));
  private static final FxSingle FX_PRODUCT = FxSingle.of(EUR_AMOUNT, USD_AMOUNT, PAYMENT_DATE);

  private static final FxVanillaOption OPTION_PRODUCT1 = FxVanillaOption.builder()
      .longShort(SHORT)
      .expiryDate(LocalDate.of(2014, 5, 9))
      .expiryTime(LocalTime.of(13, 10))
      .expiryZone(ZONE)
      .underlying(FX_PRODUCT)
      .build();
  private static final FxVanillaOption OPTION_PRODUCT2 = FxVanillaOption.builder()
      .longShort(LONG)
      .expiryDate(LocalDate.of(2014, 5, 9))
      .expiryTime(LocalTime.of(13, 10))
      .expiryZone(ZONE)
      .underlying(FX_PRODUCT)
      .build();

  private static final TradeInfo TRADE_INFO = TradeInfo.builder().tradeDate(VAL_DATE).build();
  private static final LocalDate CASH_SETTLE_DATE = LocalDate.of(2014, 1, 25);
  private static final AdjustablePayment PREMIUM = AdjustablePayment.of(EUR, NOTIONAL * 0.027, CASH_SETTLE_DATE);
  private static final FxCollar COLLAR_PRODUCT = FxCollar.of(OPTION_PRODUCT1, OPTION_PRODUCT2);
  public static final FxCollarTrade TRADE = FxCollarTrade.of(TRADE_INFO, COLLAR_PRODUCT, PREMIUM);

  private static final CurveId DISCOUNT_CURVE_EUR_ID = CurveId.of("Default", "Discount-EUR");
  private static final CurveId DISCOUNT_CURVE_USD_ID = CurveId.of("Default", "Discount-USD");
  static final RatesMarketDataLookup RATES_LOOKUP = RatesMarketDataLookup.of(
      ImmutableMap.of(EUR, DISCOUNT_CURVE_EUR_ID, USD, DISCOUNT_CURVE_USD_ID),
      ImmutableMap.of());
  private static final CurrencyPair CURRENCY_PAIR = CurrencyPair.of(EUR, USD);
  private static final FxOptionVolatilitiesId VOL_ID = FxOptionVolatilitiesId.of("EUR-USD");
  public static final FxOptionMarketDataLookup FX_OPTION_LOOKUP = FxOptionMarketDataLookup.of(CURRENCY_PAIR, VOL_ID);
  private static final CalculationParameters PARAMS = CalculationParameters.of(RATES_LOOKUP, FX_OPTION_LOOKUP);

  private static final DoubleArray TIME_TO_EXPIRY = DoubleArray.of(0.01, 0.252, 0.501, 1.0, 2.0, 5.0);
  private static final DoubleArray ATM = DoubleArray.of(0.175, 0.185, 0.18, 0.17, 0.16, 0.16);
  private static final DoubleArray DELTA = DoubleArray.of(0.10, 0.25);
  private static final DoubleMatrix RISK_REVERSAL = DoubleMatrix.ofUnsafe(new double[][] {
      {-0.010, -0.0050}, {-0.011, -0.0060}, {-0.012, -0.0070},
      {-0.013, -0.0080}, {-0.014, -0.0090}, {-0.014, -0.0090}});
  private static final DoubleMatrix STRANGLE = DoubleMatrix.ofUnsafe(new double[][] {
      {0.0300, 0.0100}, {0.0310, 0.0110}, {0.0320, 0.0120},
      {0.0330, 0.0130}, {0.0340, 0.0140}, {0.0340, 0.0140}});
  private static final InterpolatedStrikeSmileDeltaTermStructure SMILE_TERM =
      InterpolatedStrikeSmileDeltaTermStructure.of(TIME_TO_EXPIRY, DELTA, ATM, RISK_REVERSAL, STRANGLE, ACT_365F);
  public static final BlackFxOptionSmileVolatilities VOLS =
      BlackFxOptionSmileVolatilities.of(
          FxOptionVolatilitiesName.of("Test"),
          CURRENCY_PAIR,
          VAL_DATE.atStartOfDay(ZoneOffset.UTC),
          SMILE_TERM);
  public static final ResolvedFxCollarTrade RTRADE = TRADE.resolve(REF_DATA);

  @Test
  public void test_requirementsAndCurrency() {
    FxCollarTradeCalculationFunction function = new FxCollarTradeCalculationFunction();
    Set<Measure> measures = function.supportedMeasures();
    FunctionRequirements reqs = function.requirements(TRADE, measures, PARAMS, REF_DATA);
    assertThat(reqs.getOutputCurrencies()).containsExactly(EUR, USD);
    assertThat(reqs.getValueRequirements()).isEqualTo(
        ImmutableSet.of(DISCOUNT_CURVE_EUR_ID, DISCOUNT_CURVE_USD_ID, VOL_ID));
    assertThat(reqs.getTimeSeriesRequirements()).isEmpty();
    assertThat(function.naturalCurrency(TRADE, REF_DATA)).isEqualTo(EUR);
  }

  @Test
  public void test_simpleMeasures() {
    FxCollarTradeCalculationFunction function = new FxCollarTradeCalculationFunction();
    ScenarioMarketData md = marketData();
    RatesProvider provider = RATES_LOOKUP.ratesProvider(md.scenario(0));
    DiscountingFxCollarTradePricer pricer = DiscountingFxCollarTradePricer.DEFAULT;
    MultiCurrencyAmount expectedPv = pricer.presentValue(RTRADE, provider, VOLS);
    MultiCurrencyAmount expectedCurrencyExp = pricer.currencyExposure(RTRADE, provider, VOLS);
    CurrencyAmount expectedCash = pricer.currentCash(RTRADE, VAL_DATE);

    Set<Measure> measures = ImmutableSet.of(
        Measures.PRESENT_VALUE,
        Measures.CURRENCY_EXPOSURE,
        Measures.CURRENT_CASH,
        Measures.RESOLVED_TARGET);
    assertThat(function.calculate(TRADE, measures, PARAMS, md, REF_DATA))
        .containsEntry(
            Measures.PRESENT_VALUE, Result.success(MultiCurrencyScenarioArray.of(ImmutableList.of(expectedPv))))
        .containsEntry(
            Measures.CURRENCY_EXPOSURE, Result.success(MultiCurrencyScenarioArray.of(ImmutableList.of(expectedCurrencyExp))))
        .containsEntry(
            Measures.CURRENT_CASH, Result.success(CurrencyScenarioArray.of(ImmutableList.of(expectedCash))));
  }

  @Test
  public void test_pv01() {
    FxCollarTradeCalculationFunction function = new FxCollarTradeCalculationFunction();
    ScenarioMarketData md = marketData();
    RatesProvider provider = RATES_LOOKUP.ratesProvider(md.scenario(0));
    DiscountingFxCollarTradePricer pricer = DiscountingFxCollarTradePricer.DEFAULT;
    PointSensitivities pvPointSens = pricer.presentValueSensitivityRatesStickyStrike(RTRADE, provider, VOLS);
    CurrencyParameterSensitivities pvParamSens = provider.parameterSensitivity(pvPointSens);
    MultiCurrencyAmount expectedPv01 = pvParamSens.total().multipliedBy(1e-4);
    CurrencyParameterSensitivities expectedBucketedPv01 = pvParamSens.multipliedBy(1e-4);

    Set<Measure> measures = ImmutableSet.of(
        Measures.PV01_CALIBRATED_SUM,
        Measures.PV01_CALIBRATED_BUCKETED);
    assertThat(function.calculate(TRADE, measures, PARAMS, md, REF_DATA))
        .containsEntry(
            Measures.PV01_CALIBRATED_SUM, Result.success(MultiCurrencyScenarioArray.of(ImmutableList.of(expectedPv01))))
        .containsEntry(
            Measures.PV01_CALIBRATED_BUCKETED, Result.success(ScenarioArray.of(ImmutableList.of(expectedBucketedPv01))));
  }

  static ScenarioMarketData marketData() {
    Curve curve1 = ConstantCurve.of(Curves.discountFactors("Test", ACT_360), 0.992);
    Curve curve2 = ConstantCurve.of(Curves.discountFactors("Test", ACT_360), 0.991);
    TestMarketDataMap md = new TestMarketDataMap(
        VAL_DATE,
        ImmutableMap.of(
            DISCOUNT_CURVE_EUR_ID, curve1,
            DISCOUNT_CURVE_USD_ID, curve2,
            VOL_ID, VOLS,
            FxRateId.of(EUR, USD), FxRate.of(EUR, USD, 1.62)),
        ImmutableMap.of());
    return md;
  }
}
