/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product;

import java.util.Optional;

import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.collect.Messages;

/**
 * An item in a portfolio.
 * <p>
 * This represents a single item in a portfolio.
 * Typically a portfolio will consist of {@linkplain Trade trades} and {@linkplain Position positions}.
 * <p>
 * Implementations of this interface must be immutable beans.
 */
public interface PortfolioItem extends CalculationTarget {

  /**
   * Gets the additional information about the portfolio item.
   * 
   * @return the additional information
   */
  public abstract PortfolioItemInfo getInfo();

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
  public default Optional<StandardId> getId() {
    return getInfo().getId();
  }

  /**
   * Returns an instance with the specified info.
   *
   * @param info  the new info
   * @return the instance with the specified info
   */
  public default PortfolioItem withInfo(PortfolioItemInfo info) {
    throw new UnsupportedOperationException();
  }

  /**
   * Summarizes the portfolio item.
   * <p>
   * This provides a summary, including a human readable description.
   * 
   * @return the summary of the item
   */
  public abstract PortfolioItemSummary summarize();

  /**
   * Gets the attribute associated with the specified type.
   * <p>
   * This method obtains the specified attribute.
   * This allows an attribute to be obtained if available.
   * <p>
   * If the attribute is not found, an exception is thrown.
   *
   * @param <T>  the type of the attribute value
   * @param type  the type to find
   * @return the attribute value
   * @throws IllegalArgumentException if the attribute is not found
   */
  public default <T> T getAttribute(AttributeType<T> type) {
    return getInfo().findAttribute(type).orElseThrow(() -> new IllegalArgumentException(
        Messages.format("Attribute not found for type '{}', on {}", type, summarize())));
  }

}
