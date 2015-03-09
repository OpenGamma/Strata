/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.impl.fra;

import static com.opengamma.basics.currency.Currency.GBP;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.basics.currency.MultiCurrencyAmount;
import com.opengamma.platform.finance.fra.ExpandedFra;
import com.opengamma.platform.pricer.PricingEnvironment;
import com.opengamma.platform.pricer.fra.FraProductPricerFn;
import com.opengamma.platform.pricer.impl.MockPricingEnvironment;

/**
 * Test.
 */
@Test
public class ExpandingFraProductPricerFnTest {

  private static final PricingEnvironment MOCK_ENV = new MockPricingEnvironment();

  public void test_presentValue() {
    MultiCurrencyAmount expected = MultiCurrencyAmount.of(GBP, 1000d);
    FraProductPricerFn<ExpandedFra> mockFraProductFn = mock(FraProductPricerFn.class);
    when(mockFraProductFn.presentValue(MOCK_ENV, FraDummyData.FRA.expand()))
        .thenReturn(expected);
    ExpandingFraProductPricerFn test = new ExpandingFraProductPricerFn(mockFraProductFn);
    assertEquals(test.presentValue(MOCK_ENV, FraDummyData.FRA), expected);
  }

  public void test_futureValue() {
    MultiCurrencyAmount expected = MultiCurrencyAmount.of(GBP, 1000d);
    FraProductPricerFn<ExpandedFra> mockFraProductFn = mock(FraProductPricerFn.class);
    when(mockFraProductFn.futureValue(MOCK_ENV, FraDummyData.FRA.expand()))
        .thenReturn(expected);
    ExpandingFraProductPricerFn test = new ExpandingFraProductPricerFn(mockFraProductFn);
    assertEquals(test.futureValue(MOCK_ENV, FraDummyData.FRA), expected);
  }

}
