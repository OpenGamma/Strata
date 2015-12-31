/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.market;

import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.util.Map;
import java.util.Optional;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;

/**
 * Test {@link TypedReferenceData} and {@link ImmutableTypedReferenceData}.
 */
@Test
public class TypedReferenceDataTest {

  private static final LocalDateDoubleTimeSeries TIME_SERIES = LocalDateDoubleTimeSeries.builder()
      .put(date(2011, 3, 8), 1.1)
      .put(date(2011, 3, 10), 1.2)
      .build();
  private static final LocalDateDoubleTimeSeries TIME_SERIES2 = LocalDateDoubleTimeSeries.builder()
      .put(date(2011, 3, 8), 1.1)
      .build();

  //-------------------------------------------------------------------------
  public void test_of_single() {
    ImmutableTypedReferenceData test = ImmutableTypedReferenceData.of(LocalDateDoubleTimeSeries.class, TIME_SERIES);
    assertEquals(test.types(), ImmutableSet.of(LocalDateDoubleTimeSeries.class));
  }

  public void test_of() {
    Map<Class<?>, Object> dataMap = ImmutableMap.of(LocalDateDoubleTimeSeries.class, TIME_SERIES, String.class, "foo");
    TypedReferenceData test = TypedReferenceData.of(dataMap);
    assertEquals(test.types(), ImmutableSet.of(LocalDateDoubleTimeSeries.class, String.class));

    assertEquals(test.containsValue(LocalDateDoubleTimeSeries.class), true);
    assertEquals(test.getValue(LocalDateDoubleTimeSeries.class), TIME_SERIES);
    assertEquals(test.findValue(LocalDateDoubleTimeSeries.class), Optional.of(TIME_SERIES));

    assertEquals(test.containsValue(String.class), true);
    assertEquals(test.getValue(String.class), "foo");
    assertEquals(test.findValue(String.class), Optional.of("foo"));

    assertEquals(test.containsValue(Integer.class), false);
    assertThrowsIllegalArg(() -> test.getValue(Integer.class));
    assertEquals(test.findValue(Integer.class), Optional.empty());
  }

  public void test_empty() {
    ReferenceData test = ReferenceData.empty();
    assertEquals(test.identifiers(), ImmutableSet.of());
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    ImmutableTypedReferenceData test = ImmutableTypedReferenceData.of(LocalDateDoubleTimeSeries.class, TIME_SERIES);
    coverImmutableBean(test);
    ImmutableTypedReferenceData test2 = ImmutableTypedReferenceData.of(LocalDateDoubleTimeSeries.class, TIME_SERIES2);
    coverBeanEquals(test, test2);
  }

}
