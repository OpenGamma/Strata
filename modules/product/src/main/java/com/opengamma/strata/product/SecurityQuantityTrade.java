/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product;

/**
 * A trade that is based on security, quantity and price.
 * <p>
 * If the trade is directly based on a securitized product, the trade type is {@link SecuritizedProductTrade}.
 * If not, the financial instrument involved in the trade is represented in alternative form, e.g., {@link Security}.
 * See individual implementations for more details.
 */
public interface SecurityQuantityTrade
    extends Trade, SecurityQuantity {

  /**
   * Gets the price that was traded.
   * <p>
   * This is the unit price agreed when the trade occurred.
   * 
   * @return the price
   */
  public abstract double getPrice();

  //-------------------------------------------------------------------------
  /**
   * Returns an instance with the specified info.
   * 
   * @param info  the new info
   * @return the instance with the specified info
   */
  @Override
  public abstract SecurityQuantityTrade withInfo(TradeInfo info);

  /**
   * Returns an instance with the specified quantity.
   * 
   * @param quantity  the new quantity
   * @return the instance with the specified quantity
   */
  public abstract SecurityQuantityTrade withQuantity(double quantity);

  /**
   * Returns an instance with the specified price.
   * 
   * @param price  the new price
   * @return the instance with the specified price
   */
  public abstract SecurityQuantityTrade withPrice(double price);

}
