/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.csv;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.collect.ArgChecker;

/**
 * Standard CSV information resolver.
 */
final class StandardCsvInfoResolver implements TradeCsvInfoResolver, PositionCsvInfoResolver {

  /**
   * The reference data.
   */
  private final ReferenceData refData;

  /**
   * Obtains an instance that uses the specified set of reference data.
   * 
   * @param refData  the reference data
   * @return the loader
   */
  public static StandardCsvInfoResolver of(ReferenceData refData) {
    return new StandardCsvInfoResolver(refData);
  }

  // restricted constructor
  private StandardCsvInfoResolver(ReferenceData refData) {
    this.refData = ArgChecker.notNull(refData, "refData");
  }

  //-------------------------------------------------------------------------
  @Override
  public ReferenceData getReferenceData() {
    return refData;
  }

}
