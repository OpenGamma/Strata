/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import java.util.Map;
import java.util.Optional;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;

/**
 * Test {@link CombinedReferenceData}.
 */
@Test
public class CombinedReferenceDataTest {

  private static final TestingReferenceDataId ID1 = new TestingReferenceDataId("1");
  private static final TestingReferenceDataId ID2 = new TestingReferenceDataId("2");
  private static final TestingReferenceDataId ID3 = new TestingReferenceDataId("3");
  private static final TestingReferenceDataId ID4 = new TestingReferenceDataId("4");
  private static final Double VAL1 = 123d;
  private static final Double VAL2 = 234d;
  private static final Double VAL3 = 999d;
  private static final ImmutableReferenceData BASE_DATA1 = baseData1();
  private static final ImmutableReferenceData BASE_DATA2 = baseData2();

  //-------------------------------------------------------------------------
  public void test_combination() {
    CombinedReferenceData test = new CombinedReferenceData(BASE_DATA1, BASE_DATA2);
    assertEquals(test.containsValue(ID1), true);
    assertEquals(test.containsValue(ID2), true);
    assertEquals(test.containsValue(ID3), true);
    assertEquals(test.containsValue(ID4), false);
    assertEquals(test.getValue(ID1), VAL1);
    assertEquals(test.getValue(ID2), VAL2);
    assertEquals(test.getValue(ID3), VAL3);
    assertThrows(() -> test.getValue(ID4), ReferenceDataNotFoundException.class);
    assertEquals(test.findValue(ID1), Optional.of(VAL1));
    assertEquals(test.findValue(ID2), Optional.of(VAL2));
    assertEquals(test.findValue(ID3), Optional.of(VAL3));
    assertEquals(test.findValue(ID4), Optional.empty());
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    CombinedReferenceData test = new CombinedReferenceData(BASE_DATA1, BASE_DATA2);
    coverImmutableBean(test);
    CombinedReferenceData test2 = new CombinedReferenceData(BASE_DATA2, BASE_DATA1);
    coverBeanEquals(test, test2);
  }

  public void serialization() {
    CombinedReferenceData test = new CombinedReferenceData(BASE_DATA1, BASE_DATA2);
    assertSerialization(test);
  }

  //-------------------------------------------------------------------------
  private static ImmutableReferenceData baseData1() {
    Map<ReferenceDataId<?>, Object> dataMap = ImmutableMap.of(ID1, VAL1, ID2, VAL2);
    return ImmutableReferenceData.of(dataMap);
  }

  private static ImmutableReferenceData baseData2() {
    Map<ReferenceDataId<?>, Object> dataMap = ImmutableMap.of(ID1, VAL3, ID3, VAL3);
    return ImmutableReferenceData.of(dataMap);
  }

}
