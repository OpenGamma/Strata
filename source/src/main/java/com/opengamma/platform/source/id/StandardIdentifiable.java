/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.source.id;

/**
 * Provides uniform access to objects that can supply a standard identifier.
 * <p>
 * This interface makes no guarantees about the thread-safety of implementations.
 * However, wherever possible calls to this method should be thread-safe.
 */
public interface StandardIdentifiable {

  /**
   * Gets the standard identifier for the instance.
   *
   * @return the identifier
   */
  StandardId getStandardId();

}
