/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product;

/**
 * A trade that is directly based on a product.
 * <p>
 * A product trade is a {@link Trade} that directly contains a reference to a {@link Product}.
 * <p>
 * Implementations of this interface must be immutable beans.
 */
public interface ProductTrade
    extends Trade {

  /**
   * Gets the underlying product that was agreed when the trade occurred.
   * <p>
   * The product captures the contracted financial details of the trade.
   * 
   * @return the product
   */
  public abstract Product getProduct();

}
