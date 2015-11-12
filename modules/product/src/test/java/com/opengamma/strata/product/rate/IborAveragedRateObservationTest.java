/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.rate;

import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_1M;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.util.List;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.basics.index.Index;

/**
 * Test.
 */
@Test
public class IborAveragedRateObservationTest {

  ImmutableList<IborAveragedFixing> FIXINGS = ImmutableList.of(
      IborAveragedFixing.of(date(2014, 6, 30)),
      IborAveragedFixing.of(date(2014, 7, 30)));

  public void test_of_List() {
    IborAveragedRateObservation test = IborAveragedRateObservation.of(GBP_LIBOR_3M, FIXINGS);
    IborAveragedRateObservation expected = IborAveragedRateObservation.builder()
        .index(IborIndices.GBP_LIBOR_3M)
        .fixings(IborAveragedFixing.of(date(2014, 6, 30)), IborAveragedFixing.of(date(2014, 7, 30)))
        .build();
    assertEquals(test, expected);
    assertEquals(test.getTotalWeight(), 2d, 0d);
  }

  public void test_of_List_null() {
    assertThrowsIllegalArg(() -> IborAveragedRateObservation.of(null, FIXINGS));
    assertThrowsIllegalArg(() -> IborAveragedRateObservation.of(GBP_LIBOR_3M, (List<IborAveragedFixing>) null));
    assertThrowsIllegalArg(() -> IborAveragedRateObservation.of(null, (List<IborAveragedFixing>) null));
  }

  //-------------------------------------------------------------------------
  public void test_collectIndices() {
    IborAveragedRateObservation test = IborAveragedRateObservation.of(GBP_LIBOR_3M, FIXINGS);
    ImmutableSet.Builder<Index> builder = ImmutableSet.builder();
    test.collectIndices(builder);
    assertEquals(builder.build(), ImmutableSet.of(GBP_LIBOR_3M));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    IborAveragedRateObservation test = IborAveragedRateObservation.of(GBP_LIBOR_3M, FIXINGS);
    coverImmutableBean(test);
    IborAveragedRateObservation test2 = IborAveragedRateObservation.of(GBP_LIBOR_1M, FIXINGS.subList(0, 1));
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    IborAveragedRateObservation test = IborAveragedRateObservation.of(GBP_LIBOR_3M, FIXINGS);
    assertSerialization(test);
  }

}
