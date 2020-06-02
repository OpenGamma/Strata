/*
 * Copyright (C) 2020 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.data;

import static com.opengamma.strata.basics.currency.Currency.CHF;
import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Test {@link MarketDataId}.
 */
public class MarketDataIdTest {

  @Test
  public void test_compareSingleType() {
    FxRateId test1 = FxRateId.of(EUR, CHF);
    FxRateId test2 = FxRateId.of(EUR, GBP);
    assertThat(test1).isLessThan(test2);
  }

  @Test
  @SuppressWarnings({"rawtypes", "unchecked"})
  public void test_compareCrossType() {
    MarketDataId test1 = FxMatrixId.standard();
    MarketDataId test2 = FxRateId.of(EUR, GBP);
    assertThat(test1).isLessThan(test2);
  }

}
