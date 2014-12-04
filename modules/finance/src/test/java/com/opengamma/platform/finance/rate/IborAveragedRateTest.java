/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.finance.rate;

import static com.opengamma.basics.index.IborIndices.GBP_LIBOR_1M;
import static com.opengamma.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.collect.TestHelper.assertSerialization;
import static com.opengamma.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.collect.TestHelper.coverBeanEquals;
import static com.opengamma.collect.TestHelper.coverImmutableBean;
import static com.opengamma.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.util.List;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.basics.index.IborIndices;

/**
 * Test.
 */
@Test
public class IborAveragedRateTest {

  ImmutableList<IborAveragedFixing> FIXINGS = ImmutableList.of(
      IborAveragedFixing.of(date(2014, 6, 30)),
      IborAveragedFixing.of(date(2014, 7, 30)));

  public void test_of_varargs() {
    IborAveragedRate test = IborAveragedRate.of(GBP_LIBOR_3M, FIXINGS.toArray(new IborAveragedFixing[2]));
    IborAveragedRate expected = IborAveragedRate.builder()
        .index(IborIndices.GBP_LIBOR_3M)
        .fixings(FIXINGS)
        .build();
    assertEquals(test, expected);
  }

  public void test_of_varargs_null() {
    assertThrowsIllegalArg(() -> IborAveragedRate.of(null, FIXINGS));
    assertThrowsIllegalArg(() -> IborAveragedRate.of(GBP_LIBOR_3M, (IborAveragedFixing[]) null));
    assertThrowsIllegalArg(() -> IborAveragedRate.of(null, (IborAveragedFixing[]) null));
  }

  public void test_of_List() {
    IborAveragedRate test = IborAveragedRate.of(GBP_LIBOR_3M, FIXINGS);
    IborAveragedRate expected = IborAveragedRate.builder()
        .index(IborIndices.GBP_LIBOR_3M)
        .fixings(FIXINGS)
        .build();
    assertEquals(test, expected);
  }

  public void test_of_List_null() {
    assertThrowsIllegalArg(() -> IborAveragedRate.of(null, FIXINGS));
    assertThrowsIllegalArg(() -> IborAveragedRate.of(GBP_LIBOR_3M, (List<IborAveragedFixing>) null));
    assertThrowsIllegalArg(() -> IborAveragedRate.of(null, (List<IborAveragedFixing>) null));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    IborAveragedRate test = IborAveragedRate.of(GBP_LIBOR_3M, FIXINGS);
    coverImmutableBean(test);
    IborAveragedRate test2 = IborAveragedRate.of(GBP_LIBOR_1M, FIXINGS.subList(0, 1));
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    IborAveragedRate test = IborAveragedRate.of(GBP_LIBOR_3M, FIXINGS);
    assertSerialization(test);
  }

}
