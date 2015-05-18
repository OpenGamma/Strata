/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.range;

import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;

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
    assertEquals(test.isEmpty(), false);
    assertEquals(test.isUnboundedStart(), true);
    assertEquals(test.isUnboundedEnd(), true);
    assertEquals(test.toString(), "[-INFINITY,+INFINITY]");
  }

  //-------------------------------------------------------------------------
  public void test_of() {
    LocalDateRange test = LocalDateRange.of(DATE_2012_07_28, DATE_2012_07_31);
    assertEquals(test.getStart(), DATE_2012_07_28);
    assertEquals(test.getEndInclusive(), DATE_2012_07_30);
    assertEquals(test.getEndExclusive(), DATE_2012_07_31);
    assertEquals(test.isEmpty(), false);
    assertEquals(test.isUnboundedStart(), false);
    assertEquals(test.isUnboundedEnd(), false);
    assertEquals(test.toString(), "[2012-07-28,2012-07-31)");
  }

  public void test_of_MIN() {
    LocalDateRange test = LocalDateRange.of(LocalDate.MIN, DATE_2012_07_31);
    assertEquals(test.getStart(), LocalDate.MIN);
    assertEquals(test.getEndInclusive(), DATE_2012_07_30);
    assertEquals(test.getEndExclusive(), DATE_2012_07_31);
    assertEquals(test.isEmpty(), false);
    assertEquals(test.isUnboundedStart(), true);
    assertEquals(test.isUnboundedEnd(), false);
    assertEquals(test.toString(), "[-INFINITY,2012-07-31)");
  }

  public void test_of_MAX() {
    LocalDateRange test = LocalDateRange.of(DATE_2012_07_28, LocalDate.MAX);
    assertEquals(test.getStart(), DATE_2012_07_28);
    assertEquals(test.getEndInclusive(), LocalDate.MAX);
    assertEquals(test.getEndExclusive(), LocalDate.MAX);
    assertEquals(test.isEmpty(), false);
    assertEquals(test.isUnboundedStart(), false);
    assertEquals(test.isUnboundedEnd(), true);
    assertEquals(test.toString(), "[2012-07-28,+INFINITY]");
  }

  public void test_of_MIN_MAX() {
    LocalDateRange test = LocalDateRange.of(LocalDate.MIN, LocalDate.MAX);
    assertEquals(test.getStart(), LocalDate.MIN);
    assertEquals(test.getEndInclusive(), LocalDate.MAX);
    assertEquals(test.getEndExclusive(), LocalDate.MAX);
    assertEquals(test.isEmpty(), false);
    assertEquals(test.isUnboundedStart(), true);
    assertEquals(test.isUnboundedEnd(), true);
    assertEquals(test.toString(), "[-INFINITY,+INFINITY]");
  }

  public void test_of_empty() {
    LocalDateRange test = LocalDateRange.of(DATE_2012_07_30, DATE_2012_07_30);
    assertEquals(test.getStart(), DATE_2012_07_30);
    assertEquals(test.getEndInclusive(), DATE_2012_07_29);
    assertEquals(test.getEndExclusive(), DATE_2012_07_30);
    assertEquals(test.isEmpty(), true);
    assertEquals(test.isUnboundedStart(), false);
    assertEquals(test.isUnboundedEnd(), false);
    assertEquals(test.toString(), "[2012-07-30,2012-07-30)");
  }

  public void test_of_nulls() {
    assertThrowsIllegalArg(() -> LocalDateRange.of(null, DATE_2012_07_30));
    assertThrowsIllegalArg(() -> LocalDateRange.of(DATE_2012_07_30, null));
    assertThrowsIllegalArg(() -> LocalDateRange.of(null, null));
  }

  public void test_of_badOrder() {
    assertThrowsIllegalArg(() -> LocalDateRange.of(DATE_2012_07_31, DATE_2012_07_30));
  }

  //-------------------------------------------------------------------------
  public void test_ofClosed() {
    LocalDateRange test = LocalDateRange.ofClosed(DATE_2012_07_28, DATE_2012_07_30);
    assertEquals(test.getStart(), DATE_2012_07_28);
    assertEquals(test.getEndInclusive(), DATE_2012_07_30);
    assertEquals(test.getEndExclusive(), DATE_2012_07_31);
    assertEquals(test.isUnboundedStart(), false);
    assertEquals(test.isUnboundedEnd(), false);
    assertEquals(test.toString(), "[2012-07-28,2012-07-31)");
  }

  public void test_ofClosed_MIN() {
    LocalDateRange test = LocalDateRange.ofClosed(LocalDate.MIN, DATE_2012_07_30);
    assertEquals(test.getStart(), LocalDate.MIN);
    assertEquals(test.getEndInclusive(), DATE_2012_07_30);
    assertEquals(test.getEndExclusive(), DATE_2012_07_31);
    assertEquals(test.isUnboundedStart(), true);
    assertEquals(test.isUnboundedEnd(), false);
    assertEquals(test.toString(), "[-INFINITY,2012-07-31)");
  }

  public void test_ofClosed_MAX() {
    LocalDateRange test = LocalDateRange.ofClosed(DATE_2012_07_28, LocalDate.MAX);
    assertEquals(test.getStart(), DATE_2012_07_28);
    assertEquals(test.getEndInclusive(), LocalDate.MAX);
    assertEquals(test.getEndExclusive(), LocalDate.MAX);
    assertEquals(test.isUnboundedStart(), false);
    assertEquals(test.isUnboundedEnd(), true);
    assertEquals(test.toString(), "[2012-07-28,+INFINITY]");
  }

  public void test_ofClosed_MIN_MAX() {
    LocalDateRange test = LocalDateRange.ofClosed(LocalDate.MIN, LocalDate.MAX);
    assertEquals(test.getStart(), LocalDate.MIN);
    assertEquals(test.getEndInclusive(), LocalDate.MAX);
    assertEquals(test.getEndExclusive(), LocalDate.MAX);
    assertEquals(test.isUnboundedStart(), true);
    assertEquals(test.isUnboundedEnd(), true);
    assertEquals(test.toString(), "[-INFINITY,+INFINITY]");
  }

  public void test_ofClosed_nulls() {
    assertThrowsIllegalArg(() -> LocalDateRange.ofClosed(null, DATE_2012_07_30));
    assertThrowsIllegalArg(() -> LocalDateRange.ofClosed(DATE_2012_07_30, null));
    assertThrowsIllegalArg(() -> LocalDateRange.ofClosed(null, null));
  }

  public void test_ofClosed_badOrder() {
    assertThrowsIllegalArg(() -> LocalDateRange.ofClosed(DATE_2012_07_31, DATE_2012_07_30));
  }

  //-------------------------------------------------------------------------
  public void test_withStart() {
    LocalDateRange base = LocalDateRange.of(DATE_2012_07_28, DATE_2012_07_31);
    LocalDateRange test = base.withStart(DATE_2012_07_27);
    assertEquals(test.getStart(), DATE_2012_07_27);
    assertEquals(test.getEndInclusive(), DATE_2012_07_30);
    assertEquals(test.getEndExclusive(), DATE_2012_07_31);
  }

  public void test_withStart_adjuster() {
    LocalDateRange base = LocalDateRange.of(DATE_2012_07_28, DATE_2012_07_31);
    LocalDateRange test = base.withStart(date -> date.minus(1, ChronoUnit.WEEKS));
    assertEquals(test.getStart(), DATE_2012_07_28.minusWeeks(1));
    assertEquals(test.getEndInclusive(), DATE_2012_07_30);
    assertEquals(test.getEndExclusive(), DATE_2012_07_31);
  }

  public void test_withStart_min() {
    LocalDateRange base = LocalDateRange.of(DATE_2012_07_28, DATE_2012_07_31);
    LocalDateRange test = base.withStart(LocalDate.MIN);
    assertEquals(test.getStart(), LocalDate.MIN);
    assertEquals(test.getEndInclusive(), DATE_2012_07_30);
    assertEquals(test.getEndExclusive(), DATE_2012_07_31);
  }

  public void test_withStart_empty() {
    LocalDateRange base = LocalDateRange.of(DATE_2012_07_28, DATE_2012_07_31);
    LocalDateRange test = base.withStart(DATE_2012_07_31);
    assertEquals(test.getStart(), DATE_2012_07_31);
    assertEquals(test.getEndInclusive(), DATE_2012_07_30);
    assertEquals(test.getEndExclusive(), DATE_2012_07_31);
  }

  public void test_withStart_invalid() {
    LocalDateRange base = LocalDateRange.of(DATE_2012_07_28, DATE_2012_07_30);
    assertThrowsIllegalArg(() -> base.withStart(DATE_2012_07_31));
    assertThrowsIllegalArg(() -> base.withStart(LocalDate.MAX));
    assertThrowsIllegalArg(() -> base.withStart(d -> null));
  }

  public void test_withStart_null() {
    LocalDateRange test = LocalDateRange.of(DATE_2012_07_28, DATE_2012_07_31);
    assertThrowsIllegalArg(() -> test.withStart(null));
  }

  //-------------------------------------------------------------------------
  public void test_withEndExclusive() {
    LocalDateRange base = LocalDateRange.of(DATE_2012_07_28, DATE_2012_07_31);
    LocalDateRange test = base.withEndExclusive(DATE_2012_07_30);
    assertEquals(test.getStart(), DATE_2012_07_28);
    assertEquals(test.getEndInclusive(), DATE_2012_07_29);
    assertEquals(test.getEndExclusive(), DATE_2012_07_30);
  }

  public void test_withEndExclusive_adjuster() {
    LocalDateRange base = LocalDateRange.of(DATE_2012_07_28, DATE_2012_07_31);
    LocalDateRange test = base.withEndExclusive(date -> date.plus(1, ChronoUnit.WEEKS));
    assertEquals(test.getStart(), DATE_2012_07_28);
    assertEquals(test.getEndInclusive(), DATE_2012_07_30.plusWeeks(1));
    assertEquals(test.getEndExclusive(), DATE_2012_07_31.plusWeeks(1));
  }

  public void test_withEndExclusive_max() {
    LocalDateRange base = LocalDateRange.of(DATE_2012_07_28, DATE_2012_07_31);
    LocalDateRange test = base.withEndExclusive(LocalDate.MAX);
    assertEquals(test.getStart(), DATE_2012_07_28);
    assertEquals(test.getEndInclusive(), LocalDate.MAX);
    assertEquals(test.getEndExclusive(), LocalDate.MAX);
  }

  public void test_withEndExclusive_empty() {
    LocalDateRange base = LocalDateRange.of(DATE_2012_07_30, DATE_2012_07_31);
    LocalDateRange test = base.withEndExclusive(DATE_2012_07_30);
    assertEquals(test.getStart(), DATE_2012_07_30);
    assertEquals(test.getEndInclusive(), DATE_2012_07_29);
    assertEquals(test.getEndExclusive(), DATE_2012_07_30);
  }

  public void test_withEndExclusive_invalid() {
    LocalDateRange base = LocalDateRange.of(DATE_2012_07_28, DATE_2012_07_31);
    assertThrowsIllegalArg(() -> base.withEndExclusive(DATE_2012_07_27));
    assertThrowsIllegalArg(() -> base.withEndExclusive(LocalDate.MIN));
    assertThrowsIllegalArg(() -> base.withEndExclusive(d -> null));
  }

  public void test_withEndExclusive_null() {
    LocalDateRange test = LocalDateRange.of(DATE_2012_07_28, DATE_2012_07_31);
    assertThrowsIllegalArg(() -> test.withEndExclusive(null));
  }

  //-------------------------------------------------------------------------
  public void test_contains() {
    LocalDateRange test = LocalDateRange.of(DATE_2012_07_28, DATE_2012_07_31);
    assertEquals(test.contains(LocalDate.MIN), false);
    assertEquals(test.contains(DATE_2012_07_27), false);
    assertEquals(test.contains(DATE_2012_07_28), true);
    assertEquals(test.contains(DATE_2012_07_29), true);
    assertEquals(test.contains(DATE_2012_07_30), true);
    assertEquals(test.contains(DATE_2012_07_31), false);
    assertEquals(test.contains(DATE_2012_08_01), false);
    assertEquals(test.contains(LocalDate.MAX), false);
  }

  public void test_contains_empty() {
    LocalDateRange test = LocalDateRange.of(DATE_2012_07_28, DATE_2012_07_28);
    assertEquals(test.contains(LocalDate.MIN), false);
    assertEquals(test.contains(DATE_2012_07_27), false);
    assertEquals(test.contains(DATE_2012_07_28), false);
    assertEquals(test.contains(DATE_2012_07_29), false);
    assertEquals(test.contains(LocalDate.MAX), false);
  }

  public void test_contains_max() {
    LocalDateRange test = LocalDateRange.of(DATE_2012_07_28, LocalDate.MAX);
    assertEquals(test.contains(LocalDate.MIN), false);
    assertEquals(test.contains(DATE_2012_07_27), false);
    assertEquals(test.contains(DATE_2012_07_28), true);
    assertEquals(test.contains(DATE_2012_07_29), true);
    assertEquals(test.contains(DATE_2012_07_30), true);
    assertEquals(test.contains(DATE_2012_07_31), true);
    assertEquals(test.contains(DATE_2012_08_01), true);
    assertEquals(test.contains(LocalDate.MAX), true);
  }

  public void test_contains_null() {
    LocalDateRange test = LocalDateRange.of(DATE_2012_07_28, DATE_2012_07_31);
    assertThrowsIllegalArg(() -> test.contains(null));
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
        // empty
        {DATE_2012_07_27, DATE_2012_07_27, false},
        {DATE_2012_07_28, DATE_2012_07_28, true},
        {DATE_2012_07_29, DATE_2012_07_29, true},
        {DATE_2012_07_30, DATE_2012_07_30, true},
        {DATE_2012_08_31, DATE_2012_08_31, false},
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
  public void test_encloses(LocalDate start, LocalDate end, boolean isEnofClosedBy) {
    LocalDateRange test = LocalDateRange.of(DATE_2012_07_28, DATE_2012_07_31);
    assertEquals(test.encloses(LocalDateRange.of(start, end)), isEnofClosedBy);
  }

  public void test_encloses_max() {
    LocalDateRange test = LocalDateRange.of(DATE_2012_07_28, LocalDate.MAX);
    assertEquals(test.encloses(LocalDateRange.of(DATE_2012_07_28, DATE_2012_07_28)), true);
    assertEquals(test.encloses(LocalDateRange.of(DATE_2012_07_28, DATE_2012_07_29)), true);
    assertEquals(test.encloses(LocalDateRange.of(DATE_2012_07_28, LocalDate.MAX)), true);
    assertEquals(test.encloses(LocalDateRange.of(DATE_2012_07_01, DATE_2012_07_27)), false);
    assertEquals(test.encloses(LocalDateRange.of(DATE_2012_07_27, DATE_2012_07_29)), false);
    assertEquals(test.encloses(LocalDateRange.of(DATE_2012_07_27, LocalDate.MAX)), false);
  }

  public void test_encloses_empty() {
    LocalDateRange test = LocalDateRange.of(DATE_2012_07_28, DATE_2012_07_28);
    assertEquals(test.encloses(LocalDateRange.of(DATE_2012_07_27, DATE_2012_07_27)), false);
    assertEquals(test.encloses(LocalDateRange.of(DATE_2012_07_28, DATE_2012_07_28)), true);
    assertEquals(test.encloses(LocalDateRange.of(DATE_2012_07_29, DATE_2012_07_29)), false);
    assertEquals(test.encloses(LocalDateRange.of(DATE_2012_07_27, LocalDate.MAX)), false);
    assertEquals(test.encloses(LocalDateRange.of(DATE_2012_07_28, LocalDate.MAX)), false);
  }

  public void test_encloses_null() {
    LocalDateRange test = LocalDateRange.of(DATE_2012_07_28, DATE_2012_07_31);
    assertThrowsIllegalArg(() -> test.encloses(null));
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "intersection")
  Object[][] data_intersection() {
    return new Object[][] {
        // adjacent
        {DATE_2012_07_01, DATE_2012_07_28, DATE_2012_07_28, DATE_2012_07_30, DATE_2012_07_28, DATE_2012_07_28},
        // adjacent empty
        {DATE_2012_07_01, DATE_2012_07_30, DATE_2012_07_30, DATE_2012_07_30, DATE_2012_07_30, DATE_2012_07_30},
        // overlap
        {DATE_2012_07_01, DATE_2012_07_29, DATE_2012_07_28, DATE_2012_07_30, DATE_2012_07_28, DATE_2012_07_29},
        // encloses
        {DATE_2012_07_01, DATE_2012_07_30, DATE_2012_07_28, DATE_2012_07_29, DATE_2012_07_28, DATE_2012_07_29},
        // encloses empty
        {DATE_2012_07_01, DATE_2012_07_30, DATE_2012_07_28, DATE_2012_07_28, DATE_2012_07_28, DATE_2012_07_28},
    };
  }

  @Test(dataProvider = "intersection")
  public void test_intersection(
      LocalDate start1, LocalDate end1, LocalDate start2, LocalDate end2, LocalDate expStart, LocalDate expEnd) {

    LocalDateRange test1 = LocalDateRange.of(start1, end1);
    LocalDateRange test2 = LocalDateRange.of(start2, end2);
    LocalDateRange expected = LocalDateRange.of(expStart, expEnd);
    assertTrue(test1.overlaps(test2));
    assertEquals(test1.intersection(test2), expected);
  }

  @Test(dataProvider = "intersection")
  public void test_intersection_reverse(
      LocalDate start1, LocalDate end1, LocalDate start2, LocalDate end2, LocalDate expStart, LocalDate expEnd) {

    LocalDateRange test1 = LocalDateRange.of(start1, end1);
    LocalDateRange test2 = LocalDateRange.of(start2, end2);
    LocalDateRange expected = LocalDateRange.of(expStart, expEnd);
    assertTrue(test2.overlaps(test1));
    assertEquals(test2.intersection(test1), expected);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "union")
  Object[][] data_union() {
    return new Object[][] {
        // adjacent
        {DATE_2012_07_01, DATE_2012_07_28, DATE_2012_07_28, DATE_2012_07_30, DATE_2012_07_01, DATE_2012_07_30},
        // adjacent empty
        {DATE_2012_07_01, DATE_2012_07_30, DATE_2012_07_30, DATE_2012_07_30, DATE_2012_07_01, DATE_2012_07_30},
        // overlap
        {DATE_2012_07_01, DATE_2012_07_29, DATE_2012_07_28, DATE_2012_07_30, DATE_2012_07_01, DATE_2012_07_30},
        // encloses
        {DATE_2012_07_01, DATE_2012_07_30, DATE_2012_07_28, DATE_2012_07_29, DATE_2012_07_01, DATE_2012_07_30},
        // encloses empty
        {DATE_2012_07_01, DATE_2012_07_30, DATE_2012_07_28, DATE_2012_07_28, DATE_2012_07_01, DATE_2012_07_30},
    };
  }

  @Test(dataProvider = "union")
  public void test_union(
      LocalDate start1, LocalDate end1, LocalDate start2, LocalDate end2, LocalDate expStart, LocalDate expEnd) {

    LocalDateRange test1 = LocalDateRange.of(start1, end1);
    LocalDateRange test2 = LocalDateRange.of(start2, end2);
    LocalDateRange expected = LocalDateRange.of(expStart, expEnd);
    assertTrue(test1.overlaps(test2));
    assertEquals(test1.union(test2), expected);
  }

  @Test(dataProvider = "union")
  public void test_union_reverse(
      LocalDate start1, LocalDate end1, LocalDate start2, LocalDate end2, LocalDate expStart, LocalDate expEnd) {

    LocalDateRange test1 = LocalDateRange.of(start1, end1);
    LocalDateRange test2 = LocalDateRange.of(start2, end2);
    LocalDateRange expected = LocalDateRange.of(expStart, expEnd);
    assertTrue(test2.overlaps(test1));
    assertEquals(test2.union(test1), expected);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "noOverlap")
  Object[][] data_noOverlap() {
    return new Object[][] {
        {DATE_2012_07_01, DATE_2012_07_27, DATE_2012_07_28, DATE_2012_07_29},
        {DATE_2012_07_01, DATE_2012_07_27, DATE_2012_07_29, DATE_2012_07_30},
        {DATE_2012_07_01, DATE_2012_07_27, DATE_2012_07_29, DATE_2012_07_29},
    };
  }

  @Test(dataProvider = "noOverlap")
  public void test_noOverlap(LocalDate start1, LocalDate end1, LocalDate start2, LocalDate end2) {
    LocalDateRange test1 = LocalDateRange.of(start1, end1);
    LocalDateRange test2 = LocalDateRange.of(start2, end2);
    assertFalse(test1.overlaps(test2));
    assertThrowsIllegalArg(() -> test1.intersection(test2));
    assertThrowsIllegalArg(() -> test1.union(test2));
  }

  @Test(dataProvider = "noOverlap")
  public void test_noOverlap_reverse(LocalDate start1, LocalDate end1, LocalDate start2, LocalDate end2) {
    LocalDateRange test1 = LocalDateRange.of(start1, end1);
    LocalDateRange test2 = LocalDateRange.of(start2, end2);
    assertFalse(test2.overlaps(test1));
    assertThrowsIllegalArg(() -> test2.intersection(test1));
    assertThrowsIllegalArg(() -> test2.union(test1));
  }

  public void test_overlaps_same() {
    LocalDateRange test = LocalDateRange.of(DATE_2012_07_28, DATE_2012_07_31);
    assertEquals(test.overlaps(test), true);
  }

  public void test_overlaps_empty() {
    LocalDateRange test = LocalDateRange.of(DATE_2012_07_28, DATE_2012_07_30);
    assertEquals(test.overlaps(LocalDateRange.of(DATE_2012_07_27, DATE_2012_07_27)), false);
    assertEquals(test.overlaps(LocalDateRange.of(DATE_2012_07_28, DATE_2012_07_28)), true);
    assertEquals(test.overlaps(LocalDateRange.of(DATE_2012_07_29, DATE_2012_07_29)), true);
    assertEquals(test.overlaps(LocalDateRange.of(DATE_2012_07_30, DATE_2012_07_30)), true);
    assertEquals(test.overlaps(LocalDateRange.of(DATE_2012_07_31, DATE_2012_07_31)), false);
  }

  public void test_overlaps_null() {
    LocalDateRange test = LocalDateRange.of(DATE_2012_07_28, DATE_2012_07_31);
    assertThrowsIllegalArg(() -> test.overlaps(null));
  }

  public void test_intersection_same() {
    LocalDateRange test = LocalDateRange.of(DATE_2012_07_28, DATE_2012_07_31);
    assertEquals(test.intersection(test), test);
  }

  public void test_intersection_null() {
    LocalDateRange test = LocalDateRange.of(DATE_2012_07_28, DATE_2012_07_31);
    assertThrowsIllegalArg(() -> test.intersection(null));
  }

  public void test_union_same() {
    LocalDateRange test = LocalDateRange.of(DATE_2012_07_28, DATE_2012_07_31);
    assertEquals(test.union(test), test);
  }

  public void test_union_null() {
    LocalDateRange test = LocalDateRange.of(DATE_2012_07_28, DATE_2012_07_31);
    assertThrowsIllegalArg(() -> test.union(null));
  }

  //-------------------------------------------------------------------------
  public void test_stream() {
    LocalDateRange test = LocalDateRange.of(DATE_2012_07_28, DATE_2012_07_31);
    List<LocalDate> result = test.stream().collect(Collectors.toList());
    assertEquals(result, ImmutableList.of(DATE_2012_07_28, DATE_2012_07_29, DATE_2012_07_30));
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
        // empty
        {DATE_2012_07_30, DATE_2012_07_30, false},
        {DATE_2012_07_31, DATE_2012_07_31, true},
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
    LocalDateRange test = LocalDateRange.of(DATE_2012_07_28, DATE_2012_07_31);
    assertEquals(test.isBefore(LocalDateRange.of(start, end)), before);
  }

  public void test_isBefore_empty() {
    LocalDateRange test = LocalDateRange.of(DATE_2012_07_29, DATE_2012_07_29);
    assertEquals(test.isBefore(LocalDateRange.of(DATE_2012_07_27, DATE_2012_07_28)), false);
    assertEquals(test.isBefore(LocalDateRange.of(DATE_2012_07_27, DATE_2012_07_29)), false);
    assertEquals(test.isBefore(LocalDateRange.of(DATE_2012_07_29, DATE_2012_07_29)), false);
    assertEquals(test.isBefore(LocalDateRange.of(DATE_2012_07_29, DATE_2012_07_30)), true);
    assertEquals(test.isBefore(LocalDateRange.of(DATE_2012_07_30, DATE_2012_07_30)), true);
    assertEquals(test.isBefore(LocalDateRange.of(DATE_2012_07_30, DATE_2012_07_31)), true);
  }

  public void test_isBefore_null() {
    LocalDateRange test = LocalDateRange.of(DATE_2012_07_28, DATE_2012_07_31);
    assertThrowsIllegalArg(() -> test.isBefore(null));
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
        // empty
        {DATE_2012_07_28, DATE_2012_07_28, true},
        {DATE_2012_07_29, DATE_2012_07_29, false},
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
    LocalDateRange test = LocalDateRange.of(DATE_2012_07_28, DATE_2012_07_31);
    assertEquals(test.isAfter(LocalDateRange.of(start, end)), before);
  }

  public void test_isAfter_empty() {
    LocalDateRange test = LocalDateRange.of(DATE_2012_07_29, DATE_2012_07_29);
    assertEquals(test.isAfter(LocalDateRange.of(DATE_2012_07_27, DATE_2012_07_28)), true);
    assertEquals(test.isAfter(LocalDateRange.of(DATE_2012_07_27, DATE_2012_07_29)), true);
    assertEquals(test.isAfter(LocalDateRange.of(DATE_2012_07_28, DATE_2012_07_28)), true);
    assertEquals(test.isAfter(LocalDateRange.of(DATE_2012_07_29, DATE_2012_07_29)), false);
    assertEquals(test.isAfter(LocalDateRange.of(DATE_2012_07_29, DATE_2012_07_30)), false);
    assertEquals(test.isAfter(LocalDateRange.of(DATE_2012_07_30, DATE_2012_07_30)), false);
    assertEquals(test.isAfter(LocalDateRange.of(DATE_2012_07_30, DATE_2012_07_31)), false);
  }

  public void test_isAfter_null() {
    LocalDateRange test = LocalDateRange.of(DATE_2012_07_28, DATE_2012_07_31);
    assertThrowsIllegalArg(() -> test.isAfter(null));
  }

  //-------------------------------------------------------------------------
  public void test_equalsHashCode() {
    LocalDateRange a1 = LocalDateRange.of(DATE_2012_07_28, DATE_2012_07_31);
    LocalDateRange a2 = LocalDateRange.of(DATE_2012_07_28, DATE_2012_07_31);
    LocalDateRange b = LocalDateRange.of(DATE_2012_07_28, DATE_2012_07_30);
    LocalDateRange c = LocalDateRange.of(DATE_2012_07_30, DATE_2012_07_31);

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

  //-------------------------------------------------------------------------
  public void coverage() {
    coverImmutableBean(LocalDateRange.of(DATE_2012_07_28, DATE_2012_07_31));
  }

}
