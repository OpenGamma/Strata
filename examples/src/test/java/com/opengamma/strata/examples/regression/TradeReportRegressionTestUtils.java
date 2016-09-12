/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.examples.regression;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.testng.Assert;

import com.google.common.base.Strings;
import com.opengamma.strata.collect.Messages;

/**
 * Utility class for trade report regression tests.
 */
public final class TradeReportRegressionTestUtils {

  /**
   * Restricted constructor.
   */
  private TradeReportRegressionTestUtils() {
  }

  public static void assertAsciiTableEquals(String actual, String expected) {
    List<String> actualLines = toLines(actual);
    List<String> expectedLines = toLines(expected);

    int maxLines = Math.max(actualLines.size(), expectedLines.size());

    for (int i = 0; i < maxLines; i++) {
      if (i >= actualLines.size()) {
        String expectedLine = expectedLines.get(i);
        Assert.fail(Messages.format("No more results but expected:\n{}", expectedLine));
      }
      if (i >= expectedLines.size()) {
        String actualLine = actualLines.get(i);
        Assert.fail(Messages.format("Expected end of results but got:\n{}", actualLine));
      }
      String actualLine = actualLines.get(i);
      String expectedLine = expectedLines.get(i);
      if (!actualLine.equals(expectedLine)) {
        if (isDataRow(expectedLine) && isDataRow(actualLine)) {
          List<String> actualCells = toCells(actualLine);
          List<String> expectedCells = toCells(expectedLine);
          Assert.assertEquals(actualCells, expectedCells, "Mismatch at line " + i);
        } else {
          Assert.fail(Messages.format(
              "Mismatch at line {}:\n" +
                  "Expected:\n" +
                  "{}\n" +
                  "Got:\n" +
                  "{}\n" +
                  "Expected table:\n" +
                  "{}\n" +
                  "Actual table:\n" +
                  "{}",
              i,
              expectedLine,
              actualLine,
              expected,
              actual));
        }
      }
    }
  }

  private static List<String> toLines(String asciiTable) {
    return Arrays.asList(asciiTable.split("\\r?\\n")).stream()
        .filter(line -> !Strings.nullToEmpty(line).trim().isEmpty())
        .collect(Collectors.toList());
  }

  private static boolean isDataRow(String asciiLine) {
    return asciiLine.contains("|");
  }

  private static List<String> toCells(String asciiLine) {
    return Arrays.asList(asciiLine.split("\\|"));
  }

}
