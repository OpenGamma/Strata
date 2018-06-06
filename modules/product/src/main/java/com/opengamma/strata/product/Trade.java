/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product;

import com.opengamma.strata.product.common.SummarizerUtils;

/**
 * A trade with additional structured information.
 * <p>
 * A trade is a transaction that occurred on a specific date between two counterparties.
 * For example, an interest rate swap trade agreed on a particular date for
 * cash-flows in the future.
 * <p>
 * The reference to {@link TradeInfo} captures structured information common to different types of trade.
 * <p>
 * Implementations of this interface must be immutable beans.
 */
public interface Trade
    extends PortfolioItem {

  @Override
  public default PortfolioItemSummary summarize() {
    return SummarizerUtils.summary(this, ProductType.OTHER, "Unknown: " + getClass().getSimpleName());
  }

  /**
   * Gets the standard trade information.
   * <p>
   * All trades contain this standard set of information.
   * 
   * @return the trade information
   */
  @Override
  public abstract TradeInfo getInfo();

  //-------------------------------------------------------------------------
  /**
   * Returns an instance with the specified info.
   * 
   * @param info  the new info
   * @return the instance with the specified info
   */
  public abstract Trade withInfo(TradeInfo info);

}
