/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.id;

/**
 * Provides uniform access to objects that can supply a bundle of external identifiers.
 * <p>
 * This interface makes no guarantees about the thread-safety of implementations.
 * However, wherever possible calls to this method should be thread-safe.
 */
public interface ExternalBundleIdentifiable {

  /**
   * Gets the external identifier bundle that defines the object.
   * <p>
   * Each external system has one or more identifiers by which they refer to the object.
   * Some of these may be unique within that system, while others may be more descriptive.
   * This bundle stores the set of these external identifiers.
   * 
   * @return the bundle defining the object
   */
  ExternalIdBundle getExternalIdBundle();

}
