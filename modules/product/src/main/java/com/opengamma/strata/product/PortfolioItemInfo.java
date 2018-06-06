/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product;

import java.util.Optional;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.StandardId;

/**
 * Additional information about a portfolio item.
 * <p>
 * This allows additional information to be associated with an item.
 * It is kept in a separate object as the information is generally optional for pricing.
 * <p>
 * Implementations of this interface must be immutable beans.
 */
public interface PortfolioItemInfo extends Attributes {

  /**
   * Gets the primary identifier for the portfolio item, optional.
   * <p>
   * The identifier is used to identify the portfolio item.
   * It will typically be an identifier in an external data system.
   * <p>
   * A portfolio item may have multiple active identifiers. Any identifier may be chosen here.
   * Certain uses of the identifier, such as storage in a database, require that the
   * identifier does not change over time, and this should be considered best practice.
   * 
   * @return the identifier, optional
   */
  public abstract Optional<StandardId> getId();

  /**
   * Gets the attribute types that the info contains.
   * 
   * @return the attribute types
   */
  public abstract ImmutableSet<AttributeType<?>> getAttributeTypes();

  @Override
  public abstract <T> PortfolioItemInfo withAttribute(AttributeType<T> type, T value);

}
