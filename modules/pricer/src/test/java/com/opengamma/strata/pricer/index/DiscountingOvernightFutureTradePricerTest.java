/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.index;

import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.Tenor.TENOR_1M;
import static com.opengamma.strata.basics.index.OvernightIndices.USD_FED_FUND;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.value.Rounding;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolators;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.index.OvernightFuture;
import com.opengamma.strata.product.index.OvernightFutureTrade;
import com.opengamma.strata.product.index.ResolvedOvernightFutureTrade;
import com.opengamma.strata.product.swap.OvernightAccrualMethod;

/**
 * Test {@link DiscountingOvernightFutureTradePricer}.
 */
@Test
public class DiscountingOvernightFutureTradePricerTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate VALUATION = LocalDate.of(2018, 7, 12);
  private static final double NOTIONAL = 5_000_000d;
  private static final double ACCRUAL_FACTOR = TENOR_1M.getPeriod().toTotalMonths() / 12.0;
  private static final LocalDate LAST_TRADE_DATE = date(2018, 9, 28);
  private static final LocalDate START_DATE = date(2018, 9, 1);
  private static final LocalDate END_DATE = date(2018, 9, 30);
  private static final Rounding ROUNDING = Rounding.ofDecimalPlaces(5);
  private static final SecurityId SECURITY_ID = SecurityId.of("OG-Test", "OnFuture");
  private static final OvernightFuture FUTURE = OvernightFuture.builder()
      .securityId(SECURITY_ID)
      .currency(USD)
      .notional(NOTIONAL)
      .accrualFactor(ACCRUAL_FACTOR)
      .startDate(START_DATE)
      .endDate(END_DATE)
      .lastTradeDate(LAST_TRADE_DATE)
      .index(USD_FED_FUND)
      .accrualMethod(OvernightAccrualMethod.AVERAGED_DAILY)
      .rounding(ROUNDING)
      .build();
  private static final LocalDate TRADE_DATE = date(2018, 2, 17);
  private static final long FUTURE_QUANTITY = 35;
  private static final double FUTURE_INITIAL_PRICE = 1.015;
  private static final ResolvedOvernightFutureTrade RESOLVED_TRADE = OvernightFutureTrade.builder()
      .info(TradeInfo.builder().tradeDate(TRADE_DATE).build())
      .product(FUTURE)
      .quantity(FUTURE_QUANTITY)
      .price(FUTURE_INITIAL_PRICE)
      .build().resolve(REF_DATA);

  private static final DoubleArray TIME = DoubleArray.of(0.02, 0.08, 0.25, 0.5);
  private static final DoubleArray RATE = DoubleArray.of(0.01, 0.015, 0.008, 0.005);
  private static final Curve CURVE = InterpolatedNodalCurve.of(
      Curves.zeroRates("FED-FUND", DayCounts.ACT_365F), TIME, RATE, CurveInterpolators.NATURAL_SPLINE);
  private static final RatesProvider RATES_PROVIDER = getRatesProvider(VALUATION);
  private static final RatesProvider RATES_PROVIDER_ON = getRatesProvider(TRADE_DATE);
  private static final RatesProvider RATES_PROVIDER_AFTER = getRatesProvider(TRADE_DATE.plusWeeks(1));

  private static RatesProvider getRatesProvider(LocalDate valuationDate) {
    return ImmutableRatesProvider.builder(valuationDate)
        .indexCurve(USD_FED_FUND, CURVE)
        .build();
  }

  private static final double TOL = 1.0e-14;
  private static final DiscountingOvernightFutureProductPricer PRICER_PRODUCT = DiscountingOvernightFutureProductPricer.DEFAULT;
  private static final DiscountingOvernightFutureTradePricer PRICER_TRADE = DiscountingOvernightFutureTradePricer.DEFAULT;
  private static final double TOLERANCE_PRICE = 1.0e-9;
  private static final double TOLERANCE_PV = 1.0e-4;

  //------------------------------------------------------------------------- 
  public void test_price() {
    double computed = PRICER_TRADE.price(RESOLVED_TRADE, RATES_PROVIDER);
    double expected = PRICER_PRODUCT.price(RESOLVED_TRADE.getProduct(), RATES_PROVIDER);
    assertEquals(computed, expected, TOL);
  }

  //-------------------------------------------------------------------------
  public void test_presentValue() {
    double currentPrice = 0.995;
    double referencePrice = 0.9925;
    double currentPriceIndex = PRICER_PRODUCT.marginIndex(RESOLVED_TRADE.getProduct(), currentPrice);
    double referencePriceIndex = PRICER_PRODUCT.marginIndex(RESOLVED_TRADE.getProduct(), referencePrice);
    double presentValueExpected = (currentPriceIndex - referencePriceIndex) * RESOLVED_TRADE.getQuantity();
    CurrencyAmount presentValueComputed = PRICER_TRADE.presentValue(RESOLVED_TRADE, currentPrice, referencePrice);
    assertEquals(presentValueComputed.getAmount(), presentValueExpected, TOLERANCE_PV);
  }

  //-------------------------------------------------------------------------
  public void test_reference_price_after_trade_date() {
    LocalDate tradeDate = RESOLVED_TRADE.getTradedPrice().get().getTradeDate();
    LocalDate valuationDate = tradeDate.plusDays(1);
    double settlementPrice = 0.995;
    double referencePrice = PRICER_TRADE.referencePrice(RESOLVED_TRADE, valuationDate, settlementPrice);
    assertEquals(referencePrice, settlementPrice);
  }

  public void test_reference_price_on_trade_date() {
    LocalDate tradeDate = RESOLVED_TRADE.getTradedPrice().get().getTradeDate();
    LocalDate valuationDate = tradeDate;
    double settlementPrice = 0.995;
    double referencePrice = PRICER_TRADE.referencePrice(RESOLVED_TRADE, valuationDate, settlementPrice);
    assertEquals(referencePrice, RESOLVED_TRADE.getTradedPrice().get().getPrice());
  }

  //-------------------------------------------------------------------------
  public void test_parSpread_after_trade_date() {
    double lastClosingPrice = 0.99;
    double parSpreadExpected = PRICER_TRADE.price(RESOLVED_TRADE, RATES_PROVIDER_AFTER) - lastClosingPrice;
    double parSpreadComputed = PRICER_TRADE.parSpread(RESOLVED_TRADE, RATES_PROVIDER_AFTER, lastClosingPrice);
    assertEquals(parSpreadComputed, parSpreadExpected, TOLERANCE_PRICE);
  }

  public void test_parSpread_on_trade_date() {
    double lastClosingPrice = 0.99;
    double parSpreadExpected = PRICER_TRADE.price(
        RESOLVED_TRADE, RATES_PROVIDER_ON) - RESOLVED_TRADE.getTradedPrice().get().getPrice();
    double parSpreadComputed = PRICER_TRADE.parSpread(RESOLVED_TRADE, RATES_PROVIDER_ON, lastClosingPrice);
    assertEquals(parSpreadComputed, parSpreadExpected, TOLERANCE_PRICE);
  }

  //------------------------------------------------------------------------- 
  public void test_presentValue_after_trade_date() {
    double lastClosingPrice = 1.005;
    double expected = (PRICER_PRODUCT.price(RESOLVED_TRADE.getProduct(), RATES_PROVIDER_AFTER) - lastClosingPrice) *
        FUTURE.getAccrualFactor() * FUTURE.getNotional() * RESOLVED_TRADE.getQuantity();
    CurrencyAmount computed = PRICER_TRADE.presentValue(RESOLVED_TRADE, RATES_PROVIDER_AFTER, lastClosingPrice);
    assertEquals(computed.getAmount(), expected, NOTIONAL * TOL);
    assertEquals(computed.getCurrency(), FUTURE.getCurrency());
  }

  public void test_presentValue_on_trade_date() {
    double lastClosingPrice = 1.005;
    double expected = (PRICER_PRODUCT.price(RESOLVED_TRADE.getProduct(), RATES_PROVIDER_ON) -
        RESOLVED_TRADE.getTradedPrice().get().getPrice()) * FUTURE.getAccrualFactor() * FUTURE.getNotional() *
        RESOLVED_TRADE.getQuantity();
    CurrencyAmount computed = PRICER_TRADE.presentValue(RESOLVED_TRADE, RATES_PROVIDER_ON, lastClosingPrice);
    assertEquals(computed.getAmount(), expected, TOLERANCE_PV);
    assertEquals(computed.getCurrency(), FUTURE.getCurrency());
  }

  //-------------------------------------------------------------------------   
  public void test_presentValueSensitivity() {
    PointSensitivities computed = PRICER_TRADE.presentValueSensitivity(RESOLVED_TRADE, RATES_PROVIDER);
    PointSensitivities expected = PRICER_PRODUCT.priceSensitivity(RESOLVED_TRADE.getProduct(), RATES_PROVIDER)
        .multipliedBy(FUTURE.getNotional() * FUTURE.getAccrualFactor() * RESOLVED_TRADE.getQuantity());
    assertTrue(computed.equalWithTolerance(expected, NOTIONAL * TOL));
  }

  //-------------------------------------------------------------------------
  public void test_parSpreadSensitivity() {
    PointSensitivities sensiExpected = PRICER_PRODUCT.priceSensitivity(RESOLVED_TRADE.getProduct(), RATES_PROVIDER);
    PointSensitivities sensiComputed = PRICER_TRADE.parSpreadSensitivity(RESOLVED_TRADE, RATES_PROVIDER);
    assertTrue(sensiComputed.equalWithTolerance(sensiExpected, TOL));
  }

  //-------------------------------------------------------------------------
  public void test_pricedSensitivity() {
    PointSensitivities sensiExpected = PRICER_PRODUCT.priceSensitivity(RESOLVED_TRADE.getProduct(), RATES_PROVIDER);
    PointSensitivities sensiComputed = PRICER_TRADE.priceSensitivity(RESOLVED_TRADE, RATES_PROVIDER);
    assertTrue(sensiComputed.equalWithTolerance(sensiExpected, TOL));
  }

}
