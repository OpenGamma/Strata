/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.swaption;

import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.GBLO;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.USNY;
import static com.opengamma.strata.product.common.LongShort.LONG;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Set;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.AdjustablePayment;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.AdjustableDate;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.calc.Measure;
import com.opengamma.strata.calc.runner.CalculationParameters;
import com.opengamma.strata.calc.runner.FunctionRequirements;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.data.scenario.CurrencyScenarioArray;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.market.curve.ConstantCurve;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveId;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.observable.IndexQuoteId;
import com.opengamma.strata.measure.Measures;
import com.opengamma.strata.measure.curve.TestMarketDataMap;
import com.opengamma.strata.measure.rate.RatesMarketDataLookup;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.swaption.NormalSwaptionExpiryTenorVolatilities;
import com.opengamma.strata.pricer.swaption.NormalSwaptionTradePricer;
import com.opengamma.strata.pricer.swaption.SwaptionNormalVolatilityDataSets;
import com.opengamma.strata.pricer.swaption.SwaptionVolatilitiesId;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.swap.Swap;
import com.opengamma.strata.product.swap.type.FixedIborSwapConventions;
import com.opengamma.strata.product.swaption.PhysicalSwaptionSettlement;
import com.opengamma.strata.product.swaption.ResolvedSwaptionTrade;
import com.opengamma.strata.product.swaption.Swaption;
import com.opengamma.strata.product.swaption.SwaptionSettlement;
import com.opengamma.strata.product.swaption.SwaptionTrade;

/**
 * Test {@link SwaptionTradeCalculationFunction}.
 */
@Test
public class SwaptionTradeCalculationFunctionTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final double FIXED_RATE = 0.015;
  private static final double NOTIONAL = 100000000d;
  private static final Swap SWAP = FixedIborSwapConventions.USD_FIXED_6M_LIBOR_3M
      .createTrade(LocalDate.of(2014, 6, 12), Tenor.TENOR_10Y, BuySell.BUY, NOTIONAL, FIXED_RATE, REF_DATA).getProduct();
  private static final BusinessDayAdjustment ADJUSTMENT =
      BusinessDayAdjustment.of(BusinessDayConventions.FOLLOWING, GBLO.combinedWith(USNY));
  private static final LocalDate EXPIRY_DATE = LocalDate.of(2014, 6, 14);
  private static final AdjustableDate ADJUSTABLE_EXPIRY_DATE = AdjustableDate.of(EXPIRY_DATE, ADJUSTMENT);
  private static final LocalTime EXPIRY_TIME = LocalTime.of(11, 0);
  private static final ZoneId ZONE = ZoneId.of("Z");
  private static final SwaptionSettlement PHYSICAL_SETTLE = PhysicalSwaptionSettlement.DEFAULT;
  private static final Swaption SWAPTION = Swaption.builder()
      .expiryDate(ADJUSTABLE_EXPIRY_DATE)
      .expiryTime(EXPIRY_TIME)
      .expiryZone(ZONE)
      .longShort(LONG)
      .swaptionSettlement(PHYSICAL_SETTLE)
      .underlying(SWAP)
      .build();
  private static final AdjustablePayment PREMIUM =
      AdjustablePayment.of(CurrencyAmount.of(Currency.USD, -3150000d), LocalDate.of(2014, 3, 17));
  public static final SwaptionTrade TRADE = SwaptionTrade.builder().premium(PREMIUM).product(SWAPTION).build();
  public static final ResolvedSwaptionTrade RTRADE = TRADE.resolve(REF_DATA);
  private static final Currency CURRENCY = Currency.USD;
  private static final IborIndex INDEX = IborIndices.USD_LIBOR_3M;

  public static final NormalSwaptionExpiryTenorVolatilities NORMAL_VOL_SWAPTION_PROVIDER_USD =
      SwaptionNormalVolatilityDataSets.NORMAL_SWAPTION_VOLS_USD_STD;
  private static final CurveId DISCOUNT_CURVE_ID = CurveId.of("Default", "Discount");
  private static final CurveId FORWARD_CURVE_ID = CurveId.of("Default", "Forward");
  private static final SwaptionVolatilitiesId VOL_ID = SwaptionVolatilitiesId.of("SwaptionVols.Normal.USD");
  static final RatesMarketDataLookup RATES_LOOKUP = RatesMarketDataLookup.of(
      ImmutableMap.of(CURRENCY, DISCOUNT_CURVE_ID),
      ImmutableMap.of(INDEX, FORWARD_CURVE_ID));
  static final SwaptionMarketDataLookup SWAPTION_LOOKUP = SwaptionMarketDataLookup.of(INDEX, VOL_ID);
  private static final CalculationParameters PARAMS = CalculationParameters.of(RATES_LOOKUP, SWAPTION_LOOKUP);
  private static final LocalDate VAL_DATE = NORMAL_VOL_SWAPTION_PROVIDER_USD.getValuationDate();

  //-------------------------------------------------------------------------
  public void test_requirementsAndCurrency() {
    SwaptionTradeCalculationFunction function = new SwaptionTradeCalculationFunction();
    Set<Measure> measures = function.supportedMeasures();
    FunctionRequirements reqs = function.requirements(TRADE, measures, PARAMS, REF_DATA);
    assertThat(reqs.getOutputCurrencies()).containsOnly(CURRENCY);
    assertThat(reqs.getValueRequirements()).isEqualTo(
        ImmutableSet.of(DISCOUNT_CURVE_ID, FORWARD_CURVE_ID, VOL_ID));
    assertThat(reqs.getTimeSeriesRequirements()).isEqualTo(ImmutableSet.of(IndexQuoteId.of(INDEX)));
    assertThat(function.naturalCurrency(TRADE, REF_DATA)).isEqualTo(CURRENCY);
  }

  public void test_simpleMeasures() {
    SwaptionTradeCalculationFunction function = new SwaptionTradeCalculationFunction();
    ScenarioMarketData md = marketData();
    RatesProvider provider = RATES_LOOKUP.ratesProvider(md.scenario(0));
    NormalSwaptionTradePricer pricer = NormalSwaptionTradePricer.DEFAULT;
    ResolvedSwaptionTrade resolved = TRADE.resolve(REF_DATA);
    CurrencyAmount expectedPv = pricer.presentValue(resolved, provider, NORMAL_VOL_SWAPTION_PROVIDER_USD);

    Set<Measure> measures = ImmutableSet.of(Measures.PRESENT_VALUE, Measures.RESOLVED_TARGET);
    assertThat(function.calculate(TRADE, measures, PARAMS, md, REF_DATA))
        .containsEntry(
            Measures.PRESENT_VALUE, Result.success(CurrencyScenarioArray.of(ImmutableList.of(expectedPv))))
        .containsEntry(
            Measures.RESOLVED_TARGET, Result.success(RTRADE));
  }

  //-------------------------------------------------------------------------
  static ScenarioMarketData marketData() {
    Curve curve = ConstantCurve.of(Curves.discountFactors("Test", ACT_360), 0.99);
    TestMarketDataMap md = new TestMarketDataMap(
        VAL_DATE,
        ImmutableMap.of(
            DISCOUNT_CURVE_ID, curve,
            FORWARD_CURVE_ID, curve,
            VOL_ID, NORMAL_VOL_SWAPTION_PROVIDER_USD),
        ImmutableMap.of());
    return md;
  }

}
