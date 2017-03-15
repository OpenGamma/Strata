/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.index;

/**
 * A single observation of an index.
 * <p>
 * Implementations of this interface represent observations of an index.
 * For example, an observation of 'GBP-LIBOR-3M' at a specific fixing date.
 */
public interface IndexObservation {

  /**
   * Gets the index to be observed.
   * 
   * @return the index
   */
  public abstract Index getIndex();

}
