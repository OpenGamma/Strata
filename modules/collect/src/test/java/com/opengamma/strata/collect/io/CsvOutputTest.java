/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.io;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertThrows;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.List;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.collect.io.CsvOutput.CsvRowOutputWithHeaders;

/**
 * Test {@link CsvOutput}.
 */
@Test
public class CsvOutputTest {

  private static final String LINE_SEP = System.lineSeparator();
  private static final String LINE_ITEM_SEP_COMMA = ",";
  private static final String LINE_ITEM_SEP_TAB = "\t";

  //-------------------------------------------------------------------------
  public void test_standard_writeLines_alwaysQuote() {
    List<List<String>> rows = Arrays.asList(Arrays.asList("a", "x"), Arrays.asList("b", "y"));
    StringBuilder buf = new StringBuilder();
    CsvOutput.standard(buf, "\n").writeLines(rows, true);
    assertEquals(buf.toString(), "\"a\",\"x\"\n\"b\",\"y\"\n");
  }

  public void test_standard_writeLines_selectiveQuote_commaAndQuote() {
    List<List<String>> rows = Arrays.asList(Arrays.asList("a", "1,000"), Arrays.asList("b\"c", "y"));
    StringBuilder buf = new StringBuilder();
    CsvOutput.standard(buf, "\n", LINE_ITEM_SEP_COMMA).writeLines(rows, false);
    assertEquals(buf.toString(), "a,\"1,000\"\n\"b\"\"c\",y\n");
  }

  public void test_standard_writeLines_selectiveQuote_trimmable() {
    List<List<String>> rows = Arrays.asList(Arrays.asList("a", " x"), Arrays.asList("b ", "y"));
    StringBuilder buf = new StringBuilder();
    CsvOutput.standard(buf, "\n", LINE_ITEM_SEP_COMMA).writeLines(rows, false);
    assertEquals(buf.toString(), "a,\" x\"\n\"b \",y\n");
  }

  public void test_standard_writeLines_systemNewLine() {
    List<List<String>> rows = Arrays.asList(Arrays.asList("a", "x"), Arrays.asList("b", "y"));
    StringBuilder buf = new StringBuilder();
    CsvOutput.standard(buf).writeLines(rows, false);
    assertEquals(buf.toString(), "a,x" + LINE_SEP + "b,y" + LINE_SEP);
  }

  public void test_standard_writeLine_selectiveQuote() {
    StringBuilder buf = new StringBuilder();
    CsvOutput.standard(buf, "\n", LINE_ITEM_SEP_COMMA).writeLine(Arrays.asList("a", "1,000"));
    assertEquals(buf.toString(), "a,\"1,000\"\n");
  }

  public void test_standard_writeLines_tab_separated() {
    StringBuilder buf = new StringBuilder();
    CsvOutput.standard(buf, "\n", LINE_ITEM_SEP_TAB).writeLine(Arrays.asList("a", "1,000"));
    assertEquals(buf.toString(), "a\t\"1,000\"\n");
  }

  public void test_standard_expressionPrefix() {
    StringBuilder buf = new StringBuilder();
    CsvOutput.standard(buf, "\n").writeLine(Arrays.asList("=cmd", "+cmd", "-cmd", "@cmd", ""));
    assertEquals(buf.toString(), "\"=cmd\",+cmd,-cmd,\"@cmd\",\n");
  }

  public void test_standard_expressionPrefixNumbers() {
    StringBuilder buf = new StringBuilder();
    CsvOutput.standard(buf, "\n").writeLine(Arrays.asList("+8", "-7", "+8-7", "-7+8", "NaN", "-Infinity"));
    assertEquals(buf.toString(), "+8,-7,+8-7,-7+8,NaN,-Infinity\n");
  }

  //-------------------------------------------------------------------------
  public void test_safe_writeLines_systemNewLine() {
    List<List<String>> rows = Arrays.asList(Arrays.asList("a", "=x"), Arrays.asList("b", "y"));
    StringBuilder buf = new StringBuilder();
    CsvOutput.safe(buf).writeLines(rows, false);
    assertEquals(buf.toString(), "a,=\"=x\"" + LINE_SEP + "b,y" + LINE_SEP);
  }

  public void test_safe_expressionPrefix() {
    StringBuilder buf = new StringBuilder();
    CsvOutput.safe(buf, "\n").writeLine(Arrays.asList("=cmd", "+cmd", "-cmd", "@cmd"));
    assertEquals(buf.toString(), "=\"=cmd\",=\"+cmd\",=\"-cmd\",=\"@cmd\"\n");
  }

  public void test_safe_expressionPrefixNumbers() {
    StringBuilder buf = new StringBuilder();
    CsvOutput.safe(buf, "\n", LINE_ITEM_SEP_COMMA)
        .writeLine(Arrays.asList("+8", "-7", "+8-7", "-7+8", "NaN", "-Infinity"));
    assertEquals(buf.toString(), "+8,-7,=\"+8-7\",=\"-7+8\",NaN,=\"-Infinity\"\n");
  }

  //-------------------------------------------------------------------------
  public void test_writeCell() {
    StringBuilder buf = new StringBuilder();
    CsvOutput.standard(buf, "\n")
        .writeCell("a")
        .writeCell("x")
        .writeNewLine()
        .writeCell("b", true)
        .writeCell("y", true)
        .writeNewLine();
    assertEquals(buf.toString(), "a,x\n\"b\",\"y\"\n");
  }

  public void test_mixed() {
    List<String> row = Arrays.asList("x", "y");
    StringBuilder buf = new StringBuilder();
    CsvOutput.standard(buf, "\n")
        .writeCell("a")
        .writeCell("b")
        .writeLine(row);
    assertEquals(buf.toString(), "a,b,x,y\n");
  }

  //-------------------------------------------------------------------------
  public void test_withHeaders_writeCell() {
    List<String> headers = Arrays.asList("h1", "h2", "h3");
    StringBuilder buf = new StringBuilder();
    CsvRowOutputWithHeaders csv = CsvOutput.standard(buf).withHeaders(headers, false);
    assertEquals(csv.headers(), headers);
    assertEquals(buf.toString(), "h1,h2,h3" + LINE_SEP);
    csv.writeCell("h1", "a");
    csv.writeCell("h3", "c");
    csv.writeCell("h1", "A");
    assertEquals(buf.toString(), "h1,h2,h3" + LINE_SEP);
    csv.writeNewLine();
    assertEquals(buf.toString(), "h1,h2,h3" + LINE_SEP + "A,,c" + LINE_SEP);
    assertThrows(IllegalArgumentException.class, () -> csv.writeCell("H1", "x"));
  }

  public void test_withHeaders_writeCells() {
    List<String> headers = Arrays.asList("h1", "h2", "h3");
    StringBuilder buf = new StringBuilder();
    CsvRowOutputWithHeaders csv = CsvOutput.standard(buf).withHeaders(headers, false);
    assertEquals(buf.toString(), "h1,h2,h3" + LINE_SEP);
    csv.writeCells(ImmutableMap.of("h1", "a", "h2", "b"));
    csv.writeCell("h3", "c");
    assertEquals(buf.toString(), "h1,h2,h3" + LINE_SEP);
    csv.writeNewLine();
    assertEquals(buf.toString(), "h1,h2,h3" + LINE_SEP + "a,b,c" + LINE_SEP);
  }

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
    assertEquals(buf.toString(), "h1,h2,h3" + LINE_SEP + "1.23,123,123" + LINE_SEP + "123,123,123" + LINE_SEP);
  }

  public void test_withHeaders_writeLine() {
    List<String> headers = Arrays.asList("h1", "h2", "h3");
    StringBuilder buf = new StringBuilder();
    CsvRowOutputWithHeaders csv = CsvOutput.standard(buf).withHeaders(headers, false);
    assertEquals(buf.toString(), "h1,h2,h3" + LINE_SEP);
    csv.writeLine(ImmutableMap.of("h1", "a", "h2", "b"));
    assertEquals(buf.toString(), "h1,h2,h3" + LINE_SEP + "a,b," + LINE_SEP);
    csv.writeCell("h3", "c");
    assertEquals(buf.toString(), "h1,h2,h3" + LINE_SEP + "a,b," + LINE_SEP);
    csv.writeNewLine();
    assertEquals(buf.toString(), "h1,h2,h3" + LINE_SEP + "a,b," + LINE_SEP + ",,c" + LINE_SEP);
  }

  //-------------------------------------------------------------------------
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
    assertThrows(UncheckedIOException.class, () -> output.writeCell("a"));
    assertThrows(UncheckedIOException.class, () -> output.writeNewLine());
  }

}
