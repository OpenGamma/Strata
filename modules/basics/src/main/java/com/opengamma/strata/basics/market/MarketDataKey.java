/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.market;

/**
 * A key that identifies an item of market data.
 * <p>
 * An application can request market data using implementations of this interface.
 * Implementations can identify any piece of market data.
 * This includes observable values, such as the quoted market value of a security, and derived
 * values, such as a volatility surface or a discounting curve.
 * <p>
 * For example, to request a USD discounting curve, an application must create an instance of
 * a class implementing this interface that represents a request for a discounting curve.
 * The key implementation will store the desired currency, USD in this case.
 * <p>
 * A market data key does not necessarily identify a unique item of data.
 * A simple market data implementation will have a single value associated with the key.
 * A more complex system may have multiple values associated, which will involve a mapping
 * step to convert the {@code MarketDataKey} to a {@link MarketDataId}.
 * For example, the mapping step might involve selecting the curve group which supplied a curve.
 *
 * @param <T>  the type of the market data identified by the key
 */
public interface MarketDataKey<T> {

  /**
   * Gets the type of market data identified by the key.
   *
   * @return the type of market data identified by the key
   */
  public abstract Class<T> getMarketDataType();

  /**
   * Converts this key to the matching identifier.
   * <p>
   * Market data keys identify a piece of market data in the context of a single calculation, whereas
   * market data identifiers are a globally unique identifier for an item of data.
   * <p>
   * For example, a calculation has access to a single USD discounting curve, but the system
   * can contain multiple curve groups, each with a USD discounting curve. For cases such as curves there
   * is a mapping step to transform keys to IDs, controlled by the market data rules.
   * <p>
   * The market data key for a simple item of market data contains enough information to uniquely
   * identify the market data in the global set of data. Therefore there is no mapping step required
   * for the data and the market data ID can be directly derived from the market data key.
   *
   * @param feed  the market data feed that is the source of the market data
   * @return the identifier corresponding to this key
   */
  public abstract MarketDataId<T> toMarketDataId(MarketDataFeed feed);

}
