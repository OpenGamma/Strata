/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.csv;

import com.opengamma.strata.basics.ReferenceData;

/**
 * Resolves security information from CSV files, enriching the parser.
 * <p>
 * Data loaded from a CSV may contain additional information that needs to be captured.
 * This plugin point allows the additional CSV columns to be parsed and captured.
 * 
 * @deprecated Use {@link TradeCsvInfoResolver} or {@link PositionCsvInfoResolver}
 */
@Deprecated
public interface CsvInfoResolver extends TradeCsvInfoResolver, PositionCsvInfoResolver {

  /**
   * Obtains an instance that uses the standard set of reference data.
   * 
   * @return the loader
   */
  public static CsvInfoResolver standard() {
    return StandardCsvInfoResolver.of(ReferenceData.standard());
  }

  /**
   * Obtains an instance that uses the specified set of reference data.
   * 
   * @param refData  the reference data
   * @return the loader
   */
  public static CsvInfoResolver of(ReferenceData refData) {
    return StandardCsvInfoResolver.of(refData);
  }

}
