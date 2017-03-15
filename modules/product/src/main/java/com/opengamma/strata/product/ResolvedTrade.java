/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product;

/**
 * A trade that has been resolved for pricing.
 * <p>
 * Resolved trades are the primary input to pricers.
 * <p>
 * Resolved objects may be bound to data that changes over time, such as holiday calendars.
 * If the data changes, such as the addition of a new holiday, the resolved form will not be updated.
 * Care must be taken when placing the resolved form in a cache or persistence layer.
 */
public interface ResolvedTrade {

  /**
   * Gets the standard trade information.
   * <p>
   * All trades contain this standard set of information.
   * 
   * @return the trade information
   */
  public abstract TradeInfo getInfo();

  /**
   * Gets the underlying product that was agreed when the trade occurred.
   * <p>
   * The product captures the contracted financial details of the trade.
   * 
   * @return the product
   */
  public abstract ResolvedProduct getProduct();

}
