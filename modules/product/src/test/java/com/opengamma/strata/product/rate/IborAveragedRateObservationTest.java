/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.rate;

import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.index.IborIndexObservation;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.market.ReferenceData;

/**
 * Test.
 */
@Test
public class IborAveragedRateObservationTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final IborIndexObservation GBP_LIBOR_3M_OBS1 =
      IborIndexObservation.of(GBP_LIBOR_3M, date(2014, 6, 30), REF_DATA);
  private static final IborIndexObservation GBP_LIBOR_3M_OBS2 =
      IborIndexObservation.of(GBP_LIBOR_3M, date(2014, 7, 30), REF_DATA);

  ImmutableList<IborAveragedFixing> FIXINGS = ImmutableList.of(
      IborAveragedFixing.of(GBP_LIBOR_3M_OBS1),
      IborAveragedFixing.of(GBP_LIBOR_3M_OBS2));

  //-------------------------------------------------------------------------
  public void test_of_List() {
    IborAveragedRateObservation test = IborAveragedRateObservation.of(FIXINGS);
    assertEquals(test.getFixings(), FIXINGS);
    assertEquals(test.getTotalWeight(), 2d, 0d);
  }

  //-------------------------------------------------------------------------
  public void test_collectIndices() {
    IborAveragedRateObservation test = IborAveragedRateObservation.of(FIXINGS);
    ImmutableSet.Builder<Index> builder = ImmutableSet.builder();
    test.collectIndices(builder);
    assertEquals(builder.build(), ImmutableSet.of(GBP_LIBOR_3M));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    IborAveragedRateObservation test = IborAveragedRateObservation.of(FIXINGS);
    coverImmutableBean(test);
    IborAveragedRateObservation test2 = IborAveragedRateObservation.of(FIXINGS.subList(0, 1));
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    IborAveragedRateObservation test = IborAveragedRateObservation.of(FIXINGS);
    assertSerialization(test);
  }

}
