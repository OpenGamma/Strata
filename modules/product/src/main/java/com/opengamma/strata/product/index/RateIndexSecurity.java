/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.index;

import com.opengamma.strata.basics.index.RateIndex;
import com.opengamma.strata.product.Security;
import com.opengamma.strata.product.SecurityInfo;

/**
 * An instrument representing a security associated with a rate index.
 * <p>
 * Examples include Ibor or Overnight rate futures.
 */
public interface RateIndexSecurity extends Security {

  /**
   * Get the rate index.
   * <p>
   * The index of the rate to be observed.
   * 
   * @return the rate index
   */
  public abstract RateIndex getIndex();

  //-------------------------------------------------------------------------
  @Override
  public abstract RateIndexSecurity withInfo(SecurityInfo info);

}
