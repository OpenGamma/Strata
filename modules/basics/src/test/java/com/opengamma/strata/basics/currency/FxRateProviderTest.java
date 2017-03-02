/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.currency;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

/**
 * Test {@link FxRateProvider}.
 */
@Test
public class FxRateProviderTest {

  public void emptyMatrixCanHandleTrivialRate() {
    FxRateProvider test = (ccy1, ccy2) -> {
      return 2.5d;
    };
    assertThat(test.fxRate(CurrencyPair.of(GBP, USD))).isEqualTo(2.5d);
  }

}
