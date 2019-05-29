/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.csv;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.collect.io.CsvRow;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.param.TenoredParameterMetadata;
import com.opengamma.strata.product.PortfolioItemInfo;

/**
 * Resolves additional information when parsing sensitivity CSV files.
 * <p>
 * Data loaded from a CSV may contain additional information that needs to be captured.
 * This plugin point allows the additional CSV columns to be parsed and captured.
 */
public interface SensitivityCsvInfoResolver {

  /**
   * Obtains an instance that uses the standard set of reference data.
   * 
   * @return the resolver
   */
  public static SensitivityCsvInfoResolver standard() {
    return StandardCsvInfoImpl.INSTANCE;
  }

  /**
   * Obtains an instance that uses the specified set of reference data.
   * 
   * @param refData  the reference data
   * @return the resolver
   */
  public static SensitivityCsvInfoResolver of(ReferenceData refData) {
    return StandardCsvInfoImpl.of(refData);
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the reference data being used.
   * 
   * @return the reference data
   */
  public abstract ReferenceData getReferenceData();

  /**
   * Checks if the column header is an info column that this resolver will parse.
   * 
   * @param headerLowerCase  the header case, in lower case (ENGLISH) form
   * @return true if the header is for an info column
   */
  public default boolean isInfoColumn(String headerLowerCase) {
    return false;
  }

  /**
   * Parses attributes to update {@code PortfolioItemInfo}.
   * <p>
   * If it is available, the sensitivity ID will have been set before this method is called.
   * It may be altered if necessary, although this is not recommended.
   * 
   * @param row  the CSV row to parse
   * @param info  the info to update and return
   * @return the updated info
   */
  public default PortfolioItemInfo parseSensitivityInfo(CsvRow row, PortfolioItemInfo info) {
    // do nothing
    return info;
  }

  /**
   * Checks whether a tenor is required.
   * <p>
   * This defaults to true, ensuring that the metadata will implement {@link TenoredParameterMetadata}.
   * Override to set it to false.
   * 
   * @return true if the tenor is required, false to allow date-based sensitvities
   */
  public default boolean isTenorRequired() {
    return true;
  }

  /**
   * Checks the parsed sensitivity tenor, potentially altering the value.
   * <p>
   * The input is the tenor exactly as parsed.
   * The default implementation normalizes it and ensures that it does not contain a combination
   * of years/months and days.
   * 
   * @param tenor  the tenor to check and potentially alter
   * @return the potentially adjusted tenor
   */
  public default Tenor checkSensitivityTenor(Tenor tenor) {
    Tenor resultTenor = tenor.normalized();
    if (resultTenor.getPeriod().toTotalMonths() > 0 && resultTenor.getPeriod().getDays() > 0) {
      throw new IllegalArgumentException(
          "Invalid tenor, cannot mix years/months and days: " + tenor);
    }
    return resultTenor;
  }

  /**
   * Checks the parsed curve name, potentially altering the value.
   * <p>
   * The input is the curve name exactly as parsed.
   * The default implementation simply returns it.
   * 
   * @param curveName  the curve name to check and potentially alter
   * @return the potentially adjusted curve name
   */
  public default CurveName checkCurveName(CurveName curveName) {
    return curveName;
  }

}
