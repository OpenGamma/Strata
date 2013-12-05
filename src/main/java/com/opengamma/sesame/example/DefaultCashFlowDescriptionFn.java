/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.example;

import com.opengamma.financial.security.cashflow.CashFlowSecurity;

/**
 * Returns the security name as the description.
 */
public class DefaultCashFlowDescriptionFn implements CashFlowDescriptionFn {

  /**
   * Returns a description of the security.
   * @param security A security
   * @return The security name
   */
  @Override
  public String getDescription(CashFlowSecurity security) {
    return security.getName();
  }
}
