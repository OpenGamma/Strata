/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.data;

import org.joda.convert.FromString;

import com.opengamma.strata.collect.TypedString;

/**
 * The name of a field in a market data record.
 * <p>
 * Market data is typically provided as a record containing multiple fields. Each field contains an item
 * of data. The record is identified by a unique ID and the fields are identified by name.
 * Therefore an item of market data is uniquely identified by the combination its ID and field name.
 * <p>
 * Different market data providers use different sets of field names. The names in this class are
 * not specific to any provider, and are mapped to the provider field names by the market data
 * system. This allows calculations to request an item of data using its field name, such as
 * "closing price", without having to know which data provider it is coming from.
 */
public final class FieldName
    extends TypedString<FieldName> {

  /** Serialization version. */
  private static final long serialVersionUID = 1L;

  //-------------------------------------------------------------------------
  /**
   * The field name for the market value - 'MarketValue'.
   * <p>
   * This is used to refer to the standard market quote for the identifier.
   * It is typically used as the default when no field name is specified.
   */
  public static final FieldName MARKET_VALUE = of("MarketValue");
  /**
   * The field name for the settlement price - 'SettlementPrice'.
   * <p>
   * This is used to refer to the daily settlement price used in margining.
   */
  public static final FieldName SETTLEMENT_PRICE = of("SettlementPrice");
  /**
   * The field name for the clean price of a coupon bond.
   * <p>
   * The clean price does not include the accrued interest.
   */
  public static final FieldName CLEAN_PRICE = of("CleanPrice");
  /**
   * The field name for the clean real price of a capital indexed bond.
   * <p>
   * The clean real price does not include the accrued interest.
   */
  public static final FieldName CLEAN_REAL_PRICE = of("CleanRealPrice");
  /**
   * The field name for the clean nominal price of a capital indexed bond.
   * <p>
   * The clean nominal price does not include the accrued interest.
   */
  public static final FieldName CLEAN_NOMINAL_PRICE = of("CleanNominalPrice");
  /**
   * The field name for the dirty price of a coupon bond.
   * <p>
   * The dirty price includes the accrued interest.
   */
  public static final FieldName DIRTY_PRICE = of("DirtyPrice");
  /**
   * The field name for the volatility of an asset.
   */
  public static final FieldName VOLATILITY = of("Volatility");
  /**
   * The field name for the yield to maturity.
   * <p>
   * This is used to refer to the yield of a coupon bond or bill.
   */
  public static final FieldName YIELD_TO_MATURITY = of("YieldToMaturity");
  /**
   * The field name for the real yield.
   * <p>
   * This is used to refer to the real yield of a capital indexed bond.
   */
  public static final FieldName REAL_YIELD_TO_MATURITY = of("RealYieldToMaturity");
  /**
   * The field name for the nominal yield.
   * <p>
   * This is used to refer to the yield of a coupon bond or bill.
   */
  public static final FieldName NOMINAL_YIELD_TO_MATURITY = of("NominalYieldToMaturity");
  /**
   * The field name for the par yield.
   * <p>
   * This is used to refer to the par yield of a coupon bond or bill.
   */
  public static final FieldName PAR_YIELD = of("ParYield");

  //-------------------------------------------------------------------------

  /**
   * Obtains an instance from the specified name.
   * <p>
   * Field names may contain any character, but must not be empty.
   *
   * @param name the name of the field
   * @return a field with the specified name
   */
  @FromString
  public static FieldName of(String name) {
    return new FieldName(name);
  }

  /**
   * Creates an instance.
   *
   * @param name the name of the field
   */
  private FieldName(String name) {
    super(name);
  }

}
