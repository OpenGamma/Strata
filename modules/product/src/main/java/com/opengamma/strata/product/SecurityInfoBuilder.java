/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product;

import java.util.HashMap;
import java.util.Map;

import com.opengamma.strata.collect.ArgChecker;

/**
 * Builder to create {@code SecurityInfo}.
 * <p>
 * This builder allows a {@link SecurityInfo} to be created.
 */
public final class SecurityInfoBuilder {

  /**
   * The security identifier.
   * <p>
   * This identifier uniquely identifies the security within the system.
   */
  private SecurityId id;
  /**
   * The information about the security price.
   * <p>
   * This provides information about the security price.
   * This can be used to convert the price into a monetary value.
   */
  private SecurityPriceInfo priceInfo;
  /**
   * The security attributes.
   * <p>
   * Security attributes, provide the ability to associate arbitrary information
   * with a security in a key-value map.
   */
  private final Map<SecurityAttributeType<?>, Object> attributes = new HashMap<>();

  // creates an empty instance
  SecurityInfoBuilder() {
  }

  //-----------------------------------------------------------------------
  /**
   * Sets the security identifier.
   * <p>
   * This identifier uniquely identifies the security within the system.
   * 
   * @param id  the identifier
   * @return this, for chaining
   */
  public SecurityInfoBuilder id(SecurityId id) {
    this.id = ArgChecker.notNull(id, "id");
    return this;
  }

  /**
   * Sets the information about the security price.
   * <p>
   * This provides information about the security price.
   * This can be used to convert the price into a monetary value.
   * 
   * @param priceInfo  the price info
   * @return this, for chaining
   */
  public SecurityInfoBuilder priceInfo(SecurityPriceInfo priceInfo) {
    this.priceInfo = ArgChecker.notNull(priceInfo, "priceInfo");
    return this;
  }

  /**
   * Adds a security attribute to the map of attributes.
   * <p>
   * The attribute is added using {@code Map.put(type, value)} semantics.
   * 
   * @param <T> the type of the value
   * @param type  the type providing meaning to the value
   * @param value  the value
   * @return this, for chaining
   */
  @SuppressWarnings("unchecked")
  public <T> SecurityInfoBuilder addAttribute(SecurityAttributeType<T> type, T value) {
    ArgChecker.notNull(type, "type");
    ArgChecker.notNull(value, "value");
    // ImmutableMap.Builder would not provide Map.put semantics
    attributes.put(type, value);
    return this;
  }

  /**
   * Builds the security information.
   * 
   * @return the security information
   */
  public SecurityInfo build() {
    return new SecurityInfo(id, priceInfo, attributes);
  }

}
