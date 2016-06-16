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

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.index.IborIndexObservation;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.pricer.DiscountFactors;
import com.opengamma.strata.pricer.datasets.RatesProviderDataSets;
import com.opengamma.strata.pricer.rate.IborIndexRates;
import com.opengamma.strata.pricer.rate.IborRateSensitivity;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.rate.SimpleRatesProvider;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.fra.FraTrade;
import com.opengamma.strata.product.fra.ResolvedFra;
import com.opengamma.strata.product.fra.ResolvedFraTrade;
import com.opengamma.strata.product.rate.IborRateComputation;

/**
 * Test {@link DiscountingFraTradePricer}.
 * <p>
 * Some of the methods in the trade pricer are comparable to the product pricer methods, thus tested in  
 * {@link DiscountingFraProductPricerTest}.
 */
@Test
public class DiscountingFraTradePricerTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate VAL_DATE = LocalDate.of(2014, 1, 22);
  private static final double DISCOUNT_FACTOR = 0.98d;
  private static final double FORWARD_RATE = 0.02;
  private static final DiscountingFraProductPricer PRICER_PRODUCT = DiscountingFraProductPricer.DEFAULT;
  private static final DiscountingFraTradePricer PRICER_TRADE = new DiscountingFraTradePricer(PRICER_PRODUCT);

  private static final ResolvedFraTrade RFRA_TRADE = FRA_TRADE.resolve(REF_DATA);
  private static final ResolvedFra RFRA = FRA.resolve(REF_DATA);
  private static final SimpleRatesProvider RATES_PROVIDER;
  static {
    DiscountFactors mockDf = mock(DiscountFactors.class);
    IborIndexRates mockIbor = mock(IborIndexRates.class);
    RATES_PROVIDER = new SimpleRatesProvider(VAL_DATE, mockDf);
    RATES_PROVIDER.setIborRates(mockIbor);
    IborIndexObservation obs = ((IborRateComputation) RFRA.getFloatingRate()).getObservation();
    IborRateSensitivity sens = IborRateSensitivity.of(obs, 1d);
    when(mockIbor.ratePointSensitivity(obs)).thenReturn(sens);
    when(mockIbor.rate(obs)).thenReturn(FORWARD_RATE);
    when(mockDf.discountFactor(RFRA.getPaymentDate())).thenReturn(DISCOUNT_FACTOR);
  }

  //-------------------------------------------------------------------------
  public void test_getters() {
    assertEquals(DiscountingFraTradePricer.DEFAULT.getProductPricer(), DiscountingFraProductPricer.DEFAULT);
  }

  //-------------------------------------------------------------------------
  public void test_currencyExposure() {
    assertEquals(PRICER_TRADE.currencyExposure(RFRA_TRADE, RATES_PROVIDER),
        MultiCurrencyAmount.of(PRICER_TRADE.presentValue(RFRA_TRADE, RATES_PROVIDER)));
  }

  public void test_currentCash_zero() {
    assertEquals(PRICER_TRADE.currentCash(RFRA_TRADE, RATES_PROVIDER), CurrencyAmount.zero(FRA.getCurrency()));
  }

  public void test_currentCash_onPaymentDate() {
    LocalDate paymentDate = RFRA.getPaymentDate();
    double publishedRate = 0.025;
    ResolvedFraTrade trade = FraTrade.builder()
        .info(TradeInfo.builder().tradeDate(paymentDate).build())
        .product(FRA)
        .build()
        .resolve(REF_DATA);
    ImmutableRatesProvider ratesProvider = RatesProviderDataSets.multiGbp(paymentDate).toBuilder()
        .timeSeries(GBP_LIBOR_3M, LocalDateDoubleTimeSeries.of(paymentDate, publishedRate))
        .build();
    assertEquals(PRICER_TRADE.currentCash(trade, ratesProvider), CurrencyAmount.of(FRA.getCurrency(),
        (publishedRate - FRA.getFixedRate()) / (1d + publishedRate * RFRA.getYearFraction()) *
            RFRA.getYearFraction() * RFRA.getNotional()));
  }

}
