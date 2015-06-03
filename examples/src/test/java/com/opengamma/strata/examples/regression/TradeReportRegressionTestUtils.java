/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.examples.regression;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.testng.Assert;

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
              "{}", i, expectedLine, actualLine));
        }
      }
    }
  }
  
  private static List<String> toLines(String asciiTable) {
    return Arrays.asList(asciiTable.split(System.lineSeparator())).stream()
        .filter(line -> StringUtils.isNotBlank(line))
        .collect(Collectors.toList());
  }
  
  private static boolean isDataRow(String asciiLine) {
    return asciiLine.contains("|");
  }
  
  private static List<String> toCells(String asciiLine) {
    return Arrays.asList(asciiLine.split("\\|"));
  }
  
}
