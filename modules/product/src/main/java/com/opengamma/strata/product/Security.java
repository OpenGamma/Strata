/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;

/**
 * A security that can be traded.
 * <p>
 * A security is one of the building blocks of finance, representing a fungible instrument that can be traded.
 * This is intended to cover instruments such as listed equities, bonds and exchange traded derivatives (ETD).
 * Within Strata, a financial instrument is a security if it is a standardized
 * exchange-based contract that can be referenced by a {@link SecurityId}.
 * The security can be looked up in {@link ReferenceData} using the identifier.
 * <p>
 * It is intended that Over-The-Counter (OTC) instruments, such as an interest rate swap,
 * are embedded directly within the trade, rather than handled as one-off securities.
 * <p>
 * Implementations of this interface must be immutable beans.
 */
public interface Security {

  /**
   * Gets the standard security information.
   * <p>
   * All securities contain this standard set of information.
   * It includes the identifier, information about the price and an extensible data map.
   * 
   * @return the security information
   */
  public abstract SecurityInfo getInfo();

  /**
   * Gets the security identifier.
   * <p>
   * This identifier uniquely identifies the security within the system.
   * 
   * @return the security identifier
   */
  public default SecurityId getSecurityId() {
    return getInfo().getId();
  }

  /**
   * Gets the currency that the security is traded in.
   * 
   * @return the trading currency
   */
  public default Currency getCurrency() {
    return getInfo().getPriceInfo().getCurrency();
  }

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
   * @throws UnsupportedOperationException if the security does not contain information about the product model
   */
  public abstract SecuritizedProduct createProduct(ReferenceData refData);

  /**
   * Creates a trade based on this security.
   * <p>
   * This creates a trade of a suitable type for this security.
   * 
   * @param tradeInfo  the trade information
   * @param quantity  the number of contracts in the trade
   * @param tradePrice  the price agreed when the trade occurred
   * @param refData  the reference data used to find underlying securities
   * @return the trade
   */
  public abstract Trade createTrade(
      TradeInfo tradeInfo,
      double quantity,
      double tradePrice,
      ReferenceData refData);

}
