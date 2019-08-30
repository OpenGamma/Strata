/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fx;

import static com.opengamma.strata.basics.date.HolidayCalendarIds.USNY;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.index.FxIndex;
import com.opengamma.strata.basics.index.FxIndexObservation;
import com.opengamma.strata.basics.index.ImmutableFxIndex;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.fx.ResolvedFxNdf;
import com.opengamma.strata.product.fx.ResolvedFxNdfTrade;

/**
 * Test {@link DiscountingFxNdfProductPricer}.
 */
public class DiscountingFxNdfTradePricerTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final RatesProvider PROVIDER = RatesProviderFxDataSets.createProvider();
  private static final Currency KRW = Currency.KRW;
  private static final Currency USD = Currency.USD;
  private static final LocalDate PAYMENT_DATE = RatesProviderFxDataSets.VAL_DATE_2014_01_22.plusWeeks(8);
  private static final double NOMINAL_USD = 100_000_000;
  private static final CurrencyAmount CURRENCY_NOTIONAL = CurrencyAmount.of(USD, NOMINAL_USD);
  private static final double FX_RATE = 1123.45;
  private static final FxIndex INDEX = ImmutableFxIndex.builder()
      .name("USD/KRW")
      .currencyPair(CurrencyPair.of(USD, KRW))
      .fixingCalendar(USNY)
      .maturityDateOffset(DaysAdjustment.ofBusinessDays(2, USNY))
      .build();
  private static final LocalDate FIXING_DATE = INDEX.calculateFixingFromMaturity(PAYMENT_DATE, REF_DATA);

  private static final ResolvedFxNdf PRODUCT = ResolvedFxNdf.builder()
      .settlementCurrencyNotional(CURRENCY_NOTIONAL)
      .agreedFxRate(FxRate.of(USD, KRW, FX_RATE))
      .observation(FxIndexObservation.of(INDEX, FIXING_DATE, REF_DATA))
      .paymentDate(PAYMENT_DATE)
      .build();
  private static final ResolvedFxNdfTrade TRADE = ResolvedFxNdfTrade.of(TradeInfo.empty(), PRODUCT);

  private static final DiscountingFxNdfProductPricer PRODUCT_PRICER = DiscountingFxNdfProductPricer.DEFAULT;
  private static final DiscountingFxNdfTradePricer TRADE_PRICER = DiscountingFxNdfTradePricer.DEFAULT;

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
  public void test_currencyExposure() {
    assertThat(TRADE_PRICER.currencyExposure(TRADE, PROVIDER)).isEqualTo(PRODUCT_PRICER.currencyExposure(PRODUCT, PROVIDER));
  }

  @Test
  public void test_currentCash() {
    assertThat(TRADE_PRICER.currentCash(TRADE, PROVIDER)).isEqualTo(PRODUCT_PRICER.currentCash(PRODUCT, PROVIDER));
  }

  @Test
  public void test_forwardFxRate() {
    assertThat(TRADE_PRICER.forwardFxRate(TRADE, PROVIDER)).isEqualTo(PRODUCT_PRICER.forwardFxRate(PRODUCT, PROVIDER));
  }

}
