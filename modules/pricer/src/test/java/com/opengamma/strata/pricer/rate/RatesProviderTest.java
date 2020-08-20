/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.pricer.impl.MockRatesProvider.RATE;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.pricer.impl.MockRatesProvider;

/**
 * Test {@link RatesProvider}.
 */
public class RatesProviderTest {

  @Test
  public void test_fxRate_CurrencyPair() {
    RatesProvider mockProv = new MockRatesProvider();
    assertThat(mockProv.fxRate(CurrencyPair.of(GBP, USD))).isEqualTo(RATE);
  }

  @Test
  public void test_indices() {
    RatesProvider mockProv = new MockRatesProvider();
    assertThat(mockProv.indices()).isEmpty();
  }

}
