/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.example;

import com.opengamma.financial.security.cashflow.CashFlowSecurity;
import com.opengamma.sesame.OutputNames;
import com.opengamma.sesame.function.Output;

/**
 * Trivial example function that returns the description of a cash flow security.
 */
public interface CashFlowDescriptionFn {

  /**
   * Returns a description of the security
   *
   *
   *
   * @param security A security
   * @return A description of the security
   */
  @Output(OutputNames.DESCRIPTION)
  String getDescription(CashFlowSecurity security);
}
