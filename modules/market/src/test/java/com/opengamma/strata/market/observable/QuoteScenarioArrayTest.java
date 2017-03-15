/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.observable;

import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

import com.opengamma.strata.collect.array.DoubleArray;

@Test
public class QuoteScenarioArrayTest {

  private static final QuoteScenarioArray ARRAY = QuoteScenarioArray.of(DoubleArray.of(1d, 2d, 3d));

  public void get() {
    assertThat(ARRAY.get(0)).isEqualTo(1d);
    assertThat(ARRAY.get(1)).isEqualTo(2d);
    assertThat(ARRAY.get(2)).isEqualTo(3d);
    assertThrows(() -> ARRAY.get(-1), IndexOutOfBoundsException.class);
    assertThrows(() -> ARRAY.get(3), IndexOutOfBoundsException.class);
  }

  public void getValues() {
    assertThat(ARRAY.getQuotes()).isEqualTo(DoubleArray.of(1d, 2d, 3d));
  }

  public void getScenarioCount() {
    assertThat(ARRAY.getScenarioCount()).isEqualTo(3);
  }

}
