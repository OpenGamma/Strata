/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * <p>
 * Please see distribution for license.
 */
package com.opengamma.strata.market.value.scenario;

import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

import com.opengamma.strata.collect.array.DoubleArray;

@Test
public class QuotesArrayTest {

  private static final QuotesArray ARRAY = QuotesArray.of(DoubleArray.of(1d, 2d, 3d));

  public void getValue() {
    assertThat(ARRAY.getValue(0)).isEqualTo(1d);
    assertThat(ARRAY.getValue(1)).isEqualTo(2d);
    assertThat(ARRAY.getValue(2)).isEqualTo(3d);
    assertThrows(() -> ARRAY.getValue(-1), IndexOutOfBoundsException.class);
    assertThrows(() -> ARRAY.getValue(3), IndexOutOfBoundsException.class);
  }

  public void getValues() {
    assertThat(ARRAY.getQuotes()).isEqualTo(DoubleArray.of(1d, 2d, 3d));
  }

  public void getScenarioCount() {
    assertThat(ARRAY.getScenarioCount()).isEqualTo(3);
  }
}
