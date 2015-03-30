/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.impl.rate.fra;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import org.testng.annotations.Test;

import com.opengamma.platform.pricer.PricingEnvironment;
import com.opengamma.platform.pricer.impl.MockPricingEnvironment;
import com.opengamma.platform.pricer.rate.fra.FraProductPricerFn;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.finance.rate.fra.ExpandedFra;

/**
 * Test.
 */
@Test
public class ExpandingFraProductPricerFnTest {

  private static final PricingEnvironment MOCK_ENV = new MockPricingEnvironment();

  public void test_presentValue() {
    CurrencyAmount expected = CurrencyAmount.of(GBP, 1000d);
    FraProductPricerFn<ExpandedFra> mockFraProductFn = mock(FraProductPricerFn.class);
    when(mockFraProductFn.presentValue(MOCK_ENV, FraDummyData.FRA.expand()))
        .thenReturn(expected);
    ExpandingFraProductPricerFn test = new ExpandingFraProductPricerFn(mockFraProductFn);
    assertEquals(test.presentValue(MOCK_ENV, FraDummyData.FRA), expected);
  }

  public void test_futureValue() {
    CurrencyAmount expected = CurrencyAmount.of(GBP, 1000d);
    FraProductPricerFn<ExpandedFra> mockFraProductFn = mock(FraProductPricerFn.class);
    when(mockFraProductFn.futureValue(MOCK_ENV, FraDummyData.FRA.expand()))
        .thenReturn(expected);
    ExpandingFraProductPricerFn test = new ExpandingFraProductPricerFn(mockFraProductFn);
    assertEquals(test.futureValue(MOCK_ENV, FraDummyData.FRA), expected);
  }

}
