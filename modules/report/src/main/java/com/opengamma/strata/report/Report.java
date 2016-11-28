/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.report;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;

import com.google.common.collect.ImmutableList;

/**
 * Represents a business report.
 * <p>
 * A report is a transformation of calculation engine results for a specific purpose, for example
 * a trade report on a list of trades, or a cashflow report on a single trade.
 * <p>
 * The report physically represents a table of data, with column headers.
 */
public interface Report {

  /**
   * Gets the valuation date of the results driving the report.
   * 
   * @return the valuation date
   */
  public abstract LocalDate getValuationDate();

  /**
   * Gets the instant at which the report was run, which is independent of the valuation date.
   * 
   * @return the run instant
   */
  public abstract Instant getRunInstant();

  /**
   * Gets the number of rows in the report table.
   * 
   * @return the number of rows in the report table
   */
  public abstract int getRowCount();

  /**
   * Gets the report column headers.
   * 
   * @return the column headers
   */
  public abstract ImmutableList<String> getColumnHeaders();

  /**
   * Writes this report out in a CSV format.
   * 
   * @param out  the output stream to write to
   */
  public abstract void writeCsv(OutputStream out);

  /**
   * Writes this report out as an ASCII table.
   * 
   * @param out  the output stream to write to
   */
  public abstract void writeAsciiTable(OutputStream out);

  //-------------------------------------------------------------------------
  /**
   * Gets the number of columns in the report table.
   * 
   * @return the number of columns in the report table
   */
  public default int getColumnCount() {
    return getColumnHeaders().size();
  }

  /**
   * Gets this report as an ASCII table string.
   * 
   * @return the ASCII table string
   */
  public default String toAsciiTableString() {
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    writeAsciiTable(os);
    return new String(os.toByteArray(), StandardCharsets.UTF_8);
  }

}
