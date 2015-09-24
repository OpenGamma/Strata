/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.finance;

import java.util.Optional;

import com.google.common.collect.ImmutableMap;

/**
 * Provides access to an set of additional attributes.
 * <p>
 * A domain model consists primarily of a set of known typed properties.
 * To provide for extensibility, some key domain objects provide the ability
 * to add a set of attributes, which consist of a string to string map.
 * <p>
 * Implementations of this interface may be mutable but the map of attributes returned is immutable.
 */
public interface Attributable {

  /**
   * Gets the entire set of additional attributes.
   * <p>
   * Attributes are typically used to tag the object with additional information.
   * 
   * @return the complete set of attributes
   */
  public abstract ImmutableMap<String, String> getAttributes();

  /**
   * Finds the value of an attribute by key.
   * <p>
   * Attributes are typically used to tag the object with additional information.
   * If the attribute is not found, optional empty is returned.
   * 
   * @param key  the key to find
   * @return the the value of the specified attribute, empty if not found
   */
  public default Optional<String> findAttribute(String key) {
    return Optional.ofNullable(getAttributes().get(key));
  }

}
