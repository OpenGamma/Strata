/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.report.framework.format;

import java.util.List;

import com.google.common.base.Strings;

/**
 * An ASCII table generator.
 * <p>
 * Provides the ability to generate an ASCII table, typically used on the command line.
 */
final class AsciiTable {

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private AsciiTable() {
  }

  /**
   * Generates the ASCII table.
   * 
   * @param alignments  the table alignments
   * @param headers  the table headers
   * @param cells  the table cells
   * @return the table
   */
  static String generate(
      List<AsciiTableAlignment> alignments,
      List<String> headers,
      List<? extends List<String>> cells) {

    int colCount = alignments.size();

    // find max length of each column
    int[] colLengths = new int[colCount];
    for (int colIdx = 0; colIdx < colCount; colIdx++) {
      colLengths[colIdx] = headers.get(colIdx).length();
    }
    for (List<String> row : cells) {
      for (int colIdx = 0; colIdx < colCount; colIdx++) {
        colLengths[colIdx] = Math.max(colLengths[colIdx], row.get(colIdx).length());
      }
    }

    // write table
    StringBuilder buf = new StringBuilder(2048);
    writeSeparatorLine(buf, colLengths);
    writeDataLine(buf, colLengths, alignments, headers);
    writeSeparatorLine(buf, colLengths);
    for (int rowIdx = 0; rowIdx < cells.size(); rowIdx++) {
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
    buf.append("+\n");
  }

  // write a data line
  private static void writeDataLine(
      StringBuilder buf,
      int[] colLengths,
      List<AsciiTableAlignment> alignments,
      List<String> headers) {

    for (int colIdx = 0; colIdx < colLengths.length; colIdx++) {
      buf.append('|')
          .append(' ')
          .append(formatData(buf, colLengths[colIdx], alignments.get(colIdx), headers.get(colIdx)))
          .append(' ');
    }
    buf.append("|\n");
  }

  // writes a data item
  private static String formatData(
      StringBuilder buf,
      int colLength,
      AsciiTableAlignment alignment,
      String data) {

    if (alignment == AsciiTableAlignment.RIGHT) {
      return Strings.padStart(data, colLength, ' ');
    } else {
      return Strings.padEnd(data, colLength, ' ');
    }
  }

}
