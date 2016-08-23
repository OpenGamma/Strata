/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static org.testng.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.date.HolidayCalendarIds;

/**
 * Test {@link ReferenceData} and {@link ImmutableReferenceData}.
 */
@Test
public class ReferenceDataTest {

  private static final TestingReferenceDataId ID1 = new TestingReferenceDataId("1");
  private static final TestingReferenceDataId ID2 = new TestingReferenceDataId("2");
  private static final TestingReferenceDataId ID3 = new TestingReferenceDataId("3");
  private static final Number VAL1 = 1;
  private static final Number VAL2 = 2;
  private static final Number VAL3 = 3;
  private static final ReferenceData REF_DATA1 = new ReferenceData() {
    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> findValue(ReferenceDataId<T> id) {
      return id.equals(ID1) ? Optional.of((T) VAL1) : Optional.empty();
    }
  };
  private static final ReferenceData REF_DATA2 = new ReferenceData() {
    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> findValue(ReferenceDataId<T> id) {
      return id.equals(ID2) ? Optional.of((T) VAL2) : Optional.empty();
    }
  };
  private static final ReferenceData REF_DATA3 = new ReferenceData() {
    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> findValue(ReferenceDataId<T> id) {
      return id.equals(ID1) ? Optional.of((T) VAL3) : Optional.empty();
    }
  };
  private static final ReferenceData REF_DATA12 = new ReferenceData() {
    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> findValue(ReferenceDataId<T> id) {
      return id.equals(ID2) ? Optional.of((T) VAL2) : (id.equals(ID1) ? Optional.of((T) VAL1) : Optional.empty());
    }
  };

  //-------------------------------------------------------------------------
  public void test_standard() {
    ReferenceData test = ReferenceData.standard();
    assertEquals(test.containsValue(HolidayCalendarIds.NO_HOLIDAYS), true);
    assertEquals(test.containsValue(HolidayCalendarIds.SAT_SUN), true);
    assertEquals(test.containsValue(HolidayCalendarIds.FRI_SAT), true);
    assertEquals(test.containsValue(HolidayCalendarIds.THU_FRI), true);
    assertEquals(test.containsValue(HolidayCalendarIds.GBLO), true);
  }

  public void test_minimal() {
    ReferenceData test = ReferenceData.minimal();
    assertEquals(test.containsValue(HolidayCalendarIds.NO_HOLIDAYS), true);
    assertEquals(test.containsValue(HolidayCalendarIds.SAT_SUN), true);
    assertEquals(test.containsValue(HolidayCalendarIds.FRI_SAT), true);
    assertEquals(test.containsValue(HolidayCalendarIds.THU_FRI), true);
    assertEquals(test.containsValue(HolidayCalendarIds.GBLO), false);
  }

  //-------------------------------------------------------------------------
  public void test_of_RD() {
    Map<ReferenceDataId<?>, Object> dataMap = ImmutableMap.of(ID1, VAL1, ID2, VAL2);
    ReferenceData test = ReferenceData.of(dataMap);

    assertEquals(test.containsValue(HolidayCalendarIds.NO_HOLIDAYS), true);
    assertEquals(test.containsValue(HolidayCalendarIds.SAT_SUN), true);
    assertEquals(test.containsValue(HolidayCalendarIds.FRI_SAT), true);
    assertEquals(test.containsValue(HolidayCalendarIds.THU_FRI), true);

    assertEquals(test.containsValue(ID1), true);
    assertEquals(test.getValue(ID1), VAL1);
    assertEquals(test.findValue(ID1), Optional.of(VAL1));

    assertEquals(test.containsValue(ID2), true);
    assertEquals(test.getValue(ID2), VAL2);
    assertEquals(test.findValue(ID2), Optional.of(VAL2));

    assertEquals(test.containsValue(ID3), false);
    assertThrows(() -> test.getValue(ID3), ReferenceDataNotFoundException.class);
    assertEquals(test.findValue(ID3), Optional.empty());
  }

  public void test_of_IRD() {
    Map<ReferenceDataId<?>, Object> dataMap = ImmutableMap.of(ID1, VAL1, ID2, VAL2);
    ImmutableReferenceData test = ImmutableReferenceData.of(dataMap);

    assertEquals(test.containsValue(HolidayCalendarIds.NO_HOLIDAYS), false);
    assertEquals(test.containsValue(HolidayCalendarIds.SAT_SUN), false);
    assertEquals(test.containsValue(HolidayCalendarIds.FRI_SAT), false);
    assertEquals(test.containsValue(HolidayCalendarIds.THU_FRI), false);

    assertEquals(test.containsValue(ID1), true);
    assertEquals(test.getValue(ID1), VAL1);
    assertEquals(test.findValue(ID1), Optional.of(VAL1));

    assertEquals(test.containsValue(ID2), true);
    assertEquals(test.getValue(ID2), VAL2);
    assertEquals(test.findValue(ID2), Optional.of(VAL2));

    assertEquals(test.containsValue(ID3), false);
    assertThrows(() -> test.getValue(ID3), ReferenceDataNotFoundException.class);
    assertEquals(test.findValue(ID3), Optional.empty());
  }

  public void test_of_single() {
    ReferenceData test = ImmutableReferenceData.of(ID1, VAL1);

    assertEquals(test.containsValue(HolidayCalendarIds.NO_HOLIDAYS), false);
    assertEquals(test.containsValue(HolidayCalendarIds.SAT_SUN), false);
    assertEquals(test.containsValue(HolidayCalendarIds.FRI_SAT), false);
    assertEquals(test.containsValue(HolidayCalendarIds.THU_FRI), false);

    assertEquals(test.containsValue(ID1), true);
    assertEquals(test.getValue(ID1), VAL1);
    assertEquals(test.findValue(ID1), Optional.of(VAL1));

    assertEquals(test.containsValue(ID2), false);
    assertThrows(() -> test.getValue(ID2), ReferenceDataNotFoundException.class);
    assertEquals(test.findValue(ID2), Optional.empty());
  }

  public void test_empty() {
    ReferenceData test = ReferenceData.empty();

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
  public void test_defaultMethods() {
    assertEquals(REF_DATA1.containsValue(ID1), true);
    assertEquals(REF_DATA1.containsValue(ID2), false);

    assertEquals(REF_DATA1.getValue(ID1), VAL1);
    assertThrows(() -> REF_DATA1.getValue(ID2), ReferenceDataNotFoundException.class);

    assertEquals(REF_DATA1.findValue(ID1), Optional.of(VAL1));
    assertEquals(REF_DATA1.findValue(ID2), Optional.empty());

    assertEquals(REF_DATA1.queryValueOrNull(ID1), VAL1);
    assertEquals(REF_DATA1.queryValueOrNull(ID2), null);

    assertEquals(ID1.queryValueOrNull(REF_DATA1), VAL1);
    assertEquals(ID2.queryValueOrNull(REF_DATA1), null);
  }

  //-------------------------------------------------------------------------
  public void test_combinedWith_other_other_noClash() {
    ReferenceData test = REF_DATA1.combinedWith(REF_DATA2);
    assertEquals(test.getValue(ID1), VAL1);
    assertEquals(test.getValue(ID2), VAL2);
  }

  public void test_combinedWith_other_other_noClashSame() {
    ReferenceData test = REF_DATA1.combinedWith(REF_DATA12);
    assertEquals(test.getValue(ID1), VAL1);
    assertEquals(test.getValue(ID2), VAL2);
  }

  public void test_combinedWith_other_other_clash() {
    ReferenceData combined = REF_DATA1.combinedWith(REF_DATA3);
    assertEquals(combined.getValue(ID1), VAL1);
  }

  //-------------------------------------------------------------------------
  public void test_combinedWith_IRD_IRD_noClash() {
    Map<ReferenceDataId<?>, Object> dataMap1 = ImmutableMap.of(ID1, VAL1);
    ImmutableReferenceData test1 = ImmutableReferenceData.of(dataMap1);
    Map<ReferenceDataId<?>, Object> dataMap2 = ImmutableMap.of(ID2, VAL2);
    ImmutableReferenceData test2 = ImmutableReferenceData.of(dataMap2);

    ReferenceData test = test1.combinedWith(test2);
    assertEquals(test.getValue(ID1), VAL1);
    assertEquals(test.getValue(ID2), VAL2);
  }

  public void test_combinedWith_IRD_IRD_noClashSame() {
    Map<ReferenceDataId<?>, Object> dataMap1 = ImmutableMap.of(ID1, VAL1);
    ImmutableReferenceData test1 = ImmutableReferenceData.of(dataMap1);
    Map<ReferenceDataId<?>, Object> dataMap2 = ImmutableMap.of(ID1, VAL1, ID2, VAL2);
    ImmutableReferenceData test2 = ImmutableReferenceData.of(dataMap2);

    ReferenceData test = test1.combinedWith(test2);
    assertEquals(test.getValue(ID1), VAL1);
    assertEquals(test.getValue(ID2), VAL2);
  }

  public void test_combinedWith_IRD_IRD_clash() {
    Map<ReferenceDataId<?>, Object> dataMap1 = ImmutableMap.of(ID1, VAL1);
    ImmutableReferenceData test1 = ImmutableReferenceData.of(dataMap1);
    Map<ReferenceDataId<?>, Object> dataMap2 = ImmutableMap.of(ID1, VAL3);
    ImmutableReferenceData test2 = ImmutableReferenceData.of(dataMap2);
    ReferenceData combined = test1.combinedWith(test2);
    assertEquals(combined.getValue(ID1), VAL1);
  }

  //-------------------------------------------------------------------------
  public void test_combinedWith_IRD_other_noClash() {
    Map<ReferenceDataId<?>, Object> dataMap1 = ImmutableMap.of(ID1, VAL1);
    ImmutableReferenceData test1 = ImmutableReferenceData.of(dataMap1);

    ReferenceData test = test1.combinedWith(REF_DATA2);
    assertEquals(test.getValue(ID1), VAL1);
    assertEquals(test.getValue(ID2), VAL2);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    Map<ReferenceDataId<?>, Object> dataMap = ImmutableMap.of(ID1, VAL1);
    ImmutableReferenceData test = ImmutableReferenceData.of(dataMap);
    coverImmutableBean(test);
    Map<ReferenceDataId<?>, Object> dataMap2 = ImmutableMap.of(ID2, VAL2);
    ImmutableReferenceData test2 = ImmutableReferenceData.of(dataMap2);
    coverBeanEquals(test, test2);

    coverPrivateConstructor(StandardReferenceData.class);
  }

  public void test_serialization() {
    Map<ReferenceDataId<?>, Object> dataMap = ImmutableMap.of(ID1, VAL1);
    ReferenceData test = ImmutableReferenceData.of(dataMap);
    assertSerialization(test);
  }

}
