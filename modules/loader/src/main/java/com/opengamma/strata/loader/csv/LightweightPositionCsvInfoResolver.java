/*
 * Copyright (C) 2019 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.csv;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.collect.io.CsvRow;
import com.opengamma.strata.product.Position;
import com.opengamma.strata.product.PositionInfo;
import com.opengamma.strata.product.SecurityPosition;
import com.opengamma.strata.product.etd.EtdIdUtils;

/**
 * Resolves additional information when parsing position CSV files.
 * <p>
 * Data loaded from a CSV may contain additional information that needs to be captured.
 * This plugin point allows the additional CSV columns to be parsed and captured.
 * It also allows the ETD contract specification to be loaded.
 * <p>
 * This extension to {@link PositionCsvInfoResolver} parses positions without the need for reference data.
 * When an ETD row is found, {@link EtdIdUtils} is used to create an identifier.
 * This resolver must always return an instance of {@link SecurityPosition} from the parsing methods.
 */
public interface LightweightPositionCsvInfoResolver extends PositionCsvInfoResolver {

  /**
   * Obtains an instance that uses the standard set of reference data.
   * 
   * @return the resolver
   */
  public static LightweightPositionCsvInfoResolver standard() {
    return LightweightCsvInfoImpl.INSTANCE;
  }

  /**
   * Obtains an instance that uses the specified set of reference data.
   * 
   * @param refData  the reference data
   * @return the resolver
   */
  public static LightweightPositionCsvInfoResolver of(ReferenceData refData) {
    return LightweightCsvInfoImpl.of(refData);
  }

  //-------------------------------------------------------------------------
  @Override
  public default Position parseNonEtdPosition(CsvRow row, PositionInfo info) {
    return PositionCsvInfoResolver.super.parseNonEtdSecurityPosition(row, info);
  }

  @Override
  public default Position parseEtdFuturePosition(CsvRow row, PositionInfo info) {
    return PositionCsvInfoResolver.super.parseEtdFutureSecurityPosition(row, info);
  }

  @Override
  public default Position parseEtdOptionPosition(CsvRow row, PositionInfo info) {
    return PositionCsvInfoResolver.super.parseEtdOptionSecurityPosition(row, info);
  }

}
