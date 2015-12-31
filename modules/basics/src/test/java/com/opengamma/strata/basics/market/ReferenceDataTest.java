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
 * Test {@link ReferenceData} and {@link ImmutableReferenceData}.
 */
@Test
public class ReferenceDataTest {

  private static final TestObservableId ID1 = TestObservableId.of("1");
  private static final TestObservableId ID2 = TestObservableId.of("2");
  private static final TestObservableId ID3 = TestObservableId.of("3");
  private static final LocalDateDoubleTimeSeries TIME_SERIES = LocalDateDoubleTimeSeries.builder()
      .put(date(2011, 3, 8), 1.1)
      .put(date(2011, 3, 10), 1.2)
      .build();
  private static final LocalDateDoubleTimeSeries TIME_SERIES2 = LocalDateDoubleTimeSeries.builder()
      .put(date(2011, 3, 8), 1.1)
      .build();
  private static final TypedReferenceData TRD1 = ImmutableTypedReferenceData.of(LocalDateDoubleTimeSeries.class, TIME_SERIES);
  private static final TypedReferenceData TRD1B = ImmutableTypedReferenceData.of(LocalDateDoubleTimeSeries.class, TIME_SERIES2);
  private static final TypedReferenceData TRD2 = ImmutableTypedReferenceData.of(String.class, "foo");

  //-------------------------------------------------------------------------
  public void test_of() {
    Map<ReferenceDataId, TypedReferenceData> dataMap = ImmutableMap.of(ID1, TRD1, ID2, TRD2);
    ReferenceData test = ReferenceData.of(dataMap);
    assertEquals(test.identifiers(), ImmutableSet.of(ID1, ID2));

    assertEquals(test.getTyped(ID1), TRD1);
    assertEquals(test.findTyped(ID1), Optional.of(TRD1));

    assertEquals(test.getTyped(ID2), TRD2);
    assertEquals(test.findTyped(ID2), Optional.of(TRD2));

    assertThrowsIllegalArg(() -> test.getTyped(ID3));
    assertEquals(test.findTyped(ID3), Optional.empty());
  }

  public void test_empty() {
    ReferenceData test = ReferenceData.empty();
    assertEquals(test.identifiers(), ImmutableSet.of());
  }

  //-------------------------------------------------------------------------
  public void test_queryValues() {
    Map<ReferenceDataId, TypedReferenceData> dataMap = ImmutableMap.of(ID1, TRD1, ID2, TRD2);
    ReferenceData test = ReferenceData.of(dataMap);

    assertEquals(test.containsValue(ID1, LocalDateDoubleTimeSeries.class), true);
    assertEquals(test.getValue(ID1, LocalDateDoubleTimeSeries.class), TIME_SERIES);
    assertEquals(test.findValue(ID1, LocalDateDoubleTimeSeries.class), Optional.of(TIME_SERIES));

    assertEquals(test.containsValue(ID2, String.class), true);
    assertEquals(test.getValue(ID2, String.class), "foo");
    assertEquals(test.findValue(ID2, String.class), Optional.of("foo"));

    assertEquals(test.containsValue(ID1, String.class), false);
    assertThrowsIllegalArg(() -> test.getValue(ID1, String.class));
    assertEquals(test.findValue(ID1, String.class), Optional.empty());

    assertEquals(test.containsValue(ID3, String.class), false);
    assertThrowsIllegalArg(() -> test.getValue(ID3, String.class));
    assertEquals(test.findValue(ID3, String.class), Optional.empty());
  }

  //-------------------------------------------------------------------------
  public void test_combinedWith_noClash() {
    Map<ReferenceDataId, TypedReferenceData> dataMap1 = ImmutableMap.of(ID1, TRD1);
    ReferenceData test1 = ReferenceData.of(dataMap1);
    Map<ReferenceDataId, TypedReferenceData> dataMap2 = ImmutableMap.of(ID2, TRD2);
    ReferenceData test2 = ReferenceData.of(dataMap2);

    ReferenceData test = test1.combinedWith(test2);
    assertEquals(test.identifiers(), ImmutableSet.of(ID1, ID2));
    assertEquals(test.getTyped(ID1), TRD1);
    assertEquals(test.getTyped(ID2), TRD2);
  }

  public void test_combinedWith_noClashMerge() {
    Map<ReferenceDataId, TypedReferenceData> dataMap1 = ImmutableMap.of(ID1, TRD1);
    ReferenceData test1 = ReferenceData.of(dataMap1);
    Map<ReferenceDataId, TypedReferenceData> dataMap2 = ImmutableMap.of(ID1, TRD2);
    ReferenceData test2 = ReferenceData.of(dataMap2);

    ReferenceData test = test1.combinedWith(test2);
    assertEquals(test.identifiers(), ImmutableSet.of(ID1));
    assertEquals(test.getTyped(ID1), TRD1.combinedWith(TRD2));
  }

  public void test_combinedWith_noClashSame() {
    Map<ReferenceDataId, TypedReferenceData> dataMap1 = ImmutableMap.of(ID1, TRD1);
    ReferenceData test1 = ReferenceData.of(dataMap1);
    Map<ReferenceDataId, TypedReferenceData> dataMap2 = ImmutableMap.of(ID1, TRD1, ID2, TRD2);
    ReferenceData test2 = ReferenceData.of(dataMap2);

    ReferenceData test = test1.combinedWith(test2);
    assertEquals(test.identifiers(), ImmutableSet.of(ID1, ID2));
    assertEquals(test.getTyped(ID1), TRD1);
    assertEquals(test.getTyped(ID2), TRD2);
  }

  public void test_combinedWith_clash() {
    Map<ReferenceDataId, TypedReferenceData> dataMap1 = ImmutableMap.of(ID1, TRD1);
    ReferenceData test1 = ReferenceData.of(dataMap1);
    Map<ReferenceDataId, TypedReferenceData> dataMap2 = ImmutableMap.of(ID1, TRD1B);
    ReferenceData test2 = ReferenceData.of(dataMap2);

    assertThrowsIllegalArg(() -> test1.combinedWith(test2));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    Map<ReferenceDataId, TypedReferenceData> dataMap = ImmutableMap.of(ID1, TRD1);
    ImmutableReferenceData test = ImmutableReferenceData.of(dataMap);
    coverImmutableBean(test);
    Map<ReferenceDataId, TypedReferenceData> dataMap2 = ImmutableMap.of(ID2, TRD2);
    ImmutableReferenceData test2 = ImmutableReferenceData.of(dataMap2);
    coverBeanEquals(test, test2);
  }

}
