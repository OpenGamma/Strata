/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.observable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.collect.array.DoubleArray;

public class QuoteScenarioArrayTest {

  private static final QuoteScenarioArray ARRAY = QuoteScenarioArray.of(DoubleArray.of(1d, 2d, 3d));

  @Test
  public void get() {
    assertThat(ARRAY.get(0)).isEqualTo(1d);
    assertThat(ARRAY.get(1)).isEqualTo(2d);
    assertThat(ARRAY.get(2)).isEqualTo(3d);
    assertThatExceptionOfType(IndexOutOfBoundsException.class)
        .isThrownBy(() -> ARRAY.get(-1));
    assertThatExceptionOfType(IndexOutOfBoundsException.class)
        .isThrownBy(() -> ARRAY.get(3));
  }

  @Test
  public void getValues() {
    assertThat(ARRAY.getQuotes()).isEqualTo(DoubleArray.of(1d, 2d, 3d));
  }

  @Test
  public void getScenarioCount() {
    assertThat(ARRAY.getScenarioCount()).isEqualTo(3);
  }

}
