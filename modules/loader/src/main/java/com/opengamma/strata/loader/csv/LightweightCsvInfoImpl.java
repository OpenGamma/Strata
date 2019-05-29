/*
 * Copyright (C) 2019 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.csv;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.collect.ArgChecker;

/**
 * Lightweight CSV information resolver.
 */
final class LightweightCsvInfoImpl implements LightweightPositionCsvInfoResolver {

  /**
   * Standard instance.
   */
  static final LightweightCsvInfoImpl INSTANCE = new LightweightCsvInfoImpl(ReferenceData.standard());

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
  static LightweightCsvInfoImpl of(ReferenceData refData) {
    return new LightweightCsvInfoImpl(refData);
  }

  // restricted constructor
  private LightweightCsvInfoImpl(ReferenceData refData) {
    this.refData = ArgChecker.notNull(refData, "refData");
  }

  //-------------------------------------------------------------------------
  @Override
  public ReferenceData getReferenceData() {
    return refData;
  }

}
