/*
 * Copyright (C) 2019 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.csv;

import java.util.List;

import com.opengamma.strata.collect.io.CsvOutput.CsvRowOutputWithHeaders;
import com.opengamma.strata.product.Trade;

/**
 * Writes the CSV for a single type of trade.
 * 
 * @param <T>  the trade type
 */
interface TradeTypeCsvWriter<T extends Trade> {

  /**
   * Returns the list of headers needed for this type of trade.
   * 
   * @param trades  the trades to output
   * @return the list of additional headers
   */
  public abstract List<String> headers(List<T> trades);

  /**
   * Writes the CSV for the specified trade.
   * 
   * @param csv  the CSV to write to
   * @param trade  the trade to output
   */
  public abstract void writeCsv(CsvRowOutputWithHeaders csv, T trade);

}
