/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.example;

import com.opengamma.financial.security.cashflow.CashFlowSecurity;

/**
 * {@link CashFlowDescriptionFunction} that returns the value of one of the security's external IDs as the description.
 */
public class CashFlowIdDescription implements CashFlowDescriptionFunction {

  /** Returns the scheme that specifies which ID to use for the description. */
  private final IdSchemeFunction _idSchemeFunction;

  /**
   * @param idSchemeFunction Returns the scheme that specifies which ID to use for the description.
   */
  public CashFlowIdDescription(IdSchemeFunction idSchemeFunction) {
    _idSchemeFunction = idSchemeFunction;
  }

  /**
   *
   *
   *
   * @param security A security
   * @return The value of one of the security's external IDs
   */
  @Override
  public String execute(CashFlowSecurity security) {
    return security.getExternalIdBundle().getExternalId(_idSchemeFunction.getScheme()).getValue();
  }
}
