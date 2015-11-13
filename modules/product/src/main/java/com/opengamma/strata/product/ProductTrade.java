/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product;

import com.opengamma.strata.basics.Trade;

/**
 * A trade that is directly based on a product.
 * <p>
 * A product trade is a {@link Trade} that directly contains a reference to a {@link Product}.
 * This design is typically used for Over-The-Counter (OTC) trades, where the instrument is not fungible.
 * <p>
 * Implementations of this interface must be immutable beans.
 * 
 * @param <P>  the type of the product
 */
public interface ProductTrade<P extends Product>
    extends FinanceTrade {

  /**
   * Gets the underlying product that was agreed when the trade occurred.
   * <p>
   * The product captures the contracted financial details of the trade.
   * 
   * @return the product
   */
  public abstract P getProduct();

}
