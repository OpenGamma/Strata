/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.id;

/**
 * Provides uniform access to objects that can supply an object identifier.
 * <p>
 * This interface makes no guarantees about the thread-safety of implementations.
 * However, wherever possible calls to this method should be thread-safe.
 */
public interface ObjectIdentifiable {

  /**
   * Gets the object identifier for this item.
   * 
   * @return the object identifier, may be null
   */
  ObjectId getObjectId();

}
