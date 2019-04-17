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
final class StandardCsvInfoImpl
    implements
    TradeCsvInfoResolver,
    TradeCsvInfoSupplier,
    PositionCsvInfoResolver,
    SensitivityCsvInfoResolver,
    SensitivityCsvInfoSupplier {

  /**
   * Standard instance.
   */
  static final StandardCsvInfoImpl INSTANCE = new StandardCsvInfoImpl(ReferenceData.standard());

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
  public static StandardCsvInfoImpl of(ReferenceData refData) {
    return new StandardCsvInfoImpl(refData);
  }

  // restricted constructor
  private StandardCsvInfoImpl(ReferenceData refData) {
    this.refData = ArgChecker.notNull(refData, "refData");
  }

  //-------------------------------------------------------------------------
  @Override
  public ReferenceData getReferenceData() {
    return refData;
  }

}
