/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.market.ReferenceData;
import com.opengamma.strata.product.index.IborFutureSecurity;
import com.opengamma.strata.product.swap.DeliverableSwapFutureSecurity;

/**
 * A security that is defined with an underlying product model.
 * <p>
 * This interface extends {@link Security} to provide access to the
 * {@linkplain Product product model} underlying the security.
 * <p>
 * Most securities have some form of underlying model that allows pricing from first principles.
 * For example, an {@linkplain IborFutureSecurity STIR future} contains details of the
 * underlying Ibor index, and a {@linkplain DeliverableSwapFutureSecurity DSF} contains
 * information on the swap that will be delivered when the future expires.
 * This underlying model is implemented as a {@code Product}.
 * <p>
 * Implementations of this interface must be immutable beans.
 */
public interface ModelledSecurity extends Security {

  /**
   * Gets the set of underlying security identifiers.
   * <p>
   * The set must contain all the security identifiers that this security directly refers to.
   * For example, a bond future will return the identifiers of the underlying basket of bonds,
   * but a bond future option will only return the identifier of the underlying future, not the basket.
   * 
   * @return the underlying security identifiers
   */
  public abstract ImmutableSet<SecurityId> getUnderlyingIds();

  //-------------------------------------------------------------------------
  /**
   * Creates the product associated with this security.
   * <p>
   * The product of a security is distinct from the security.
   * The product includes the financial details from this security,
   * but excludes the additional information.
   * The product also includes the products of any underlying securities.
   * 
   * @param refData  the reference data used to find underlying securities
   * @return the product
   */
  public abstract SecuritizedProduct createProduct(ReferenceData refData);

  /**
   * Creates a trade based on this security.
   * <p>
   * This creates a trade of a suitable type for this security.
   * The result is a {@link ProductTrade} suitable for model-based pricing.
   * 
   * @param tradeInfo  the trade information
   * @param quantity  the number of contracts in the trade
   * @param tradePrice  the price that was traded, in decimal form
   * @param refData  the reference data used to find underlying securities
   * @return the trade
   */
  @Override
  public abstract ProductTrade createTrade(
      TradeInfo tradeInfo,
      long quantity,
      double tradePrice,
      ReferenceData refData);

}
