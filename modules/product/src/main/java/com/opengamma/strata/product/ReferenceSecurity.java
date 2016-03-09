/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product;

import java.time.LocalDate;
import java.util.Optional;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.market.ReferenceData;
import com.opengamma.strata.collect.Messages;

/**
 * A single security that can be traded.
 * <p>
 * A security is one of the building blocks of finance, representing a fungible instrument that can be traded.
 * This is intended to cover instruments such as listed equities and futures.
 * It is intended that Over-The-Counter (OTC) instruments, such as an interest rate swap,
 * are embedded directly within the trade, rather than handled as one-off securities.
 * <p>
 * A security may be referred to using a {@link SecurityId}.
 * The identifier can be looked up in {@link ReferenceData}.
 * <p>
 * Implementations of this interface must be immutable beans.
 */
public interface ReferenceSecurity {

  /**
   * Gets the security identifier.
   * <p>
   * This identifier uniquely identifies the security within the system.
   * It is the key used to lookup the security in {@link ReferenceData}.
   * <p>
   * A real-world security will typically have multiple identifiers.
   * The only restriction placed on the identifier is that it is sufficiently
   * unique for the reference data lookup. As such, it is acceptable to use
   * an identifier from a well-known global or vendor symbology.
   * 
   * @return the security identifier
   */
  public abstract SecurityId getSecurityId();

  /**
   * Gets the currency that the security is traded in.
   * 
   * @return the trading currency
   */
  public abstract Currency getCurrency();

  /**
   * Gets the underlying security identifiers.
   * <p>
   * The set must contain all the security identifiers that this security refers to.
   * 
   * @return the underlying security identifiers
   */
  public abstract ImmutableSet<SecurityId> getUnderlyingSecurities();

  /**
   * Gets additional information about the security.
   * <p>
   * This method obtains the specified additional information.
   * This allows additional information about a security to be obtained if available.
   * <p>
   * If the info is not found, an exception is thrown.
   * 
   * @param <T>  the type of the info
   * @param type  the type to find
   * @return the security information
   * @throws IllegalArgumentException if the information is not found
   */
  public default <T> T getInfo(SecurityInfoType<T> type) {
    return findInfo(type).orElseThrow(() -> new IllegalArgumentException(
        Messages.format("Security info not found for type '{}'", type)));
  }

  /**
   * Finds additional information about the security.
   * <p>
   * This method obtains the specified additional information.
   * This allows additional information about a security to be obtained if available.
   * <p>
   * If the info is not found, optional empty is returned.
   * 
   * @param <T>  the type of the info
   * @param type  the type to find
   * @return the security information
   */
  public abstract <T> Optional<T> findInfo(SecurityInfoType<T> type);

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
  public abstract SecurityProduct createProduct(ReferenceData refData);

  /**
   * Creates a trade based on this security.
   * <p>
   * This creates a trade of the correct type from this security.
   * The trade will contain all underlying securities, looked up using the reference data.
   * 
   * @param tradeDate  the date of the trade
   * @param quantity  the number of contracts in the trade
   * @param tradePrice  the price that was traded, in decimal form
   * @param refData  the reference data used to find underlying securities
   * @return the trade
   */
  public default FinanceTrade createTrade(
      LocalDate tradeDate,
      long quantity,
      double tradePrice,
      ReferenceData refData) {
    
    return createTrade(TradeInfo.builder().tradeDate(tradeDate).build(), quantity, tradePrice, refData);
  }

  /**
   * Creates a trade based on this security.
   * <p>
   * This creates a trade of the correct type from this security.
   * The trade will contain all underlying securities, looked up using the reference data.
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
