/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.bond;

import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.LINEAR;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.calc.Measure;
import com.opengamma.strata.calc.runner.CalculationParameters;
import com.opengamma.strata.calc.runner.FunctionRequirements;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.data.FieldName;
import com.opengamma.strata.data.scenario.CurrencyScenarioArray;
import com.opengamma.strata.data.scenario.MultiCurrencyScenarioArray;
import com.opengamma.strata.data.scenario.ScenarioArray;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.ConstantCurve;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveId;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.observable.QuoteId;
import com.opengamma.strata.market.option.LogMoneynessStrike;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.surface.DefaultSurfaceMetadata;
import com.opengamma.strata.market.surface.InterpolatedNodalSurface;
import com.opengamma.strata.market.surface.SurfaceMetadata;
import com.opengamma.strata.market.surface.SurfaceName;
import com.opengamma.strata.market.surface.interpolator.GridSurfaceInterpolator;
import com.opengamma.strata.market.surface.interpolator.SurfaceInterpolator;
import com.opengamma.strata.measure.Measures;
import com.opengamma.strata.measure.curve.TestMarketDataMap;
import com.opengamma.strata.pricer.bond.BlackBondFutureExpiryLogMoneynessVolatilities;
import com.opengamma.strata.pricer.bond.BlackBondFutureOptionMarginedTradePricer;
import com.opengamma.strata.pricer.bond.BondDataSets;
import com.opengamma.strata.pricer.bond.BondFutureVolatilitiesId;
import com.opengamma.strata.pricer.bond.LegalEntityDiscountingProvider;
import com.opengamma.strata.pricer.bond.LegalEntityGroup;
import com.opengamma.strata.pricer.bond.RepoGroup;
import com.opengamma.strata.pricer.common.GenericVolatilitySurfaceYearFractionParameterMetadata;
import com.opengamma.strata.pricer.datasets.LegalEntityDiscountingProviderDataSets;
import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.bond.BondFutureOption;
import com.opengamma.strata.product.bond.BondFutureOptionTrade;
import com.opengamma.strata.product.bond.ResolvedBondFutureOptionTrade;

/**
 * Test {@link BondFutureTradeCalculationFunction}.
 */
@Test
public class BondFutureOptionTradeCalculationFunctionTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();

  // product and trade
  private static final BondFutureOption PRODUCT = BondDataSets.FUTURE_OPTION_PRODUCT_EUR_115;
  private static final BondFutureOptionTrade TRADE = BondDataSets.FUTURE_OPTION_TRADE_EUR;
  public static final ResolvedBondFutureOptionTrade RTRADE = TRADE.resolve(REF_DATA);
  // curves
  private static final LegalEntityDiscountingProvider LED_PROVIDER =
      LegalEntityDiscountingProviderDataSets.ISSUER_REPO_ZERO_EUR;
  // vol surface
  private static final SurfaceInterpolator INTERPOLATOR_2D = GridSurfaceInterpolator.of(LINEAR, LINEAR);
  private static final DoubleArray TIME = DoubleArray.of(0.20, 0.20, 0.20, 0.20, 0.20, 0.45, 0.45, 0.45, 0.45, 0.45);
  private static final DoubleArray MONEYNESS =
      DoubleArray.of(-0.050, -0.005, 0.000, 0.005, 0.050, -0.050, -0.005, 0.000, 0.005, 0.050);
  private static final DoubleArray VOL = DoubleArray.of(0.50, 0.49, 0.47, 0.48, 0.51, 0.45, 0.44, 0.42, 0.43, 0.46);
  private static final SurfaceMetadata METADATA;
  static {
    List<GenericVolatilitySurfaceYearFractionParameterMetadata> list = new ArrayList<>();
    int nData = TIME.size();
    for (int i = 0; i < nData; ++i) {
      GenericVolatilitySurfaceYearFractionParameterMetadata parameterMetadata =
          GenericVolatilitySurfaceYearFractionParameterMetadata.of(
              TIME.get(i), LogMoneynessStrike.of(MONEYNESS.get(i)));
      list.add(parameterMetadata);
    }
    METADATA = DefaultSurfaceMetadata.builder()
        .surfaceName(SurfaceName.of("GOVT1-BOND-FUT-VOL"))
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.LOG_MONEYNESS)
        .zValueType(ValueType.BLACK_VOLATILITY)
        .parameterMetadata(list)
        .dayCount(ACT_365F)
        .build();
  }
  private static final InterpolatedNodalSurface SURFACE =
      InterpolatedNodalSurface.of(METADATA, TIME, MONEYNESS, VOL, INTERPOLATOR_2D);
  private static final LocalDate VAL_DATE = LED_PROVIDER.getValuationDate();
  private static final LocalTime VAL_TIME = LocalTime.of(0, 0);
  private static final ZoneId ZONE = PRODUCT.getExpiry().getZone();
  private static final ZonedDateTime VAL_DATE_TIME = VAL_DATE.atTime(VAL_TIME).atZone(ZONE);
  private static final BlackBondFutureExpiryLogMoneynessVolatilities VOLS =
      BlackBondFutureExpiryLogMoneynessVolatilities.of(VAL_DATE_TIME, SURFACE);
  private static final double SETTLE_PRICE = 0.01;

  private static final StandardId ISSUER_ID = PRODUCT.getUnderlyingFuture().getDeliveryBasket().get(0).getLegalEntityId();
  private static final SecurityId FUTURE_SEC_ID = PRODUCT.getUnderlyingFuture().getSecurityId();
  private static final RepoGroup REPO_GROUP = RepoGroup.of("Repo");
  private static final LegalEntityGroup ISSUER_GROUP = LegalEntityGroup.of("Issuer");
  private static final Currency CURRENCY = TRADE.getProduct().getCurrency();
  private static final QuoteId QUOTE_ID = QuoteId.of(PRODUCT.getSecurityId().getStandardId(), FieldName.SETTLEMENT_PRICE);
  private static final CurveId REPO_CURVE_ID = CurveId.of("Default", "Repo");
  private static final CurveId ISSUER_CURVE_ID = CurveId.of("Default", "Issuer");
  private static final BondFutureVolatilitiesId VOLS_ID = BondFutureVolatilitiesId.of("Vols");
  public static final LegalEntityDiscountingMarketDataLookup LED_LOOKUP = LegalEntityDiscountingMarketDataLookup.of(
      ImmutableMap.of(ISSUER_ID, REPO_GROUP),
      ImmutableMap.of(Pair.of(REPO_GROUP, CURRENCY), REPO_CURVE_ID),
      ImmutableMap.of(ISSUER_ID, ISSUER_GROUP),
      ImmutableMap.of(Pair.of(ISSUER_GROUP, CURRENCY), ISSUER_CURVE_ID));
  public static final BondFutureOptionMarketDataLookup VOL_LOOKUP = BondFutureOptionMarketDataLookup.of(
      ImmutableMap.of(FUTURE_SEC_ID, VOLS_ID));
  private static final CalculationParameters PARAMS = CalculationParameters.of(LED_LOOKUP, VOL_LOOKUP);

  //-------------------------------------------------------------------------
  public void test_requirementsAndCurrency() {
    BondFutureOptionTradeCalculationFunction function = new BondFutureOptionTradeCalculationFunction();
    Set<Measure> measures = function.supportedMeasures();
    FunctionRequirements reqs = function.requirements(TRADE, measures, PARAMS, REF_DATA);
    assertThat(reqs.getOutputCurrencies()).containsOnly(CURRENCY);
    assertThat(reqs.getValueRequirements()).isEqualTo(
        ImmutableSet.of(QUOTE_ID, REPO_CURVE_ID, ISSUER_CURVE_ID, VOLS_ID));
    assertThat(reqs.getTimeSeriesRequirements()).isEqualTo(ImmutableSet.of());
    assertThat(function.naturalCurrency(TRADE, REF_DATA)).isEqualTo(CURRENCY);
  }

  public void test_simpleMeasures() {
    BondFutureOptionTradeCalculationFunction function = new BondFutureOptionTradeCalculationFunction();
    ScenarioMarketData md = marketData();
    LegalEntityDiscountingProvider provider = LED_LOOKUP.marketDataView(md.scenario(0)).discountingProvider();
    BlackBondFutureOptionMarginedTradePricer pricer = BlackBondFutureOptionMarginedTradePricer.DEFAULT;
    CurrencyAmount expectedPv = pricer.presentValue(RTRADE, provider, VOLS, SETTLE_PRICE);
    MultiCurrencyAmount expectedCurrencyExposure = pricer.currencyExposure(RTRADE, provider, VOLS, SETTLE_PRICE);

    Set<Measure> measures = ImmutableSet.of(
        Measures.PRESENT_VALUE,
        Measures.CURRENCY_EXPOSURE,
        Measures.RESOLVED_TARGET);
    assertThat(function.calculate(TRADE, measures, PARAMS, md, REF_DATA))
        .containsEntry(
            Measures.PRESENT_VALUE, Result.success(CurrencyScenarioArray.of(ImmutableList.of(expectedPv))))
        .containsEntry(
            Measures.CURRENCY_EXPOSURE, Result.success(MultiCurrencyScenarioArray.of(ImmutableList.of(expectedCurrencyExposure))))
        .containsEntry(
            Measures.RESOLVED_TARGET, Result.success(RTRADE));
  }

  public void test_pv01() {
    BondFutureOptionTradeCalculationFunction function = new BondFutureOptionTradeCalculationFunction();
    ScenarioMarketData md = marketData();
    LegalEntityDiscountingProvider provider = LED_LOOKUP.marketDataView(md.scenario(0)).discountingProvider();
    BlackBondFutureOptionMarginedTradePricer pricer = BlackBondFutureOptionMarginedTradePricer.DEFAULT;
    PointSensitivities pvPointSens = pricer.presentValueSensitivityRates(RTRADE, provider, VOLS);
    CurrencyParameterSensitivities pvParamSens = provider.parameterSensitivity(pvPointSens);
    MultiCurrencyAmount expectedPv01Cal = pvParamSens.total().multipliedBy(1e-4);
    CurrencyParameterSensitivities expectedPv01CalBucketed = pvParamSens.multipliedBy(1e-4);

    Set<Measure> measures = ImmutableSet.of(
        Measures.PV01_CALIBRATED_SUM,
        Measures.PV01_CALIBRATED_BUCKETED);
    assertThat(function.calculate(TRADE, measures, PARAMS, md, REF_DATA))
        .containsEntry(
            Measures.PV01_CALIBRATED_SUM, Result.success(MultiCurrencyScenarioArray.of(ImmutableList.of(expectedPv01Cal))))
        .containsEntry(
            Measures.PV01_CALIBRATED_BUCKETED, Result.success(ScenarioArray.of(ImmutableList.of(expectedPv01CalBucketed))));
  }

  //-------------------------------------------------------------------------
  static ScenarioMarketData marketData() {
    Curve curve = ConstantCurve.of(Curves.discountFactors("Test", ACT_360), 0.99);
    return new TestMarketDataMap(
        VAL_DATE,
        ImmutableMap.of(
            REPO_CURVE_ID, curve,
            ISSUER_CURVE_ID, curve,
            QUOTE_ID, SETTLE_PRICE * 100,
            VOLS_ID, VOLS),
        ImmutableMap.of());
  }

}
