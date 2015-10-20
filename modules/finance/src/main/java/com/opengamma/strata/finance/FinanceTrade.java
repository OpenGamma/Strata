/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.finance;

import com.opengamma.strata.basics.Trade;

/**
 * A trade with additional structured information.
 * <p>
 * This extends {@link Trade} to add a reference to {@link TradeInfo}, which captures
 * structured information common to different types of trade.
 * <p>
 * Implementations of this interface must be immutable beans.
 */
public interface FinanceTrade
    extends Trade {

  /**
   * The additional trade information.
   * <p>
   * This allows additional information to be attached to the trade.
   * 
   * @return the additional trade info
   */
  public abstract TradeInfo getTradeInfo();

}
