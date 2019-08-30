/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.fx;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.market.observable.QuoteId;

public class FxRateConfigTest {

  private static final QuoteId QUOTE_KEY = QuoteId.of(StandardId.of("test", "EUR/USD"));
  private static final CurrencyPair CURRENCY_PAIR = CurrencyPair.of(Currency.EUR, Currency.USD);

  @Test
  public void containsPair() {
    assertThat(config().getObservableRateKey(CURRENCY_PAIR)).hasValue(QUOTE_KEY);
  }

  @Test
  public void containsInversePair() {
    assertThat(config().getObservableRateKey(CURRENCY_PAIR.inverse())).hasValue(QUOTE_KEY);
  }

  @Test
  public void missingPair() {
    assertThat(config().getObservableRateKey(CurrencyPair.of(Currency.GBP, Currency.USD))).isEmpty();
  }

  @Test
  public void nonConventionPair() {
    Map<CurrencyPair, QuoteId> ratesMap = ImmutableMap.of(CurrencyPair.of(Currency.USD, Currency.EUR), QUOTE_KEY);
    String regex = "Currency pairs must be quoted using market conventions but USD/EUR is not";
    assertThatIllegalArgumentException()
        .isThrownBy(() -> FxRateConfig.builder().observableRates(ratesMap).build())
        .withMessageMatching(regex);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> FxRateConfig.of(ratesMap))
        .withMessageMatching(regex);
  }

  private static FxRateConfig config() {
    Map<CurrencyPair, QuoteId> ratesMap = ImmutableMap.of(CURRENCY_PAIR, QUOTE_KEY);
    return FxRateConfig.of(ratesMap);
  }
}
