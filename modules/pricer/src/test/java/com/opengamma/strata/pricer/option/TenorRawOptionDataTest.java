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
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.date.Tenor;

/**
 * Tests {@link TenorRawOptionData}.
 */
@Test
public class TenorRawOptionDataTest {

  private static final RawOptionData DATA1 = RawOptionDataTest.sut();
  private static final RawOptionData DATA2 = RawOptionDataTest.sut2();

  private static final ImmutableMap<Tenor, RawOptionData> DATA_MAP =
      ImmutableMap.of(TENOR_3M, DATA1, TENOR_6M, DATA2);

  //-------------------------------------------------------------------------
  public void of() {
    TenorRawOptionData test = TenorRawOptionData.of(DATA_MAP);
    assertEquals(test.getData(), DATA_MAP);
    assertEquals(test.getData(TENOR_3M), DATA1);
    assertEquals(test.getTenors(), ImmutableList.of(TENOR_3M, TENOR_6M));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    TenorRawOptionData test = TenorRawOptionData.of(DATA_MAP);
    coverImmutableBean(test);
  }

  public void test_serialization() {
    TenorRawOptionData test = TenorRawOptionData.of(DATA_MAP);
    assertSerialization(test);
  }

}
