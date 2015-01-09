/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.impl.swap;

import static com.opengamma.collect.TestHelper.assertThrowsIllegalArg;
import static org.mockito.Mockito.mock;

import org.testng.annotations.Test;

import com.opengamma.platform.finance.swap.PaymentPeriod;
import com.opengamma.platform.pricer.PricingEnvironment;
import com.opengamma.platform.pricer.impl.swap.DispatchingPaymentPeriodPricerFn;

/**
 * Test.
 */
@Test
public class DispatchingPaymentPeriodFnTest {

  private final PricingEnvironment mockEnv = mock(PricingEnvironment.class);

  public void test_presentValue_unknownType() {
    PaymentPeriod mockPaymentPeriod = mock(PaymentPeriod.class);
    DispatchingPaymentPeriodPricerFn test = DispatchingPaymentPeriodPricerFn.DEFAULT;
    assertThrowsIllegalArg(() -> test.presentValue(mockEnv, mockPaymentPeriod));
  }

  public void test_futureValue_unknownType() {
    PaymentPeriod mockPaymentPeriod = mock(PaymentPeriod.class);
    DispatchingPaymentPeriodPricerFn test = DispatchingPaymentPeriodPricerFn.DEFAULT;
    assertThrowsIllegalArg(() -> test.futureValue(mockEnv, mockPaymentPeriod));
  }

}
