/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.option;

import static com.opengamma.strata.basics.date.Tenor.TENOR_3M;
import static com.opengamma.strata.basics.date.Tenor.TENOR_6M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.date.Tenor;

/**
 * Tests {@link TenorRawOptionData}.
 */
public class TenorRawOptionDataTest {

  private static final RawOptionData DATA1 = RawOptionDataTest.sut();
  private static final RawOptionData DATA2 = RawOptionDataTest.sut2();

  private static final ImmutableMap<Tenor, RawOptionData> DATA_MAP =
      ImmutableMap.of(TENOR_3M, DATA1, TENOR_6M, DATA2);

  //-------------------------------------------------------------------------
  @Test
  public void of() {
    TenorRawOptionData test = TenorRawOptionData.of(DATA_MAP);
    assertThat(test.getData()).isEqualTo(DATA_MAP);
    assertThat(test.getData(TENOR_3M)).isEqualTo(DATA1);
    assertThat(test.getTenors()).containsExactly(TENOR_3M, TENOR_6M);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    TenorRawOptionData test = TenorRawOptionData.of(DATA_MAP);
    coverImmutableBean(test);
  }

  @Test
  public void test_serialization() {
    TenorRawOptionData test = TenorRawOptionData.of(DATA_MAP);
    assertSerialization(test);
  }

}
