/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.source.id;

import java.util.Set;

import com.google.common.collect.ImmutableSet;

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
  public abstract StandardId getStandardId();

  /**
   * Gets secondary identifiers for the instance.
   * <p>
   * The default implementation will return an immutable empty set.
   * This can be overridden to provide a set of ids. Note that the
   * set should not contain the primary id.
   *
   * @return the set of secondary ids
   */
  public default Set<StandardId> getSecondaryIds() {
    return ImmutableSet.of();
  }

}
