/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.trade;

import com.opengamma.core.position.Trade;
import com.opengamma.financial.security.future.BondFutureSecurity;

/**
 * Trade wrapper for bond future trades.
 */
public class BondFutureTrade extends TradeWrapper {

  public BondFutureTrade(Trade trade) {
    super(trade);
    if (!(trade.getSecurity() instanceof BondFutureSecurity)) {
      throw new IllegalArgumentException("Invalid trade type " + trade.getSecurity().getClass());
    }
  }
}
