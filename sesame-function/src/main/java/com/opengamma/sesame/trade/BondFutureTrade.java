/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.trade;

import com.opengamma.core.position.Trade;
import com.opengamma.financial.security.future.BondFutureSecurity;
import com.opengamma.util.ArgumentChecker;

/**
 * Trade wrapper for bond future trades.
 */
public class BondFutureTrade extends TradeWrapper<BondFutureSecurity> {

  /**
   * Constructs a bond future trade wrapper.
   * @param trade a trade instance of a bond future, not null.
   */
  public BondFutureTrade(Trade trade) {
    super(ArgumentChecker.notNull(trade, "trade"), BondFutureSecurity.class);
  }
}
