/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.example;

import com.opengamma.financial.security.equity.EquitySecurity;
import com.opengamma.id.ExternalId;

/**
 * {@link EquityDescriptionFn} that returns the value of one of the security's external IDs as the description.
 */
public class EquityIdDescriptionFn implements EquityDescriptionFn {

  /** Returns the scheme that specifies which ID to use for the description. */
  private final IdSchemeFn _idSchemeFn;

  /**
   * @param idSchemeFn Returns the scheme that specifies which ID to use for the description.
   */
  public EquityIdDescriptionFn(IdSchemeFn idSchemeFn) {
    _idSchemeFn = idSchemeFn;
  }

  /**
   * @param security A security
   * @return The value of one of the security's external IDs
   */
  @Override
  public String getDescription(EquitySecurity security) {
    ExternalId externalId = security.getExternalIdBundle().getExternalId(_idSchemeFn.getScheme());
    if (externalId != null) {
      return externalId.getValue();
    } else {
      return null;
    }
  }
}
