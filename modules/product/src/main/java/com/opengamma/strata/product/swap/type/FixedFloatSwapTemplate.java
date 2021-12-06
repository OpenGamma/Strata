/*
 * Copyright (C) 2021 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap.type;

import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.product.TradeTemplate;
import com.opengamma.strata.product.swap.SwapTrade;

/**
 * A template for creating Fixed-Float swap trades.
 * <p>
 * This defines almost all the data necessary to create a Fixed-Float single currency {@link SwapTrade}.
 * The trade date, notional and fixed rate are required to complete the template and create the trade.
 * As such, it is often possible to get a market price for a trade based on the template.
 * The market price is typically quoted as a bid/ask on the fixed rate.
 * 
 * @param <C> The float rate swap leg convention type
 */
public interface FixedFloatSwapTemplate<C extends FloatRateSwapLegConvention>
    extends TradeTemplate {

  /**
   * The market convention of the associated swap.
   * 
   * @return the swap convention
   */
  public abstract FixedFloatSwapConvention<C> getConvention();

  /**
   * The associated swap tenor.
   * 
   * @return the tenor
   */
  public abstract Tenor getTenor();

}
