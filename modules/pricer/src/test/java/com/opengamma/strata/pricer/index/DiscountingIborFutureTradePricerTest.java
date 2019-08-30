/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.index;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.data.Offset.offset;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.rate.IborIndexRates;
import com.opengamma.strata.pricer.rate.SimpleRatesProvider;
import com.opengamma.strata.product.index.ResolvedIborFuture;
import com.opengamma.strata.product.index.ResolvedIborFutureTrade;

/**
 * Test {@link DiscountingIborFutureTradePricer}.
 */
public class DiscountingIborFutureTradePricerTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final DiscountingIborFutureTradePricer PRICER_TRADE = DiscountingIborFutureTradePricer.DEFAULT;
  private static final DiscountingIborFutureProductPricer PRICER_PRODUCT = DiscountingIborFutureProductPricer.DEFAULT;
  private static final ResolvedIborFutureTrade FUTURE_TRADE = IborFutureDummyData.IBOR_FUTURE_TRADE.resolve(REF_DATA);
  private static final ResolvedIborFuture FUTURE = FUTURE_TRADE.getProduct();

  private static final double RATE = 0.045;

  private static final double TOLERANCE_PRICE = 1.0e-9;
  private static final double TOLERANCE_PRICE_DELTA = 1.0e-9;
  private static final double TOLERANCE_PV = 1.0e-4;
  private static final double TOLERANCE_PV_DELTA = 1.0e-2;

  //------------------------------------------------------------------------- 
  @Test
  public void test_price() {
    IborIndexRates mockIbor = mock(IborIndexRates.class);
    SimpleRatesProvider prov = new SimpleRatesProvider();
    prov.setIborRates(mockIbor);
    when(mockIbor.rate(FUTURE.getIborRate().getObservation())).thenReturn(RATE);

    assertThat(PRICER_TRADE.price(FUTURE_TRADE, prov)).isCloseTo(1.0 - RATE, offset(TOLERANCE_PRICE));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_presentValue() {
    double currentPrice = 0.995;
    double referencePrice = 0.9925;
    double currentPriceIndex = PRICER_PRODUCT.marginIndex(FUTURE_TRADE.getProduct(), currentPrice);
    double referencePriceIndex = PRICER_PRODUCT.marginIndex(FUTURE_TRADE.getProduct(), referencePrice);
    double presentValueExpected = (currentPriceIndex - referencePriceIndex) * FUTURE_TRADE.getQuantity();
    CurrencyAmount presentValueComputed = PRICER_TRADE.presentValue(FUTURE_TRADE, currentPrice, referencePrice);
    assertThat(presentValueComputed.getAmount()).isCloseTo(presentValueExpected, offset(TOLERANCE_PV));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_reference_price_after_trade_date() {
    LocalDate tradeDate = FUTURE_TRADE.getTradedPrice().get().getTradeDate();
    LocalDate valuationDate = tradeDate.plusDays(1);
    double settlementPrice = 0.995;
    double referencePrice = PRICER_TRADE.referencePrice(FUTURE_TRADE, valuationDate, settlementPrice);
    assertThat(referencePrice).isEqualTo(settlementPrice);
  }

  @Test
  public void test_reference_price_on_trade_date() {
    LocalDate tradeDate = FUTURE_TRADE.getTradedPrice().get().getTradeDate();
    LocalDate valuationDate = tradeDate;
    double settlementPrice = 0.995;
    double referencePrice = PRICER_TRADE.referencePrice(FUTURE_TRADE, valuationDate, settlementPrice);
    assertThat(referencePrice).isEqualTo(FUTURE_TRADE.getTradedPrice().get().getPrice());
  }

  @Test
  public void test_reference_price_val_date_not_null() {
    double settlementPrice = 0.995;
    assertThatIllegalArgumentException()
        .isThrownBy(() -> PRICER_TRADE.referencePrice(FUTURE_TRADE, null, settlementPrice));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_parSpread_after_trade_date() {
    IborIndexRates mockIbor = mock(IborIndexRates.class);
    SimpleRatesProvider prov = new SimpleRatesProvider();
    prov.setIborRates(mockIbor);
    prov.setValuationDate(FUTURE_TRADE.getTradedPrice().get().getTradeDate().plusDays(1));
    when(mockIbor.rate(FUTURE.getIborRate().getObservation())).thenReturn(RATE);
    double lastClosingPrice = 0.99;
    double parSpreadExpected = PRICER_TRADE.price(FUTURE_TRADE, prov) - lastClosingPrice;
    double parSpreadComputed = PRICER_TRADE.parSpread(FUTURE_TRADE, prov, lastClosingPrice);
    assertThat(parSpreadComputed).isCloseTo(parSpreadExpected, offset(TOLERANCE_PRICE));
  }
  
  @Test
  public void test_parSpread_on_trade_date() {
    IborIndexRates mockIbor = mock(IborIndexRates.class);
    SimpleRatesProvider prov = new SimpleRatesProvider();
    prov.setIborRates(mockIbor);
    prov.setValuationDate(FUTURE_TRADE.getTradedPrice().get().getTradeDate());
    when(mockIbor.rate(FUTURE.getIborRate().getObservation())).thenReturn(RATE);

    double lastClosingPrice = 0.99;
    double parSpreadExpected = PRICER_TRADE.price(FUTURE_TRADE, prov) - FUTURE_TRADE.getTradedPrice().get().getPrice();
    double parSpreadComputed = PRICER_TRADE.parSpread(FUTURE_TRADE, prov, lastClosingPrice);
    assertThat(parSpreadComputed).isCloseTo(parSpreadExpected, offset(TOLERANCE_PRICE));
  }

  //------------------------------------------------------------------------- 
  @Test
  public void test_presentValue_after_trade_date() {
    IborIndexRates mockIbor = mock(IborIndexRates.class);
    SimpleRatesProvider prov = new SimpleRatesProvider();
    prov.setIborRates(mockIbor);
    prov.setValuationDate(FUTURE_TRADE.getTradedPrice().get().getTradeDate().plusDays(1));
    when(mockIbor.rate(FUTURE.getIborRate().getObservation())).thenReturn(RATE);

    double lastClosingPrice = 1.025;
    DiscountingIborFutureTradePricer pricerFn = DiscountingIborFutureTradePricer.DEFAULT;
    double expected = ((1.0 - RATE) - lastClosingPrice) *
        FUTURE.getAccrualFactor() * FUTURE.getNotional() * FUTURE_TRADE.getQuantity();
    CurrencyAmount computed = pricerFn.presentValue(FUTURE_TRADE, prov, lastClosingPrice);
    assertThat(computed.getAmount()).isCloseTo(expected, offset(TOLERANCE_PV));
    assertThat(computed.getCurrency()).isEqualTo(FUTURE.getCurrency());
  }

  @Test
  public void test_presentValue_on_trade_date() {
    IborIndexRates mockIbor = mock(IborIndexRates.class);
    SimpleRatesProvider prov = new SimpleRatesProvider();
    prov.setIborRates(mockIbor);
    prov.setValuationDate(FUTURE_TRADE.getTradedPrice().get().getTradeDate());
    when(mockIbor.rate(FUTURE.getIborRate().getObservation())).thenReturn(RATE);

    double lastClosingPrice = 1.025;
    DiscountingIborFutureTradePricer pricerFn = DiscountingIborFutureTradePricer.DEFAULT;
    double expected = ((1.0 - RATE) - FUTURE_TRADE.getTradedPrice().get().getPrice()) *
        FUTURE.getAccrualFactor() * FUTURE.getNotional() * FUTURE_TRADE.getQuantity();
    CurrencyAmount computed = pricerFn.presentValue(FUTURE_TRADE, prov, lastClosingPrice);
    assertThat(computed.getAmount()).isCloseTo(expected, offset(TOLERANCE_PV));
    assertThat(computed.getCurrency()).isEqualTo(FUTURE.getCurrency());
  }

  //-------------------------------------------------------------------------   
  @Test
  public void test_presentValueSensitivity() {
    IborIndexRates mockIbor = mock(IborIndexRates.class);
    SimpleRatesProvider prov = new SimpleRatesProvider();
    prov.setIborRates(mockIbor);

    PointSensitivities sensiPrice = PRICER_PRODUCT.priceSensitivity(FUTURE, prov);
    PointSensitivities sensiPresentValueExpected = sensiPrice.multipliedBy(
        FUTURE.getNotional() * FUTURE.getAccrualFactor() * FUTURE_TRADE.getQuantity());
    PointSensitivities sensiPresentValueComputed = PRICER_TRADE.presentValueSensitivity(FUTURE_TRADE, prov);
    assertThat(sensiPresentValueComputed.equalWithTolerance(sensiPresentValueExpected, TOLERANCE_PV_DELTA)).isTrue();
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_parSpreadSensitivity() {
    IborIndexRates mockIbor = mock(IborIndexRates.class);
    SimpleRatesProvider prov = new SimpleRatesProvider();
    prov.setIborRates(mockIbor);

    PointSensitivities sensiExpected = PRICER_PRODUCT.priceSensitivity(FUTURE, prov);
    PointSensitivities sensiComputed = PRICER_TRADE.parSpreadSensitivity(FUTURE_TRADE, prov);
    assertThat(sensiComputed.equalWithTolerance(sensiExpected, TOLERANCE_PRICE_DELTA)).isTrue();
  }

}
