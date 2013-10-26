/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.example;

import com.opengamma.financial.security.equity.EquitySecurity;
import com.opengamma.sesame.function.DefaultImplementation;
import com.opengamma.sesame.function.OutputName;
import com.opengamma.sesame.function.Target;

/**
 * Trivial example function that returns the description of an equity security.
 * TODO convert to using PortfolioOutputFunction
 */
@DefaultImplementation(EquityDescription.class)
@OutputName(EquityDescriptionFunction.VALUE_NAME)
public interface EquityDescriptionFunction {

  public static final String VALUE_NAME = "EquityDescription";

  /**
   * Returns a description of the security
   * @param security A security
   * @return A description of the security
   */
  String getDescription(@Target EquitySecurity security);
}
