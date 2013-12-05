/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.example;

import com.opengamma.financial.security.cashflow.CashFlowSecurity;

/**
 * {@link CashFlowDescriptionFn} that returns the value of one of the security's external IDs as the description.
 */
public class CashFlowIdDescriptionFn implements CashFlowDescriptionFn {

  /** Returns the scheme that specifies which ID to use for the description. */
  private final IdSchemeFn _idSchemeFn;

  /**
   * @param idSchemeFn Returns the scheme that specifies which ID to use for the description.
   */
  public CashFlowIdDescriptionFn(IdSchemeFn idSchemeFn) {
    _idSchemeFn = idSchemeFn;
  }

  /**
   *
   *
   *
   * @param security A security
   * @return The value of one of the security's external IDs
   */
  @Override
  public String getDescription(CashFlowSecurity security) {
    return security.getExternalIdBundle().getExternalId(_idSchemeFn.getScheme()).getValue();
  }
}
