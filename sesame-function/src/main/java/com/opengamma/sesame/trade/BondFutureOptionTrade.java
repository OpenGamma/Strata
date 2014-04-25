/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.trade;

import com.opengamma.core.position.Trade;
import com.opengamma.financial.security.option.BondFutureOptionSecurity;
import com.opengamma.util.ArgumentChecker;

/**
 * Trade wrapper for deliverable swap future trades.
 */
public class BondFutureOptionTrade extends TradeWrapper<BondFutureOptionSecurity> {

  /**
   * Constructs a bond future option trade wrapper.
   * @param trade a trade instance of a bond future option, not null;
   */
  public BondFutureOptionTrade(Trade trade) {
    super(ArgumentChecker.notNull(trade, "trade"), BondFutureOptionSecurity.class);
  }
}
