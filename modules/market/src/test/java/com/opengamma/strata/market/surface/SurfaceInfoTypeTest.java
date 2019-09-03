/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.surface;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.market.model.MoneynessType;

/**
 * Test {@link SurfaceInfoType}.
 */
public class SurfaceInfoTypeTest {

  @Test
  public void test_DAY_COUNT() {
    SurfaceInfoType<DayCount> test = SurfaceInfoType.DAY_COUNT;
    assertThat(test.toString()).isEqualTo("DayCount");
  }

  @Test
  public void test_MONEYNESS_TYPE() {
    SurfaceInfoType<MoneynessType> test = SurfaceInfoType.MONEYNESS_TYPE;
    assertThat(test.toString()).isEqualTo("MoneynessType");
  }

  @Test
  public void coverage() {
    SurfaceInfoType<String> test = SurfaceInfoType.of("Foo");
    assertThat(test.toString()).isEqualTo("Foo");
  }

}
