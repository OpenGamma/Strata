/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * <p>
 * Please see distribution for license.
 */
package com.opengamma.strata.marketdata.key;

import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.collect.id.StandardIdentifiable;
import com.opengamma.strata.marketdata.id.FieldName;
import com.opengamma.strata.marketdata.id.MarketDataVendor;
import com.opengamma.strata.marketdata.id.ObservableId;

/**
 * A market data key that identifies observable data.
 * <p>
 * Observable data can be requested from an external data provider, for example Bloomberg or Reuters.
 */
public interface ObservableKey extends MarketDataKey<Double>, StandardIdentifiable {

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
   * Returns the field name in the market data record that contains the market data item.
   *
   * @return the field name in the market data record that contains the market data item
   */
  public abstract FieldName getFieldName();

  @Override
  public default Class<Double> getMarketDataType() {
    return Double.class;
  }

  /**
   * Returns the ID corresponding to this key.
   * <p>
   * Market data keys identify a piece of market data in the context of a single calculation, whereas
   * market data IDs are a globally unique identifier for an item of data.
   * <p>
   * For example, a calculation has access to a single USD discounting curve, but the system
   * can contain multiple curve groups, each with a USD discounting curve. For cases such as curves there
   * is a mapping step to transform keys to IDs, controlled by the market data rules.
   * <p>
   * The market data key for an observable item of market data contains enough information to uniquely
   * identify the market data in the global set of data. Therefore there is no mapping step required
   * for observable data and the market data ID can be directly derived from the market data key.
   *
   * @param marketDataVendor  the market data vendor that is the source of the observable market data
   * @return the ID corresponding to this key
   */
  ObservableId toObservableId(MarketDataVendor marketDataVendor);
}
