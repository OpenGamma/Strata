/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.swaption;

import static com.opengamma.strata.basics.LongShort.LONG;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.HolidayCalendars.GBLO;
import static com.opengamma.strata.basics.date.HolidayCalendars.USNY;
import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Set;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.BuySell;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.basics.date.AdjustableDate;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.calc.config.FunctionConfig;
import com.opengamma.strata.calc.config.Measure;
import com.opengamma.strata.calc.config.Measures;
import com.opengamma.strata.calc.config.pricing.FunctionGroup;
import com.opengamma.strata.calc.marketdata.CalculationMarketData;
import com.opengamma.strata.calc.marketdata.FunctionRequirements;
import com.opengamma.strata.calc.runner.function.result.CurrencyValuesArray;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.function.marketdata.curve.TestMarketDataMap;
import com.opengamma.strata.market.curve.ConstantNodalCurve;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.key.DiscountCurveKey;
import com.opengamma.strata.market.key.IborIndexCurveKey;
import com.opengamma.strata.market.key.IndexRateKey;
import com.opengamma.strata.market.key.SwaptionVolatilitiesKey;
import com.opengamma.strata.pricer.rate.MarketDataRatesProvider;
import com.opengamma.strata.pricer.swaption.NormalSwaptionExpiryTenorVolatilities;
import com.opengamma.strata.pricer.swaption.SwaptionNormalVolatilityDataSets;
import com.opengamma.strata.pricer.swaption.VolatilitySwaptionPhysicalProductPricer;
import com.opengamma.strata.product.swap.Swap;
import com.opengamma.strata.product.swap.type.FixedIborSwapConventions;
import com.opengamma.strata.product.swaption.PhysicalSettlement;
import com.opengamma.strata.product.swaption.Swaption;
import com.opengamma.strata.product.swaption.SwaptionSettlement;
import com.opengamma.strata.product.swaption.SwaptionTrade;

/**
 * Test {@link SwaptionCalculationFunction}.
 */
@Test
public class SwaptionCalculationFunctionTest {

  private static final NormalSwaptionExpiryTenorVolatilities NORMAL_VOL_SWAPTION_PROVIDER_USD =
      SwaptionNormalVolatilityDataSets.NORMAL_VOL_SWAPTION_PROVIDER_USD_STD;

  private static final double FIXED_RATE = 0.015;
  private static final double NOTIONAL = 100000000d;
  private static final Swap SWAP = FixedIborSwapConventions.USD_FIXED_6M_LIBOR_3M
      .createTrade(LocalDate.of(2014, 6, 12), Tenor.TENOR_10Y, BuySell.BUY, NOTIONAL, FIXED_RATE).getProduct();
  private static final BusinessDayAdjustment ADJUSTMENT =
      BusinessDayAdjustment.of(BusinessDayConventions.FOLLOWING, GBLO.combineWith(USNY));
  private static final LocalDate EXPIRY_DATE = LocalDate.of(2014, 6, 14);
  private static final AdjustableDate ADJUSTABLE_EXPIRY_DATE = AdjustableDate.of(EXPIRY_DATE, ADJUSTMENT);
  private static final LocalTime EXPIRY_TIME = LocalTime.of(11, 0);
  private static final ZoneId ZONE = ZoneId.of("Z");
  private static final SwaptionSettlement PHYSICAL_SETTLE = PhysicalSettlement.DEFAULT;
  private static final Swaption SWAPTION = Swaption.builder()
      .expiryDate(ADJUSTABLE_EXPIRY_DATE)
      .expiryTime(EXPIRY_TIME)
      .expiryZone(ZONE)
      .longShort(LONG)
      .swaptionSettlement(PHYSICAL_SETTLE)
      .underlying(SWAP)
      .build();
  private static final Payment PREMIUM = Payment.of(CurrencyAmount.of(Currency.USD, -3150000d), LocalDate.of(2014, 3, 17));
  public static final SwaptionTrade TRADE = SwaptionTrade.builder().premium(PREMIUM).product(SWAPTION).build();
  private static final IborIndex INDEX = IborIndices.USD_LIBOR_3M;
  private static final Currency CURRENCY = Currency.USD;
  private static final LocalDate VAL_DATE = NORMAL_VOL_SWAPTION_PROVIDER_USD.getValuationDate();

  //-------------------------------------------------------------------------
  public void test_group() {
    FunctionGroup<SwaptionTrade> test = SwaptionFunctionGroups.standard();
    assertThat(test.configuredMeasures(TRADE)).contains(
        Measures.PRESENT_VALUE);
    FunctionConfig<SwaptionTrade> config =
        SwaptionFunctionGroups.standard().functionConfig(TRADE, Measures.PRESENT_VALUE).get();
    assertThat(config.createFunction()).isInstanceOf(SwaptionCalculationFunction.class);
  }

  public void test_requirementsAndCurrency() {
    SwaptionCalculationFunction function = new SwaptionCalculationFunction();
    Set<Measure> measures = function.supportedMeasures();
    FunctionRequirements reqs = function.requirements(TRADE, measures);
    assertThat(reqs.getOutputCurrencies()).containsOnly(CURRENCY);
    assertThat(reqs.getSingleValueRequirements()).isEqualTo(
        ImmutableSet.of(
            DiscountCurveKey.of(CURRENCY),
            IborIndexCurveKey.of(INDEX),
            SwaptionVolatilitiesKey.of(INDEX)));
    assertThat(reqs.getTimeSeriesRequirements()).isEqualTo(ImmutableSet.of(IndexRateKey.of(INDEX)));
    assertThat(function.naturalCurrency(TRADE)).isEqualTo(CURRENCY);
  }

  public void test_simpleMeasures() {
    SwaptionCalculationFunction function = new SwaptionCalculationFunction();
    CalculationMarketData md = marketData();
    MarketDataRatesProvider provider = MarketDataRatesProvider.of(md.scenario(0));
    VolatilitySwaptionPhysicalProductPricer pricer = VolatilitySwaptionPhysicalProductPricer.DEFAULT;
    CurrencyAmount expectedPv = pricer.presentValue(TRADE.getProduct(), provider, NORMAL_VOL_SWAPTION_PROVIDER_USD);

    Set<Measure> measures = ImmutableSet.of(Measures.PRESENT_VALUE, Measures.PRESENT_VALUE_MULTI_CCY);
    assertThat(function.calculate(TRADE, measures, md))
        .containsEntry(
            Measures.PRESENT_VALUE, Result.success(CurrencyValuesArray.of(ImmutableList.of(expectedPv))))
        .containsEntry(
            Measures.PRESENT_VALUE_MULTI_CCY, Result.success(CurrencyValuesArray.of(ImmutableList.of(expectedPv))));
  }

  //-------------------------------------------------------------------------
  private CalculationMarketData marketData() {
    Curve curve = ConstantNodalCurve.of(Curves.discountFactors("Test", ACT_360), 0.99);
    TestMarketDataMap md = new TestMarketDataMap(
        VAL_DATE,
        ImmutableMap.of(
            DiscountCurveKey.of(CURRENCY), curve,
            IborIndexCurveKey.of(INDEX), curve,
            SwaptionVolatilitiesKey.of(INDEX), NORMAL_VOL_SWAPTION_PROVIDER_USD),
        ImmutableMap.of());
    return md;
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverPrivateConstructor(SwaptionFunctionGroups.class);
    coverPrivateConstructor(SwaptionMeasureCalculations.class);
  }

}
