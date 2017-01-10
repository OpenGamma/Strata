/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.io;

import static org.testng.Assert.assertEquals;

import java.util.List;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;

/**
 * Test {@link  AsciiTable}.
 */
@Test
public class AsciiTableTest {

  private static final String LINE_SEPARATOR = System.lineSeparator();

  public void test_generate_padData() {
    List<AsciiTableAlignment> alignments = ImmutableList.of(AsciiTableAlignment.LEFT, AsciiTableAlignment.RIGHT);
    List<String> headers = ImmutableList.of("Alpha", "Beta");
    List<List<String>> cells = ImmutableList.of(ImmutableList.of("12", "23"), ImmutableList.of("12345", ""));
    String test = AsciiTable.generate(headers, alignments, cells);
    String expected = "" +
        "+-------+------+" + LINE_SEPARATOR +
        "| Alpha | Beta |" + LINE_SEPARATOR +
        "+-------+------+" + LINE_SEPARATOR +
        "| 12    |   23 |" + LINE_SEPARATOR +
        "| 12345 |      |" + LINE_SEPARATOR +
        "+-------+------+" + LINE_SEPARATOR;
    assertEquals(test, expected);
  }

  public void test_generate_padHeader() {
    List<AsciiTableAlignment> alignments = ImmutableList.of(AsciiTableAlignment.LEFT, AsciiTableAlignment.RIGHT);
    List<String> headers = ImmutableList.of("A", "B");
    List<List<String>> cells = ImmutableList.of(ImmutableList.of("12", "23"), ImmutableList.of("12345", ""));
    String test = AsciiTable.generate(headers, alignments, cells);
    String expected = "" +
        "+-------+----+" + LINE_SEPARATOR +
        "| A     |  B |" + LINE_SEPARATOR +
        "+-------+----+" + LINE_SEPARATOR +
        "| 12    | 23 |" + LINE_SEPARATOR +
        "| 12345 |    |" + LINE_SEPARATOR +
        "+-------+----+" + LINE_SEPARATOR;
    assertEquals(test, expected);
  }

}
