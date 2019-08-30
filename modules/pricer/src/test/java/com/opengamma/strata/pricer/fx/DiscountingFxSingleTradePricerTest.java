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
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.fx.ResolvedFxSingle;
import com.opengamma.strata.product.fx.ResolvedFxSingleTrade;

/**
 * Test {@link DiscountingFxSingleProductPricer}.
 */
public class DiscountingFxSingleTradePricerTest {

  private static final RatesProvider PROVIDER = RatesProviderFxDataSets.createProvider();
  private static final Currency KRW = Currency.KRW;
  private static final Currency USD = Currency.USD;
  private static final LocalDate PAYMENT_DATE = RatesProviderFxDataSets.VAL_DATE_2014_01_22.plusWeeks(8);
  private static final double NOMINAL_USD = 100_000_000;
  private static final double FX_RATE = 1123.45;

  private static final ResolvedFxSingle PRODUCT = ResolvedFxSingle.of(
      CurrencyAmount.of(USD, NOMINAL_USD), FxRate.of(USD, KRW, FX_RATE), PAYMENT_DATE);
  private static final ResolvedFxSingleTrade TRADE = ResolvedFxSingleTrade.of(TradeInfo.empty(), PRODUCT);

  private static final DiscountingFxSingleProductPricer PRODUCT_PRICER = DiscountingFxSingleProductPricer.DEFAULT;
  private static final DiscountingFxSingleTradePricer TRADE_PRICER = DiscountingFxSingleTradePricer.DEFAULT;

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
  public void test_currencyExposure() {
    assertThat(TRADE_PRICER.currencyExposure(TRADE, PROVIDER)).isEqualTo(PRODUCT_PRICER.currencyExposure(PRODUCT, PROVIDER));
  }

  @Test
  public void test_currentCash() {
    assertThat(TRADE_PRICER.currentCash(TRADE, PROVIDER)).isEqualTo(PRODUCT_PRICER.currentCash(PRODUCT, PROVIDER.getValuationDate()));
  }

  @Test
  public void test_forwardFxRate() {
    assertThat(TRADE_PRICER.forwardFxRate(TRADE, PROVIDER)).isEqualTo(PRODUCT_PRICER.forwardFxRate(PRODUCT, PROVIDER));
  }

  @Test
  public void test_forwardFxRatePointSensitivity() {
    assertThat(TRADE_PRICER.forwardFxRatePointSensitivity(TRADE, PROVIDER)).isEqualTo(PRODUCT_PRICER.forwardFxRatePointSensitivity(PRODUCT, PROVIDER).build());
  }

  @Test
  public void test_forwardFxRateSpotSensitivity() {
    assertThat(TRADE_PRICER.forwardFxRateSpotSensitivity(TRADE, PROVIDER)).isEqualTo(PRODUCT_PRICER.forwardFxRateSpotSensitivity(PRODUCT, PROVIDER));
  }

}
