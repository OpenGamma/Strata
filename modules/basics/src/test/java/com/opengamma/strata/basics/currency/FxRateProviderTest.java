/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.currency;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Test {@link FxRateProvider}.
 */
public class FxRateProviderTest {

  @Test
  public void emptyMatrixCanHandleTrivialRate() {
    FxRateProvider test = (ccy1, ccy2) -> {
      return 2.5d;
    };
    assertThat(test.fxRate(CurrencyPair.of(GBP, USD))).isEqualTo(2.5d);
  }

}
