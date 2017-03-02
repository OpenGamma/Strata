/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.fx;

import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.market.observable.QuoteId;

@Test
public class FxRateConfigTest {

  private static final QuoteId QUOTE_KEY = QuoteId.of(StandardId.of("test", "EUR/USD"));
  private static final CurrencyPair CURRENCY_PAIR = CurrencyPair.of(Currency.EUR, Currency.USD);

  public void containsPair() {
    assertThat(config().getObservableRateKey(CURRENCY_PAIR)).hasValue(QUOTE_KEY);
  }

  public void containsInversePair() {
    assertThat(config().getObservableRateKey(CURRENCY_PAIR.inverse())).hasValue(QUOTE_KEY);
  }

  public void missingPair() {
    assertThat(config().getObservableRateKey(CurrencyPair.of(Currency.GBP, Currency.USD))).isEmpty();
  }

  public void nonConventionPair() {
    Map<CurrencyPair, QuoteId> ratesMap = ImmutableMap.of(CurrencyPair.of(Currency.USD, Currency.EUR), QUOTE_KEY);
    String regex = "Currency pairs must be quoted using market conventions but USD/EUR is not";
    assertThrowsIllegalArg(() -> FxRateConfig.builder().observableRates(ratesMap).build(), regex);
    assertThrowsIllegalArg(() -> FxRateConfig.of(ratesMap), regex);
  }

  private static FxRateConfig config() {
    Map<CurrencyPair, QuoteId> ratesMap = ImmutableMap.of(CURRENCY_PAIR, QUOTE_KEY);
    return FxRateConfig.of(ratesMap);
  }
}
