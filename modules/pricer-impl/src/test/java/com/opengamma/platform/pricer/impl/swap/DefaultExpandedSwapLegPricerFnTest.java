/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.impl.swap;

import static com.opengamma.basics.index.IborIndices.GBP_LIBOR_3M;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.Test;

import com.opengamma.basics.currency.Currency;
import com.opengamma.basics.index.IborIndex;
import com.opengamma.collect.tuple.Pair;
import com.opengamma.platform.finance.swap.ExpandedSwapLeg;
import com.opengamma.platform.finance.swap.PaymentEvent;
import com.opengamma.platform.finance.swap.PaymentPeriod;
import com.opengamma.platform.pricer.PricingEnvironment;
import com.opengamma.platform.pricer.impl.CurveSensitivityTestUtil;
import com.opengamma.platform.pricer.sensitivity.multicurve.ForwardRateSensitivityLD;
import com.opengamma.platform.pricer.sensitivity.multicurve.MulticurveSensitivity3LD;
import com.opengamma.platform.pricer.sensitivity.multicurve.ZeroRateSensitivityLD;
import com.opengamma.platform.pricer.swap.PaymentEventPricerFn;
import com.opengamma.platform.pricer.swap.PaymentPeriodPricerFn;

/**
 * Test.
 */
@Test
public class DefaultExpandedSwapLegPricerFnTest {

  private final PricingEnvironment mockEnv = mock(PricingEnvironment.class);
  private static final double TOL = 1.0e-12;

  public void test_presentValue() {
    PaymentPeriodPricerFn<PaymentPeriod> mockPeriod = mock(PaymentPeriodPricerFn.class);
    when(mockPeriod.presentValue(mockEnv, SwapDummyData.IBOR_RATE_PAYMENT_PERIOD))
        .thenReturn(1000d);
    PaymentEventPricerFn<PaymentEvent> mockEvent = mock(PaymentEventPricerFn.class);
    when(mockEvent.presentValue(mockEnv, SwapDummyData.NOTIONAL_EXCHANGE))
        .thenReturn(1000d);
    DefaultExpandedSwapLegPricerFn test = new DefaultExpandedSwapLegPricerFn(mockPeriod, mockEvent);
    assertEquals(test.presentValue(mockEnv, SwapDummyData.IBOR_EXPANDED_SWAP_LEG), 2000d, 0d);
  }

  public void test_futureValue() {
    PaymentPeriodPricerFn<PaymentPeriod> mockPeriod = mock(PaymentPeriodPricerFn.class);
    when(mockPeriod.futureValue(mockEnv, SwapDummyData.IBOR_RATE_PAYMENT_PERIOD))
        .thenReturn(1000d);
    PaymentEventPricerFn<PaymentEvent> mockEvent = mock(PaymentEventPricerFn.class);
    when(mockEvent.futureValue(mockEnv, SwapDummyData.NOTIONAL_EXCHANGE))
        .thenReturn(1000d);
    DefaultExpandedSwapLegPricerFn test = new DefaultExpandedSwapLegPricerFn(mockPeriod, mockEvent);
    assertEquals(test.futureValue(mockEnv, SwapDummyData.IBOR_EXPANDED_SWAP_LEG), 2000d, 0d);
  }

  /**
   * Test presentValueCurveSensitivity. 
   */
  public void test_presentValueCurveSensitivity() {
    PricingEnvironment env = mock(PricingEnvironment.class);
    PaymentPeriodPricerFn<PaymentPeriod> period = mock(PaymentPeriodPricerFn.class);
    PaymentEventPricerFn<PaymentEvent> event = mock(PaymentEventPricerFn.class);
    ExpandedSwapLeg expSwapLeg = SwapDummyData.IBOR_EXPANDED_SWAP_LEG;
    double pvPeriod = 1100d;
    double pvEvent = 1000d;
    IborIndex index = GBP_LIBOR_3M;
    Currency ccy = GBP_LIBOR_3M.getCurrency();
    LocalDate fixingDate = SwapDummyData.IBOR_RATE_OBSERVATION.getFixingDate();
    LocalDate paymentDate = SwapDummyData.IBOR_RATE_PAYMENT_PERIOD.getPaymentDate();

    List<ForwardRateSensitivityLD> forwardRateSensi = new ArrayList<>();
    ForwardRateSensitivityLD fwdSense = new ForwardRateSensitivityLD(index, fixingDate, 140.0, ccy);
    forwardRateSensi.add(fwdSense);
    MulticurveSensitivity3LD sensiPeriod = MulticurveSensitivity3LD.ofForwardRate(forwardRateSensi);
    List<ZeroRateSensitivityLD> dscRateSensi = new ArrayList<>();
    ZeroRateSensitivityLD dscSense = new ZeroRateSensitivityLD(ccy, paymentDate, -162.0, ccy);
    dscRateSensi.add(dscSense);
    MulticurveSensitivity3LD sensiDsc = MulticurveSensitivity3LD.ofZeroRate(dscRateSensi);
    sensiPeriod.add(sensiDsc);

    LocalDate paymentDateEvent = SwapDummyData.NOTIONAL_EXCHANGE.getPaymentDate();
    List<ZeroRateSensitivityLD> dscRateSensiEvent = new ArrayList<>();
    ZeroRateSensitivityLD dscSenseEvent = new ZeroRateSensitivityLD(ccy, paymentDateEvent, -134.0, ccy);
    dscRateSensiEvent.add(dscSenseEvent);
    MulticurveSensitivity3LD sensiEvent = MulticurveSensitivity3LD.ofZeroRate(dscRateSensiEvent);

    DefaultExpandedSwapLegPricerFn pricer = new DefaultExpandedSwapLegPricerFn(period, event);
    when(period.presentValueCurveSensitivity3LD(env, expSwapLeg.getPaymentPeriods().get(0))).thenReturn(
        Pair.of(pvPeriod, sensiPeriod));
    when(event.presentValueCurveSensitivity3LD(env, expSwapLeg.getPaymentEvents().get(0))).thenReturn(
        Pair.of(pvEvent, sensiEvent));
    Pair<Double, MulticurveSensitivity3LD> res = pricer.presentValueCurveSensitivity3LD(env, expSwapLeg);
    
    assertEquals(res.getFirst(), pvPeriod + pvEvent, TOL);
    sensiEvent.add(sensiPeriod);
    CurveSensitivityTestUtil.assertMulticurveSensitivity3LD(res.getSecond(), sensiEvent, TOL);
  }
}
