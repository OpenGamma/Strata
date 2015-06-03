/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.report;

import java.io.OutputStream;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Represents a business report.
 * <p>
 * A report is a transformation of calculation engine results for a specific purpose, for example
 * a trade report on a list of trades, or a cashflow report on a single trade.
 */
public interface Report {
  
  /**
   * Gets the valuation date of the results driving the report.
   * 
   * @return the valuation date
   */
  LocalDate getValuationDate();
  
  /**
   * Gets the instant at which the report was run, which may be independent of the valuation date.
   * 
   * @return the run instant
   */
  Instant getRunInstant();
  
  /**
   * Writes this report out in a CSV format.
   * 
   * @param out  the output stream to write to
   */
  void writeCsv(OutputStream out);
  
  /**
   * Writes this report out as an ASCII table.
   * 
   * @param out  the output stream to write to
   */
  void writeAsciiTable(OutputStream out);
  
}
