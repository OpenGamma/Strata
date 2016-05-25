/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.surface;

import static org.testng.Assert.assertEquals;

import java.util.List;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.product.swap.type.FixedIborSwapConvention;

/**
 * Test {@link SurfaceInfoType}.
 */
@Test
public class SurfaceInfoTypeTest {

  public void test_DAY_COUNT() {
    SurfaceInfoType<DayCount> test = SurfaceInfoType.DAY_COUNT;
    assertEquals(test.toString(), "DayCount");
  }

  public void test_SWAP_CONVENTION() {
    SurfaceInfoType<FixedIborSwapConvention> test = SurfaceInfoType.SWAP_CONVENTION;
    assertEquals(test.toString(), "SwapConvention");
  }

  public void test_DATA_SENSITIVITY_INFO() {
    SurfaceInfoType<List<DoubleArray>> test = SurfaceInfoType.DATA_SENSITIVITY_INFO;
    assertEquals(test.toString(), "DataSensitivity");
  }

  public void coverage() {
    SurfaceInfoType<String> test = SurfaceInfoType.of("Foo");
    assertEquals(test.toString(), "Foo");
  }

}
