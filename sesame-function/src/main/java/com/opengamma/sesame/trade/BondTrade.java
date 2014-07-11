/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.trade;

import com.opengamma.core.position.Trade;
import com.opengamma.financial.security.bond.BondSecurity;
import com.opengamma.util.ArgumentChecker;

/**
 * Trade wrapper for bond trades.
 */
public class BondTrade extends TradeWrapper<BondSecurity> {

  /**
   * Base trade wrapper constructor that wraps a trade in an explicit instrument type.
   *
   * @param trade the trade containing the instrument, not null.
   */
  public BondTrade(Trade trade) {
    super(ArgumentChecker.notNull(trade, "trade"), BondSecurity.class);
  }
}
