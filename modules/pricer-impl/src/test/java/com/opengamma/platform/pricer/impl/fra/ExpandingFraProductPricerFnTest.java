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

/**
 * Test.
 */
@Test
public class ExpandingFraProductPricerFnTest {

  private final PricingEnvironment mockEnv = mock(PricingEnvironment.class);

  // Present value
  public void test_presentValue() {
    MultiCurrencyAmount expected = MultiCurrencyAmount.of(GBP, 1000d);
    FraProductPricerFn<ExpandedFra> mockFraLegFn = mock(FraProductPricerFn.class);
    when(mockFraLegFn.presentValue(mockEnv, FraDummyData.FRA.expand())).thenReturn(expected);
    ExpandingFraProductPricerFn test = new ExpandingFraProductPricerFn(mockFraLegFn);
    assertEquals(test.presentValue(mockEnv, FraDummyData.FRA), expected);
  }

  // Future value
  public void test_futureValue() {
    MultiCurrencyAmount expected = MultiCurrencyAmount.of(GBP, 1000d);
    FraProductPricerFn<ExpandedFra> mockFraLegFn = mock(FraProductPricerFn.class);
    when(mockFraLegFn.futureValue(mockEnv, FraDummyData.FRA.expand())).thenReturn(expected);
    ExpandingFraProductPricerFn test = new ExpandingFraProductPricerFn(mockFraLegFn);
    assertEquals(test.futureValue(mockEnv, FraDummyData.FRA), expected);
  }

}
