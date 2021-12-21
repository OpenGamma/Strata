/*
 * Copyright (C) 2021 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap.type;

import java.time.LocalDate;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.ReferenceDataNotFoundException;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.product.TradeTemplate;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.swap.SwapTrade;

/**
 * A template for creating Fixed-Float swap trades.
 * <p>
 * This defines almost all the data necessary to create a Fixed-Float single currency {@link SwapTrade}.
 * The trade date, notional and fixed rate are required to complete the template and create the trade.
 * As such, it is often possible to get a market price for a trade based on the template.
 * The market price is typically quoted as a bid/ask on the fixed rate.
 */
public interface FixedFloatSwapTemplate
    extends TradeTemplate {

  /**
   * The market convention of the associated swap.
   * 
   * @return the swap convention
   */
  public abstract FixedFloatSwapConvention getConvention();

  /**
   * The associated swap tenor.
   * 
   * @return the tenor
   */
  public abstract Tenor getTenor();

  /**
   * Creates a trade based on this template.
   * <p>
   * This returns a trade based on the specified trade date.
   * <p>
   * The notional is unsigned, with buy/sell determining the direction of the trade.
   * If buying the swap, the floating rate is received from the counterparty, with the fixed rate being paid.
   * If selling the swap, the floating rate is paid to the counterparty, with the fixed rate being received.
   * 
   * @param tradeDate  the date of the trade
   * @param buySell  the buy/sell flag
   * @param notional  the notional amount, in the payment currency of the template
   * @param fixedRate  the fixed rate, typically derived from the market
   * @param refData  the reference data, used to resolve the trade dates
   * @return the trade
   * @throws ReferenceDataNotFoundException if an identifier cannot be resolved in the reference data
   */
  public abstract SwapTrade createTrade(
      LocalDate tradeDate,
      BuySell buySell,
      double notional,
      double fixedRate,
      ReferenceData refData);

}
