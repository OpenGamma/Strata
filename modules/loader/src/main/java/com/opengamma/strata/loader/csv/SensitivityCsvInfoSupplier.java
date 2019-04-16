/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.csv;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.market.param.CurrencyParameterSensitivity;
import com.opengamma.strata.market.sensitivity.CurveSensitivities;

/**
 * Resolves additional information when writing sensitivity CSV files.
 * <p>
 * This allows additional CSV columns to be written.
 */
public interface SensitivityCsvInfoSupplier {

  /**
   * Obtains an instance that uses the standard set of reference data.
   * 
   * @return the loader
   */
  public static SensitivityCsvInfoSupplier standard() {
    return StandardCsvInfoImpl.INSTANCE;
  }

  //-------------------------------------------------------------------------
  /**
   * Returns the list of additional headers this supplier provides.
   * 
   * @param curveSens  the curve sensitivities to output
   * @return the list of additional headers
   */
  public default List<String> headers(CurveSensitivities curveSens) {
    return ImmutableList.of();
  }

  /**
   * Gets the values associated with the headers.
   * <p>
   * This must return a list of the same size as {@code additionalHeaders}
   * where each element in the list is the value for the matching header.
   * <p>
   * This will be invoked once for each {@link CurrencyParameterSensitivity} in the {@link CurveSensitivities}.
   * 
   * @param additionalHeaders  the additional headers
   * @param curveSens  the curve sensitivities to output
   * @param paramSens  the parameter sensitivities to output
   * @return the value for the specified header, not null
   */
  public default List<String> values(
      List<String> additionalHeaders,
      CurveSensitivities curveSens,
      CurrencyParameterSensitivity paramSens) {

    return ImmutableList.of();
  }

}
