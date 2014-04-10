/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.trade;

import com.opengamma.core.position.Trade;
import com.opengamma.financial.security.future.FederalFundsFutureSecurity;

/**
 * Trade wrapper for federal funds future trades.
 */
public class FedFundsFutureTrade extends TradeWrapper {

  public FedFundsFutureTrade(Trade trade) {
    super(trade);
    if (!(trade.getSecurity() instanceof FederalFundsFutureSecurity)) {
      throw new IllegalArgumentException("Invalid trade type " + trade.getSecurity().getClass());
    }
  }

}
