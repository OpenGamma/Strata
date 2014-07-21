/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.basics.date;

import static com.opengamma.basics.date.BusinessDayConvention.FOLLOWING;
import static com.opengamma.basics.date.BusinessDayConvention.MODIFIED_FOLLOWING;
import static com.opengamma.basics.date.BusinessDayConvention.MODIFIED_FOLLOWING_BI_MONTHLY;
import static com.opengamma.basics.date.BusinessDayConvention.MODIFIED_PRECEDING;
import static com.opengamma.basics.date.BusinessDayConvention.NEAREST;
import static com.opengamma.basics.date.BusinessDayConvention.NO_ADJUST;
import static com.opengamma.basics.date.BusinessDayConvention.PRECEDING;
import static com.opengamma.collect.TestHelper.assertThrows;
import static com.opengamma.collect.TestHelper.coverEnum;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test {@link BusinessDayConvention}.
 */
@Test
public class BusinessDayConventionTest {

  private static final LocalDate FRI_2014_07_11 = LocalDate.of(2014, 7, 11);
  private static final LocalDate SAT_2014_07_12 = LocalDate.of(2014, 7, 12);
  private static final LocalDate SUN_2014_07_13 = LocalDate.of(2014, 7, 13);
  private static final LocalDate MON_2014_07_14 = LocalDate.of(2014, 7, 14);

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
  @DataProvider(name = "types")
  Object[][] data_types() {
    BusinessDayConventions[] conv = BusinessDayConventions.values();
    Object[][] result = new Object[conv.length][];
    for (int i = 0; i < conv.length; i++) {
      result[i] = new Object[] {conv[i]};
    }
    return result;
  }

  @Test(dataProvider = "types")
  public void test_null(BusinessDayConvention type) {
    assertThrows(() -> type.adjust(null, BusinessDayCalendar.ALL), IllegalArgumentException.class);
    assertThrows(() -> type.adjust(FRI_2014_11_14, null), IllegalArgumentException.class);
    assertThrows(() -> type.adjust(null, null), IllegalArgumentException.class);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "convention")
  Object[][] data_convention() {
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

  @Test(dataProvider = "convention")
  public void test_convention(BusinessDayConvention convention, LocalDate input, LocalDate expected) {
    assertEquals(convention.adjust(input, BusinessDayCalendar.WEEKENDS), expected);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "name")
  Object[][] data_name() {
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

  @Test(dataProvider = "name")
  public void test_name(BusinessDayConvention convention, String name) {
    assertEquals(convention.getName(), name);
  }

  @Test(dataProvider = "name")
  public void test_of_lookup(BusinessDayConvention convention, String name) {
    assertEquals(BusinessDayConvention.of(name), convention);
  }

  public void test_of_lookup_notFound() {
    assertThrows(() -> BusinessDayConvention.of("Rubbish"), IllegalArgumentException.class);
  }

  public void test_of_lookup_null() {
    assertThrows(() -> BusinessDayConvention.of(null), IllegalArgumentException.class);
  }

  //-------------------------------------------------------------------------
  public void covergage() {
    coverEnum(BusinessDayConventions.class);
  }

}
