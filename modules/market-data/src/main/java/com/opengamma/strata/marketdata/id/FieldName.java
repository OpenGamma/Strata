/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * <p>
 * Please see distribution for license.
 */
package com.opengamma.strata.marketdata.id;

import com.opengamma.strata.collect.type.TypedString;

/**
 * The name of a field in a market data record.
 * <p>
 * Market data is typically provided as a record containing multiple fields. Each field contains an item
 * of data. The record is identified by a unique ID and the fields are identified by name. Therefore
 * an item of market data is uniquely identified by the combination its ID and field name.
 * <p>
 * Different market data providers use different sets of field names. The names in this class are
 * not specific to any provider, and are mapped to the provider field names by the market data
 * system. This allows calculations to request an item of data using its field name (e.g.
 * closing price) without having to know which data provider it is coming from.
 */
public final class FieldName extends TypedString<FieldName> {

  /** The field name for market value, used as the default when no field name is specified. */
  public static final FieldName MARKET_VALUE = of("MarketValue");

  /**
   * Returns a field name identifying the specified field
   *
   * @param name  the name of the field in the market data record
   * @return a field name identifying the specified field
   */
  public static FieldName of(String name) {
    return new FieldName(name);
  }

  /**
   * @param name  the name of the field in the market data record
   */
  protected FieldName(String name) {
    super(name);
  }
}
