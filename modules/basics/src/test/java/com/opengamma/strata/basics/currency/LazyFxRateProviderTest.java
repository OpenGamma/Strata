/*
 * Copyright (C) 2019 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link LazyFxRateProvider}.
 */
public class LazyFxRateProviderTest {

  @Test
  public void testLaziness() {
    @SuppressWarnings("unchecked")
    Supplier<FxRateProvider> underlying = (Supplier<FxRateProvider>) mock(Supplier.class);

    FxRateProvider provider = FxRateProvider.lazy(underlying);

    assertThat(provider.convert(1, Currency.USD, Currency.USD)).isEqualTo(1);
    assertThat(provider.fxRate(Currency.USD, Currency.USD)).isEqualTo(1);
    assertThat(provider.fxRate(CurrencyPair.of(Currency.USD, Currency.USD))).isEqualTo(1);
    verifyNoInteractions(underlying);
  }

  @Test
  public void testDelegation() {
    @SuppressWarnings("unchecked")
    Supplier<FxRateProvider> underlying = (Supplier<FxRateProvider>) mock(Supplier.class);

    double expectedRate = 3d;
    when(underlying.get())
        .thenReturn((baseCurrency, counterCurrency) -> expectedRate);

    FxRateProvider provider = FxRateProvider.lazy(underlying);

    assertThat(provider.convert(1, Currency.USD, Currency.EUR)).isEqualTo(expectedRate);
    assertThat(provider.fxRate(Currency.USD, Currency.EUR)).isEqualTo(expectedRate);
    assertThat(provider.fxRate(CurrencyPair.of(Currency.USD, Currency.EUR))).isEqualTo(expectedRate);

    // We only fetch the underlying provider once; after which it's memoized
    verify(underlying, times(1)).get();
  }
}
