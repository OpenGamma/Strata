/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.io;

import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static java.util.stream.Collectors.toList;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import java.io.File;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.NoSuchElementException;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.io.CharSource;
import com.google.common.io.Files;

/**
 * Test {@link CsvIterator}.
 */
@Test
public class CsvIteratorTest {

  private final String CSV1 = "" +
      "h1,h2\n" +
      "r11,r12\n" +
      "r21,r22";

  private final String CSV1T = "" +
      "h1\th2\n" +
      "r11\tr12\n" +
      "r21\tr22";

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

  //-------------------------------------------------------------------------
  public void test_of_ioException() {
    assertThrows(
        () -> CsvIterator.of(Files.asCharSource(new File("src/test/resources"), StandardCharsets.UTF_8), false),
        UncheckedIOException.class);
  }

  public void test_of_empty_no_header() {
    try (CsvIterator csvFile = CsvIterator.of(CharSource.wrap(""), false)) {
      assertEquals(csvFile.headers().size(), 0);
      assertEquals(csvFile.hasNext(), false);
      assertEquals(csvFile.hasNext(), false);
      assertThrows(() -> csvFile.peek(), NoSuchElementException.class);
      assertThrows(() -> csvFile.next(), NoSuchElementException.class);
      assertThrows(() -> csvFile.next(), NoSuchElementException.class);
      assertThrows(() -> csvFile.remove(), UnsupportedOperationException.class);
    }
  }

  public void test_of_empty_with_header() {
    assertThrowsIllegalArg(() -> CsvIterator.of(CharSource.wrap(""), true));
  }

  public void test_of_simple_no_header() {
    try (CsvIterator csvFile = CsvIterator.of(CharSource.wrap(CSV1), false)) {
      assertEquals(csvFile.headers().size(), 0);
      assertEquals(csvFile.hasNext(), true);
      assertEquals(csvFile.hasNext(), true);
      CsvRow peeked = csvFile.peek();
      CsvRow row0 = csvFile.next();
      assertEquals(row0, peeked);
      assertEquals(row0.headers().size(), 0);
      assertEquals(row0.fieldCount(), 2);
      assertEquals(row0.field(0), "h1");
      assertEquals(row0.field(1), "h2");
      CsvRow row1 = csvFile.next();
      assertEquals(row1.headers().size(), 0);
      assertEquals(row1.fieldCount(), 2);
      assertEquals(row1.field(0), "r11");
      assertEquals(row1.field(1), "r12");
      CsvRow row2 = csvFile.next();
      assertEquals(row2.headers().size(), 0);
      assertEquals(row2.fieldCount(), 2);
      assertEquals(row2.field(0), "r21");
      assertEquals(row2.field(1), "r22");
      assertEquals(csvFile.hasNext(), false);
      assertThrows(() -> csvFile.peek(), NoSuchElementException.class);
      assertThrows(() -> csvFile.peek(), NoSuchElementException.class);
      assertThrows(() -> csvFile.next(), NoSuchElementException.class);
      assertThrows(() -> csvFile.next(), NoSuchElementException.class);
      assertThrows(() -> csvFile.remove(), UnsupportedOperationException.class);
    }
  }

  public void test_of_simple_no_header_tabs() {
    try (CsvIterator csvFile = CsvIterator.of(CharSource.wrap(CSV1T), false, '\t')) {
      assertEquals(csvFile.headers().size(), 0);
      CsvRow row0 = csvFile.next();
      assertEquals(row0.headers().size(), 0);
      assertEquals(row0.fieldCount(), 2);
      assertEquals(row0.field(0), "h1");
      assertEquals(row0.field(1), "h2");
      assertEquals(csvFile.hasNext(), true);
      CsvRow row1 = csvFile.next();
      assertEquals(row1.headers().size(), 0);
      assertEquals(row1.fieldCount(), 2);
      assertEquals(row1.field(0), "r11");
      assertEquals(row1.field(1), "r12");
      assertEquals(csvFile.hasNext(), true);
      CsvRow row2 = csvFile.next();
      assertEquals(row2.headers().size(), 0);
      assertEquals(row2.fieldCount(), 2);
      assertEquals(row2.field(0), "r21");
      assertEquals(row2.field(1), "r22");
      assertEquals(csvFile.hasNext(), false);
      assertThrows(() -> csvFile.next(), NoSuchElementException.class);
      assertThrows(() -> csvFile.next(), NoSuchElementException.class);
      assertThrows(() -> csvFile.remove(), UnsupportedOperationException.class);
    }
  }

  public void test_of_simple_with_header() {
    try (CsvIterator csvFile = CsvIterator.of(CharSource.wrap(CSV1), true)) {
      ImmutableList<String> headers = csvFile.headers();
      assertEquals(headers.size(), 2);
      assertEquals(headers.get(0), "h1");
      assertEquals(headers.get(1), "h2");
      CsvRow peeked = csvFile.peek();
      CsvRow row0 = csvFile.next();
      assertEquals(row0, peeked);
      assertEquals(row0.headers(), headers);
      assertEquals(row0.fieldCount(), 2);
      assertEquals(row0.field(0), "r11");
      assertEquals(row0.field(1), "r12");
      CsvRow row1 = csvFile.next();
      assertEquals(row1.headers(), headers);
      assertEquals(row1.fieldCount(), 2);
      assertEquals(row1.field(0), "r21");
      assertEquals(row1.field(1), "r22");
      assertEquals(csvFile.hasNext(), false);
      assertThrows(() -> csvFile.peek(), NoSuchElementException.class);
      assertThrows(() -> csvFile.peek(), NoSuchElementException.class);
      assertThrows(() -> csvFile.next(), NoSuchElementException.class);
      assertThrows(() -> csvFile.next(), NoSuchElementException.class);
      assertThrows(() -> csvFile.remove(), UnsupportedOperationException.class);
    }
  }

  public void test_of_comment_blank_no_header() {
    try (CsvIterator csvFile = CsvIterator.of(CharSource.wrap(CSV2), false)) {
      assertEquals(csvFile.headers().size(), 0);
      assertEquals(csvFile.hasNext(), true);
      CsvRow row0 = csvFile.next();
      assertEquals(row0.fieldCount(), 2);
      assertEquals(row0.field(0), "h1");
      assertEquals(row0.field(1), "h2");
      CsvRow row1 = csvFile.next();
      assertEquals(row1.fieldCount(), 2);
      assertEquals(row1.field(0), "r21");
      assertEquals(row1.field(1), "r22");
      assertEquals(csvFile.hasNext(), false);
    }
  }

  public void test_of_comment_blank_with_header() {
    try (CsvIterator csvFile = CsvIterator.of(CharSource.wrap(CSV2), true)) {
      ImmutableList<String> headers = csvFile.headers();
      assertEquals(headers.size(), 2);
      assertEquals(headers.get(0), "h1");
      assertEquals(headers.get(1), "h2");
      assertEquals(csvFile.hasNext(), true);
      CsvRow row0 = csvFile.next();
      assertEquals(row0.fieldCount(), 2);
      assertEquals(row0.field(0), "r21");
      assertEquals(row0.field(1), "r22");
      assertEquals(csvFile.hasNext(), false);
    }
  }

  public void test_of_blank_row() {
    try (CsvIterator csvFile = CsvIterator.of(CharSource.wrap(CSV3), false)) {
      assertEquals(csvFile.hasNext(), true);
      CsvRow row0 = csvFile.next();
      assertEquals(row0.fieldCount(), 2);
      assertEquals(row0.field(0), "r11");
      assertEquals(row0.field(1), "r12");
      CsvRow row1 = csvFile.next();
      assertEquals(row1.fieldCount(), 2);
      assertEquals(row1.field(0), "r21");
      assertEquals(row1.field(1), "r22");
      assertEquals(csvFile.hasNext(), false);
    }
  }

  //-------------------------------------------------------------------------
  public void test_of_empty_no_header_reader() {
    try (CsvIterator csvFile = CsvIterator.of(new StringReader(""), false, ',')) {
      assertEquals(csvFile.headers().size(), 0);
      assertEquals(csvFile.hasNext(), false);
    }
  }

  //-------------------------------------------------------------------------
  public void test_nextBatch1() {
    try (CsvIterator csvFile = CsvIterator.of(CharSource.wrap(CSV1), true)) {
      ImmutableList<String> headers = csvFile.headers();
      assertEquals(headers.size(), 2);
      assertEquals(headers.get(0), "h1");
      assertEquals(headers.get(1), "h2");
      List<CsvRow> a = csvFile.nextBatch(0);
      assertEquals(a.size(), 0);
      List<CsvRow> b = csvFile.nextBatch(1);
      assertEquals(b.size(), 1);
      CsvRow row0 = b.get(0);
      assertEquals(row0.headers(), headers);
      assertEquals(row0.fieldCount(), 2);
      assertEquals(row0.field(0), "r11");
      assertEquals(row0.field(1), "r12");
      List<CsvRow> c = csvFile.nextBatch(2);
      assertEquals(c.size(), 1);
      CsvRow row1 = c.get(0);
      assertEquals(row1.headers(), headers);
      assertEquals(row1.fieldCount(), 2);
      assertEquals(row1.field(0), "r21");
      assertEquals(row1.field(1), "r22");
      List<CsvRow> d = csvFile.nextBatch(2);
      assertEquals(d.size(), 0);
      assertEquals(csvFile.hasNext(), false);
    }
  }

  public void test_nextBatch2() {
    try (CsvIterator csvFile = CsvIterator.of(CharSource.wrap(CSV1), true)) {
      ImmutableList<String> headers = csvFile.headers();
      assertEquals(headers.size(), 2);
      assertEquals(headers.get(0), "h1");
      assertEquals(headers.get(1), "h2");
      List<CsvRow> a = csvFile.nextBatch(3);
      assertEquals(a.size(), 2);
      CsvRow row0 = a.get(0);
      assertEquals(row0.headers(), headers);
      assertEquals(row0.fieldCount(), 2);
      assertEquals(row0.field(0), "r11");
      assertEquals(row0.field(1), "r12");
      CsvRow row1 = a.get(1);
      assertEquals(row1.headers(), headers);
      assertEquals(row1.fieldCount(), 2);
      assertEquals(row1.field(0), "r21");
      assertEquals(row1.field(1), "r22");
      List<CsvRow> d = csvFile.nextBatch(2);
      assertEquals(d.size(), 0);
      assertEquals(csvFile.hasNext(), false);
      assertEquals(csvFile.hasNext(), false);
    }
  }

  //-------------------------------------------------------------------------
  public void test_asStream_empty_no_header() {
    try (CsvIterator csvFile = CsvIterator.of(CharSource.wrap(""), false)) {
      assertEquals(csvFile.asStream().collect(toList()).size(), 0);
    }
  }

  public void test_asStream_simple_no_header() {
    try (CsvIterator csvFile = CsvIterator.of(CharSource.wrap(CSV1), false)) {
      assertEquals(csvFile.headers().size(), 0);
      List<CsvRow> rows = csvFile.asStream().collect(toList());
      assertEquals(csvFile.hasNext(), false);
      assertEquals(rows.size(), 3);
      CsvRow row0 = rows.get(0);
      assertEquals(row0.headers().size(), 0);
      assertEquals(row0.fieldCount(), 2);
      assertEquals(row0.field(0), "h1");
      assertEquals(row0.field(1), "h2");
      CsvRow row1 = rows.get(1);
      assertEquals(row1.headers().size(), 0);
      assertEquals(row1.fieldCount(), 2);
      assertEquals(row1.field(0), "r11");
      assertEquals(row1.field(1), "r12");
      CsvRow row2 = rows.get(2);
      assertEquals(row2.headers().size(), 0);
      assertEquals(row2.fieldCount(), 2);
      assertEquals(row2.field(0), "r21");
      assertEquals(row2.field(1), "r22");
    }
  }

  public void test_asStream_simple_with_header() {
    try (CsvIterator csvFile = CsvIterator.of(CharSource.wrap(CSV1), true)) {
      ImmutableList<String> headers = csvFile.headers();
      assertEquals(headers.size(), 2);
      assertEquals(headers.get(0), "h1");
      assertEquals(headers.get(1), "h2");
      List<CsvRow> rows = csvFile.asStream().collect(toList());
      assertEquals(csvFile.hasNext(), false);
      assertEquals(rows.size(), 2);
      CsvRow row0 = rows.get(0);
      assertEquals(row0.headers(), headers);
      assertEquals(row0.fieldCount(), 2);
      assertEquals(row0.field(0), "r11");
      assertEquals(row0.field(1), "r12");
      CsvRow row1 = rows.get(1);
      assertEquals(row1.headers(), headers);
      assertEquals(row1.fieldCount(), 2);
      assertEquals(row1.field(0), "r21");
      assertEquals(row1.field(1), "r22");
    }
  }

  //-------------------------------------------------------------------------
  public void test_toString() {
    try (CsvIterator test = CsvIterator.of(CharSource.wrap(CSV1), true)) {
      assertNotNull(test.toString());
    }
  }

}
