/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.trade;

import com.opengamma.core.position.Trade;
import com.opengamma.financial.security.option.IRFutureOptionSecurity;

/**
 * Trade wrapper for deliverable swap future trades.
 */
public class IRFutureOptionTrade extends TradeWrapper {

  public IRFutureOptionTrade(Trade trade) {
    super(trade);
    if (!(trade.getSecurity() instanceof IRFutureOptionSecurity)) {
      throw new IllegalArgumentException("Invalid trade type " + trade.getSecurity().getClass());
    }
  }
}
