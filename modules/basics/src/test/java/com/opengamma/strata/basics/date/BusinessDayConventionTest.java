/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.date;

import static com.opengamma.strata.basics.date.BusinessDayConventions.FOLLOWING;
import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING_BI_MONTHLY;
import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_PRECEDING;
import static com.opengamma.strata.basics.date.BusinessDayConventions.NEAREST;
import static com.opengamma.strata.basics.date.BusinessDayConventions.NO_ADJUST;
import static com.opengamma.strata.basics.date.BusinessDayConventions.PRECEDING;
import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverEnum;
import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static java.time.DayOfWeek.SATURDAY;
import static java.time.DayOfWeek.SUNDAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.collect.named.ExtendedEnum;

/**
 * Test {@link BusinessDayConvention}.
 */
public class BusinessDayConventionTest {

  private static final LocalDate FRI_2014_07_11 = LocalDate.of(2014, 7, 11);
  private static final LocalDate SAT_2014_07_12 = LocalDate.of(2014, 7, 12);
  private static final LocalDate SUN_2014_07_13 = LocalDate.of(2014, 7, 13);
  private static final LocalDate MON_2014_07_14 = LocalDate.of(2014, 7, 14);
  private static final LocalDate TUE_2014_07_15 = LocalDate.of(2014, 7, 15);

  private static final LocalDate FRI_2014_08_29 = LocalDate.of(2014, 8, 29);
  private static final LocalDate SAT_2014_08_30 = LocalDate.of(2014, 8, 30);
  private static final LocalDate SUN_2014_08_31 = LocalDate.of(2014, 8, 31);
  private static final LocalDate MON_2014_09_01 = LocalDate.of(2014, 9, 1);

  private static final LocalDate FRI_2014_10_31 = LocalDate.of(2014, 10, 31);
  private static final LocalDate SAT_2014_11_01 = LocalDate.of(2014, 11, 1);
  private static final LocalDate SUN_2014_11_02 = LocalDate.of(2014, 11, 2);
  private static final LocalDate MON_2014_11_03 = LocalDate.of(2014, 11, 3);

  private static final LocalDate FRI_2014_11_14 = LocalDate.of(2014, 11, 14);
  private static final LocalDate SAT_2014_11_15 = LocalDate.of(2014, 11, 15);
  private static final LocalDate SUN_2014_11_16 = LocalDate.of(2014, 11, 16);
  private static final LocalDate MON_2014_11_17 = LocalDate.of(2014, 11, 17);

  //-------------------------------------------------------------------------
  public static Object[][] data_convention() {
    return new Object[][] {
        {NO_ADJUST, FRI_2014_07_11, FRI_2014_07_11},
        {NO_ADJUST, SAT_2014_07_12, SAT_2014_07_12},
        {NO_ADJUST, SUN_2014_07_13, SUN_2014_07_13},
        {NO_ADJUST, MON_2014_07_14, MON_2014_07_14},

        {FOLLOWING, FRI_2014_07_11, FRI_2014_07_11},
        {FOLLOWING, SAT_2014_07_12, MON_2014_07_14},
        {FOLLOWING, SUN_2014_07_13, MON_2014_07_14},
        {FOLLOWING, MON_2014_07_14, MON_2014_07_14},

        {FOLLOWING, FRI_2014_08_29, FRI_2014_08_29},
        {FOLLOWING, SAT_2014_08_30, MON_2014_09_01},
        {FOLLOWING, SUN_2014_08_31, MON_2014_09_01},
        {FOLLOWING, MON_2014_09_01, MON_2014_09_01},

        {FOLLOWING, FRI_2014_10_31, FRI_2014_10_31},
        {FOLLOWING, SAT_2014_11_01, MON_2014_11_03},
        {FOLLOWING, SUN_2014_11_02, MON_2014_11_03},
        {FOLLOWING, MON_2014_11_03, MON_2014_11_03},

        {MODIFIED_FOLLOWING, FRI_2014_07_11, FRI_2014_07_11},
        {MODIFIED_FOLLOWING, SAT_2014_07_12, MON_2014_07_14},
        {MODIFIED_FOLLOWING, SUN_2014_07_13, MON_2014_07_14},
        {MODIFIED_FOLLOWING, MON_2014_07_14, MON_2014_07_14},

        {MODIFIED_FOLLOWING, FRI_2014_08_29, FRI_2014_08_29},
        {MODIFIED_FOLLOWING, SAT_2014_08_30, FRI_2014_08_29},  // modified
        {MODIFIED_FOLLOWING, SUN_2014_08_31, FRI_2014_08_29},  // modified
        {MODIFIED_FOLLOWING, MON_2014_09_01, MON_2014_09_01},

        {MODIFIED_FOLLOWING, FRI_2014_10_31, FRI_2014_10_31},
        {MODIFIED_FOLLOWING, SAT_2014_11_01, MON_2014_11_03},
        {MODIFIED_FOLLOWING, SUN_2014_11_02, MON_2014_11_03},
        {MODIFIED_FOLLOWING, MON_2014_11_03, MON_2014_11_03},

        {MODIFIED_FOLLOWING_BI_MONTHLY, FRI_2014_07_11, FRI_2014_07_11},
        {MODIFIED_FOLLOWING_BI_MONTHLY, SAT_2014_07_12, MON_2014_07_14},
        {MODIFIED_FOLLOWING_BI_MONTHLY, SUN_2014_07_13, MON_2014_07_14},
        {MODIFIED_FOLLOWING_BI_MONTHLY, MON_2014_07_14, MON_2014_07_14},

        {MODIFIED_FOLLOWING_BI_MONTHLY, FRI_2014_08_29, FRI_2014_08_29},
        {MODIFIED_FOLLOWING_BI_MONTHLY, SAT_2014_08_30, FRI_2014_08_29},  // modified
        {MODIFIED_FOLLOWING_BI_MONTHLY, SUN_2014_08_31, FRI_2014_08_29},  // modified
        {MODIFIED_FOLLOWING_BI_MONTHLY, MON_2014_09_01, MON_2014_09_01},

        {MODIFIED_FOLLOWING_BI_MONTHLY, FRI_2014_10_31, FRI_2014_10_31},
        {MODIFIED_FOLLOWING_BI_MONTHLY, SAT_2014_11_01, MON_2014_11_03},
        {MODIFIED_FOLLOWING_BI_MONTHLY, SUN_2014_11_02, MON_2014_11_03},
        {MODIFIED_FOLLOWING_BI_MONTHLY, MON_2014_11_03, MON_2014_11_03},

        {MODIFIED_FOLLOWING_BI_MONTHLY, FRI_2014_11_14, FRI_2014_11_14},
        {MODIFIED_FOLLOWING_BI_MONTHLY, SAT_2014_11_15, FRI_2014_11_14},  // modified
        {MODIFIED_FOLLOWING_BI_MONTHLY, SUN_2014_11_16, MON_2014_11_17},
        {MODIFIED_FOLLOWING_BI_MONTHLY, MON_2014_11_17, MON_2014_11_17},

        {PRECEDING, FRI_2014_07_11, FRI_2014_07_11},
        {PRECEDING, SAT_2014_07_12, FRI_2014_07_11},
        {PRECEDING, SUN_2014_07_13, FRI_2014_07_11},
        {PRECEDING, MON_2014_07_14, MON_2014_07_14},

        {PRECEDING, FRI_2014_08_29, FRI_2014_08_29},
        {PRECEDING, SAT_2014_08_30, FRI_2014_08_29},
        {PRECEDING, SUN_2014_08_31, FRI_2014_08_29},
        {PRECEDING, MON_2014_09_01, MON_2014_09_01},

        {PRECEDING, FRI_2014_10_31, FRI_2014_10_31},
        {PRECEDING, SAT_2014_11_01, FRI_2014_10_31},
        {PRECEDING, SUN_2014_11_02, FRI_2014_10_31},
        {PRECEDING, MON_2014_11_03, MON_2014_11_03},

        {MODIFIED_PRECEDING, FRI_2014_07_11, FRI_2014_07_11},
        {MODIFIED_PRECEDING, SAT_2014_07_12, FRI_2014_07_11},
        {MODIFIED_PRECEDING, SUN_2014_07_13, FRI_2014_07_11},
        {MODIFIED_PRECEDING, MON_2014_07_14, MON_2014_07_14},

        {MODIFIED_PRECEDING, FRI_2014_08_29, FRI_2014_08_29},
        {MODIFIED_PRECEDING, SAT_2014_08_30, FRI_2014_08_29},
        {MODIFIED_PRECEDING, SUN_2014_08_31, FRI_2014_08_29},
        {MODIFIED_PRECEDING, MON_2014_09_01, MON_2014_09_01},

        {MODIFIED_PRECEDING, FRI_2014_10_31, FRI_2014_10_31},
        {MODIFIED_PRECEDING, SAT_2014_11_01, MON_2014_11_03},  // modified
        {MODIFIED_PRECEDING, SUN_2014_11_02, MON_2014_11_03},  // modified
        {MODIFIED_PRECEDING, MON_2014_11_03, MON_2014_11_03},

        {NEAREST, FRI_2014_07_11, FRI_2014_07_11},
        {NEAREST, SAT_2014_07_12, FRI_2014_07_11},
        {NEAREST, SUN_2014_07_13, MON_2014_07_14},
        {NEAREST, MON_2014_07_14, MON_2014_07_14},
    };
  }

  @ParameterizedTest
  @MethodSource("data_convention")
  public void test_convention(BusinessDayConvention convention, LocalDate input, LocalDate expected) {
    assertThat(convention.adjust(input, HolidayCalendars.SAT_SUN)).isEqualTo(expected);
  }

  @Test
  public void test_nearest() {
    HolidayCalendar cal = ImmutableHolidayCalendar.of(
        HolidayCalendarId.of("Test"), ImmutableList.of(MON_2014_07_14), SATURDAY, SUNDAY);
    assertThat(NEAREST.adjust(FRI_2014_07_11, cal)).isEqualTo(FRI_2014_07_11);
    assertThat(NEAREST.adjust(SAT_2014_07_12, cal)).isEqualTo(FRI_2014_07_11);
    assertThat(NEAREST.adjust(SUN_2014_07_13, cal)).isEqualTo(TUE_2014_07_15);
    assertThat(NEAREST.adjust(MON_2014_07_14, cal)).isEqualTo(TUE_2014_07_15);
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_name() {
    return new Object[][] {
        {NO_ADJUST, "NoAdjust"},
        {FOLLOWING, "Following"},
        {MODIFIED_FOLLOWING, "ModifiedFollowing"},
        {MODIFIED_FOLLOWING_BI_MONTHLY, "ModifiedFollowingBiMonthly"},
        {PRECEDING, "Preceding"},
        {MODIFIED_PRECEDING, "ModifiedPreceding"},
        {NEAREST, "Nearest"},
    };
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_name(BusinessDayConvention convention, String name) {
    assertThat(convention.getName()).isEqualTo(name);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_toString(BusinessDayConvention convention, String name) {
    assertThat(convention.toString()).isEqualTo(name);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_of_lookup(BusinessDayConvention convention, String name) {
    assertThat(BusinessDayConvention.of(name)).isEqualTo(convention);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_lenientLookup_standardNames(BusinessDayConvention convention, String name) {
    assertThat(BusinessDayConvention.extendedEnum().findLenient(name.toLowerCase(Locale.ENGLISH)).get()).isEqualTo(convention);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_extendedEnum(BusinessDayConvention convention, String name) {
    ImmutableMap<String, BusinessDayConvention> map = BusinessDayConvention.extendedEnum().lookupAll();
    assertThat(map.get(name)).isEqualTo(convention);
  }

  @Test
  public void test_of_lookup_notFound() {
    assertThatIllegalArgumentException().isThrownBy(() -> BusinessDayConvention.of("Rubbish"));
  }

  @Test
  public void test_of_lookup_null() {
    assertThatIllegalArgumentException().isThrownBy(() -> BusinessDayConvention.of(null));
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_lenient() {
    return new Object[][] {
        {"FOLLOWING", FOLLOWING},
        {"MODIFIED_FOLLOWING", MODIFIED_FOLLOWING},
        {"MODIFIED_FOLLOWING_BI_MONTHLY", MODIFIED_FOLLOWING_BI_MONTHLY},
        {"PRECEDING", PRECEDING},
        {"MODIFIED_PRECEDING", MODIFIED_PRECEDING},

        {"F", FOLLOWING},
        {"M", MODIFIED_FOLLOWING},
        {"MF", MODIFIED_FOLLOWING},
        {"P", PRECEDING},
        {"MP", MODIFIED_PRECEDING},

        {"Follow", FOLLOWING},
        {"None", NO_ADJUST},
        {"Modified", MODIFIED_FOLLOWING},
        {"Mod", MODIFIED_FOLLOWING},

        {"Modified Following", MODIFIED_FOLLOWING},
        {"ModifiedFollowing", MODIFIED_FOLLOWING},
        {"Modified Follow", MODIFIED_FOLLOWING},
        {"ModifiedFollow", MODIFIED_FOLLOWING},
        {"Mod Following", MODIFIED_FOLLOWING},
        {"ModFollowing", MODIFIED_FOLLOWING},
        {"Mod Follow", MODIFIED_FOLLOWING},
        {"ModFollow", MODIFIED_FOLLOWING},

        {"Modified Preceding", MODIFIED_PRECEDING},
        {"ModifiedPreceding", MODIFIED_PRECEDING},
        {"Mod Preceding", MODIFIED_PRECEDING},
        {"ModPreceding", MODIFIED_PRECEDING},

        {"ModFollowingBiMonthly", MODIFIED_FOLLOWING_BI_MONTHLY},
    };
  }

  @ParameterizedTest
  @MethodSource("data_lenient")
  public void test_lenientLookup_specialNames(String name, BusinessDayConvention convention) {
    assertThat(BusinessDayConvention.extendedEnum().findLenient(name.toLowerCase(Locale.ENGLISH)))
        .isEqualTo(Optional.of(convention));
  }

  @Test
  public void test_lenientLookup_constants() throws IllegalAccessException {
    Field[] fields = BusinessDayConventions.class.getDeclaredFields();
    for (Field field : fields) {
      if (Modifier.isPublic(field.getModifiers()) &&
          Modifier.isStatic(field.getModifiers()) &&
          Modifier.isFinal(field.getModifiers())) {

        String name = field.getName();
        Object value = field.get(null);
        ExtendedEnum<BusinessDayConvention> ext = BusinessDayConvention.extendedEnum();
        assertThat(ext.findLenient(name)).isEqualTo(Optional.of(value));
        assertThat(ext.findLenient(name.toLowerCase(Locale.ENGLISH))).isEqualTo(Optional.of(value));
      }
    }
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverPrivateConstructor(BusinessDayConventions.class);
    coverEnum(StandardBusinessDayConventions.class);
  }

  @Test
  public void test_serialization() {
    assertSerialization(NO_ADJUST);
  }

  @Test
  public void test_jodaConvert() {
    assertJodaConvert(BusinessDayConvention.class, NO_ADJUST);
    assertJodaConvert(BusinessDayConvention.class, MODIFIED_FOLLOWING);
  }

}
