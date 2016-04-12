/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.report.framework.format;

import static org.testng.Assert.assertEquals;

import java.util.List;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;

/**
 * Test {@link  AsciiTable}.
 */
@Test
public class AsciiTableTest {

  public void test_generate_padData() {
    List<AsciiTableAlignment> alignments = ImmutableList.of(AsciiTableAlignment.LEFT, AsciiTableAlignment.RIGHT);
    List<String> headers = ImmutableList.of("Alpha", "Beta");
    List<List<String>> cells = ImmutableList.of(ImmutableList.of("12", "23"), ImmutableList.of("12345", ""));
    String test = AsciiTable.generate(alignments, headers, cells);
    String expected = "" +
        "+-------+------+\n" +
        "| Alpha | Beta |\n" +
        "+-------+------+\n" +
        "| 12    |   23 |\n" +
        "| 12345 |      |\n" +
        "+-------+------+\n";
    assertEquals(test, expected);
  }

  public void test_generate_padHeader() {
    List<AsciiTableAlignment> alignments = ImmutableList.of(AsciiTableAlignment.LEFT, AsciiTableAlignment.RIGHT);
    List<String> headers = ImmutableList.of("A", "B");
    List<List<String>> cells = ImmutableList.of(ImmutableList.of("12", "23"), ImmutableList.of("12345", ""));
    String test = AsciiTable.generate(alignments, headers, cells);
    String expected = "" +
        "+-------+----+\n" +
        "| A     |  B |\n" +
        "+-------+----+\n" +
        "| 12    | 23 |\n" +
        "| 12345 |    |\n" +
        "+-------+----+\n";
    assertEquals(test, expected);
  }

}
