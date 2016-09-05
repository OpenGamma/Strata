/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.index;

import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_2M;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.LINEAR;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Set;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.calc.Measure;
import com.opengamma.strata.calc.runner.CalculationParameters;
import com.opengamma.strata.calc.runner.FunctionRequirements;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.data.FieldName;
import com.opengamma.strata.data.scenario.CurrencyScenarioArray;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.market.curve.ConstantCurve;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveId;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.model.MoneynessType;
import com.opengamma.strata.market.observable.IndexQuoteId;
import com.opengamma.strata.market.observable.QuoteId;
import com.opengamma.strata.market.surface.InterpolatedNodalSurface;
import com.opengamma.strata.market.surface.Surfaces;
import com.opengamma.strata.market.surface.interpolator.GridSurfaceInterpolator;
import com.opengamma.strata.market.surface.interpolator.SurfaceInterpolator;
import com.opengamma.strata.measure.Measures;
import com.opengamma.strata.measure.curve.TestMarketDataMap;
import com.opengamma.strata.measure.rate.RatesMarketDataLookup;
import com.opengamma.strata.pricer.index.IborFutureDummyData;
import com.opengamma.strata.pricer.index.IborFutureOptionVolatilitiesId;
import com.opengamma.strata.pricer.index.NormalIborFutureOptionExpirySimpleMoneynessVolatilities;
import com.opengamma.strata.pricer.index.NormalIborFutureOptionMarginedTradePricer;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.index.IborFutureOption;
import com.opengamma.strata.product.index.IborFutureOptionTrade;
import com.opengamma.strata.product.index.ResolvedIborFutureOptionTrade;

/**
 * Test {@link IborFutureOptionTradeCalculationFunction}.
 */
@Test
public class IborFutureOptionTradeCalculationFunctionTest {

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

  private static final NormalIborFutureOptionExpirySimpleMoneynessVolatilities VOL_SIMPLE_MONEY_PRICE =
      NormalIborFutureOptionExpirySimpleMoneynessVolatilities.of(GBP_LIBOR_2M, VAL_DATE_TIME, PARAMETERS_PRICE);

  private static final IborFutureOption OPTION = IborFutureDummyData.IBOR_FUTURE_OPTION_2;
  private static final LocalDate TRADE_DATE = date(2015, 2, 16);
  private static final long OPTION_QUANTITY = 12345;
  private static final double TRADE_PRICE = 0.0100;
  private static final double SETTLEMENT_PRICE = 0.0120;
  private static final IborFutureOptionTrade TRADE = IborFutureOptionTrade.builder()
      .info(TradeInfo.builder()
          .tradeDate(TRADE_DATE)
          .build())
      .product(OPTION)
      .quantity(OPTION_QUANTITY)
      .price(TRADE_PRICE)
      .build();

  private static final Currency CURRENCY = Currency.GBP;
  private static final IborIndex INDEX = GBP_LIBOR_2M;

  private static final CurveId DISCOUNT_CURVE_ID = CurveId.of("Default", "Discount");
  private static final CurveId FORWARD_CURVE_ID = CurveId.of("Default", "Forward");
  private static final IborFutureOptionVolatilitiesId VOL_ID =
      IborFutureOptionVolatilitiesId.of("IborFutureOptionVols.Normal.USD");
  private static final QuoteId QUOTE_ID_OPTION =
      QuoteId.of(TRADE.getSecurityId().getStandardId(), FieldName.SETTLEMENT_PRICE);
  static final RatesMarketDataLookup RATES_LOOKUP = RatesMarketDataLookup.of(
      ImmutableMap.of(CURRENCY, DISCOUNT_CURVE_ID),
      ImmutableMap.of(INDEX, FORWARD_CURVE_ID));
  static final IborFutureOptionMarketDataLookup OPTION_LOOKUP = IborFutureOptionMarketDataLookup.of(INDEX, VOL_ID);
  private static final CalculationParameters PARAMS = CalculationParameters.of(RATES_LOOKUP, OPTION_LOOKUP);

  //-------------------------------------------------------------------------
  public void test_requirementsAndCurrency() {
    IborFutureOptionTradeCalculationFunction function = new IborFutureOptionTradeCalculationFunction();
    Set<Measure> measures = function.supportedMeasures();
    FunctionRequirements reqs = function.requirements(TRADE, measures, PARAMS, REF_DATA);
    assertThat(reqs.getOutputCurrencies()).containsOnly(CURRENCY);
    assertThat(reqs.getValueRequirements()).isEqualTo(
        ImmutableSet.of(DISCOUNT_CURVE_ID, FORWARD_CURVE_ID, VOL_ID, QUOTE_ID_OPTION));
    assertThat(reqs.getTimeSeriesRequirements()).isEqualTo(ImmutableSet.of(IndexQuoteId.of(INDEX)));
    assertThat(function.naturalCurrency(TRADE, REF_DATA)).isEqualTo(CURRENCY);
  }

  public void test_simpleMeasures() {
    IborFutureOptionTradeCalculationFunction function = new IborFutureOptionTradeCalculationFunction();
    ScenarioMarketData md = marketData();
    RatesProvider provider = RATES_LOOKUP.ratesProvider(md.scenario(0));
    NormalIborFutureOptionMarginedTradePricer pricer = NormalIborFutureOptionMarginedTradePricer.DEFAULT;
    ResolvedIborFutureOptionTrade resolved = TRADE.resolve(REF_DATA);
    CurrencyAmount expectedPv = pricer.presentValue(resolved, provider, VOL_SIMPLE_MONEY_PRICE, SETTLEMENT_PRICE);

    Set<Measure> measures = ImmutableSet.of(Measures.PRESENT_VALUE, Measures.RESOLVED_TARGET);
    assertThat(function.calculate(TRADE, measures, PARAMS, md, REF_DATA))
        .containsEntry(
            Measures.PRESENT_VALUE, Result.success(CurrencyScenarioArray.of(ImmutableList.of(expectedPv))))
        .containsEntry(
            Measures.RESOLVED_TARGET, Result.success(resolved));
  }

  //-------------------------------------------------------------------------
  static ScenarioMarketData marketData() {
    Curve curve = ConstantCurve.of(Curves.discountFactors("Test", ACT_360), 0.99);
    TestMarketDataMap md = new TestMarketDataMap(
        VAL_DATE,
        ImmutableMap.of(
            DISCOUNT_CURVE_ID, curve,
            FORWARD_CURVE_ID, curve,
            VOL_ID, VOL_SIMPLE_MONEY_PRICE,
            QUOTE_ID_OPTION, SETTLEMENT_PRICE),
        ImmutableMap.of());
    return md;
  }

}
