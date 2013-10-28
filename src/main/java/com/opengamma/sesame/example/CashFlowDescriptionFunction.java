/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.example;

import com.opengamma.financial.security.cashflow.CashFlowSecurity;
import com.opengamma.sesame.MarketData;
import com.opengamma.sesame.function.DefaultImplementation;
import com.opengamma.sesame.function.OutputFunction;
import com.opengamma.sesame.function.OutputName;

/**
 * Trivial example function that returns the description of a cash flow security.
 */
@DefaultImplementation(CashFlowDescription.class)
@OutputName(OutputNames.DESCRIPTION)
public interface CashFlowDescriptionFunction extends OutputFunction<CashFlowSecurity, String> {

  /**
   * Returns a description of the security
   *
   *
   * @param marketData
   * @param security A security
   * @return A description of the security
   */
  @Override
  String execute(MarketData marketData, CashFlowSecurity security);
}
