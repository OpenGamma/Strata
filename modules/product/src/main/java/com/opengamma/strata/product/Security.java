/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.market.ReferenceData;

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
   * Gets the security identifier.
   * <p>
   * This identifier uniquely identifies the security within the system.
   * 
   * @return the security identifier
   */
  public abstract SecurityInfo getSecurityInfo();

  /**
   * Gets the security identifier.
   * <p>
   * This identifier uniquely identifies the security within the system.
   * 
   * @return the security identifier
   */
  public default SecurityId getSecurityId() {
    return getSecurityInfo().getId();
  }

  /**
   * Gets the currency that the security is traded in.
   * 
   * @return the trading currency
   */
  public default Currency getCurrency() {
    return getSecurityInfo().getPriceInfo().getCurrency();
  }

  //-------------------------------------------------------------------------
  /**
   * Creates a trade based on this security.
   * <p>
   * This creates a trade of a suitable type for this security.
   * 
   * @param tradeInfo  the trade information
   * @param quantity  the number of contracts in the trade
   * @param tradePrice  the price that was traded, in decimal form
   * @param refData  the reference data used to find underlying securities
   * @return the trade
   */
  public abstract FinanceTrade createTrade(
      TradeInfo tradeInfo,
      long quantity,
      double tradePrice,
      ReferenceData refData);

}
