/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.example;

import com.opengamma.financial.security.equity.EquitySecurity;
import com.opengamma.sesame.function.DefaultImplementation;
import com.opengamma.sesame.function.OutputFunction;
import com.opengamma.sesame.function.OutputName;

/**
 * Trivial example function that returns the description of an equity security.
 */
@DefaultImplementation(EquityDescription.class)
@OutputName(OutputNames.DESCRIPTION)
public interface EquityDescriptionFunction extends OutputFunction<EquitySecurity, String> {

  /**
   * Returns a description of the security
   *
   *
   *
   * @param security A security
   * @return A description of the security
   */
  @Override
  String execute(EquitySecurity security);
}
