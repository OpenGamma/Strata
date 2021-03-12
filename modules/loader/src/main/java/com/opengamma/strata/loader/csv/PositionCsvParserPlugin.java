/*
 * Copyright (C) 2021 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.csv;

import java.util.Optional;
import java.util.Set;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.io.CsvRow;
import com.opengamma.strata.collect.named.ExtendedEnum;
import com.opengamma.strata.collect.named.Named;
import com.opengamma.strata.product.Position;
import com.opengamma.strata.product.PositionInfo;
import com.opengamma.strata.product.Product;

/**
 * Pluggable CSV position parser.
 * <p>
 * Implementations of this interface parse a CSV file.
 * <p>
 * See {@link PositionCsvLoader} for the main entry point to parsing.
 */
public interface PositionCsvParserPlugin
    extends Named {

  /**
   * Obtains an instance from the specified unique name.
   * 
   * @param uniqueName  the unique name
   * @return the parser
   * @throws IllegalArgumentException if the name is not known
   */
  @FromString
  public static PositionCsvParserPlugin of(String uniqueName) {
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
  public static ExtendedEnum<PositionCsvParserPlugin> extendedEnum() {
    return PositionCsvLoader.ENUM_LOOKUP;
  }

  //-------------------------------------------------------------------------
  /**
   * Returns the upper-case product types that this plugin supports.
   * <p>
   * These are matched against the CSV file type column.
   * 
   * @return the types that this plugin supports
   */
  public abstract Set<String> positionTypeNames();

  /**
   * Parses a single CSV format position from the input.
   * <p>
   * This parses a single position from the CSV rows provided.
   * The position may exist on multiple rows
   * 
   * @param requiredJavaType  the Java type to return
   * @param row  the row to parse
   * @param info  the position info
   * @param resolver  the resolver
   * @return the position object, empty if choosing not to parse because the Java type does not match
   * @throws RuntimeException if unable to parse
   */
  public abstract Optional<Position> parsePosition(
      Class<?> requiredJavaType,
      CsvRow row,
      PositionInfo info,
      PositionCsvInfoResolver resolver);

  //-------------------------------------------------------------------------
  /**
   * Gets the name that uniquely identifies this parser.
   * <p>
   * The name should typically be the name of the {@link Product} that can be parsed.
   * <p>
   * This name is used in serialization and can be parsed using {@link #of(String)}.
   * 
   * @return the unique name
   */
  @ToString
  @Override
  public abstract String getName();

}
