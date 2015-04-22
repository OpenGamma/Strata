/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.pricer.impl.MockRatesProvider.RATE;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
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

  public void test_fxConvert_CurrencyAmount() {
    RatesProvider mockProv = new MockRatesProvider();
    CurrencyAmount amount = CurrencyAmount.of(GBP, 100);
    assertEquals(mockProv.fxConvert(amount, USD), CurrencyAmount.of(USD, 100d * RATE));
  }

  public void test_fxConvert_CurrencyAmount_same() {
    RatesProvider mockProv = new MockRatesProvider();
    CurrencyAmount amount = CurrencyAmount.of(USD, 100);
    assertEquals(mockProv.fxConvert(amount, USD), CurrencyAmount.of(USD, 100));
  }

  public void test_fxConvert_MultiCurrencyAmount() {
    RatesProvider mockProv = new MockRatesProvider();
    MultiCurrencyAmount amount = MultiCurrencyAmount.of(CurrencyAmount.of(GBP, 100), CurrencyAmount.of(USD, 50));
    assertEquals(mockProv.fxConvert(amount, USD), CurrencyAmount.of(USD, 100d * RATE + 50));
  }

}
