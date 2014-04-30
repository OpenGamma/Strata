/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.trade;

import com.opengamma.core.position.Trade;
import com.opengamma.financial.security.future.FederalFundsFutureSecurity;
import com.opengamma.util.ArgumentChecker;

/**
 * Trade wrapper for federal funds future trades.
 */
public class FedFundsFutureTrade extends TradeWrapper<FederalFundsFutureSecurity> {

  /**
   * Constructs a federal funds trade wrapper.
   * @param trade a trade instance of a federal funds future, not null.
   */
  public FedFundsFutureTrade(Trade trade) {
    super(ArgumentChecker.notNull(trade, "trade"), FederalFundsFutureSecurity.class);
  }

}
