/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.examples.marketdata;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.io.CharSource;

/**
 * Test {@link CsvFile}.
 */
@Test
public class CsvFileTest {

  private final String CSV1 = "" +
      "h1,h2\n" +
      "r11,r12\n" +
      "r21,r22";

  private final String CSV2 = "" +
      "h1,h2\n" +
      "#r11,r12\n" +
      ";r11,r12\n" +
      "\n" +
      "r21,r22\n";

  private final String CSV3 = "" +
      "r11,r12\n" +
      ",\n" +
      "r21,r22\n";

  public void test_empty_no_header() {
    CsvFile csvFile = CsvFile.of(CharSource.wrap(""), false);
    assertFalse(csvFile.headers().isPresent());
    assertEquals(csvFile.lineCount(), 0);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_empty_with_header() {
    CsvFile.of(CharSource.wrap(""), true);
  }

  public void test_simple_no_header() {
    CsvFile csvFile = CsvFile.of(CharSource.wrap(CSV1), false);
    assertFalse(csvFile.headers().isPresent());
    assertEquals(csvFile.lineCount(), 3);
    assertEquals(csvFile.line(0).size(), 2);
    assertEquals(csvFile.line(0).get(0), "h1");
    assertEquals(csvFile.line(0).get(1), "h2");
    assertEquals(csvFile.line(1).size(), 2);
    assertEquals(csvFile.line(1).get(0), "r11");
    assertEquals(csvFile.line(1).get(1), "r12");
    assertEquals(csvFile.line(2).size(), 2);
    assertEquals(csvFile.line(2).get(0), "r21");
    assertEquals(csvFile.line(2).get(1), "r22");
  }

  public void test_simple_with_header() {
    CsvFile csvFile = CsvFile.of(CharSource.wrap(CSV1), true);
    assertTrue(csvFile.headers().isPresent());
    ImmutableList<String> headers = csvFile.headers().get();
    assertEquals(headers.size(), 2);
    assertEquals(headers.get(0), "h1");
    assertEquals(headers.get(1), "h2");
    assertEquals(csvFile.lineCount(), 2);
    assertEquals(csvFile.line(0).size(), 2);
    assertEquals(csvFile.line(0).get(0), "r11");
    assertEquals(csvFile.line(0).get(1), "r12");
    assertEquals(csvFile.line(1).size(), 2);
    assertEquals(csvFile.line(1).get(0), "r21");
    assertEquals(csvFile.line(1).get(1), "r22");

    assertEquals(csvFile.field(0, "h1"), "r11");
    assertEquals(csvFile.field(0, "h2"), "r12");
    assertEquals(csvFile.field(1, "h1"), "r21");
    assertEquals(csvFile.field(1, "h2"), "r22");
  }

  public void test_comment_blank_no_header() {
    CsvFile csvFile = CsvFile.of(CharSource.wrap(CSV2), false);
    assertFalse(csvFile.headers().isPresent());
    assertEquals(csvFile.lineCount(), 2);
    assertEquals(csvFile.line(0).size(), 2);
    assertEquals(csvFile.line(0).get(0), "h1");
    assertEquals(csvFile.line(0).get(1), "h2");
    assertEquals(csvFile.line(1).size(), 2);
    assertEquals(csvFile.line(1).get(0), "r21");
    assertEquals(csvFile.line(1).get(1), "r22");
  }

  public void test_comment_blank_with_header() {
    CsvFile csvFile = CsvFile.of(CharSource.wrap(CSV2), true);
    assertTrue(csvFile.headers().isPresent());
    ImmutableList<String> headers = csvFile.headers().get();
    assertEquals(headers.size(), 2);
    assertEquals(csvFile.lines().size(), 1);
    assertEquals(headers.get(0), "h1");
    assertEquals(headers.get(1), "h2");
    assertEquals(csvFile.lineCount(), 1);
    assertEquals(csvFile.line(0).size(), 2);
    assertEquals(csvFile.line(0).get(0), "r21");
    assertEquals(csvFile.line(0).get(1), "r22");
    assertEquals(csvFile.lines().get(0), csvFile.line(0));
  }

  @Test(expectedExceptions = UnsupportedOperationException.class)
  public void test_simple_no_header_access_by_field() {
    CsvFile csvFile = CsvFile.of(CharSource.wrap(CSV1), false);
    csvFile.field(0, "h1");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_simple_with_header_access_by_invalid_field() {
    CsvFile csvFile = CsvFile.of(CharSource.wrap(CSV1), true);
    csvFile.field(0, "h3");
  }

  public void test_blank_row() {
    CsvFile csvFile = CsvFile.of(CharSource.wrap(CSV3), false);
    assertEquals(csvFile.lineCount(), 2);
    assertEquals(csvFile.line(0).size(), 2);
    assertEquals(csvFile.line(0).get(0), "r11");
    assertEquals(csvFile.line(0).get(1), "r12");
    assertEquals(csvFile.line(1).size(), 2);
    assertEquals(csvFile.line(1).get(0), "r21");
    assertEquals(csvFile.line(1).get(1), "r22");
  }

}
