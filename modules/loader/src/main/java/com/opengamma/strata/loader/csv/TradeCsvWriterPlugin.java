/*
 * Copyright (C) 2019 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.csv;

import java.util.List;
import java.util.Set;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.io.CsvOutput.CsvRowOutputWithHeaders;
import com.opengamma.strata.collect.named.ExtendedEnum;
import com.opengamma.strata.collect.named.Named;
import com.opengamma.strata.product.Product;
import com.opengamma.strata.product.Trade;

/**
 * Pluggable CSV trade writer.
 * <p>
 * Implementations of this interface write trades to a CSV file.
 * <p>
 * See {@link TradeCsvWriter} for the main entry point to writing.
 * <p>
 * @param <T> the trade type
 */
public interface TradeCsvWriterPlugin<T extends Trade> extends Named {

  /**
   * Obtains an instance from the specified unique name.
   *
   * @param uniqueName the unique name
   * @return the writer
   * @throws IllegalArgumentException if the name is not known
   */
  @FromString
  public static TradeCsvWriterPlugin of(String uniqueName) {
    ArgChecker.notNull(uniqueName, "uniqueName");
    return extendedEnum().lookup(uniqueName);
  }

  /**
   * Gets the extended enum helper.
   * <p>
   * This helper allows instances of the parser to be looked up.
   * It also provides the complete set of available instances.
   *
   * @return the extended enum helper
   */
  public static ExtendedEnum<TradeCsvWriterPlugin> extendedEnum() {
    return TradeCsvWriter.ENUM_LOOKUP;
  }

  /**
   * Returns the set of headers needed for this type of trade.
   * <p>
   * The headers are ordered and all distinct.
   *
   * @param trades  the trades to output
   * @return the set of additional headers
   */
  public abstract Set<String> headers(List<T> trades);

  /**
   * Writes the CSV for the specified trade.
   *
   * @param csv  the CSV to write to
   * @param trade  the trade to output
   */
  public abstract void writeCsv(CsvRowOutputWithHeaders csv, T trade);

  /**
   * Gets the name that uniquely identifies this parser.
   * <p>
   * The name should typically be the name of the {@link Product} that can be parsed.
   * For example, 'Fra', 'Swap' or 'FxSingle'.
   * <p>
   * This name is used in serialization and an be parsed using {@link #of(String)}.
   *
   * @return the unique name
   */
  @ToString
  @Override
  public abstract String getName();

  /**
   * Provides the supported trade types for this plugin.
   * <p>
   * The set typically contains the names of the {@link Trade} that can be written.
   * For example, 'FraTrade', 'SwapTrade'.
   *
   * @return the set of supported trade types.
   */
  public abstract Set<Class<?>> supportedTradeTypes();

}
