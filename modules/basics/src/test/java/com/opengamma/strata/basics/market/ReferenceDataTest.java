/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.market;

import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

/**
 * Test {@link ReferenceData} and {@link ImmutableReferenceData}.
 */
@Test
public class ReferenceDataTest {

  private static final TestObservableId ID1 = TestObservableId.of("1");
  private static final TestObservableId ID2 = TestObservableId.of("2");
  private static final TestObservableId ID3 = TestObservableId.of("3");
  private static final Number VAL1 = 1;
  private static final Number VAL2 = 2;
  private static final Number VAL3 = 3;

  //-------------------------------------------------------------------------
  public void test_of() {
    Map<ReferenceDataId<?>, Object> dataMap = ImmutableMap.of(ID1, VAL1, ID2, VAL2);
    ReferenceData test = ReferenceData.of(dataMap);
    assertEquals(test.identifiers(), ImmutableSet.of(ID1, ID2));

    assertEquals(test.containsValue(ID1), true);
    assertEquals(test.getValue(ID1), VAL1);
    assertEquals(test.getValue(ID1, Integer.class), VAL1);
    assertThrows(() -> test.getValue(ID1, Double.class), ClassCastException.class);
    assertEquals(test.findValue(ID1), Optional.of(VAL1));
    assertEquals(test.findValue(ID1, Integer.class), Optional.of(VAL1));
    assertEquals(test.findValue(ID1, Double.class), Optional.empty());

    assertEquals(test.containsValue(ID2), true);
    assertEquals(test.getValue(ID2), VAL2);
    assertEquals(test.findValue(ID2), Optional.of(VAL2));

    assertEquals(test.containsValue(ID3), false);
    assertThrows(() -> test.getValue(ID3), ReferenceDataNotFoundException.class);
    assertEquals(test.findValue(ID3), Optional.empty());
  }

  public void test_of_single() {
    ReferenceData test = ImmutableReferenceData.of(ID1, VAL1);
    assertEquals(test.identifiers(), ImmutableSet.of(ID1));

    assertEquals(test.containsValue(ID1), true);
    assertEquals(test.getValue(ID1), VAL1);
    assertEquals(test.findValue(ID1), Optional.of(VAL1));

    assertEquals(test.containsValue(ID2), false);
    assertThrows(() -> test.getValue(ID2), ReferenceDataNotFoundException.class);
    assertEquals(test.findValue(ID2), Optional.empty());
  }

  public void test_empty() {
    ReferenceData test = ReferenceData.empty();
    assertEquals(test.identifiers(), ImmutableSet.of());

    assertEquals(test.containsValue(ID1), false);
    assertThrows(() -> test.getValue(ID1), ReferenceDataNotFoundException.class);
    assertEquals(test.findValue(ID1), Optional.empty());
  }

  public void test_of_badType() {
    Map<ReferenceDataId<?>, Object> dataMap = ImmutableMap.of(ID1, "67");  // not a Number
    assertThrows(() -> ReferenceData.of(dataMap), ClassCastException.class);
  }

  public void test_of_null() {
    Map<ReferenceDataId<?>, Object> dataMap = new HashMap<>();
    dataMap.put(ID1, null);
    assertThrows(() -> ReferenceData.of(dataMap), IllegalArgumentException.class);
  }

  //-------------------------------------------------------------------------
  public void test_combinedWith_noClash() {
    Map<ReferenceDataId<?>, Object> dataMap1 = ImmutableMap.of(ID1, VAL1);
    ReferenceData test1 = ReferenceData.of(dataMap1);
    Map<ReferenceDataId<?>, Object> dataMap2 = ImmutableMap.of(ID2, VAL2);
    ReferenceData test2 = ReferenceData.of(dataMap2);

    ReferenceData test = test1.combinedWith(test2);
    assertEquals(test.identifiers(), ImmutableSet.of(ID1, ID2));
    assertEquals(test.getValue(ID1), VAL1);
    assertEquals(test.getValue(ID2), VAL2);
  }

  public void test_combinedWith_noClashSame() {
    Map<ReferenceDataId<?>, Object> dataMap1 = ImmutableMap.of(ID1, VAL1);
    ReferenceData test1 = ReferenceData.of(dataMap1);
    Map<ReferenceDataId<?>, Object> dataMap2 = ImmutableMap.of(ID1, VAL1, ID2, VAL2);
    ReferenceData test2 = ReferenceData.of(dataMap2);

    ReferenceData test = test1.combinedWith(test2);
    assertEquals(test.identifiers(), ImmutableSet.of(ID1, ID2));
    assertEquals(test.getValue(ID1), VAL1);
    assertEquals(test.getValue(ID2), VAL2);
  }

  public void test_combinedWith_clash() {
    Map<ReferenceDataId<?>, Object> dataMap1 = ImmutableMap.of(ID1, VAL1);
    ReferenceData test1 = ReferenceData.of(dataMap1);
    Map<ReferenceDataId<?>, Object> dataMap2 = ImmutableMap.of(ID1, VAL3);
    ReferenceData test2 = ReferenceData.of(dataMap2);

    assertThrowsIllegalArg(() -> test1.combinedWith(test2));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    Map<ReferenceDataId<?>, Object> dataMap = ImmutableMap.of(ID1, VAL1);
    ImmutableReferenceData test = ImmutableReferenceData.of(dataMap);
    coverImmutableBean(test);
    Map<ReferenceDataId<?>, Object> dataMap2 = ImmutableMap.of(ID2, VAL2);
    ImmutableReferenceData test2 = ImmutableReferenceData.of(dataMap2);
    coverBeanEquals(test, test2);
  }

}
