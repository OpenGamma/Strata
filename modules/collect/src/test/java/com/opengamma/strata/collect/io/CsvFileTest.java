/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.io;

import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import java.io.File;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.io.CharSource;
import com.google.common.io.Files;

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

  private final String CSV4 = "" +
      "\"alpha\",\"be, \"\"at\"\", one\"\n" +
      "\"alpha\"\",\"be\"\"\", \"\"at\"\", one\"\n" +
      "r21,\" r22 \"\n";

  //-------------------------------------------------------------------------
  public void test_of_ioException() {
    assertThrows(
        () -> CsvFile.of(Files.asCharSource(new File("src/test/resources"), StandardCharsets.UTF_8), false),
        UncheckedIOException.class);
  }

  public void test_empty_no_header() {
    CsvFile csvFile = CsvFile.of(CharSource.wrap(""), false);
    assertEquals(csvFile.headers().size(), 0);
    assertEquals(csvFile.rowCount(), 0);
  }

  public void test_empty_with_header() {
    assertThrowsIllegalArg(() -> CsvFile.of(CharSource.wrap(""), true));
  }

  public void test_simple_no_header() {
    CsvFile csvFile = CsvFile.of(CharSource.wrap(CSV1), false);
    assertEquals(csvFile.headers().size(), 0);
    assertEquals(csvFile.rowCount(), 3);
    assertEquals(csvFile.row(0).size(), 2);
    assertEquals(csvFile.row(0).get(0), "h1");
    assertEquals(csvFile.row(0).get(1), "h2");
    assertEquals(csvFile.row(1).size(), 2);
    assertEquals(csvFile.row(1).get(0), "r11");
    assertEquals(csvFile.row(1).get(1), "r12");
    assertEquals(csvFile.row(2).size(), 2);
    assertEquals(csvFile.row(2).get(0), "r21");
    assertEquals(csvFile.row(2).get(1), "r22");
  }

  public void test_simple_with_header() {
    CsvFile csvFile = CsvFile.of(CharSource.wrap(CSV1), true);
    ImmutableList<String> headers = csvFile.headers();
    assertEquals(headers.size(), 2);
    assertEquals(headers.get(0), "h1");
    assertEquals(headers.get(1), "h2");
    assertEquals(csvFile.rowCount(), 2);
    assertEquals(csvFile.row(0).size(), 2);
    assertEquals(csvFile.row(0).get(0), "r11");
    assertEquals(csvFile.row(0).get(1), "r12");
    assertEquals(csvFile.row(1).size(), 2);
    assertEquals(csvFile.row(1).get(0), "r21");
    assertEquals(csvFile.row(1).get(1), "r22");

    assertEquals(csvFile.field(0, "h1"), "r11");
    assertEquals(csvFile.field(0, "h2"), "r12");
    assertEquals(csvFile.field(1, "h1"), "r21");
    assertEquals(csvFile.field(1, "h2"), "r22");
  }

  public void test_comment_blank_no_header() {
    CsvFile csvFile = CsvFile.of(CharSource.wrap(CSV2), false);
    assertEquals(csvFile.headers().size(), 0);
    assertEquals(csvFile.rowCount(), 2);
    assertEquals(csvFile.row(0).size(), 2);
    assertEquals(csvFile.row(0).get(0), "h1");
    assertEquals(csvFile.row(0).get(1), "h2");
    assertEquals(csvFile.row(1).size(), 2);
    assertEquals(csvFile.row(1).get(0), "r21");
    assertEquals(csvFile.row(1).get(1), "r22");
  }

  public void test_comment_blank_with_header() {
    CsvFile csvFile = CsvFile.of(CharSource.wrap(CSV2), true);
    ImmutableList<String> headers = csvFile.headers();
    assertEquals(headers.size(), 2);
    assertEquals(headers.get(0), "h1");
    assertEquals(headers.get(1), "h2");
    assertEquals(csvFile.rows().size(), 1);
    assertEquals(csvFile.rowCount(), 1);
    assertEquals(csvFile.row(0).size(), 2);
    assertEquals(csvFile.row(0).get(0), "r21");
    assertEquals(csvFile.row(0).get(1), "r22");
    assertEquals(csvFile.rows().get(0), csvFile.row(0));
  }

  public void test_simple_no_header_access_by_field() {
    CsvFile csvFile = CsvFile.of(CharSource.wrap(CSV1), false);
    assertThrowsIllegalArg(() -> csvFile.field(0, "h1"));
  }

  public void test_simple_with_header_access_by_invalid_field() {
    CsvFile csvFile = CsvFile.of(CharSource.wrap(CSV1), true);
    assertThrowsIllegalArg(() -> csvFile.field(0, "h3"));
  }

  public void test_blank_row() {
    CsvFile csvFile = CsvFile.of(CharSource.wrap(CSV3), false);
    assertEquals(csvFile.rowCount(), 2);
    assertEquals(csvFile.row(0).size(), 2);
    assertEquals(csvFile.row(0).get(0), "r11");
    assertEquals(csvFile.row(0).get(1), "r12");
    assertEquals(csvFile.row(1).size(), 2);
    assertEquals(csvFile.row(1).get(0), "r21");
    assertEquals(csvFile.row(1).get(1), "r22");
  }

  public void test_quoting() {
    CsvFile csvFile = CsvFile.of(CharSource.wrap(CSV4), false);
    assertEquals(csvFile.rowCount(), 3);
    assertEquals(csvFile.row(0).size(), 2);
    assertEquals(csvFile.row(0).get(0), "alpha");
    assertEquals(csvFile.row(0).get(1), "be, \"at\", one");
    assertEquals(csvFile.row(1).size(), 2);
    assertEquals(csvFile.row(1).get(0), "alpha\",\"be\"");
    assertEquals(csvFile.row(1).get(1), "\"at\", one");
    assertEquals(csvFile.row(2).size(), 2);
    assertEquals(csvFile.row(2).get(0), "r21");
    assertEquals(csvFile.row(2).get(1), " r22 ");
  }

  public void test_quoting_mismatched() {
    assertThrowsIllegalArg(() -> CsvFile.of(CharSource.wrap("\"alpha"), false));
    assertThrowsIllegalArg(() -> CsvFile.of(CharSource.wrap("\"al\"pha"), false));
    assertThrowsIllegalArg(() -> CsvFile.of(CharSource.wrap("\"al\"\"pha"), false));
    assertThrowsIllegalArg(() -> CsvFile.of(CharSource.wrap("\"al,pha"), false));
  }

  //-------------------------------------------------------------------------
  public void test_of_lists_noHeader() {
    List<String> headers = Collections.emptyList();
    List<List<String>> rows = Arrays.asList(Arrays.asList("a", "x"), Arrays.asList("b", "y"));
    CsvFile csvFile = CsvFile.of(headers, rows);
    assertEquals(csvFile.headers(), headers);
    assertEquals(csvFile.rows(), rows);
  }

  public void test_of_lists_noHeaderNoRows() {
    List<String> headers = Collections.emptyList();
    List<List<String>> rows = Collections.emptyList();
    CsvFile csvFile = CsvFile.of(headers, rows);
    assertEquals(csvFile.headers(), headers);
    assertEquals(csvFile.rows(), rows);
  }

  public void test_of_lists_header() {
    List<String> headers = Arrays.asList("1", "2");
    List<List<String>> rows = Arrays.asList(Arrays.asList("a", "x"), Arrays.asList("b", "y"));
    CsvFile csvFile = CsvFile.of(headers, rows);
    assertEquals(csvFile.headers(), headers);
    assertEquals(csvFile.rows(), rows);
  }

  public void test_of_lists_sizeMismatch() {
    List<String> headers = Arrays.asList("1", "2");
    List<List<String>> rows = Arrays.asList(Arrays.asList("a", "x"), Arrays.asList("b"));
    assertThrowsIllegalArg(() -> CsvFile.of(headers, rows));
  }

  //-------------------------------------------------------------------------
  public void test_equalsHashCodeToString() {
    CsvFile a1 = CsvFile.of(CharSource.wrap(CSV1), true);
    CsvFile a2 = CsvFile.of(CharSource.wrap(CSV1), true);
    CsvFile b = CsvFile.of(CharSource.wrap(CSV2), true);
    CsvFile c = CsvFile.of(CharSource.wrap(CSV3), false);

    assertEquals(a1.equals(a1), true);
    assertEquals(a1.equals(a2), true);
    assertEquals(a1.equals(b), false);
    assertEquals(a1.equals(c), false);
    assertEquals(a1.equals(null), false);
    assertEquals(a1.equals(""), false);
    assertEquals(a1.hashCode(), a2.hashCode());
    assertNotNull(a1.toString());
  }

}
