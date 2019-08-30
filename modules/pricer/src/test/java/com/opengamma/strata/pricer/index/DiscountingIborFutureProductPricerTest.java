/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.index;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.impl.MockRatesProvider;
import com.opengamma.strata.pricer.rate.IborIndexRates;
import com.opengamma.strata.pricer.rate.IborRateSensitivity;
import com.opengamma.strata.pricer.rate.SimpleRatesProvider;
import com.opengamma.strata.product.index.ResolvedIborFuture;

/**
 * Test {@link DiscountingIborFutureTradePricer}.
 */
public class DiscountingIborFutureProductPricerTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final DiscountingIborFutureProductPricer PRICER = DiscountingIborFutureProductPricer.DEFAULT;
  private static final ResolvedIborFuture FUTURE = IborFutureDummyData.IBOR_FUTURE.resolve(REF_DATA);

  private static final double RATE = 0.045;
  private static final double TOLERANCE_PRICE = 1.0e-9;
  private static final double TOLERANCE_PRICE_DELTA = 1.0e-9;

  //------------------------------------------------------------------------- 
  @Test
  public void test_marginIndex() {
    double notional = FUTURE.getNotional();
    double accrualFactor = FUTURE.getAccrualFactor();
    double price = 0.99;
    double marginIndexExpected = price * notional * accrualFactor;
    double marginIndexComputed = PRICER.marginIndex(FUTURE, price);
    assertThat(marginIndexComputed).isEqualTo(marginIndexExpected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_marginIndexSensitivity() {
    double notional = FUTURE.getNotional();
    double accrualFactor = FUTURE.getAccrualFactor();
    PointSensitivities sensiExpected = PointSensitivities.of(
        IborRateSensitivity.of(FUTURE.getIborRate().getObservation(), -notional * accrualFactor));
    PointSensitivities priceSensitivity = PRICER.priceSensitivity(FUTURE, new MockRatesProvider());
    PointSensitivities sensiComputed = PRICER.marginIndexSensitivity(FUTURE, priceSensitivity).normalized();
    assertThat(sensiComputed.equalWithTolerance(sensiExpected, 1e-5)).isTrue();
  }

  //------------------------------------------------------------------------- 
  @Test
  public void test_price() {
    IborIndexRates mockIbor = mock(IborIndexRates.class);
    SimpleRatesProvider prov = new SimpleRatesProvider();
    prov.setIborRates(mockIbor);
    when(mockIbor.rate(FUTURE.getIborRate().getObservation())).thenReturn(RATE);

    assertThat(PRICER.price(FUTURE, prov)).isCloseTo(1.0 - RATE, offset(TOLERANCE_PRICE));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_priceSensitivity() {
    IborIndexRates mockIbor = mock(IborIndexRates.class);
    SimpleRatesProvider prov = new SimpleRatesProvider();
    prov.setIborRates(mockIbor);

    PointSensitivities sensiExpected =
        PointSensitivities.of(IborRateSensitivity.of(FUTURE.getIborRate().getObservation(), -1d));
    PointSensitivities sensiComputed = PRICER.priceSensitivity(FUTURE, prov);
    assertThat(sensiComputed.equalWithTolerance(sensiExpected, TOLERANCE_PRICE_DELTA)).isTrue();
  }

}
