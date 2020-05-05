/*
 * Copyright (C) 2020 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.csv;

import java.util.List;

import com.opengamma.strata.collect.io.CsvOutput.CsvRowOutputWithHeaders;
import com.opengamma.strata.product.Position;

/**
 * Writes the CSV for a single type of position.
 *
 * @param <T> the position type
 */
interface PositionTypeCsvWriter<T extends Position> {

  /**
   * Returns the list of headers needed for this type of position.
   *
   * @param positions the positions to output
   * @return the list of additional headers
   */
  public abstract List<String> headers(List<T> positions);

  /**
   * Writes the CSV for the specified trade.
   *
   * @param csv the CSV to write to
   * @param position the position to output
   */
  public abstract void writeCsv(CsvRowOutputWithHeaders csv, T position);

}
