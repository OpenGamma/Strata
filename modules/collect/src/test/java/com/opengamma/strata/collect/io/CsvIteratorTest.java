/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.io;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.io.File;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.io.CharSource;
import com.google.common.io.Files;

/**
 * Test {@link CsvIterator}.
 */
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

  private final String CSV4 = "" +
      "# Comment about the file\n" +
      "h1,h2\n" +
      "r1,r2\n";

  private final String CSV5GROUPED = "" +
      "id,value\n" +
      "1,a\n" +
      "1,b\n" +
      "1,c\n" +
      "2,mm\n" +
      "3,yyy\n" +
      "3,zzz";

  //-------------------------------------------------------------------------
  @Test
  public void test_of_ioException() {
    assertThatExceptionOfType(UncheckedIOException.class).isThrownBy(
        () -> CsvIterator.of(Files.asCharSource(new File("src/test/resources"), StandardCharsets.UTF_8), false));
  }

  @Test
  public void test_of_empty_no_header() {
    try (CsvIterator csvFile = CsvIterator.of(CharSource.wrap(""), false)) {
      assertThat(csvFile.headers().size()).isEqualTo(0);
      assertThat(csvFile.containsHeader("a")).isEqualTo(false);
      assertThat(csvFile.hasNext()).isEqualTo(false);
      assertThat(csvFile.hasNext()).isEqualTo(false);
      assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() -> csvFile.peek());
      assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() -> csvFile.next());
      assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() -> csvFile.next());
      assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> csvFile.remove());
    }
  }

  @Test
  public void test_of_empty_with_header() {
    assertThatIllegalArgumentException().isThrownBy(() -> CsvIterator.of(CharSource.wrap(""), true));
  }

  @Test
  public void test_of_simple_no_header() {
    try (CsvIterator csvFile = CsvIterator.of(CharSource.wrap(CSV1), false)) {
      assertThat(csvFile.headers().size()).isEqualTo(0);
      assertThat(csvFile.hasNext()).isEqualTo(true);
      assertThat(csvFile.hasNext()).isEqualTo(true);
      CsvRow peeked = csvFile.peek();
      CsvRow row0 = csvFile.next();
      assertThat(row0).isEqualTo(peeked);
      assertThat(row0.headers().size()).isEqualTo(0);
      assertThat(row0.lineNumber()).isEqualTo(1);
      assertThat(row0.fieldCount()).isEqualTo(2);
      assertThat(row0.field(0)).isEqualTo("h1");
      assertThat(row0.field(1)).isEqualTo("h2");
      CsvRow row1 = csvFile.next();
      assertThat(row1.headers().size()).isEqualTo(0);
      assertThat(row1.lineNumber()).isEqualTo(2);
      assertThat(row1.fieldCount()).isEqualTo(2);
      assertThat(row1.field(0)).isEqualTo("r11");
      assertThat(row1.field(1)).isEqualTo("r12");
      CsvRow row2 = csvFile.next();
      assertThat(row2.headers().size()).isEqualTo(0);
      assertThat(row2.lineNumber()).isEqualTo(3);
      assertThat(row2.fieldCount()).isEqualTo(2);
      assertThat(row2.field(0)).isEqualTo("r21");
      assertThat(row2.field(1)).isEqualTo("r22");
      assertThat(csvFile.hasNext()).isEqualTo(false);
      assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() -> csvFile.peek());
      assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() -> csvFile.peek());
      assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() -> csvFile.next());
      assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() -> csvFile.next());
      assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> csvFile.remove());
    }
  }

  @Test
  public void test_of_simple_no_header_tabs() {
    try (CsvIterator csvFile = CsvIterator.of(CharSource.wrap(CSV1T), false, '\t')) {
      assertThat(csvFile.headers().size()).isEqualTo(0);
      CsvRow row0 = csvFile.next();
      assertThat(row0.headers().size()).isEqualTo(0);
      assertThat(row0.fieldCount()).isEqualTo(2);
      assertThat(row0.field(0)).isEqualTo("h1");
      assertThat(row0.field(1)).isEqualTo("h2");
      assertThat(csvFile.hasNext()).isEqualTo(true);
      CsvRow row1 = csvFile.next();
      assertThat(row1.headers().size()).isEqualTo(0);
      assertThat(row1.fieldCount()).isEqualTo(2);
      assertThat(row1.field(0)).isEqualTo("r11");
      assertThat(row1.field(1)).isEqualTo("r12");
      assertThat(csvFile.hasNext()).isEqualTo(true);
      CsvRow row2 = csvFile.next();
      assertThat(row2.headers().size()).isEqualTo(0);
      assertThat(row2.fieldCount()).isEqualTo(2);
      assertThat(row2.field(0)).isEqualTo("r21");
      assertThat(row2.field(1)).isEqualTo("r22");
      assertThat(csvFile.hasNext()).isEqualTo(false);
      assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() -> csvFile.next());
      assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() -> csvFile.next());
      assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> csvFile.remove());
    }
  }

  @Test
  public void test_of_simple_with_header() {
    try (CsvIterator csvFile = CsvIterator.of(CharSource.wrap(CSV1), true)) {
      ImmutableList<String> headers = csvFile.headers();
      assertThat(headers.size()).isEqualTo(2);
      assertThat(csvFile.containsHeader("h1")).isEqualTo(true);
      assertThat(csvFile.containsHeader("h2")).isEqualTo(true);
      assertThat(csvFile.containsHeader("a")).isEqualTo(false);
      assertThat(csvFile.containsHeader(Pattern.compile("h."))).isEqualTo(true);
      assertThat(csvFile.containsHeader(Pattern.compile("a"))).isEqualTo(false);
      assertThat(headers.get(0)).isEqualTo("h1");
      assertThat(headers.get(1)).isEqualTo("h2");
      CsvRow peeked = csvFile.peek();
      CsvRow row0 = csvFile.next();
      assertThat(row0).isEqualTo(peeked);
      assertThat(row0.headers()).isEqualTo(headers);
      assertThat(row0.lineNumber()).isEqualTo(2);
      assertThat(row0.fieldCount()).isEqualTo(2);
      assertThat(row0.field(0)).isEqualTo("r11");
      assertThat(row0.field(1)).isEqualTo("r12");
      CsvRow row1 = csvFile.next();
      assertThat(row1.headers()).isEqualTo(headers);
      assertThat(row1.lineNumber()).isEqualTo(3);
      assertThat(row1.fieldCount()).isEqualTo(2);
      assertThat(row1.field(0)).isEqualTo("r21");
      assertThat(row1.field(1)).isEqualTo("r22");
      assertThat(csvFile.hasNext()).isEqualTo(false);
      assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() -> csvFile.peek());
      assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() -> csvFile.peek());
      assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() -> csvFile.next());
      assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() -> csvFile.next());
      assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> csvFile.remove());
    }
  }

  @Test
  public void test_of_comment_blank_no_header() {
    try (CsvIterator csvFile = CsvIterator.of(CharSource.wrap(CSV2), false)) {
      assertThat(csvFile.headers().size()).isEqualTo(0);
      assertThat(csvFile.hasNext()).isEqualTo(true);
      CsvRow row0 = csvFile.next();
      assertThat(row0.lineNumber()).isEqualTo(1);
      assertThat(row0.fieldCount()).isEqualTo(2);
      assertThat(row0.field(0)).isEqualTo("h1");
      assertThat(row0.field(1)).isEqualTo("h2");
      CsvRow row1 = csvFile.next();
      assertThat(row1.lineNumber()).isEqualTo(5);
      assertThat(row1.fieldCount()).isEqualTo(2);
      assertThat(row1.field(0)).isEqualTo("r21");
      assertThat(row1.field(1)).isEqualTo("r22");
      assertThat(csvFile.hasNext()).isEqualTo(false);
    }
  }

  @Test
  public void test_of_comment_blank_with_header() {
    try (CsvIterator csvFile = CsvIterator.of(CharSource.wrap(CSV2), true)) {
      ImmutableList<String> headers = csvFile.headers();
      assertThat(headers.size()).isEqualTo(2);
      assertThat(headers.get(0)).isEqualTo("h1");
      assertThat(headers.get(1)).isEqualTo("h2");
      assertThat(csvFile.hasNext()).isEqualTo(true);
      CsvRow row0 = csvFile.next();
      assertThat(row0.lineNumber()).isEqualTo(5);
      assertThat(row0.fieldCount()).isEqualTo(2);
      assertThat(row0.field(0)).isEqualTo("r21");
      assertThat(row0.field(1)).isEqualTo("r22");
      assertThat(csvFile.hasNext()).isEqualTo(false);
    }
  }

  @Test
  public void test_of_blank_row() {
    try (CsvIterator csvFile = CsvIterator.of(CharSource.wrap(CSV3), false)) {
      assertThat(csvFile.hasNext()).isEqualTo(true);
      CsvRow row0 = csvFile.next();
      assertThat(row0.fieldCount()).isEqualTo(2);
      assertThat(row0.field(0)).isEqualTo("r11");
      assertThat(row0.field(1)).isEqualTo("r12");
      CsvRow row1 = csvFile.next();
      assertThat(row1.fieldCount()).isEqualTo(2);
      assertThat(row1.field(0)).isEqualTo("r21");
      assertThat(row1.field(1)).isEqualTo("r22");
      assertThat(csvFile.hasNext()).isEqualTo(false);
    }
  }

  @Test
  public void test_of_headerComment() {
    try (CsvIterator csvFile = CsvIterator.of(CharSource.wrap(CSV4), true)) {
      assertThat(csvFile.hasNext()).isEqualTo(true);
      CsvRow row0 = csvFile.next();
      assertThat(row0.lineNumber()).isEqualTo(3);
      assertThat(csvFile.headers().size()).isEqualTo(2);
      assertThat(csvFile.headers().get(0)).isEqualTo("h1");
      assertThat(csvFile.headers().get(1)).isEqualTo("h2");
      assertThat(row0.fieldCount()).isEqualTo(2);
      assertThat(row0.field(0)).isEqualTo("r1");
      assertThat(row0.field(1)).isEqualTo("r2");
    }
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_of_empty_no_header_reader() {
    try (CsvIterator csvFile = CsvIterator.of(new StringReader(""), false, ',')) {
      assertThat(csvFile.headers().size()).isEqualTo(0);
      assertThat(csvFile.hasNext()).isEqualTo(false);
    }
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_nextBatch1() {
    try (CsvIterator csvFile = CsvIterator.of(CharSource.wrap(CSV1), true)) {
      ImmutableList<String> headers = csvFile.headers();
      assertThat(headers.size()).isEqualTo(2);
      assertThat(headers.get(0)).isEqualTo("h1");
      assertThat(headers.get(1)).isEqualTo("h2");
      List<CsvRow> a = csvFile.nextBatch(0);
      assertThat(a.size()).isEqualTo(0);
      List<CsvRow> b = csvFile.nextBatch(1);
      assertThat(b.size()).isEqualTo(1);
      CsvRow row0 = b.get(0);
      assertThat(row0.headers()).isEqualTo(headers);
      assertThat(row0.fieldCount()).isEqualTo(2);
      assertThat(row0.field(0)).isEqualTo("r11");
      assertThat(row0.field(1)).isEqualTo("r12");
      List<CsvRow> c = csvFile.nextBatch(2);
      assertThat(c.size()).isEqualTo(1);
      CsvRow row1 = c.get(0);
      assertThat(row1.headers()).isEqualTo(headers);
      assertThat(row1.fieldCount()).isEqualTo(2);
      assertThat(row1.field(0)).isEqualTo("r21");
      assertThat(row1.field(1)).isEqualTo("r22");
      List<CsvRow> d = csvFile.nextBatch(2);
      assertThat(d.size()).isEqualTo(0);
      assertThat(csvFile.hasNext()).isEqualTo(false);
    }
  }

  @Test
  public void test_nextBatch2() {
    try (CsvIterator csvFile = CsvIterator.of(CharSource.wrap(CSV1), true)) {
      ImmutableList<String> headers = csvFile.headers();
      assertThat(headers.size()).isEqualTo(2);
      assertThat(headers.get(0)).isEqualTo("h1");
      assertThat(headers.get(1)).isEqualTo("h2");
      List<CsvRow> a = csvFile.nextBatch(3);
      assertThat(a.size()).isEqualTo(2);
      CsvRow row0 = a.get(0);
      assertThat(row0.headers()).isEqualTo(headers);
      assertThat(row0.fieldCount()).isEqualTo(2);
      assertThat(row0.field(0)).isEqualTo("r11");
      assertThat(row0.field(1)).isEqualTo("r12");
      CsvRow row1 = a.get(1);
      assertThat(row1.headers()).isEqualTo(headers);
      assertThat(row1.fieldCount()).isEqualTo(2);
      assertThat(row1.field(0)).isEqualTo("r21");
      assertThat(row1.field(1)).isEqualTo("r22");
      List<CsvRow> d = csvFile.nextBatch(2);
      assertThat(d.size()).isEqualTo(0);
      assertThat(csvFile.hasNext()).isEqualTo(false);
      assertThat(csvFile.hasNext()).isEqualTo(false);
    }
  }

  //-------------------------------------------------------------------------
  @Test
  public void nextBatch_predicate() {
    try (CsvIterator csvFile = CsvIterator.of(CharSource.wrap(CSV5GROUPED), true)) {
      ImmutableList<String> headers = csvFile.headers();
      assertThat(headers.size()).isEqualTo(2);
      assertThat(headers.get(0)).isEqualTo("id");
      assertThat(headers.get(1)).isEqualTo("value");
      int batches = 0;
      int total = 0;
      while (csvFile.hasNext()) {
        CsvRow first = csvFile.peek();
        String id = first.getValue("id");
        List<CsvRow> batch = csvFile.nextBatch(row -> row.getValue("id").equals(id));
        assertThat(batch.stream().map(row -> row.getValue("id")).distinct().count()).isEqualTo(1);
        batches++;
        total += batch.size();
      }
      assertThat(batches).isEqualTo(3);
      assertThat(total).isEqualTo(6);
    }
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_asStream_empty_no_header() {
    try (CsvIterator csvFile = CsvIterator.of(CharSource.wrap(""), false)) {
      assertThat(csvFile.asStream().collect(toList()).size()).isEqualTo(0);
    }
  }

  @Test
  public void test_asStream_simple_no_header() {
    try (CsvIterator csvFile = CsvIterator.of(CharSource.wrap(CSV1), false)) {
      assertThat(csvFile.headers().size()).isEqualTo(0);
      List<CsvRow> rows = csvFile.asStream().collect(toList());
      assertThat(csvFile.hasNext()).isEqualTo(false);
      assertThat(rows.size()).isEqualTo(3);
      CsvRow row0 = rows.get(0);
      assertThat(row0.headers().size()).isEqualTo(0);
      assertThat(row0.fieldCount()).isEqualTo(2);
      assertThat(row0.field(0)).isEqualTo("h1");
      assertThat(row0.field(1)).isEqualTo("h2");
      CsvRow row1 = rows.get(1);
      assertThat(row1.headers().size()).isEqualTo(0);
      assertThat(row1.fieldCount()).isEqualTo(2);
      assertThat(row1.field(0)).isEqualTo("r11");
      assertThat(row1.field(1)).isEqualTo("r12");
      CsvRow row2 = rows.get(2);
      assertThat(row2.headers().size()).isEqualTo(0);
      assertThat(row2.fieldCount()).isEqualTo(2);
      assertThat(row2.field(0)).isEqualTo("r21");
      assertThat(row2.field(1)).isEqualTo("r22");
    }
  }

  @Test
  public void test_asStream_simple_with_header() {
    try (CsvIterator csvFile = CsvIterator.of(CharSource.wrap(CSV1), true)) {
      ImmutableList<String> headers = csvFile.headers();
      assertThat(headers.size()).isEqualTo(2);
      assertThat(headers.get(0)).isEqualTo("h1");
      assertThat(headers.get(1)).isEqualTo("h2");
      List<CsvRow> rows = csvFile.asStream().collect(toList());
      assertThat(csvFile.hasNext()).isEqualTo(false);
      assertThat(rows.size()).isEqualTo(2);
      CsvRow row0 = rows.get(0);
      assertThat(row0.headers()).isEqualTo(headers);
      assertThat(row0.fieldCount()).isEqualTo(2);
      assertThat(row0.field(0)).isEqualTo("r11");
      assertThat(row0.field(1)).isEqualTo("r12");
      CsvRow row1 = rows.get(1);
      assertThat(row1.headers()).isEqualTo(headers);
      assertThat(row1.fieldCount()).isEqualTo(2);
      assertThat(row1.field(0)).isEqualTo("r21");
      assertThat(row1.field(1)).isEqualTo("r22");
    }
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_toString() {
    try (CsvIterator test = CsvIterator.of(CharSource.wrap(CSV1), true)) {
      assertThat(test.toString()).isNotNull();
    }
  }

}
