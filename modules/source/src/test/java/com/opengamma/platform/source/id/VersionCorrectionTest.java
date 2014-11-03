/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.source.id;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import java.time.Instant;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.opengamma.collect.TestHelper;

/**
 * Test.
 */
@Test
public class VersionCorrectionTest {

  private static final Instant INSTANT1 = Instant.ofEpochSecond(1);
  private static final Instant INSTANT2 = Instant.ofEpochSecond(2);
  private static final Instant INSTANT3 = Instant.ofEpochSecond(3);

  public void test_LATEST() {
    VersionCorrection test = VersionCorrection.LATEST;
    assertEquals(test.getVersionAsOf(), null);
    assertEquals(test.getCorrectedTo(), null);
    assertEquals(test.toString(), "VLATEST.CLATEST");
  }

  //-------------------------------------------------------------------------
  public void test_of_InstantInstant() {
    VersionCorrection test = VersionCorrection.of(INSTANT1, INSTANT2);
    assertEquals(test.getVersionAsOf(), INSTANT1);
    assertEquals(test.getCorrectedTo(), INSTANT2);
    assertEquals(test.toString(), "V1970-01-01T00:00:01Z.C1970-01-01T00:00:02Z");
  }

  public void test_of_InstantInstant_nullVersion() {
    VersionCorrection test = VersionCorrection.of((Instant) null, INSTANT2);
    assertEquals(test.getVersionAsOf(), null);
    assertEquals(test.getCorrectedTo(), INSTANT2);
    assertEquals(test.toString(), "VLATEST.C1970-01-01T00:00:02Z");
  }

  public void test_of_InstantInstant_nullCorrection() {
    VersionCorrection test = VersionCorrection.of(INSTANT1, (Instant) null);
    assertEquals(test.getVersionAsOf(), INSTANT1);
    assertEquals(test.getCorrectedTo(), null);
    assertEquals(test.toString(), "V1970-01-01T00:00:01Z.CLATEST");
  }

  public void test_of_InstantInstant_nulls() {
    VersionCorrection test = VersionCorrection.of((Instant) null, (Instant) null);
    assertSame(test, VersionCorrection.LATEST);
  }

  //-------------------------------------------------------------------------
  public void test_ofVersionAsOf_Instant() {
    VersionCorrection test = VersionCorrection.ofVersionAsOf(INSTANT1);
    assertEquals(test.getVersionAsOf(), INSTANT1);
    assertEquals(test.getCorrectedTo(), null);
  }

  public void test_ofVersionAsOf_Instant_null() {
    VersionCorrection test = VersionCorrection.ofVersionAsOf((Instant) null);
    assertSame(test, VersionCorrection.LATEST);
  }

  //-------------------------------------------------------------------------
  public void test_ofCorrectedTo_Instant() {
    VersionCorrection test = VersionCorrection.ofCorrectedTo(INSTANT2);
    assertEquals(test.getVersionAsOf(), null);
    assertEquals(test.getCorrectedTo(), INSTANT2);
  }

  public void test_ofCorrectedTo_Instant_null() {
    VersionCorrection test = VersionCorrection.ofCorrectedTo((Instant) null);
    assertSame(test, VersionCorrection.LATEST);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "parseValid")
  Object[][] data_parseValid() {
    return new Object[][] {
        {"1970-01-01T00:00:01Z", "1970-01-01T00:00:02Z", VersionCorrection.of(INSTANT1, INSTANT2)},
        {"LATEST", "1970-01-01T00:00:02Z", VersionCorrection.of(null, INSTANT2)},
        {"1970-01-01T00:00:01Z", "LATEST", VersionCorrection.of(INSTANT1, null)},
        {"LATEST", "LATEST", VersionCorrection.of(null, null)},
    };
  }

  @Test(dataProvider = "parseValid")
  public void test_parse_String(String first, String second, VersionCorrection expected) {
    VersionCorrection test = VersionCorrection.parse("V" + first + ".C" + second);
    assertEquals(test, expected);
  }

  @Test(dataProvider = "parseValid")
  public void test_parse_StringString(String first, String second, VersionCorrection expected) {
    VersionCorrection test = VersionCorrection.parse(first, second);
    assertEquals(test, expected);
  }

  @Test(dataProvider = "parseValid")
  public void test_parse_StringString_nullsAllowed(String first, String second, VersionCorrection expected) {
    VersionCorrection test = VersionCorrection.parse(
        (first.equals("LATEST") ? null : first),
        (second.equals("LATEST") ? null : second));
    assertEquals(test, expected);
  }

  @DataProvider(name = "parseInvalid")
  Object[][] data_parseInvalid() {
    return new Object[][] {
        {"1970-01-01T00:00:01Z.C1970-01-01T00:00:02Z"},  // no V
        {"V1970-01-01T00:00:01Z.1970-01-01T00:00:02Z"},  // no C
        {""},  // blank
        {"V1970-01-01T00:00:01Z"},  // only half
        {"V1970-12-01 00:00:01Z.C1970-01-01T00:00:02Z"},  // invalid date 1
        {"V1970-12-01T00:00:01Z.C1970-01-20 00:00:02Z"},  // invalid date 2
        {"VLATS.CLATS"},  // invalid latest
    };
  }

  @Test(dataProvider = "parseInvalid", expectedExceptions = IllegalArgumentException.class)
  public void test_parse_String_invalid(String input) {
    VersionCorrection.parse(input);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_parse_StringString_invalid() {
    VersionCorrection.parse("LATS", "LATS");
  }

  //-------------------------------------------------------------------------
  public void test_withVersionAsOf_instantToInstant() {
    VersionCorrection test = VersionCorrection.of(INSTANT1, INSTANT2);
    assertEquals(test.withVersionAsOf(INSTANT3), VersionCorrection.of(INSTANT3, INSTANT2));
  }

  public void test_withVersionAsOf_instantToNull() {
    VersionCorrection test = VersionCorrection.of(INSTANT1, INSTANT2);
    assertEquals(test.withVersionAsOf(null), VersionCorrection.of(null, INSTANT2));
  }

  public void test_withVersionAsOf_nullToInstant() {
    VersionCorrection test = VersionCorrection.of(null, INSTANT2);
    assertEquals(test.withVersionAsOf(INSTANT3), VersionCorrection.of(INSTANT3, INSTANT2));
  }

  public void test_withVersionAsOf_nullToNull() {
    VersionCorrection test = VersionCorrection.of(null, INSTANT2);
    assertEquals(test.withVersionAsOf(null), VersionCorrection.of(null, INSTANT2));
  }

  //-------------------------------------------------------------------------
  public void test_withCorrectedTo_instantToInstant() {
    VersionCorrection test = VersionCorrection.of(INSTANT1, INSTANT2);
    assertEquals(test.withCorrectedTo(INSTANT3), VersionCorrection.of(INSTANT1, INSTANT3));
  }

  public void test_withCorrectedTo_instantToNull() {
    VersionCorrection test = VersionCorrection.of(INSTANT1, INSTANT2);
    assertEquals(test.withCorrectedTo(null), VersionCorrection.of(INSTANT1, null));
  }

  public void test_withCorrectedTo_nullToInstant() {
    VersionCorrection test = VersionCorrection.of(INSTANT1, null);
    assertEquals(test.withCorrectedTo(INSTANT3), VersionCorrection.of(INSTANT1, INSTANT3));
  }

  public void test_withCorrectedTo_nullToNull() {
    VersionCorrection test = VersionCorrection.of(INSTANT1, null);
    assertEquals(test.withCorrectedTo(null), VersionCorrection.of(INSTANT1, null));
  }

  //-------------------------------------------------------------------------
  public void test_containsLatest() {
    assertEquals((Object) VersionCorrection.of(INSTANT1, INSTANT2).containsLatest(), false);
    assertEquals((Object) VersionCorrection.of(null, INSTANT2).containsLatest(), true);
    assertEquals((Object) VersionCorrection.of(INSTANT1, null).containsLatest(), true);
    assertEquals((Object) VersionCorrection.of(null, null).containsLatest(), true);
  }

  //-------------------------------------------------------------------------
  public void test_withLatestFixed_noNulls() {
    VersionCorrection test = VersionCorrection.of(INSTANT1, INSTANT2);
    assertSame(test.withLatestFixed(INSTANT3), test);
  }

  public void test_withLatestFixed_nullVersion() {
    VersionCorrection test = VersionCorrection.of(null, INSTANT2);
    assertEquals(test.withLatestFixed(INSTANT3), VersionCorrection.of(INSTANT3, INSTANT2));
  }

  public void test_withLatestFixed_nullCorrection() {
    VersionCorrection test = VersionCorrection.of(INSTANT1, null);
    assertEquals(test.withLatestFixed(INSTANT3), VersionCorrection.of(INSTANT1, INSTANT3));
  }

  public void test_withLatestFixed_nulls() {
    VersionCorrection test = VersionCorrection.of(null, null);
    assertEquals(test.withLatestFixed(INSTANT3), VersionCorrection.of(INSTANT3, INSTANT3));
  }

  //-------------------------------------------------------------------------
  public void test_compareTo_nonNull() {
    VersionCorrection a = VersionCorrection.of(INSTANT1, INSTANT2);
    VersionCorrection b = VersionCorrection.of(INSTANT1, INSTANT3);
    VersionCorrection c = VersionCorrection.of(INSTANT2, INSTANT3);
    
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

  public void test_compareTo_nullVersion() {
    VersionCorrection a = VersionCorrection.of(INSTANT1, INSTANT2);
    VersionCorrection b = VersionCorrection.of(null, INSTANT2);
    
    assertTrue(a.compareTo(a) == 0);
    assertTrue(a.compareTo(b) < 0);
    
    assertTrue(b.compareTo(a) > 0);
    assertTrue(b.compareTo(b) == 0);
  }

  public void test_compareTo_nullCorrection() {
    VersionCorrection a = VersionCorrection.of(INSTANT1, INSTANT2);
    VersionCorrection b = VersionCorrection.of(INSTANT1, null);
    
    assertTrue(a.compareTo(a) == 0);
    assertTrue(a.compareTo(b) < 0);
    
    assertTrue(b.compareTo(a) > 0);
    assertTrue(b.compareTo(b) == 0);
  }

  @Test(expectedExceptions = NullPointerException.class)
  public void test_compareTo_null() {
    VersionCorrection test = VersionCorrection.of(INSTANT1, INSTANT2);
    test.compareTo(null);
  }

  //-------------------------------------------------------------------------
  public void test_equals() {
    VersionCorrection d1a = VersionCorrection.of(INSTANT1, INSTANT2);
    VersionCorrection d1b = VersionCorrection.of(INSTANT1, INSTANT2);
    VersionCorrection d2 = VersionCorrection.of(INSTANT1, INSTANT3);

    assertEquals((Object) d1a.equals(d1a), true);
    assertEquals((Object) d1a.equals(d1b), true);
    assertEquals((Object) d1a.equals(d2), false);

    assertEquals((Object) d1b.equals(d1a), true);
    assertEquals((Object) d1b.equals(d1b), true);
    assertEquals((Object) d1b.equals(d2), false);

    assertEquals((Object) d2.equals(d1a), false);
    assertEquals((Object) d2.equals(d1b), false);
    assertEquals((Object) d2.equals(d2), true);

    assertEquals((Object) d1b.equals("d1"), false);
    assertEquals((Object) d1b.equals(null), false);
  }

  public void test_hashCode() {
    VersionCorrection d1a = VersionCorrection.of(INSTANT1, INSTANT2);
    VersionCorrection d1b = VersionCorrection.of(INSTANT1, INSTANT2);

    assertEquals((Object) d1b.hashCode(), d1a.hashCode());
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    TestHelper.coverImmutableBean(VersionCorrection.of(INSTANT1, INSTANT2));
  }

}
