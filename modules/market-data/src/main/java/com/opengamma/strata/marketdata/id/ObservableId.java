/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.marketdata.id;

import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.collect.id.StandardIdentifiable;

/**
 * A market data ID that identifies observable data.
 * <p>
 * Observable data can be requested from an external data provider, for example Bloomberg or Reuters.
 */
public interface ObservableId extends MarketDataId<Double>, StandardIdentifiable {

  /**
   * Returns a standard identifier identifying the data.
   * <p>
   * The identifier may be the identifier used to identify the item in an
   * underlying data provider, for example a Bloomberg ticker. It also may be any arbitrary unique
   * identifier that can be resolved to one or more data provider identifiers which are used to
   * request the data from the provider.
   *
   * @return a standard identifier identifying the data
   */
  @Override
  public abstract StandardId getStandardId();

  /**
   * Returns the field name in the market data record that contains the market data item, for example
   * {@linkplain FieldName#MARKET_VALUE market value}.
   *
   * @return the field name in the market data record that contains the market data item
   */
  public abstract FieldName getFieldName();

  @Override
  public default Class<Double> getMarketDataType() {
    return Double.class;
  }
}
