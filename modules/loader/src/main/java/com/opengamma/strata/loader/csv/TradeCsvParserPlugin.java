/*
 * Copyright (C) 2021 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.csv;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.io.CsvRow;
import com.opengamma.strata.collect.named.ExtendedEnum;
import com.opengamma.strata.collect.named.Named;
import com.opengamma.strata.product.Product;
import com.opengamma.strata.product.Trade;
import com.opengamma.strata.product.TradeInfo;

/**
 * Pluggable CSV trade parser.
 * <p>
 * Implementations of this interface parse a CSV file.
 * <p>
 * See {@link TradeCsvLoader} for the main entry point to parsing.
 */
public interface TradeCsvParserPlugin
    extends Named {

  /**
   * Obtains an instance from the specified unique name.
   * 
   * @param uniqueName  the unique name
   * @return the parser
   * @throws IllegalArgumentException if the name is not known
   */
  @FromString
  public static TradeCsvParserPlugin of(String uniqueName) {
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
  public static ExtendedEnum<TradeCsvParserPlugin> extendedEnum() {
    return TradeCsvLoader.ENUM_LOOKUP;
  }

  //-------------------------------------------------------------------------
  /**
   * Returns the upper-case product type names that this plugin supports.
   * <p>
   * These are matched against the CSV file type column.
   * 
   * @return the types that this plugin supports
   */
  public abstract Set<String> tradeTypeNames();

  /**
   * Checks if there is an additional row that must be parsed alongside the base row.
   * <p>
   * An example use for this is parsing variable notional swaps.
   * 
   * @param baseRow  the base row
   * @param additionalRow  the additional row
   * @return true if the base row and additional row must be parsed together
   */
  public default boolean isAdditionalRow(CsvRow baseRow, CsvRow additionalRow) {
    return false;
  }

  /**
   * Parses a single CSV format trade from the input.
   * <p>
   * This parses a single trade from the CSV rows provided.
   * The trade may exist on multiple rows
   * 
   * @param requiredJavaType  the Java type to return
   * @param baseRow  the base row to parse
   * @param additionalRows  the additional rows to parse, may be empty
   * @param info  the trade info
   * @param resolver  the resolver
   * @return the trade object, empty if choosing not to parse because the Java type does not match
   * @throws RuntimeException if unable to parse
   */
  public abstract Optional<Trade> parseTrade(
      Class<?> requiredJavaType,
      CsvRow baseRow,
      List<CsvRow> additionalRows,
      TradeInfo info,
      TradeCsvInfoResolver resolver);

  //-------------------------------------------------------------------------
  /**
   * Gets the name that uniquely identifies this parser.
   * <p>
   * The name should typically be the name of the {@link Product} that can be parsed.
   * For example, 'Fra', 'Swap' or 'FxSingle'.
   * <p>
   * This name is used in serialization and can be parsed using {@link #of(String)}.
   * 
   * @return the unique name
   */
  @ToString
  @Override
  public abstract String getName();

}
