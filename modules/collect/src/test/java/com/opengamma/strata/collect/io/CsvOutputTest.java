/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.io;

import static org.testng.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.testng.annotations.Test;

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
    CsvOutput.safe(buf, "\n", LINE_ITEM_SEP_COMMA).writeLine(Arrays.asList("+8", "-7", "+8-7", "-7+8", "NaN", "-Infinity"));
    assertEquals(buf.toString(), "+8,-7,=\"+8-7\",=\"-7+8\",NaN,=\"-Infinity\"\n");
  }

}
