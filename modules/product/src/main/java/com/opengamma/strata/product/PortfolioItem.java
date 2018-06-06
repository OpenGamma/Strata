/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product;

import java.util.Optional;

import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.basics.StandardId;

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
   * Summarizes the portfolio item.
   * <p>
   * This provides a summary, including a human readable description.
   * 
   * @return the summary of the item
   */
  public abstract PortfolioItemSummary summarize();

}
