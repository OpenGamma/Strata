/*
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
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.index.IborIndexObservation;
import com.opengamma.strata.basics.index.Index;

/**
 * Test.
 */
@Test
public class IborAveragedRateComputationTest {

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
    IborAveragedRateComputation test = IborAveragedRateComputation.of(FIXINGS);
    assertEquals(test.getFixings(), FIXINGS);
    assertEquals(test.getTotalWeight(), 2d, 0d);
  }

  //-------------------------------------------------------------------------
  public void test_collectIndices() {
    IborAveragedRateComputation test = IborAveragedRateComputation.of(FIXINGS);
    ImmutableSet.Builder<Index> builder = ImmutableSet.builder();
    test.collectIndices(builder);
    assertEquals(builder.build(), ImmutableSet.of(GBP_LIBOR_3M));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    IborAveragedRateComputation test = IborAveragedRateComputation.of(FIXINGS);
    coverImmutableBean(test);
    IborAveragedRateComputation test2 = IborAveragedRateComputation.of(FIXINGS.subList(0, 1));
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    IborAveragedRateComputation test = IborAveragedRateComputation.of(FIXINGS);
    assertSerialization(test);
  }

}
