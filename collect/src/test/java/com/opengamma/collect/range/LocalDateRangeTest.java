/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.collect.range;

import static com.opengamma.collect.TestHelper.assertThrows;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test LocalDateRange.
 */
@Test
public class LocalDateRangeTest {

  private static final LocalDate DATE_2012_07_01 = LocalDate.of(2012, 7, 1);
  private static final LocalDate DATE_2012_07_27 = LocalDate.of(2012, 7, 27);
  private static final LocalDate DATE_2012_07_28 = LocalDate.of(2012, 7, 28);
  private static final LocalDate DATE_2012_07_29 = LocalDate.of(2012, 7, 29);
  private static final LocalDate DATE_2012_07_30 = LocalDate.of(2012, 7, 30);
  private static final LocalDate DATE_2012_07_31 = LocalDate.of(2012, 7, 31);
  private static final LocalDate DATE_2012_08_01 = LocalDate.of(2012, 8, 1);
  private static final LocalDate DATE_2012_08_31 = LocalDate.of(2012, 8, 31);

  //-------------------------------------------------------------------------
  public void test_ALL() {
    LocalDateRange test = LocalDateRange.ALL;
    assertEquals(test.getStart(), LocalDate.MIN);
    assertEquals(test.getEndInclusive(), LocalDate.MAX);
    assertEquals(test.getEndExclusive(), LocalDate.MAX);
    assertEquals(test.isUnboundedStart(), true);
    assertEquals(test.isUnboundedEnd(), true);
    assertEquals(test.toString(), "[-INFINITY,+INFINITY]");
  }

  //-------------------------------------------------------------------------
  public void test_halfOpen() {
    LocalDateRange test = LocalDateRange.halfOpen(DATE_2012_07_28, DATE_2012_07_31);
    assertEquals(test.getStart(), DATE_2012_07_28);
    assertEquals(test.getEndInclusive(), DATE_2012_07_30);
    assertEquals(test.getEndExclusive(), DATE_2012_07_31);
    assertEquals(test.isUnboundedStart(), false);
    assertEquals(test.isUnboundedEnd(), false);
    assertEquals(test.toString(), "[2012-07-28,2012-07-30]");
  }

  public void test_halfOpen_MIN() {
    LocalDateRange test = LocalDateRange.halfOpen(LocalDate.MIN, DATE_2012_07_31);
    assertEquals(test.getStart(), LocalDate.MIN);
    assertEquals(test.getEndInclusive(), DATE_2012_07_30);
    assertEquals(test.getEndExclusive(), DATE_2012_07_31);
    assertEquals(test.isUnboundedStart(), true);
    assertEquals(test.isUnboundedEnd(), false);
    assertEquals(test.toString(), "[-INFINITY,2012-07-30]");
  }

  public void test_halfOpen_MAX() {
    LocalDateRange test = LocalDateRange.halfOpen(DATE_2012_07_28, LocalDate.MAX);
    assertEquals(test.getStart(), DATE_2012_07_28);
    assertEquals(test.getEndInclusive(), LocalDate.MAX);
    assertEquals(test.getEndExclusive(), LocalDate.MAX);
    assertEquals(test.isUnboundedStart(), false);
    assertEquals(test.isUnboundedEnd(), true);
    assertEquals(test.toString(), "[2012-07-28,+INFINITY]");
  }

  public void test_halfOpen_MIN_MAX() {
    LocalDateRange test = LocalDateRange.halfOpen(LocalDate.MIN, LocalDate.MAX);
    assertEquals(test.getStart(), LocalDate.MIN);
    assertEquals(test.getEndInclusive(), LocalDate.MAX);
    assertEquals(test.getEndExclusive(), LocalDate.MAX);
    assertEquals(test.isUnboundedStart(), true);
    assertEquals(test.isUnboundedEnd(), true);
    assertEquals(test.toString(), "[-INFINITY,+INFINITY]");
  }

  public void test_halfOpen_nulls() {
    assertThrows(() -> LocalDateRange.halfOpen(null, DATE_2012_07_30), IllegalArgumentException.class);
    assertThrows(() -> LocalDateRange.halfOpen(DATE_2012_07_30, null), IllegalArgumentException.class);
    assertThrows(() -> LocalDateRange.halfOpen(null, null), IllegalArgumentException.class);
  }

  public void test_halfOpen_badOrder() {
    assertThrows(() -> LocalDateRange.halfOpen(DATE_2012_07_30, DATE_2012_07_30), IllegalArgumentException.class);
    assertThrows(() -> LocalDateRange.halfOpen(DATE_2012_07_31, DATE_2012_07_30), IllegalArgumentException.class);
  }

  //-------------------------------------------------------------------------
  public void test_closed() {
    LocalDateRange test = LocalDateRange.closed(DATE_2012_07_28, DATE_2012_07_30);
    assertEquals(test.getStart(), DATE_2012_07_28);
    assertEquals(test.getEndInclusive(), DATE_2012_07_30);
    assertEquals(test.getEndExclusive(), DATE_2012_07_31);
    assertEquals(test.isUnboundedStart(), false);
    assertEquals(test.isUnboundedEnd(), false);
    assertEquals(test.toString(), "[2012-07-28,2012-07-30]");
  }

  public void test_closed_MIN() {
    LocalDateRange test = LocalDateRange.closed(LocalDate.MIN, DATE_2012_07_30);
    assertEquals(test.getStart(), LocalDate.MIN);
    assertEquals(test.getEndInclusive(), DATE_2012_07_30);
    assertEquals(test.getEndExclusive(), DATE_2012_07_31);
    assertEquals(test.isUnboundedStart(), true);
    assertEquals(test.isUnboundedEnd(), false);
    assertEquals(test.toString(), "[-INFINITY,2012-07-30]");
  }

  public void test_closed_MAX() {
    LocalDateRange test = LocalDateRange.closed(DATE_2012_07_28, LocalDate.MAX);
    assertEquals(test.getStart(), DATE_2012_07_28);
    assertEquals(test.getEndInclusive(), LocalDate.MAX);
    assertEquals(test.getEndExclusive(), LocalDate.MAX);
    assertEquals(test.isUnboundedStart(), false);
    assertEquals(test.isUnboundedEnd(), true);
    assertEquals(test.toString(), "[2012-07-28,+INFINITY]");
  }

  public void test_closed_MIN_MAX() {
    LocalDateRange test = LocalDateRange.closed(LocalDate.MIN, LocalDate.MAX);
    assertEquals(test.getStart(), LocalDate.MIN);
    assertEquals(test.getEndInclusive(), LocalDate.MAX);
    assertEquals(test.getEndExclusive(), LocalDate.MAX);
    assertEquals(test.isUnboundedStart(), true);
    assertEquals(test.isUnboundedEnd(), true);
    assertEquals(test.toString(), "[-INFINITY,+INFINITY]");
  }

  public void test_closed_nulls() {
    assertThrows(() -> LocalDateRange.closed(null, DATE_2012_07_30), IllegalArgumentException.class);
    assertThrows(() -> LocalDateRange.closed(DATE_2012_07_30, null), IllegalArgumentException.class);
    assertThrows(() -> LocalDateRange.closed(null, null), IllegalArgumentException.class);
  }

  public void test_closed_badOrder() {
    assertThrows(() -> LocalDateRange.closed(DATE_2012_07_31, DATE_2012_07_30), IllegalArgumentException.class);
  }

  //-------------------------------------------------------------------------
  public void test_single() {
    LocalDateRange test = LocalDateRange.single(DATE_2012_07_28);
    assertEquals(test.getStart(), DATE_2012_07_28);
    assertEquals(test.getEndInclusive(), DATE_2012_07_28);
    assertEquals(test.getEndExclusive(), DATE_2012_07_29);
    assertEquals(test.isUnboundedStart(), false);
    assertEquals(test.isUnboundedEnd(), false);
    assertEquals(test.toString(), "[2012-07-28,2012-07-28]");
  }

  public void test_single_min() {
    LocalDateRange test = LocalDateRange.single(LocalDate.MIN);
    assertEquals(test.getStart(), LocalDate.MIN);
    assertEquals(test.getEndInclusive(), LocalDate.MIN);
    assertEquals(test.getEndExclusive(), LocalDate.MIN.plusDays(1));
    assertEquals(test.isUnboundedStart(), true);
    assertEquals(test.isUnboundedEnd(), false);
    assertEquals(test.toString(), "[-INFINITY," + LocalDate.MIN + "]");
  }

  public void test_single_max() {
    LocalDateRange test = LocalDateRange.single(LocalDate.MAX);
    assertEquals(test.getStart(), LocalDate.MAX);
    assertEquals(test.getEndInclusive(), LocalDate.MAX);
    assertEquals(test.getEndExclusive(), LocalDate.MAX);
    assertEquals(test.isUnboundedStart(), false);
    assertEquals(test.isUnboundedEnd(), true);
    assertEquals(test.toString(), "[" + LocalDate.MAX + ",+INFINITY]");
  }

  public void test_single_nulls() {
    assertThrows(() -> LocalDateRange.single(null), IllegalArgumentException.class);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "parseGood")
  Object[][] data_parseGood() {
    return new Object[][] {
        {"[2012-07-28,2012-07-28]", DATE_2012_07_28, DATE_2012_07_28},
        {"[2012-07-28,2012-07-30]", DATE_2012_07_28, DATE_2012_07_30},
        {"[-INFINITY,2012-07-30]", LocalDate.MIN, DATE_2012_07_30},
        {"[2012-07-28,+INFINITY]", DATE_2012_07_28, LocalDate.MAX},
        {"[-INFINITY,+INFINITY]", LocalDate.MIN, LocalDate.MAX},
    };
  }

  @Test(dataProvider = "parseGood")
  public void test_parseGood(String str, LocalDate start, LocalDate end) {
    LocalDateRange test = LocalDateRange.parse(str);
    assertEquals(test, LocalDateRange.closed(start, end));
  }

  @DataProvider(name = "parseBad")
  Object[][] data_parseBad() {
    return new Object[][] {
        {"(2012-07-28,2012-07-28]"},
        {"[2012-07-28,2012-07-28)"},
        {"(2012-07-28,2012-07-28)"},
        {"[2012-07-28--2012-07-28]"},
        {"[2012-07-28,2012-07-28,]"},
        {"[]"},
    };
  }

  @Test(dataProvider = "parseBad", expectedExceptions = {IllegalArgumentException.class, DateTimeParseException.class})
  public void test_parseBad(String str) {
    LocalDateRange.parse(str);
  }

  public void test_parse_null() {
    assertThrows(() -> LocalDateRange.parse(null), IllegalArgumentException.class);
  }

  //-------------------------------------------------------------------------
  public void test_withStart() {
    LocalDateRange base = LocalDateRange.halfOpen(DATE_2012_07_28, DATE_2012_07_31);
    LocalDateRange test = base.withStart(DATE_2012_07_27);
    assertEquals(test.getStart(), DATE_2012_07_27);
    assertEquals(test.getEndInclusive(), DATE_2012_07_30);
    assertEquals(test.getEndExclusive(), DATE_2012_07_31);
  }

  public void test_withStart_min() {
    LocalDateRange base = LocalDateRange.halfOpen(DATE_2012_07_28, DATE_2012_07_31);
    LocalDateRange test = base.withStart(LocalDate.MIN);
    assertEquals(test.getStart(), LocalDate.MIN);
    assertEquals(test.getEndInclusive(), DATE_2012_07_30);
    assertEquals(test.getEndExclusive(), DATE_2012_07_31);
  }

  public void test_withStart_invalid() {
    LocalDateRange base = LocalDateRange.halfOpen(DATE_2012_07_28, DATE_2012_07_31);
    assertThrows(() -> base.withStart(DATE_2012_07_31), IllegalArgumentException.class);
    assertThrows(() -> base.withStart(LocalDate.MAX), IllegalArgumentException.class);
    assertThrows(() -> base.withStart(d -> null), IllegalArgumentException.class);
  }

  public void test_withStart_null() {
    LocalDateRange test = LocalDateRange.halfOpen(DATE_2012_07_28, DATE_2012_07_31);
    assertThrows(() -> test.withStart(null), IllegalArgumentException.class);
  }

  //-------------------------------------------------------------------------
  public void test_withEndInclusive() {
    LocalDateRange base = LocalDateRange.halfOpen(DATE_2012_07_28, DATE_2012_07_31);
    LocalDateRange test = base.withEndInclusive(DATE_2012_07_30);
    assertEquals(test.getStart(), DATE_2012_07_28);
    assertEquals(test.getEndInclusive(), DATE_2012_07_30);
    assertEquals(test.getEndExclusive(), DATE_2012_07_31);
  }

  public void test_withEndInclusive_max() {
    LocalDateRange base = LocalDateRange.halfOpen(DATE_2012_07_28, DATE_2012_07_31);
    LocalDateRange test = base.withEndInclusive(LocalDate.MAX);
    assertEquals(test.getStart(), DATE_2012_07_28);
    assertEquals(test.getEndInclusive(), LocalDate.MAX);
    assertEquals(test.getEndExclusive(), LocalDate.MAX);
  }

  public void test_withEndInclusive_invalid() {
    LocalDateRange base = LocalDateRange.halfOpen(DATE_2012_07_28, DATE_2012_07_31);
    assertThrows(() -> base.withEndInclusive(DATE_2012_07_27), IllegalArgumentException.class);
    assertThrows(() -> base.withEndInclusive(LocalDate.MIN), IllegalArgumentException.class);
    assertThrows(() -> base.withEndInclusive(d -> null), IllegalArgumentException.class);
  }

  public void test_withEndInclusive_null() {
    LocalDateRange test = LocalDateRange.halfOpen(DATE_2012_07_28, DATE_2012_07_31);
    assertThrows(() -> test.withEndInclusive(null), IllegalArgumentException.class);
  }

  //-------------------------------------------------------------------------
  public void test_contains() {
    LocalDateRange test = LocalDateRange.halfOpen(DATE_2012_07_28, DATE_2012_07_31);
    assertEquals(test.contains(LocalDate.MIN), false);
    assertEquals(test.contains(DATE_2012_07_27), false);
    assertEquals(test.contains(DATE_2012_07_28), true);
    assertEquals(test.contains(DATE_2012_07_29), true);
    assertEquals(test.contains(DATE_2012_07_30), true);
    assertEquals(test.contains(DATE_2012_07_31), false);
    assertEquals(test.contains(DATE_2012_08_01), false);
    assertEquals(test.contains(LocalDate.MAX), false);
  }

  public void test_contains_null() {
    LocalDateRange test = LocalDateRange.halfOpen(DATE_2012_07_28, DATE_2012_07_31);
    assertThrows(() -> test.contains(null), IllegalArgumentException.class);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "encloses")
  Object[][] data_encloses() {
    return new Object[][] {
        // before start
        {DATE_2012_07_01, DATE_2012_07_27, false},
        // before end
        {DATE_2012_07_27, DATE_2012_07_30, false},
        {DATE_2012_07_28, DATE_2012_07_30, true},
        {DATE_2012_07_29, DATE_2012_07_30, true},
        // same end
        {DATE_2012_07_27, DATE_2012_07_31, false},
        {DATE_2012_07_28, DATE_2012_07_31, true},
        {DATE_2012_07_29, DATE_2012_07_31, true},
        {DATE_2012_07_30, DATE_2012_07_31, true},
        // past end
        {DATE_2012_07_27, DATE_2012_08_01, false},
        {DATE_2012_07_28, DATE_2012_08_01, false},
        {DATE_2012_07_29, DATE_2012_08_01, false},
        {DATE_2012_07_30, DATE_2012_08_01, false},
        // start past end
        {DATE_2012_07_31, DATE_2012_08_01, false},
        {DATE_2012_07_31, DATE_2012_08_31, false},
        // min
        {LocalDate.MIN, DATE_2012_07_27, false},
        {LocalDate.MIN, DATE_2012_07_28, false},
        {LocalDate.MIN, DATE_2012_07_29, false},
        {LocalDate.MIN, DATE_2012_07_30, false},
        {LocalDate.MIN, DATE_2012_07_31, false},
        {LocalDate.MIN, DATE_2012_08_01, false},
        {LocalDate.MIN, LocalDate.MAX, false},
        // max
        {DATE_2012_07_27, LocalDate.MAX, false},
        {DATE_2012_07_28, LocalDate.MAX, false},
        {DATE_2012_07_29, LocalDate.MAX, false},
        {DATE_2012_07_30, LocalDate.MAX, false},
        {DATE_2012_07_31, LocalDate.MAX, false},
        {DATE_2012_08_01, LocalDate.MAX, false},
    };
  }

  @Test(dataProvider = "encloses")
  public void test_encloses(LocalDate start, LocalDate end, boolean enclosedBy) {
    LocalDateRange test = LocalDateRange.halfOpen(DATE_2012_07_28, DATE_2012_07_31);
    assertEquals(test.encloses(LocalDateRange.halfOpen(start, end)), enclosedBy);
  }

  public void test_encloses_null() {
    LocalDateRange test = LocalDateRange.halfOpen(DATE_2012_07_28, DATE_2012_07_31);
    assertThrows(() -> test.encloses(null), IllegalArgumentException.class);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "isBefore")
  Object[][] data_isBefore() {
    return new Object[][] {
        // before start
        {DATE_2012_07_01, DATE_2012_07_27, false},
        // before end
        {DATE_2012_07_27, DATE_2012_07_30, false},
        {DATE_2012_07_28, DATE_2012_07_30, false},
        {DATE_2012_07_29, DATE_2012_07_30, false},
        // same end
        {DATE_2012_07_27, DATE_2012_07_31, false},
        {DATE_2012_07_28, DATE_2012_07_31, false},
        {DATE_2012_07_29, DATE_2012_07_31, false},
        {DATE_2012_07_30, DATE_2012_07_31, false},
        // past end
        {DATE_2012_07_27, DATE_2012_08_01, false},
        {DATE_2012_07_28, DATE_2012_08_01, false},
        {DATE_2012_07_29, DATE_2012_08_01, false},
        {DATE_2012_07_30, DATE_2012_08_01, false},
        // start past end
        {DATE_2012_07_31, DATE_2012_08_01, true},
        {DATE_2012_07_31, DATE_2012_08_31, true},
        // min
        {LocalDate.MIN, DATE_2012_07_27, false},
        {LocalDate.MIN, DATE_2012_07_28, false},
        {LocalDate.MIN, DATE_2012_07_29, false},
        {LocalDate.MIN, DATE_2012_07_30, false},
        {LocalDate.MIN, DATE_2012_07_31, false},
        {LocalDate.MIN, DATE_2012_08_01, false},
        {LocalDate.MIN, LocalDate.MAX, false},
        // max
        {DATE_2012_07_27, LocalDate.MAX, false},
        {DATE_2012_07_28, LocalDate.MAX, false},
        {DATE_2012_07_29, LocalDate.MAX, false},
        {DATE_2012_07_30, LocalDate.MAX, false},
        {DATE_2012_07_31, LocalDate.MAX, true},
        {DATE_2012_08_01, LocalDate.MAX, true},
    };
  }

  @Test(dataProvider = "isBefore")
  public void test_isBefore(LocalDate start, LocalDate end, boolean before) {
    LocalDateRange test = LocalDateRange.halfOpen(DATE_2012_07_28, DATE_2012_07_31);
    assertEquals(test.isBefore(LocalDateRange.halfOpen(start, end)), before);
  }

  public void test_isBefore_null() {
    LocalDateRange test = LocalDateRange.halfOpen(DATE_2012_07_28, DATE_2012_07_31);
    assertThrows(() -> test.isBefore(null), IllegalArgumentException.class);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "isAfter")
  Object[][] data_isAfter() {
    return new Object[][] {
        // before start
        {DATE_2012_07_01, DATE_2012_07_27, true},
        // before end
        {DATE_2012_07_27, DATE_2012_07_30, false},
        {DATE_2012_07_28, DATE_2012_07_30, false},
        {DATE_2012_07_29, DATE_2012_07_30, false},
        // same end
        {DATE_2012_07_27, DATE_2012_07_31, false},
        {DATE_2012_07_28, DATE_2012_07_31, false},
        {DATE_2012_07_29, DATE_2012_07_31, false},
        {DATE_2012_07_30, DATE_2012_07_31, false},
        // past end
        {DATE_2012_07_27, DATE_2012_08_01, false},
        {DATE_2012_07_28, DATE_2012_08_01, false},
        {DATE_2012_07_29, DATE_2012_08_01, false},
        {DATE_2012_07_30, DATE_2012_08_01, false},
        // start past end
        {DATE_2012_07_31, DATE_2012_08_01, false},
        {DATE_2012_07_31, DATE_2012_08_31, false},
        // min
        {LocalDate.MIN, DATE_2012_07_27, true},
        {LocalDate.MIN, DATE_2012_07_28, true},
        {LocalDate.MIN, DATE_2012_07_29, false},
        {LocalDate.MIN, DATE_2012_07_30, false},
        {LocalDate.MIN, DATE_2012_07_31, false},
        {LocalDate.MIN, DATE_2012_08_01, false},
        {LocalDate.MIN, LocalDate.MAX, false},
        // max
        {DATE_2012_07_27, LocalDate.MAX, false},
        {DATE_2012_07_28, LocalDate.MAX, false},
        {DATE_2012_07_29, LocalDate.MAX, false},
        {DATE_2012_07_30, LocalDate.MAX, false},
        {DATE_2012_07_31, LocalDate.MAX, false},
        {DATE_2012_08_01, LocalDate.MAX, false},
    };
  }

  @Test(dataProvider = "isAfter")
  public void test_isAfter(LocalDate start, LocalDate end, boolean before) {
    LocalDateRange test = LocalDateRange.halfOpen(DATE_2012_07_28, DATE_2012_07_31);
    assertEquals(test.isAfter(LocalDateRange.halfOpen(start, end)), before);
  }

  public void test_isAfter_null() {
    LocalDateRange test = LocalDateRange.halfOpen(DATE_2012_07_28, DATE_2012_07_31);
    assertThrows(() -> test.isAfter(null), IllegalArgumentException.class);
  }

  //-------------------------------------------------------------------------
  public void test_equalsHashCode() {
    LocalDateRange a1 = LocalDateRange.halfOpen(DATE_2012_07_28, DATE_2012_07_31);
    LocalDateRange a2 = LocalDateRange.halfOpen(DATE_2012_07_28, DATE_2012_07_31);
    LocalDateRange b = LocalDateRange.halfOpen(DATE_2012_07_28, DATE_2012_07_30);
    LocalDateRange c = LocalDateRange.halfOpen(DATE_2012_07_30, DATE_2012_07_31);
    
    assertEquals(a1.equals(a1), true);
    assertEquals(a1.equals(a2), true);
    assertEquals(a1.equals(b), false);
    assertEquals(a1.equals(c), false);
    
    assertEquals(b.equals(a1), false);
    assertEquals(b.equals(b), true);
    assertEquals(b.equals(c), false);
    
    assertEquals(c.equals(a1), false);
    assertEquals(c.equals(b), false);
    assertEquals(c.equals(c), true);
    
    assertEquals(a1.equals(""), false);
    assertEquals(a1.equals(null), false);
    
    assertEquals(a2.hashCode(), a1.hashCode());
  }

}
