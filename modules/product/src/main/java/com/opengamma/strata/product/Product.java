/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.currency.Currency;

/**
 * The product details of a financial instrument.
 * <p>
 * A product is a high level abstraction applicable to many different types.
 * For example, an Interest Rate Swap is a product, as is a Forward Rate Agreement (FRA).
 * <p>
 * A product exists independently from a {@link Trade}. It represents the economics of the
 * financial instrument regardless of the trade date or counterparties.
 * <p>
 * Implementations must be immutable and thread-safe beans.
 */
public interface Product {

  /**
   * Checks if this product is cross-currency.
   * <p>
   * A cross currency product is defined as one that refers to two or more currencies.
   * 
   * @return true if cross currency
   */
  public default boolean isCrossCurrency() {
    return allCurrencies().size() > 1;
  }

  /**
   * Returns the set of currencies that the product pays in.
   * <p>
   * This returns the complete set of payment currencies.
   * This will typically return one or two currencies.
   * 
   * @return the set of payment currencies
   */
  public default ImmutableSet<Currency> allPaymentCurrencies() {
    return allCurrencies();
  }

  /**
   * Returns the set of currencies the product refers to.
   * <p>
   * This returns the complete set of currencies, not just the payment currencies.
   * The sets will differ when a currency is non-deliverable.
   * 
   * @return the set of currencies the product refers to
   */
  public abstract ImmutableSet<Currency> allCurrencies();

}
