/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.trade;

import com.opengamma.core.position.Trade;
import com.opengamma.financial.security.future.DeliverableSwapFutureSecurity;

/**
 * Trade wrapper for deliverable swap future trades.
 */
public class DeliverableSwapFutureTrade extends TradeWrapper<DeliverableSwapFutureSecurity> {

  public DeliverableSwapFutureTrade(Trade trade) {
    super(trade, DeliverableSwapFutureSecurity.class);
    if (!(trade.getSecurity()  instanceof DeliverableSwapFutureSecurity)) {
      throw new IllegalArgumentException("Invalid trade type: " + trade.getSecurity().getClass());
    }
  }
}
