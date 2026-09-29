/*
 * Copyright (C) 2026 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.index;

import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.index.OvernightIndices.GBP_SONIA;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.LINEAR;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.index.OvernightIndex;
import com.opengamma.strata.calc.Measure;
import com.opengamma.strata.calc.runner.CalculationParameters;
import com.opengamma.strata.calc.runner.CalculationParametersId;
import com.opengamma.strata.calc.runner.FunctionRequirements;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.data.FieldName;
import com.opengamma.strata.data.ImmutableMarketData;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.data.scenario.CurrencyScenarioArray;
import com.opengamma.strata.data.scenario.MultiCurrencyScenarioArray;
import com.opengamma.strata.data.scenario.ScenarioArray;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveId;
import com.opengamma.strata.market.curve.CurveInfoType;
import com.opengamma.strata.market.curve.CurveParameterSize;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.curve.JacobianCalibrationMatrix;
import com.opengamma.strata.market.model.MoneynessType;
import com.opengamma.strata.market.observable.IndexQuoteId;
import com.opengamma.strata.market.observable.QuoteId;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.surface.InterpolatedNodalSurface;
import com.opengamma.strata.market.surface.Surfaces;
import com.opengamma.strata.market.surface.interpolator.GridSurfaceInterpolator;
import com.opengamma.strata.market.surface.interpolator.SurfaceInterpolator;
import com.opengamma.strata.measure.Measures;
import com.opengamma.strata.measure.rate.RatesMarketDataLookup;
import com.opengamma.strata.pricer.index.NormalOvernightFutureOptionExpirySimpleMoneynessVolatilities;
import com.opengamma.strata.pricer.index.NormalOvernightFutureOptionMarginedTradePricer;
import com.opengamma.strata.pricer.index.OvernightFutureDummyData;
import com.opengamma.strata.pricer.index.OvernightFutureOptionVolatilitiesId;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.sensitivity.MarketQuoteSensitivityCalculator;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.index.OvernightFutureOption;
import com.opengamma.strata.product.index.OvernightFutureOptionTrade;
import com.opengamma.strata.product.index.ResolvedOvernightFutureOptionTrade;

/**
 * Test {@link OvernightFutureOptionTradeCalculationFunction}.
 */
public class OvernightFutureOptionTradeCalculationFunctionTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();

  private static final SurfaceInterpolator INTERPOLATOR_2D = GridSurfaceInterpolator.of(LINEAR, LINEAR);
  private static final DoubleArray TIMES =
      DoubleArray.of(0.25, 0.25, 0.25, 0.25, 0.5, 0.5, 0.5, 0.5, 1, 1, 1, 1);
  private static final DoubleArray MONEYNESS_PRICES =
      DoubleArray.of(-0.02, -0.01, 0, 0.01, -0.02, -0.01, 0, 0.01, -0.02, -0.01, 0, 0.01);
  private static final DoubleArray NORMAL_VOL_PRICES =
      DoubleArray.of(0.01, 0.011, 0.012, 0.010, 0.011, 0.012, 0.013, 0.012, 0.012, 0.013, 0.014, 0.014);
  private static final InterpolatedNodalSurface PARAMETERS_PRICE = InterpolatedNodalSurface.of(
      Surfaces.normalVolatilityByExpirySimpleMoneyness("Price", ACT_365F, MoneynessType.PRICE),
      TIMES,
      MONEYNESS_PRICES,
      NORMAL_VOL_PRICES,
      INTERPOLATOR_2D);

  private static final LocalDate VAL_DATE = date(2015, 2, 17);
  private static final LocalTime VAL_TIME = LocalTime.of(13, 45);
  private static final ZoneId LONDON_ZONE = ZoneId.of("Europe/London");
  private static final ZonedDateTime VAL_DATE_TIME = VAL_DATE.atTime(VAL_TIME).atZone(LONDON_ZONE);

  private static final NormalOvernightFutureOptionExpirySimpleMoneynessVolatilities VOL_SIMPLE_MONEY_PRICE =
      NormalOvernightFutureOptionExpirySimpleMoneynessVolatilities.of(GBP_SONIA, VAL_DATE_TIME, PARAMETERS_PRICE);

  private static final OvernightFutureOption OPTION = OvernightFutureDummyData.OVERNIGHT_FUTURE_OPTION_2;
  private static final LocalDate TRADE_DATE = date(2015, 2, 16);
  private static final long OPTION_QUANTITY = 12345;
  private static final double TRADE_PRICE = 0.0100;
  private static final double SETTLEMENT_PRICE = 0.0120;
  private static final OvernightFutureOptionTrade TRADE = OvernightFutureOptionTrade.builder()
      .info(TradeInfo.builder()
          .tradeDate(TRADE_DATE)
          .build())
      .product(OPTION)
      .quantity(OPTION_QUANTITY)
      .price(TRADE_PRICE)
      .build();

  private static final Currency CURRENCY = Currency.GBP;
  private static final OvernightIndex INDEX = GBP_SONIA;

  private static final CurveId OIS_CURVE_ID = CurveId.of("Default", "OIS");
  private static final OvernightFutureOptionVolatilitiesId VOL_ID =
      OvernightFutureOptionVolatilitiesId.of("OvernightFutureOptionVols.Normal.GBP");
  private static final QuoteId QUOTE_ID_OPTION =
      QuoteId.of(TRADE.getSecurityId().getStandardId(), FieldName.SETTLEMENT_PRICE);
  static final RatesMarketDataLookup RATES_LOOKUP = RatesMarketDataLookup.of(
      ImmutableMap.of(CURRENCY, OIS_CURVE_ID),
      ImmutableMap.of(INDEX, OIS_CURVE_ID));
  static final OvernightFutureOptionMarketDataLookup OPTION_LOOKUP =
      OvernightFutureOptionMarketDataLookup.of(INDEX, VOL_ID);
  private static final CalculationParameters PARAMS = CalculationParameters.of(RATES_LOOKUP, OPTION_LOOKUP);

  //-------------------------------------------------------------------------
  @Test
  public void test_requirementsAndCurrency() {
    OvernightFutureOptionTradeCalculationFunction<OvernightFutureOptionTrade> function =
        OvernightFutureOptionTradeCalculationFunction.TRADE;
    Set<Measure> measures = function.supportedMeasures();
    FunctionRequirements reqs = function.requirements(TRADE, measures, PARAMS, REF_DATA);
    assertThat(reqs.getOutputCurrencies()).containsOnly(CURRENCY);
    assertThat(reqs.getValueRequirements()).isEqualTo(
        ImmutableSet.of(OIS_CURVE_ID, VOL_ID, QUOTE_ID_OPTION));
    assertThat(reqs.getTimeSeriesRequirements()).isEqualTo(ImmutableSet.of(IndexQuoteId.of(INDEX)));
    assertThat(function.naturalCurrency(TRADE, REF_DATA)).isEqualTo(CURRENCY);
  }

  @Test
  public void test_simpleMeasures() {
    MarketQuoteSensitivityCalculator marketQuoteSensitivityCalculator = MarketQuoteSensitivityCalculator.DEFAULT;
    OvernightFutureOptionTradeCalculationFunction<OvernightFutureOptionTrade> function =
        OvernightFutureOptionTradeCalculationFunction.TRADE;
    ScenarioMarketData md = marketData();
    RatesProvider provider = RATES_LOOKUP.ratesProvider(md.scenario(0));
    NormalOvernightFutureOptionMarginedTradePricer pricer = NormalOvernightFutureOptionMarginedTradePricer.DEFAULT;
    ResolvedOvernightFutureOptionTrade resolved = TRADE.resolve(REF_DATA);
    CurrencyAmount expectedPv = pricer.presentValue(resolved, provider, VOL_SIMPLE_MONEY_PRICE, SETTLEMENT_PRICE);
    CurrencyParameterSensitivities expectedMqDelta = marketQuoteSensitivityCalculator.sensitivity(
        provider.parameterSensitivity(pricer.presentValueSensitivityRates(
            resolved,
            provider,
            VOL_SIMPLE_MONEY_PRICE)),
        provider).multipliedBy(1e-4);
    CurrencyParameterSensitivities expectedVega = VOL_SIMPLE_MONEY_PRICE.parameterSensitivity(
        pricer.presentValueSensitivityModelParamsVolatility(resolved, provider, VOL_SIMPLE_MONEY_PRICE));

    Set<Measure> measures = ImmutableSet.of(
        Measures.PRESENT_VALUE,
        Measures.RESOLVED_TARGET,
        Measures.PV01_MARKET_QUOTE_SUM,
        Measures.PV01_MARKET_QUOTE_BUCKETED,
        Measures.VEGA_MARKET_QUOTE_BUCKETED);
    assertThat(function.calculate(TRADE, measures, PARAMS, md, REF_DATA))
        .containsEntry(
            Measures.PRESENT_VALUE, Result.success(CurrencyScenarioArray.of(ImmutableList.of(expectedPv))))
        .containsEntry(
            Measures.RESOLVED_TARGET, Result.success(resolved))
        .containsEntry(
            Measures.PV01_MARKET_QUOTE_SUM, Result.success(MultiCurrencyScenarioArray.of(ImmutableList.of(expectedMqDelta.total()))))
        .containsEntry(
            Measures.PV01_MARKET_QUOTE_BUCKETED, Result.success(ScenarioArray.of(ImmutableList.of(expectedMqDelta))))
        .containsEntry(
            Measures.VEGA_MARKET_QUOTE_BUCKETED, Result.success(ScenarioArray.of(ImmutableList.of(expectedVega))));
  }

  //-------------------------------------------------------------------------
  static ScenarioMarketData marketData() {
    Curve oisCurve = InterpolatedNodalCurve.of(
        Curves.zeroRates(OIS_CURVE_ID.getCurveName(), ACT_360).withInfo(
            CurveInfoType.JACOBIAN,
            JacobianCalibrationMatrix.of(
                ImmutableList.of(
                    CurveParameterSize.of(OIS_CURVE_ID.getCurveName(), 2)),
                DoubleMatrix.ofUnsafe(new double[][]{{0.95, 0.0}, {0.0, 0.95}}))),
        DoubleArray.of(0.1, 0.2),
        DoubleArray.of(0.01, 0.01),
        LINEAR);
    MarketData marketData = ImmutableMarketData.of(
        VAL_DATE,
        ImmutableMap.of(
            OIS_CURVE_ID, oisCurve,
            VOL_ID, VOL_SIMPLE_MONEY_PRICE,
            QUOTE_ID_OPTION, SETTLEMENT_PRICE,
            CalculationParametersId.STANDARD, PARAMS));
    return ScenarioMarketData.of(1, marketData);
  }

}
