/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.example;

import com.opengamma.financial.security.equity.EquitySecurity;
import com.opengamma.id.ExternalId;

/**
 * {@link EquityDescriptionFunction} that returns the value of one of the security's external IDs as the description.
 */
public class EquityIdDescription implements EquityDescriptionFunction {

  /** Returns the scheme that specifies which ID to use for the description. */
  private final IdSchemeFunction _idSchemeFunction;

  /**
   * @param idSchemeFunction Returns the scheme that specifies which ID to use for the description.
   */
  public EquityIdDescription(IdSchemeFunction idSchemeFunction) {
    _idSchemeFunction = idSchemeFunction;
  }

  /**
   * @param marketData Not used
   * @param security A security
   * @return The value of one of the security's external IDs
   */
  @Override
  public String getDescription(EquitySecurity security) {
    ExternalId externalId = security.getExternalIdBundle().getExternalId(_idSchemeFunction.getScheme());
    if (externalId != null) {
      return externalId.getValue();
    } else {
      return null;
    }
  }
}
