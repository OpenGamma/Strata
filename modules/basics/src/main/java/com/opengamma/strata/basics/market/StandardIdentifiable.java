/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.market;

/**
 * Provides uniform access to objects that can supply a standard identifier.
 * <p>
 * Domain objects that can be identified using a {@link StandardId} should implement this interface.
 * <p>
 * This interface makes no guarantees about the thread-safety of implementations.
 * However, wherever possible implementations should be immutable.
 */
public interface StandardIdentifiable {

  /**
   * Gets the standard identifier for the instance.
   *
   * @return the identifier
   */
  public abstract StandardId getStandardId();

}
