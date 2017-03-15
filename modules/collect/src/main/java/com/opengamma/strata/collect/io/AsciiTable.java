/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.io;

import java.util.List;

import com.google.common.base.Strings;
import com.opengamma.strata.collect.ArgChecker;

/**
 * An ASCII table generator.
 * <p>
 * Provides the ability to generate a simple ASCII table, typically used on the command line.
 * All data is provided as strings, with formatting the responsibility of the caller.
 */
public final class AsciiTable {

  /**
   * Line separator.
   */
  private static final String LINE_SEPARATOR = System.lineSeparator();

  //-------------------------------------------------------------------------
  /**
   * Generates the ASCII table.
   * <p>
   * The caller specifies the headers for each column and the alignment to use,
   * plus the list of lists representing the data. All data is provided as strings,
   * with formatting the responsibility of the caller.
   * 
   * @param headers  the table headers
   * @param alignments  the table alignments, must match the size of the headers
   * @param cells  the table cells, outer list of rows, inner list of columns
   * @return the table
   * @throws IllegalArgumentException if the number of columns specified is inconsistent
   */
  public static String generate(
      List<String> headers,
      List<AsciiTableAlignment> alignments,
      List<? extends List<String>> cells) {

    int colCount = alignments.size();
    int rowCount = cells.size();
    ArgChecker.isTrue(
        headers.size() == colCount,
        "Number of headers {} must match number of alignments {}", headers.size(), colCount);

    // find max length of each column
    int[] colLengths = new int[colCount];
    for (int colIdx = 0; colIdx < colCount; colIdx++) {
      colLengths[colIdx] = headers.get(colIdx).length();
    }
    for (int rowIdx = 0; rowIdx < rowCount; rowIdx++) {
      List<String> row = cells.get(rowIdx);
      ArgChecker.isTrue(
          row.size() == colCount,
          "Table of cells has incorrect number of columns {} in row {}", row.size(), rowIdx);
      for (int colIdx = 0; colIdx < colCount; colIdx++) {
        colLengths[colIdx] = Math.max(colLengths[colIdx], row.get(colIdx).length());
      }
    }
    int colTotalLength = 3;  // allow for last vertical separator and windows line separator
    for (int colIdx = 0; colIdx < colCount; colIdx++) {
      colTotalLength += colLengths[colIdx] + 3;  // each column has two spaces and a vertical separator
    }

    // write table
    StringBuilder buf = new StringBuilder((rowCount + 3) * colTotalLength);
    writeSeparatorLine(buf, colLengths);
    writeDataLine(buf, colLengths, alignments, headers);
    writeSeparatorLine(buf, colLengths);
    for (int rowIdx = 0; rowIdx < rowCount; rowIdx++) {
      writeDataLine(buf, colLengths, alignments, cells.get(rowIdx));
    }
    writeSeparatorLine(buf, colLengths);
    return buf.toString();
  }

  // write a separator line
  private static void writeSeparatorLine(StringBuilder buf, int[] colLengths) {
    for (int colIdx = 0; colIdx < colLengths.length; colIdx++) {
      buf.append('+');
      for (int i = 0; i < colLengths[colIdx] + 2; i++) {
        buf.append('-');
      }
    }
    buf.append('+').append(LINE_SEPARATOR);
  }

  // write a data line
  private static void writeDataLine(
      StringBuilder buf,
      int[] colLengths,
      List<AsciiTableAlignment> alignments,
      List<String> values) {

    for (int colIdx = 0; colIdx < colLengths.length; colIdx++) {
      String value = Strings.nullToEmpty(values.get(colIdx));
      buf.append('|')
          .append(' ')
          .append(formatValue(buf, colLengths[colIdx], alignments.get(colIdx), value))
          .append(' ');
    }
    buf.append('|').append(LINE_SEPARATOR);
  }

  // writes a data item
  private static String formatValue(
      StringBuilder buf,
      int colLength,
      AsciiTableAlignment alignment,
      String value) {

    if (alignment == AsciiTableAlignment.RIGHT) {
      return Strings.padStart(value, colLength, ' ');
    } else {
      return Strings.padEnd(value, colLength, ' ');
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private AsciiTable() {
  }

}
