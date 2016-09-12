/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.surface;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.market.model.MoneynessType;

/**
 * Test {@link SurfaceInfoType}.
 */
@Test
public class SurfaceInfoTypeTest {

  public void test_DAY_COUNT() {
    SurfaceInfoType<DayCount> test = SurfaceInfoType.DAY_COUNT;
    assertEquals(test.toString(), "DayCount");
  }

  public void test_MONEYNESS_TYPE() {
    SurfaceInfoType<MoneynessType> test = SurfaceInfoType.MONEYNESS_TYPE;
    assertEquals(test.toString(), "MoneynessType");
  }

  public void coverage() {
    SurfaceInfoType<String> test = SurfaceInfoType.of("Foo");
    assertEquals(test.toString(), "Foo");
  }

}
