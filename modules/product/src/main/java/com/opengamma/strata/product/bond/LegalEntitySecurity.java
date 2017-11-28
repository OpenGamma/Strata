/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.bond;

import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.product.Security;

/**
 * An instrument representing a security associated with a legal entity. 
 * <p>
 * Examples include fixed coupon bonds and capital index bonds. 
 */
public interface LegalEntitySecurity extends Security {

  /**
   * Get the legal entity identifier.
   * <p>
   * The identifier is used for the legal entity that issues the security.
   * 
   * @return the legal entity identifier
   */
  public abstract StandardId getLegalEntityId();

}
