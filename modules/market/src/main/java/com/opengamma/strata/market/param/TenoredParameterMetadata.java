/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.param;

import com.opengamma.strata.basics.date.Tenor;

/**
 * Parameter metadata that specifies a tenor.
 */
public interface TenoredParameterMetadata extends ParameterMetadata {

  /**
   * Gets the tenor associated with the parameter.
   * <p>
   * This is the tenor of the parameter.
   * 
   * @return the tenor
   */
  public abstract Tenor getTenor();

  /**
   * Returns an instance with the tenor updated.
   * 
   * @param tenor  the tenor to update to
   * @return the updated metadata
   */
  public abstract TenoredParameterMetadata withTenor(Tenor tenor);

}
