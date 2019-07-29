/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.io;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.CharSource;
import com.opengamma.strata.collect.io.CsvOutput.CsvRowOutputWithHeaders;

/**
 * Test {@link CsvOutput}.
 */
public class CsvOutputTest {

  private static final String LINE_SEP = System.lineSeparator();
  private static final String LINE_ITEM_SEP_COMMA = ",";
  private static final String LINE_ITEM_SEP_TAB = "\t";

  //-------------------------------------------------------------------------
  @Test
  public void test_standard_writeLines_alwaysQuote() {
    List<List<String>> rows = Arrays.asList(Arrays.asList("a", "x"), Arrays.asList("b", "y"));
    StringBuilder buf = new StringBuilder();
    CsvOutput.standard(buf, "\n").writeLines(rows, true);
    assertThat(buf.toString()).isEqualTo("\"a\",\"x\"\n\"b\",\"y\"\n");
  }

  @Test
  public void test_standard_writeLines_selectiveQuote_commaAndQuote() {
    List<List<String>> rows = Arrays.asList(Arrays.asList("a", "1,000"), Arrays.asList("b\"c", "y"));
    StringBuilder buf = new StringBuilder();
    CsvOutput.standard(buf, "\n", LINE_ITEM_SEP_COMMA).writeLines(rows, false);
    assertThat(buf.toString()).isEqualTo("a,\"1,000\"\n\"b\"\"c\",y\n");
  }

  @Test
  public void test_standard_writeLines_selectiveQuote_trimmable() {
    List<List<String>> rows = Arrays.asList(Arrays.asList("a", " x"), Arrays.asList("b ", "y"));
    StringBuilder buf = new StringBuilder();
    CsvOutput.standard(buf, "\n", LINE_ITEM_SEP_COMMA).writeLines(rows, false);
    assertThat(buf.toString()).isEqualTo("a,\" x\"\n\"b \",y\n");
  }

  @Test
  public void test_standard_writeLines_systemNewLine() {
    List<List<String>> rows = Arrays.asList(Arrays.asList("a", "x"), Arrays.asList("b", "y"));
    StringBuilder buf = new StringBuilder();
    CsvOutput.standard(buf).writeLines(rows, false);
    assertThat(buf.toString()).isEqualTo("a,x" + LINE_SEP + "b,y" + LINE_SEP);
  }

  @Test
  public void test_standard_writeLine_selectiveQuote() {
    StringBuilder buf = new StringBuilder();
    CsvOutput.standard(buf, "\n", LINE_ITEM_SEP_COMMA).writeLine(Arrays.asList("a", "1,000"));
    assertThat(buf.toString()).isEqualTo("a,\"1,000\"\n");
  }

  @Test
  public void test_standard_writeLines_tab_separated() {
    StringBuilder buf = new StringBuilder();
    CsvOutput.standard(buf, "\n", LINE_ITEM_SEP_TAB).writeLine(Arrays.asList("a", "1,000"));
    assertThat(buf.toString()).isEqualTo("a\t\"1,000\"\n");
  }

  @Test
  public void test_standard_expressionPrefix() {
    StringBuilder buf = new StringBuilder();
    CsvOutput.standard(buf, "\n").writeLine(Arrays.asList("=cmd", "+cmd", "-cmd", "@cmd", ""));
    assertThat(buf.toString()).isEqualTo("\"=cmd\",+cmd,-cmd,\"@cmd\",\n");
  }

  @Test
  public void test_standard_expressionPrefixNumbers() {
    StringBuilder buf = new StringBuilder();
    CsvOutput.standard(buf, "\n").writeLine(Arrays.asList("+8", "-7", "+8-7", "-7+8", "NaN", "-Infinity"));
    assertThat(buf.toString()).isEqualTo("+8,-7,+8-7,-7+8,NaN,-Infinity\n");
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_safe_writeLines_systemNewLine() {
    List<List<String>> rows = Arrays.asList(Arrays.asList("a", "=x"), Arrays.asList("b", "y"));
    StringBuilder buf = new StringBuilder();
    CsvOutput.safe(buf).writeLines(rows, false);
    assertThat(buf.toString()).isEqualTo("a,=\"=x\"" + LINE_SEP + "b,y" + LINE_SEP);
  }

  @Test
  public void test_safe_expressionPrefix() {
    StringBuilder buf = new StringBuilder();
    CsvOutput.safe(buf, "\n").writeLine(Arrays.asList("=cmd", "+cmd", "-cmd", "@cmd"));
    assertThat(buf.toString()).isEqualTo("=\"=cmd\",=\"+cmd\",=\"-cmd\",=\"@cmd\"\n");
  }

  @Test
  public void test_safe_expressionPrefixNumbers() {
    StringBuilder buf = new StringBuilder();
    CsvOutput.safe(buf, "\n", LINE_ITEM_SEP_COMMA)
        .writeLine(Arrays.asList("+8", "-7", "+8-7", "-7+8", "NaN", "-Infinity"));
    assertThat(buf.toString()).isEqualTo("+8,-7,=\"+8-7\",=\"-7+8\",NaN,=\"-Infinity\"\n");
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_writeCell() {
    StringBuilder buf = new StringBuilder();
    CsvOutput.standard(buf, "\n")
        .writeCell("a")
        .writeCell("x")
        .writeNewLine()
        .writeCell("b", true)
        .writeCell("y", true)
        .writeNewLine();
    assertThat(buf.toString()).isEqualTo("a,x\n\"b\",\"y\"\n");
  }

  @Test
  public void test_mixed() {
    List<String> row = Arrays.asList("x", "y");
    StringBuilder buf = new StringBuilder();
    CsvOutput.standard(buf, "\n")
        .writeCell("a")
        .writeCell("b")
        .writeLine(row);
    assertThat(buf.toString()).isEqualTo("a,b,x,y\n");
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_withHeaders_writeCell() {
    List<String> headers = Arrays.asList("h1", "h2", "h3");
    StringBuilder buf = new StringBuilder();
    CsvRowOutputWithHeaders csv = CsvOutput.standard(buf).withHeaders(headers, false);
    assertThat(csv.headers()).isEqualTo(headers);
    assertThat(buf.toString()).isEqualTo("h1,h2,h3" + LINE_SEP);
    csv.writeCell("h1", "a");
    csv.writeCell("h3", "c");
    csv.writeCell("h1", "A");
    assertThat(buf.toString()).isEqualTo("h1,h2,h3" + LINE_SEP);
    csv.writeNewLine();
    assertThat(buf.toString()).isEqualTo("h1,h2,h3" + LINE_SEP + "A,,c" + LINE_SEP);
    assertThatIllegalArgumentException().isThrownBy(() -> csv.writeCell("H1", "x"));
  }

  @Test
  public void test_withHeaders_writeCells() {
    List<String> headers = Arrays.asList("h1", "h2", "h3");
    StringBuilder buf = new StringBuilder();
    CsvRowOutputWithHeaders csv = CsvOutput.standard(buf).withHeaders(headers, false);
    assertThat(buf.toString()).isEqualTo("h1,h2,h3" + LINE_SEP);
    csv.writeCells(ImmutableMap.of("h1", "a", "h2", "b"));
    csv.writeCell("h3", "c");
    assertThat(buf.toString()).isEqualTo("h1,h2,h3" + LINE_SEP);
    csv.writeNewLine();
    assertThat(buf.toString()).isEqualTo("h1,h2,h3" + LINE_SEP + "a,b,c" + LINE_SEP);
  }

  @Test
  public void test_withHeaders_writeCells_numbers() {
    List<String> headers = Arrays.asList("h1", "h2", "h3");
    StringBuilder buf = new StringBuilder();
    CsvRowOutputWithHeaders csv = CsvOutput.standard(buf).withHeaders(headers, false);
    csv.writeCell("h1", 1.23d);
    csv.writeCell("h2", 123d);
    csv.writeCell("h3", 123L);
    csv.writeNewLine();
    csv.writeCell("h1", Double.valueOf(123d));
    csv.writeCell("h2", Float.valueOf(123f));
    csv.writeCell("h3", Long.valueOf(123L));
    csv.writeNewLine();
    assertThat(buf.toString()).isEqualTo("h1,h2,h3" + LINE_SEP + "1.23,123,123" + LINE_SEP + "123,123,123" + LINE_SEP);
  }

  @Test
  public void test_withHeaders_writeLine() {
    List<String> headers = Arrays.asList("h1", "h2", "h3");
    StringBuilder buf = new StringBuilder();
    CsvRowOutputWithHeaders csv = CsvOutput.standard(buf).withHeaders(headers, false);
    assertThat(buf.toString()).isEqualTo("h1,h2,h3" + LINE_SEP);
    csv.writeLine(ImmutableMap.of("h1", "a", "h2", "b"));
    assertThat(buf.toString()).isEqualTo("h1,h2,h3" + LINE_SEP + "a,b," + LINE_SEP);
    csv.writeCell("h3", "c");
    assertThat(buf.toString()).isEqualTo("h1,h2,h3" + LINE_SEP + "a,b," + LINE_SEP);
    csv.writeNewLine();
    assertThat(buf.toString()).isEqualTo("h1,h2,h3" + LINE_SEP + "a,b," + LINE_SEP + ",,c" + LINE_SEP);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_exception() {
    Appendable throwingAppendable = new Appendable() {

      @Override
      public Appendable append(CharSequence csq, int start, int end) throws IOException {
        throw new IOException();
      }

      @Override
      public Appendable append(char c) throws IOException {
        throw new IOException();
      }

      @Override
      public Appendable append(CharSequence csq) throws IOException {
        throw new IOException();
      }
    };

    CsvOutput output = CsvOutput.standard(throwingAppendable, "\n");
    assertThatExceptionOfType(UncheckedIOException.class).isThrownBy(() -> output.writeCell("a"));
    assertThatExceptionOfType(UncheckedIOException.class).isThrownBy(() -> output.writeNewLine());
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_write_csv_file() throws IOException {
    CsvFile file = CsvFile.of(CharSource.wrap("a,b,c\n1,=2,3"), true);

    try (StringWriter underlying = new StringWriter()) {
      CsvOutput.standard(underlying, "\n", ",").writeCsvFile(file, false);
      assertThat(underlying.toString()).isEqualTo("a,b,c\n1,\"=2\",3\n");
    }
  }

  @Test
  public void test_write_csv_file_always_quote() throws IOException {
    CsvFile file = CsvFile.of(CharSource.wrap("a,b,c\n1,=2,3"), true);
    try (StringWriter underlying = new StringWriter()) {
      CsvOutput.standard(underlying, "\n", ",").writeCsvFile(file, true);
      assertThat(underlying.toString()).isEqualTo("\"a\",\"b\",\"c\"\n\"1\",\"=2\",\"3\"\n");
    }
  }

  @Test
  public void test_write_csv_iterator() throws IOException {
    CsvIterator iterator = CsvIterator.of(CharSource.wrap("a,b,c\n1,=2,3"), true);
    try (StringWriter underlying = new StringWriter()) {
      CsvOutput.standard(underlying, "\n", ",").writeCsvIterator(iterator, false);
      assertThat(underlying.toString()).isEqualTo("a,b,c\n1,\"=2\",3\n");
    }
  }

  @Test
  public void test_write_csv_iterator_always_quote() throws IOException {
    CsvIterator iterator = CsvIterator.of(CharSource.wrap("a,b,c\n1,=2,3"), true);
    try (StringWriter underlying = new StringWriter()) {
      CsvOutput.standard(underlying, "\n", ",").writeCsvIterator(iterator, true);
      assertThat(underlying.toString()).isEqualTo("\"a\",\"b\",\"c\"\n\"1\",\"=2\",\"3\"\n");
    }
  }
}
