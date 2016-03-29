/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.market;

/**
 * A market data key that identifies observable data.
 * <p>
 * Observable data can be requested from an external data provider, for example Bloomberg or Reuters.
 * It is always of type {@code Double}.
 */
public interface ObservableKey
    extends SimpleMarketDataKey<Double>, StandardIdentifiable {

  /**
   * Gets the standard identifier identifying the data.
   * <p>
   * The identifier may be the identifier used to identify the item in an underlying data provider,
   * for example a Bloomberg ticker. It also may be any arbitrary unique identifier that can be resolved
   * to one or more data provider identifiers which are used to request the data from the provider.
   *
   * @return a standard identifier, such as a ticker, to identify the desired data
   */
  @Override
  public abstract StandardId getStandardId();

  /**
   * Gets the field name in the market data record that contains the market data item.
   * <p>
   * Each ticker typically exposes many different fields. The field name specifies the desired field.
   * For example, the {@linkplain FieldName#MARKET_VALUE market value}.
   *
   * @return the field name in the market data record that contains the market data item
   */
  public abstract FieldName getFieldName();

  @Override
  public default Class<Double> getMarketDataType() {
    return Double.class;
  }

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
   * The market data key for an observable item of market data contains enough information to uniquely
   * identify the market data in the global set of data. Therefore there is no mapping step required
   * for observable data and the market data ID can be directly derived from the market data key.
   *
   * @param marketDataFeed  the market data feed that is the source of the observable market data
   * @return the identifier corresponding to this key
   */
  @Override
  public abstract ObservableId toMarketDataId(MarketDataFeed marketDataFeed);

}
