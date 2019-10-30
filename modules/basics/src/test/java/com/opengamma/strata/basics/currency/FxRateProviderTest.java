/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.currency;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

/**
 * Test {@link FxRateProvider}.
 */
public class FxRateProviderTest {

  @Test
  public void noConversion() {
    FxRateProvider test = FxRateProvider.noConversion();
    assertThatIllegalArgumentException().isThrownBy(() -> test.fxRate(GBP, GBP));
    assertThatIllegalArgumentException().isThrownBy(() -> test.fxRate(GBP, USD));
  }

  @Test
  public void minimal() {
    FxRateProvider test = FxRateProvider.minimal();
    assertThat(test.fxRate(GBP, GBP)).isEqualTo(1d);
    assertThatIllegalArgumentException().isThrownBy(() -> test.fxRate(GBP, USD));
  }

  @Test
  public void emptyMatrixCanHandleTrivialRate() {
    FxRateProvider test = (ccy1, ccy2) -> {
      return 2.5d;
    };
    assertThat(test.fxRate(CurrencyPair.of(GBP, USD))).isEqualTo(2.5d);
  }

}
