/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure;

import com.opengamma.strata.calc.CalculationRunner;
import com.opengamma.strata.calc.runner.CalculationFunctions;
import com.opengamma.strata.measure.corporateaction.GenericCorporateActionPositionCalculationFunction;

/**
 * Factory methods for creating standard Strata components.
 * <p>
 * These components are suitable for performing calculations using the built-in asset classes,
 * market data types and pricers.
 * <p>
 * The market data factory can create market data values derived from other values.
 * For example it can create calibrated curves given market quotes.
 * However it cannot request market data from an external provider, such as Bloomberg,
 * or look up data from a data store, for example a time series database.
 * Instances of {@link CalculationRunner} are created directly using the static methods on the interface.
 */
public final class StandardCorporateActionComponents {

  /**
   * The standard calculation functions.
   */
  private static final CalculationFunctions STANDARD = CalculationFunctions.of(

      new GenericCorporateActionPositionCalculationFunction());


  /**
   * Restricted constructor.
   */
  private StandardCorporateActionComponents() {
  }

  public static CalculationFunctions calculationFunctions() {
    return STANDARD;
  }

}
