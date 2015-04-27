/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.examples.engine;

import java.util.List;

import com.bethecoder.table.AsciiTableInstance;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.engine.Column;
import com.opengamma.strata.engine.calculations.Results;

/**
 * Contains utility methods for formatting calculation results.
 */
public final class ResultsFormatter {

  /**
   * Restricted constructor.
   */
  private ResultsFormatter() {
  }

  //-------------------------------------------------------------------------
  /**
   * Outputs the results of a calculation as an ASCII-formatted table.
   * 
   * @param results  the calculation results
   * @param columns  the ordered columns whose names are to be used in the column headers
   */
  public static void print(Results results, List<Column> columns) {
    String[] headers = columns.stream()
        .map(c -> c.getName().toString())
        .toArray(String[]::new);
    String[][] rows = new String[results.getRowCount()][headers.length];
    for (int i = 0; i < results.getRowCount(); i++) {
      for (int j = 0; j < columns.size(); j++) {
        rows[i][j] = formatResult(results.get(i, j));
      }
    }
    AsciiTableInstance.get().printTable(headers, rows);
  }

  // formats an individual result
  private static String formatResult(Result<?> result) {
    if (result.isFailure()) {
      return "FAIL: " + result.getFailure().getMessage();
    }
    return result.getValue().toString();
  }

}
