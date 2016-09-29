/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.capfloor;

import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.index.IborIndices.EUR_EURIBOR_6M;
import static com.opengamma.strata.collect.TestHelper.dateUtc;
import static com.opengamma.strata.product.common.PayReceive.PAY;
import static com.opengamma.strata.product.common.PayReceive.RECEIVE;
import static com.opengamma.strata.product.common.PutCall.CALL;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Set;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.value.ValueSchedule;
import com.opengamma.strata.calc.Measure;
import com.opengamma.strata.calc.runner.CalculationParameters;
import com.opengamma.strata.calc.runner.FunctionRequirements;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.data.scenario.MultiCurrencyScenarioArray;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.market.curve.ConstantCurve;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveId;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.observable.IndexQuoteId;
import com.opengamma.strata.measure.Measures;
import com.opengamma.strata.measure.curve.TestMarketDataMap;
import com.opengamma.strata.measure.rate.RatesMarketDataLookup;
import com.opengamma.strata.pricer.capfloor.IborCapFloorDataSet;
import com.opengamma.strata.pricer.capfloor.IborCapletFloorletDataSet;
import com.opengamma.strata.pricer.capfloor.IborCapletFloorletVolatilitiesId;
import com.opengamma.strata.pricer.capfloor.NormalIborCapFloorTradePricer;
import com.opengamma.strata.pricer.capfloor.NormalIborCapletFloorletExpiryStrikeVolatilities;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.capfloor.IborCapFloor;
import com.opengamma.strata.product.capfloor.IborCapFloorLeg;
import com.opengamma.strata.product.capfloor.IborCapFloorTrade;
import com.opengamma.strata.product.capfloor.ResolvedIborCapFloorTrade;
import com.opengamma.strata.product.swap.SwapLeg;

/**
 * Test {@link IborCapFloorTradeCalculationFunction}.
 */
@Test
public class IborCapFloorTradeCalculationFunctionTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final double NOTIONAL_VALUE = 1.0e6;
  private static final ValueSchedule NOTIONAL = ValueSchedule.of(NOTIONAL_VALUE);
  private static final LocalDate START = LocalDate.of(2015, 10, 21);
  private static final LocalDate END = LocalDate.of(2020, 10, 21);
  private static final double STRIKE_VALUE = 0.0105;
  private static final ValueSchedule STRIKE = ValueSchedule.of(STRIKE_VALUE);
  private static final IborCapFloorLeg CAP_LEG =
      IborCapFloorDataSet.createCapFloorLegUnresolved(EUR_EURIBOR_6M, START, END, STRIKE, NOTIONAL, CALL, RECEIVE);
  private static final SwapLeg PAY_LEG =
      IborCapFloorDataSet.createFixedPayLegUnresolved(EUR_EURIBOR_6M, START, END, 0.0395, NOTIONAL_VALUE, PAY);
  private static final IborCapFloor CAP_TWO_LEGS = IborCapFloor.of(CAP_LEG, PAY_LEG);
  private static final ZonedDateTime VALUATION = dateUtc(2015, 8, 20);
  static final NormalIborCapletFloorletExpiryStrikeVolatilities VOLS = IborCapletFloorletDataSet
      .createNormalVolatilities(VALUATION, EUR_EURIBOR_6M);
  private static final TradeInfo TRADE_INFO = TradeInfo.builder().tradeDate(VALUATION.toLocalDate()).build();
  private static final IborCapFloorTrade TRADE = IborCapFloorTrade.builder()
      .product(CAP_TWO_LEGS)
      .info(TRADE_INFO)
      .build();
  static final ResolvedIborCapFloorTrade RTRADE = TRADE.resolve(REF_DATA);

  private static final Currency CURRENCY = RTRADE.getProduct().getCapFloorLeg().getCurrency();
  private static final IborIndex INDEX = RTRADE.getProduct().getCapFloorLeg().getIndex();
  private static final LocalDate VAL_DATE = VOLS.getValuationDate();
  private static final CurveId DISCOUNT_CURVE_ID = CurveId.of("Default", "Discount");
  private static final CurveId FORWARD_CURVE_ID = CurveId.of("Default", "Forward");
  private static final IborCapletFloorletVolatilitiesId VOL_ID =
      IborCapletFloorletVolatilitiesId.of("IborCapFloorVols.Normal.USD");
  static final RatesMarketDataLookup RATES_LOOKUP = RatesMarketDataLookup.of(
      ImmutableMap.of(CURRENCY, DISCOUNT_CURVE_ID),
      ImmutableMap.of(INDEX, FORWARD_CURVE_ID));
  static final IborCapFloorMarketDataLookup SWAPTION_LOOKUP = IborCapFloorMarketDataLookup.of(INDEX, VOL_ID);
  private static final CalculationParameters PARAMS = CalculationParameters.of(RATES_LOOKUP, SWAPTION_LOOKUP);

  //-------------------------------------------------------------------------
  public void test_requirementsAndCurrency() {
    IborCapFloorTradeCalculationFunction function = new IborCapFloorTradeCalculationFunction();
    Set<Measure> measures = function.supportedMeasures();
    FunctionRequirements reqs = function.requirements(TRADE, measures, PARAMS, REF_DATA);
    assertThat(reqs.getOutputCurrencies()).containsOnly(CURRENCY);
    assertThat(reqs.getValueRequirements()).isEqualTo(
        ImmutableSet.of(DISCOUNT_CURVE_ID, FORWARD_CURVE_ID, VOL_ID));
    assertThat(reqs.getTimeSeriesRequirements()).isEqualTo(ImmutableSet.of(IndexQuoteId.of(INDEX)));
    assertThat(function.naturalCurrency(TRADE, REF_DATA)).isEqualTo(CURRENCY);
  }

  public void test_simpleMeasures() {
    IborCapFloorTradeCalculationFunction function = new IborCapFloorTradeCalculationFunction();
    ScenarioMarketData md = marketData();
    RatesProvider provider = RATES_LOOKUP.ratesProvider(md.scenario(0));
    NormalIborCapFloorTradePricer pricer = NormalIborCapFloorTradePricer.DEFAULT;
    MultiCurrencyAmount expectedPv = pricer.presentValue(RTRADE, provider, VOLS);
    MultiCurrencyAmount expectedCurrencyExposure = pricer.currencyExposure(RTRADE, provider, VOLS);
    MultiCurrencyAmount expectedCurrentCash = pricer.currentCash(RTRADE, provider, VOLS);

    Set<Measure> measures = ImmutableSet.of(
        Measures.PRESENT_VALUE,
        Measures.CURRENCY_EXPOSURE,
        Measures.CURRENT_CASH);
    assertThat(function.calculate(TRADE, measures, PARAMS, md, REF_DATA))
        .containsEntry(
            Measures.PRESENT_VALUE, Result.success(MultiCurrencyScenarioArray.of(ImmutableList.of(expectedPv))))
        .containsEntry(
            Measures.CURRENCY_EXPOSURE, Result.success(MultiCurrencyScenarioArray.of(ImmutableList.of(expectedCurrencyExposure))))
        .containsEntry(
            Measures.CURRENT_CASH, Result.success(MultiCurrencyScenarioArray.of(ImmutableList.of(expectedCurrentCash))));
  }

  //-------------------------------------------------------------------------
  static ScenarioMarketData marketData() {
    Curve curve = ConstantCurve.of(Curves.discountFactors("Test", ACT_360), 0.99);
    TestMarketDataMap md = new TestMarketDataMap(
        VAL_DATE,
        ImmutableMap.of(
            DISCOUNT_CURVE_ID, curve,
            FORWARD_CURVE_ID, curve,
            VOL_ID, VOLS),
        ImmutableMap.of());
    return md;
  }

}
