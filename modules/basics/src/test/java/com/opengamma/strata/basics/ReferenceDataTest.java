/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.date.HolidayCalendarIds;

/**
 * Test {@link ReferenceData} and {@link ImmutableReferenceData}.
 */
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
    public <T> T queryValueOrNull(ReferenceDataId<T> id) {
      return id.equals(ID1) ? (T) VAL1 : null;
    }
  };
  private static final ReferenceData REF_DATA2 = new ReferenceData() {
    @Override
    @SuppressWarnings("unchecked")
    public <T> T queryValueOrNull(ReferenceDataId<T> id) {
      return id.equals(ID2) ? (T) VAL2 : null;
    }
  };
  private static final ReferenceData REF_DATA3 = new ReferenceData() {
    @Override
    @SuppressWarnings("unchecked")
    public <T> T queryValueOrNull(ReferenceDataId<T> id) {
      return id.equals(ID1) ? (T) VAL3 : null;
    }
  };
  private static final ReferenceData REF_DATA12 = new ReferenceData() {
    @Override
    @SuppressWarnings("unchecked")
    public <T> T queryValueOrNull(ReferenceDataId<T> id) {
      return id.equals(ID2) ? (T) VAL2 : (id.equals(ID1) ? (T) VAL1 : null);
    }
  };

  //-------------------------------------------------------------------------
  @Test
  public void test_standard() {
    ReferenceData test = ReferenceData.standard();
    assertThat(test.containsValue(HolidayCalendarIds.NO_HOLIDAYS)).isEqualTo(true);
    assertThat(test.containsValue(HolidayCalendarIds.SAT_SUN)).isEqualTo(true);
    assertThat(test.containsValue(HolidayCalendarIds.FRI_SAT)).isEqualTo(true);
    assertThat(test.containsValue(HolidayCalendarIds.THU_FRI)).isEqualTo(true);
    assertThat(test.containsValue(HolidayCalendarIds.GBLO)).isEqualTo(true);
  }

  @Test
  public void test_minimal() {
    ReferenceData test = ReferenceData.minimal();
    assertThat(test.containsValue(HolidayCalendarIds.NO_HOLIDAYS)).isEqualTo(true);
    assertThat(test.containsValue(HolidayCalendarIds.SAT_SUN)).isEqualTo(true);
    assertThat(test.containsValue(HolidayCalendarIds.FRI_SAT)).isEqualTo(true);
    assertThat(test.containsValue(HolidayCalendarIds.THU_FRI)).isEqualTo(true);
    assertThat(test.containsValue(HolidayCalendarIds.GBLO)).isEqualTo(false);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_of_RD() {
    Map<ReferenceDataId<?>, Object> dataMap = ImmutableMap.of(ID1, VAL1, ID2, VAL2);
    ReferenceData test = ReferenceData.of(dataMap);

    assertThat(test.containsValue(HolidayCalendarIds.NO_HOLIDAYS)).isEqualTo(true);
    assertThat(test.containsValue(HolidayCalendarIds.SAT_SUN)).isEqualTo(true);
    assertThat(test.containsValue(HolidayCalendarIds.FRI_SAT)).isEqualTo(true);
    assertThat(test.containsValue(HolidayCalendarIds.THU_FRI)).isEqualTo(true);

    assertThat(test.containsValue(ID1)).isEqualTo(true);
    assertThat(test.getValue(ID1)).isEqualTo(VAL1);
    assertThat(test.findValue(ID1)).isEqualTo(Optional.of(VAL1));

    assertThat(test.containsValue(ID2)).isEqualTo(true);
    assertThat(test.getValue(ID2)).isEqualTo(VAL2);
    assertThat(test.findValue(ID2)).isEqualTo(Optional.of(VAL2));

    assertThat(test.containsValue(ID3)).isEqualTo(false);
    assertThatExceptionOfType(ReferenceDataNotFoundException.class).isThrownBy(() -> test.getValue(ID3));
    assertThat(test.findValue(ID3)).isEqualTo(Optional.empty());
  }

  @Test
  public void test_of_IRD() {
    Map<ReferenceDataId<?>, Object> dataMap = ImmutableMap.of(ID1, VAL1, ID2, VAL2);
    ImmutableReferenceData test = ImmutableReferenceData.of(dataMap);

    assertThat(test.containsValue(HolidayCalendarIds.NO_HOLIDAYS)).isEqualTo(false);
    assertThat(test.containsValue(HolidayCalendarIds.SAT_SUN)).isEqualTo(false);
    assertThat(test.containsValue(HolidayCalendarIds.FRI_SAT)).isEqualTo(false);
    assertThat(test.containsValue(HolidayCalendarIds.THU_FRI)).isEqualTo(false);

    assertThat(test.containsValue(ID1)).isEqualTo(true);
    assertThat(test.getValue(ID1)).isEqualTo(VAL1);
    assertThat(test.findValue(ID1)).isEqualTo(Optional.of(VAL1));

    assertThat(test.containsValue(ID2)).isEqualTo(true);
    assertThat(test.getValue(ID2)).isEqualTo(VAL2);
    assertThat(test.findValue(ID2)).isEqualTo(Optional.of(VAL2));

    assertThat(test.containsValue(ID3)).isEqualTo(false);
    assertThatExceptionOfType(ReferenceDataNotFoundException.class).isThrownBy(() -> test.getValue(ID3));
    assertThat(test.findValue(ID3)).isEqualTo(Optional.empty());
  }

  @Test
  public void test_of_single() {
    ReferenceData test = ImmutableReferenceData.of(ID1, VAL1);

    assertThat(test.containsValue(HolidayCalendarIds.NO_HOLIDAYS)).isEqualTo(false);
    assertThat(test.containsValue(HolidayCalendarIds.SAT_SUN)).isEqualTo(false);
    assertThat(test.containsValue(HolidayCalendarIds.FRI_SAT)).isEqualTo(false);
    assertThat(test.containsValue(HolidayCalendarIds.THU_FRI)).isEqualTo(false);

    assertThat(test.containsValue(ID1)).isEqualTo(true);
    assertThat(test.getValue(ID1)).isEqualTo(VAL1);
    assertThat(test.findValue(ID1)).isEqualTo(Optional.of(VAL1));

    assertThat(test.containsValue(ID2)).isEqualTo(false);
    assertThatExceptionOfType(ReferenceDataNotFoundException.class).isThrownBy(() -> test.getValue(ID2));
    assertThat(test.findValue(ID2)).isEqualTo(Optional.empty());
  }

  @Test
  public void test_empty() {
    ReferenceData test = ReferenceData.empty();

    assertThat(test.containsValue(ID1)).isEqualTo(false);
    assertThatExceptionOfType(ReferenceDataNotFoundException.class).isThrownBy(() -> test.getValue(ID1));
    assertThat(test.findValue(ID1)).isEqualTo(Optional.empty());
  }

  @Test
  public void test_of_badType() {
    Map<ReferenceDataId<?>, Object> dataMap = ImmutableMap.of(ID1, "67");  // not a Number
    assertThatExceptionOfType(ClassCastException.class).isThrownBy(() -> ReferenceData.of(dataMap));
  }

  @Test
  public void test_of_null() {
    Map<ReferenceDataId<?>, Object> dataMap = new HashMap<>();
    dataMap.put(ID1, null);
    assertThatIllegalArgumentException().isThrownBy(() -> ReferenceData.of(dataMap));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_defaultMethods() {
    assertThat(REF_DATA1.containsValue(ID1)).isEqualTo(true);
    assertThat(REF_DATA1.containsValue(ID2)).isEqualTo(false);

    assertThat(REF_DATA1.getValue(ID1)).isEqualTo(VAL1);
    assertThatExceptionOfType(ReferenceDataNotFoundException.class).isThrownBy(() -> REF_DATA1.getValue(ID2));

    assertThat(REF_DATA1.findValue(ID1)).isEqualTo(Optional.of(VAL1));
    assertThat(REF_DATA1.findValue(ID2)).isEqualTo(Optional.empty());

    assertThat(REF_DATA1.queryValueOrNull(ID1)).isEqualTo(VAL1);
    assertThat(REF_DATA1.queryValueOrNull(ID2)).isEqualTo(null);

    assertThat(ID1.queryValueOrNull(REF_DATA1)).isEqualTo(VAL1);
    assertThat(ID2.queryValueOrNull(REF_DATA1)).isEqualTo(null);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_combinedWith_other_other_noClash() {
    ReferenceData test = REF_DATA1.combinedWith(REF_DATA2);
    assertThat(test.getValue(ID1)).isEqualTo(VAL1);
    assertThat(test.getValue(ID2)).isEqualTo(VAL2);
  }

  @Test
  public void test_combinedWith_other_other_noClashSame() {
    ReferenceData test = REF_DATA1.combinedWith(REF_DATA12);
    assertThat(test.getValue(ID1)).isEqualTo(VAL1);
    assertThat(test.getValue(ID2)).isEqualTo(VAL2);
  }

  @Test
  public void test_combinedWith_other_other_clash() {
    ReferenceData combined = REF_DATA1.combinedWith(REF_DATA3);
    assertThat(combined.getValue(ID1)).isEqualTo(VAL1);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_combinedWith_IRD_IRD_noClash() {
    Map<ReferenceDataId<?>, Object> dataMap1 = ImmutableMap.of(ID1, VAL1);
    ImmutableReferenceData test1 = ImmutableReferenceData.of(dataMap1);
    Map<ReferenceDataId<?>, Object> dataMap2 = ImmutableMap.of(ID2, VAL2);
    ImmutableReferenceData test2 = ImmutableReferenceData.of(dataMap2);

    ReferenceData test = test1.combinedWith(test2);
    assertThat(test.getValue(ID1)).isEqualTo(VAL1);
    assertThat(test.getValue(ID2)).isEqualTo(VAL2);
  }

  @Test
  public void test_combinedWith_IRD_IRD_noClashSame() {
    Map<ReferenceDataId<?>, Object> dataMap1 = ImmutableMap.of(ID1, VAL1);
    ImmutableReferenceData test1 = ImmutableReferenceData.of(dataMap1);
    Map<ReferenceDataId<?>, Object> dataMap2 = ImmutableMap.of(ID1, VAL1, ID2, VAL2);
    ImmutableReferenceData test2 = ImmutableReferenceData.of(dataMap2);

    ReferenceData test = test1.combinedWith(test2);
    assertThat(test.getValue(ID1)).isEqualTo(VAL1);
    assertThat(test.getValue(ID2)).isEqualTo(VAL2);
  }

  @Test
  public void test_combinedWith_IRD_IRD_clash() {
    Map<ReferenceDataId<?>, Object> dataMap1 = ImmutableMap.of(ID1, VAL1);
    ImmutableReferenceData test1 = ImmutableReferenceData.of(dataMap1);
    Map<ReferenceDataId<?>, Object> dataMap2 = ImmutableMap.of(ID1, VAL3);
    ImmutableReferenceData test2 = ImmutableReferenceData.of(dataMap2);
    ReferenceData combined = test1.combinedWith(test2);
    assertThat(combined.getValue(ID1)).isEqualTo(VAL1);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_combinedWith_IRD_other_noClash() {
    Map<ReferenceDataId<?>, Object> dataMap1 = ImmutableMap.of(ID1, VAL1);
    ImmutableReferenceData test1 = ImmutableReferenceData.of(dataMap1);

    ReferenceData test = test1.combinedWith(REF_DATA2);
    assertThat(test.getValue(ID1)).isEqualTo(VAL1);
    assertThat(test.getValue(ID2)).isEqualTo(VAL2);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    Map<ReferenceDataId<?>, Object> dataMap = ImmutableMap.of(ID1, VAL1);
    ImmutableReferenceData test = ImmutableReferenceData.of(dataMap);
    coverImmutableBean(test);
    Map<ReferenceDataId<?>, Object> dataMap2 = ImmutableMap.of(ID2, VAL2);
    ImmutableReferenceData test2 = ImmutableReferenceData.of(dataMap2);
    coverBeanEquals(test, test2);

    coverPrivateConstructor(StandardReferenceData.class);
  }

  @Test
  public void test_serialization() {
    Map<ReferenceDataId<?>, Object> dataMap = ImmutableMap.of(ID1, VAL1);
    ReferenceData test = ImmutableReferenceData.of(dataMap);
    assertSerialization(test);
  }

}
