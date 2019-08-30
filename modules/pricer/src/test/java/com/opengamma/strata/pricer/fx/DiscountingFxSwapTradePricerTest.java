/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fx;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.fx.ResolvedFxSwap;
import com.opengamma.strata.product.fx.ResolvedFxSwapTrade;

/**
 * Test {@link DiscountingFxSwapProductPricer}.
 */
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
  @Test
  public void test_presentValue() {
    assertThat(TRADE_PRICER.presentValue(TRADE, PROVIDER)).isEqualTo(PRODUCT_PRICER.presentValue(PRODUCT, PROVIDER));
  }

  @Test
  public void test_presentValueSensitivity() {
    assertThat(TRADE_PRICER.presentValueSensitivity(TRADE, PROVIDER)).isEqualTo(PRODUCT_PRICER.presentValueSensitivity(PRODUCT, PROVIDER));
  }

  @Test
  public void test_parSpread() {
    assertThat(TRADE_PRICER.parSpread(TRADE, PROVIDER)).isEqualTo(PRODUCT_PRICER.parSpread(PRODUCT, PROVIDER));
  }

  @Test
  public void test_parSpreadSensitivity() {
    assertThat(TRADE_PRICER.parSpreadSensitivity(TRADE, PROVIDER)).isEqualTo(PRODUCT_PRICER.parSpreadSensitivity(PRODUCT, PROVIDER));
  }

  @Test
  public void test_currencyExposure() {
    assertThat(TRADE_PRICER.currencyExposure(TRADE, PROVIDER)).isEqualTo(PRODUCT_PRICER.currencyExposure(PRODUCT, PROVIDER));
  }

  @Test
  public void test_currentCash() {
    assertThat(TRADE_PRICER.currentCash(TRADE, PROVIDER)).isEqualTo(PRODUCT_PRICER.currentCash(PRODUCT, PROVIDER.getValuationDate()));
  }

}
