/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.finance;

import org.joda.beans.ImmutableBean;

/**
 * A financial product that can be traded.
 * <p>
 * A product is a high level abstraction applicable to many different types.
 * For example, an Interest Rate Swap is a product, as is a Forward Rate Agreement (FRA).
 * <p>
 * A product exists independently from a {@link Trade}. It represents the economics of the
 * financial instrument regardless of the trade date or counterparties.
 * <p>
 * Each product has an expanded form, intended to be optimized for pricing.
 * In this form, the holiday calendar rules have been applied, with dates being adjusted to valid business days.
 * If the holiday calendar changes, then the expanded form may no longer be accurate.
 * Care must be taken when placing the expanded form in a cache or persistence layer.
 * <p>
 * Implementations must be immutable and thread-safe beans.
 * 
 * @param <T>  the type of the expanded form
 */
public interface Product<T extends ImmutableBean>
    extends ImmutableBean {

  /**
   * Expands this product.
   * <p>
   * This converts the object implementing this interface to the equivalent expanded form.
   * Conversion should not be lossy.
   * 
   * @return the expanded form of this object
   * @throws RuntimeException if unable to expand due to an invalid definition
   */
  public abstract T expand();

}
