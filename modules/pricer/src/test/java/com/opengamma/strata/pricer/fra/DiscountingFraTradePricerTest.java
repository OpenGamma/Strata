/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fra;

import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.pricer.fra.FraDummyData.FRA;
import static com.opengamma.strata.pricer.fra.FraDummyData.FRA_TRADE;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.market.sensitivity.IborRateSensitivity;
import com.opengamma.strata.market.value.DiscountFactors;
import com.opengamma.strata.market.value.IborIndexRates;
import com.opengamma.strata.pricer.datasets.RatesProviderDataSets;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.rate.SimpleRatesProvider;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.fra.ExpandedFra;
import com.opengamma.strata.product.fra.FraTrade;
import com.opengamma.strata.product.rate.IborRateObservation;

/**
 * Test {@link DiscountingFraTradePricer}.
 * <p>
 * Some of the methods in the trade pricer are comparable to the product pricer methods, thus tested in  
 * {@link DiscountingFraProductPricerTest}.
 */
@Test
public class DiscountingFraTradePricerTest {

  private static final LocalDate VAL_DATE = LocalDate.of(2014, 1, 22);
  private static final double DISCOUNT_FACTOR = 0.98d;
  private static final double FORWARD_RATE = 0.02;
  private static final DiscountingFraProductPricer PRICER_PRODUCT = DiscountingFraProductPricer.DEFAULT;
  private static final DiscountingFraTradePricer PRICER_TRADE = new DiscountingFraTradePricer(PRICER_PRODUCT);
  private static final ExpandedFra EXPANDED = FRA.expand();
  private static final SimpleRatesProvider RATES_PROVIDER;
  static {
    DiscountFactors mockDf = mock(DiscountFactors.class);
    IborIndexRates mockIbor = mock(IborIndexRates.class);
    RATES_PROVIDER = new SimpleRatesProvider(VAL_DATE, mockDf);
    RATES_PROVIDER.setIborRates(mockIbor);
    IborRateObservation obs = (IborRateObservation) EXPANDED.getFloatingRate();
    IborRateSensitivity sens = IborRateSensitivity.of(obs.getIndex(), obs.getFixingDate(), 1d);
    when(mockIbor.ratePointSensitivity(obs.getFixingDate())).thenReturn(sens);
    when(mockIbor.rate(obs.getFixingDate())).thenReturn(FORWARD_RATE);
    when(mockDf.discountFactor(EXPANDED.getPaymentDate())).thenReturn(DISCOUNT_FACTOR);
  }

  public void test_currencyExposure() {
    assertEquals(PRICER_TRADE.currencyExposure(FRA_TRADE, RATES_PROVIDER),
        MultiCurrencyAmount.of(PRICER_TRADE.presentValue(FRA_TRADE, RATES_PROVIDER)));
  }

  public void test_currentCash_zero() {
    assertEquals(PRICER_TRADE.currentCash(FRA_TRADE, RATES_PROVIDER), CurrencyAmount.zero(FRA.getCurrency()));
  }

  public void test_currentCash_onPaymentDate() {
    LocalDate paymentDate = EXPANDED.getPaymentDate();
    double publishedRate = 0.025;
    FraTrade trade = FraTrade.builder()
        .tradeInfo(TradeInfo.builder().tradeDate(paymentDate).build())
        .product(FRA)
        .build();
    ImmutableRatesProvider ratesProvider = RatesProviderDataSets.MULTI_GBP.toBuilder(paymentDate)
        .timeSeries(GBP_LIBOR_3M, LocalDateDoubleTimeSeries.of(paymentDate, publishedRate))
        .build();
    assertEquals(PRICER_TRADE.currentCash(trade, ratesProvider), CurrencyAmount.of(FRA.getCurrency(),
        (publishedRate - FRA.getFixedRate()) / (1d + publishedRate * EXPANDED.getYearFraction()) *
            EXPANDED.getYearFraction() * EXPANDED.getNotional()));
  }

}
