/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.market;

import static com.opengamma.strata.basics.market.MarketDataBox.toMarketDataBox;
import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

@Test
public class MarketDataBoxTest {

  public void toMarketDataBoxSingle() {
    MarketDataBox<Double> box = MarketDataBox.ofSingleValue(42d);
    MarketDataBox<Double> mappedBox = box.stream().map(value -> value + 1).collect(toMarketDataBox());
    assertThat(mappedBox).isEqualTo(MarketDataBox.ofSingleValue(43d));
  }

  public void toMarketDataBoxScenario() {
    MarketDataBox<Double> box = MarketDataBox.ofScenarioValues(42d, 43d);
    MarketDataBox<Double> mappedBox = box.stream().map(value -> value + 1).collect(toMarketDataBox());
    assertThat(mappedBox).isEqualTo(MarketDataBox.ofScenarioValues(43d, 44d));
  }
}
