/*
 * Copyright (C) 2024 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.cube;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.market.model.MoneynessType;

/**
 * Test {@link CubeInfoType}.
 */
public class CubeInfoTypeTest {

  @Test
  public void test_DAY_COUNT() {
    CubeInfoType<DayCount> test = CubeInfoType.DAY_COUNT;
    assertThat(test.toString()).isEqualTo("DayCount");
  }

  @Test
  public void test_MONEYNESS_TYPE() {
    CubeInfoType<MoneynessType> test = CubeInfoType.MONEYNESS_TYPE;
    assertThat(test.toString()).isEqualTo("MoneynessType");
  }

  @Test
  public void coverage() {
    CubeInfoType<String> test = CubeInfoType.of("Foo");
    assertThat(test.toString()).isEqualTo("Foo");
  }

}
