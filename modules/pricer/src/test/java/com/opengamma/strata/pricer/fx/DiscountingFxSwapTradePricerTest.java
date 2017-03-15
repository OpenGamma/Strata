/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fx;

import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.fx.ResolvedFxSwap;
import com.opengamma.strata.product.fx.ResolvedFxSwapTrade;

/**
 * Test {@link DiscountingFxSwapProductPricer}.
 */
@Test
public class DiscountingFxSwapTradePricerTest {

  private static final RatesProvider PROVIDER = RatesProviderFxDataSets.createProvider();
  private static final Currency KRW = Currency.KRW;
  private static final Currency USD = Currency.USD;
  private static final LocalDate PAYMENT_DATE_NEAR = RatesProviderFxDataSets.VAL_DATE_2014_01_22.plusWeeks(1);
  private static final LocalDate PAYMENT_DATE_FAR = PAYMENT_DATE_NEAR.plusMonths(1);
  private static final double NOMINAL_USD = 100_000_000;
  private static final double FX_RATE = 1109.5;
  private static final double FX_FWD_POINTS = 4.45;

  private static final ResolvedFxSwap PRODUCT = ResolvedFxSwap.ofForwardPoints(
      CurrencyAmount.of(USD, NOMINAL_USD), KRW, FX_RATE, FX_FWD_POINTS, PAYMENT_DATE_NEAR, PAYMENT_DATE_FAR);
  private static final ResolvedFxSwapTrade TRADE = ResolvedFxSwapTrade.of(TradeInfo.empty(), PRODUCT);

  private static final DiscountingFxSwapProductPricer PRODUCT_PRICER = DiscountingFxSwapProductPricer.DEFAULT;
  private static final DiscountingFxSwapTradePricer TRADE_PRICER = DiscountingFxSwapTradePricer.DEFAULT;

  //-------------------------------------------------------------------------
  public void test_presentValue() {
    assertEquals(
        TRADE_PRICER.presentValue(TRADE, PROVIDER),
        PRODUCT_PRICER.presentValue(PRODUCT, PROVIDER));
  }

  public void test_presentValueSensitivity() {
    assertEquals(
        TRADE_PRICER.presentValueSensitivity(TRADE, PROVIDER),
        PRODUCT_PRICER.presentValueSensitivity(PRODUCT, PROVIDER));
  }

  public void test_parSpread() {
    assertEquals(
        TRADE_PRICER.parSpread(TRADE, PROVIDER),
        PRODUCT_PRICER.parSpread(PRODUCT, PROVIDER));
  }

  public void test_parSpreadSensitivity() {
    assertEquals(
        TRADE_PRICER.parSpreadSensitivity(TRADE, PROVIDER),
        PRODUCT_PRICER.parSpreadSensitivity(PRODUCT, PROVIDER));
  }

  public void test_currencyExposure() {
    assertEquals(
        TRADE_PRICER.currencyExposure(TRADE, PROVIDER),
        PRODUCT_PRICER.currencyExposure(PRODUCT, PROVIDER));
  }

  public void test_currentCash() {
    assertEquals(
        TRADE_PRICER.currentCash(TRADE, PROVIDER),
        PRODUCT_PRICER.currentCash(PRODUCT, PROVIDER.getValuationDate()));
  }

}
