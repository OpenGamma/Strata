/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product;

import com.opengamma.strata.basics.StandardId;

/**
 * Interface across the various info builder classes.
 * 
 * @param <T> the info class type
 */
public interface PortfolioItemInfoBuilder<T extends PortfolioItemInfo> {

  /**
   * Sets the primary identifier for the position, optional.
   * <p>
   * The identifier is used to identify the position.
   * 
   * @param id  the identifier
   * @return this, for chaining
   */
  public abstract PortfolioItemInfoBuilder<T> id(StandardId id);

  /**
   * Adds a position attribute to the map of attributes.
   * <p>
   * The attribute is added using {@code Map.put(type, value)} semantics.
   * 
   * @param <V> the type of the value
   * @param attributeType  the type providing meaning to the value
   * @param attributeValue  the value
   * @return this, for chaining
   */
  @SuppressWarnings("unchecked")
  public abstract <V> PortfolioItemInfoBuilder<T> addAttribute(AttributeType<V> attributeType, V attributeValue);

  /**
   * Builds the position information.
   * 
   * @return the position information
   */
  public abstract T build();

}
