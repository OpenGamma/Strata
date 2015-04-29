/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.pricer.impl.MockRatesProvider.RATE;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.pricer.impl.MockRatesProvider;

/**
 * Test {@link RatesProvider}.
 */
@Test
public class RatesProviderTest {

  public void test_fxRate_CurrencyPair() {
    RatesProvider mockProv = new MockRatesProvider();
    assertEquals(mockProv.fxRate(CurrencyPair.of(GBP, USD)), RATE);
  }

}
