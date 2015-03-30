/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.timeseries;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;

import org.testng.annotations.Test;

/**
 * Test LocalDateDoublePoint.
 */
@Test
public class LocalDateDoublePointTest {

  private static final LocalDate DATE_2012_06_29 = LocalDate.of(2012, 6, 29);
  private static final LocalDate DATE_2012_06_30 = LocalDate.of(2012, 6, 30);
  private static final LocalDate DATE_2012_07_01 = LocalDate.of(2012, 7, 1);
  private static final double TOLERANCE = 0.00001d;

  //-------------------------------------------------------------------------
  public void test_of() {
    LocalDateDoublePoint test = LocalDateDoublePoint.of(DATE_2012_06_30, 1d);
    assertEquals(test.getDate(), DATE_2012_06_30);
    assertEquals(test.getValue(), 1d, TOLERANCE);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_of_nullDate() {
    LocalDateDoublePoint.of(null, 1d);
  }

  //-------------------------------------------------------------------------
  public void test_withDate() {
    LocalDateDoublePoint base = LocalDateDoublePoint.of(DATE_2012_06_30, 1d);
    LocalDateDoublePoint test = base.withDate(DATE_2012_06_29);
    assertEquals(test.getDate(), DATE_2012_06_29);
    assertEquals(test.getValue(), 1d, TOLERANCE);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_withDate_nullDate() {
    LocalDateDoublePoint base = LocalDateDoublePoint.of(DATE_2012_06_30, 1d);
    base.withDate(null);
  }

  public void test_withValue() {
    LocalDateDoublePoint base = LocalDateDoublePoint.of(DATE_2012_06_30, 1d);
    LocalDateDoublePoint test = base.withValue(2d);
    assertEquals(test.getDate(), DATE_2012_06_30);
    assertEquals(test.getValue(), 2d, TOLERANCE);
  }

  //-------------------------------------------------------------------------
  public void test_compareTo() {
    LocalDateDoublePoint a = LocalDateDoublePoint.of(DATE_2012_06_29, 1d);
    LocalDateDoublePoint b = LocalDateDoublePoint.of(DATE_2012_06_30, 1d);
    LocalDateDoublePoint c = LocalDateDoublePoint.of(DATE_2012_07_01, 1d);

    assertTrue(a.compareTo(a) == 0);
    assertTrue(a.compareTo(b) < 0);
    assertTrue(a.compareTo(c) < 0);
    assertTrue(b.compareTo(a) > 0);
    assertTrue(b.compareTo(b) == 0);
    assertTrue(b.compareTo(c) < 0);
    assertTrue(c.compareTo(a) > 0);
    assertTrue(c.compareTo(b) > 0);
    assertTrue(c.compareTo(c) == 0);
  }

  //-------------------------------------------------------------------------
  public void test_equalsHashCode_differentDates() {
    LocalDateDoublePoint a1 = LocalDateDoublePoint.of(DATE_2012_06_29, 1d);
    LocalDateDoublePoint a2 = LocalDateDoublePoint.of(DATE_2012_06_29, 1d);
    LocalDateDoublePoint b = LocalDateDoublePoint.of(DATE_2012_06_30, 1d);
    LocalDateDoublePoint c = LocalDateDoublePoint.of(DATE_2012_07_01, 1d);

    assertEquals(a1.equals(a1), true);
    assertEquals(a1.equals(a2), true);
    assertEquals(a1.equals(b), false);
    assertEquals(a1.equals(c), false);
    assertEquals(a1.hashCode(), a1.hashCode());
  }

  public void test_equalsHashCode_differentValues() {
    LocalDateDoublePoint a1 = LocalDateDoublePoint.of(DATE_2012_06_29, 1d);
    LocalDateDoublePoint a2 = LocalDateDoublePoint.of(DATE_2012_06_29, 1d);
    LocalDateDoublePoint b = LocalDateDoublePoint.of(DATE_2012_06_29, 2d);
    LocalDateDoublePoint c = LocalDateDoublePoint.of(DATE_2012_06_29, 3d);

    assertEquals(a1.equals(a1), true);
    assertEquals(a1.equals(a2), true);
    assertEquals(a1.equals(b), false);
    assertEquals(a1.equals(c), false);
    assertEquals(a1.hashCode(), a1.hashCode());
  }

  public void test_equalsBad() {
    LocalDateDoublePoint a = LocalDateDoublePoint.of(DATE_2012_06_29, 1d);
    assertEquals(a.equals(""), false);
    assertEquals(a.equals(null), false);
  }

  //-------------------------------------------------------------------------
  public void test_toString() {
    LocalDateDoublePoint test = LocalDateDoublePoint.of(DATE_2012_06_29, 1d);
    assertEquals(test.toString(), "(2012-06-29=1.0)");
  }

}
