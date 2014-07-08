/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.id;

import javax.annotation.Nullable;

/**
 * Provides uniform access to objects that can supply an external identifier.
 * <p>
 * This interface makes no guarantees about the thread-safety of implementations.
 * However, wherever possible calls to this method should be thread-safe.
 */
public interface ExternalIdentifiable {

  /**
   * Gets the external identifier for the instance.
   * <p>
   * This method may return null if the object does not yet have an identifier assigned.
   * 
   * @return the external identifier, may be null
   */
  @Nullable
  ExternalId getExternalId();

}
