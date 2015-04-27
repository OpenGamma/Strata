/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.function;

import com.opengamma.strata.engine.config.Measure;
import com.opengamma.strata.engine.config.pricing.DefaultFunctionGroup;
import com.opengamma.strata.engine.config.pricing.FunctionGroup;
import com.opengamma.strata.finance.Trade;

/**
 * Contains function groups for built-in swap engine functions.
 * <p>
 * Function groups are used in pricing rules to allow the engine to calculate the
 * measures provided by the functions in the group.
 */
public final class TradeFunctionGroups {

  /**
   * The group containing all available measures.
   */
  private static final FunctionGroup<Trade> ALL =
      DefaultFunctionGroup.builder(Trade.class).name("TradeAll")
          .addFunction(Measure.ID, TradeIdFunction.class)
          .addFunction(Measure.COUNTERPARTY, TradeCounterpartyFunction.class)
          .addFunction(Measure.TRADE_DATE, TradeDateFunction.class)
          .addFunction(Measure.SETTLEMENT_DATE, TradeSettlementDateFunction.class)
          .build();

  /**
   * Restricted constructor.
   */
  private TradeFunctionGroups() {
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains the function group providing all built-in measures on any trade.
   * <p>
   * The supported built-in measures are:
   * <ul>
   *   <li>{@linkplain Measure#ID ID}</li>
   *   <li>{@linkplain Measure#COUNTERPARTY Counterparty}</li>
   *   <li>{@linkplain Measure#TRADE_DATE Trade date}</li>
   *   <li>{@linkplain Measure#SETTLEMENT_DATE Settlement date}</li>
   * </ul>
   * 
   * @return the function group
   */
  public static FunctionGroup<Trade> all() {
    return ALL;
  }

}
