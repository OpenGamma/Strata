/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product;

import java.util.HashMap;
import java.util.Map;

import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.collect.ArgChecker;

/**
 * Builder to create {@code PositionInfo}.
 * <p>
 * This builder allows a {@link PositionInfo} to be created.
 */
public final class PositionInfoBuilder {

  /**
   * The primary identifier for the position.
   * <p>
   * The identifier is used to identify the position.
   */
  private StandardId id;
  /**
   * The position attributes.
   * <p>
   * Position attributes, provide the ability to associate arbitrary information
   * with a position in a key-value map.
   */
  private final Map<PositionAttributeType<?>, Object> attributes = new HashMap<>();

  // creates an empty instance
  PositionInfoBuilder() {
  }

  // creates a populated instance
  PositionInfoBuilder(
      StandardId id,
      Map<PositionAttributeType<?>, Object> attributes) {

    this.id = id;
    this.attributes.putAll(attributes);
  }

  //-----------------------------------------------------------------------
  /**
   * Sets the primary identifier for the position, optional.
   * <p>
   * The identifier is used to identify the position.
   * 
   * @param id  the identifier
   * @return this, for chaining
   */
  public PositionInfoBuilder id(StandardId id) {
    this.id = id;
    return this;
  }

  /**
   * Adds a position attribute to the map of attributes.
   * <p>
   * The attribute is added using {@code Map.put(type, value)} semantics.
   * 
   * @param <T> the type of the value
   * @param type  the type providing meaning to the value
   * @param value  the value
   * @return this, for chaining
   */
  @SuppressWarnings("unchecked")
  public <T> PositionInfoBuilder addAttribute(PositionAttributeType<T> type, T value) {
    ArgChecker.notNull(type, "type");
    ArgChecker.notNull(value, "value");
    // ImmutableMap.Builder would not provide Map.put semantics
    attributes.put(type, value);
    return this;
  }

  /**
   * Builds the position information.
   * 
   * @return the position information
   */
  public PositionInfo build() {
    return new PositionInfo(id, attributes);
  }

}
