/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.example;

import com.opengamma.financial.security.cashflow.CashFlowSecurity;
import com.opengamma.sesame.MarketData;

/**
 * Returns the security name as the description.
 */
public class CashFlowDescription implements CashFlowDescriptionFunction {

  /**
   *
   *
   * @param marketData
   * @param security A security
   * @return The security name
   */
  @Override
  public String execute(MarketData marketData, CashFlowSecurity security) {
    return security.getName();
  }
}
