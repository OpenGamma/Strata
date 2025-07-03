/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure;

import com.opengamma.strata.calc.ImmutableMeasure;
import com.opengamma.strata.calc.Measure;

/**
 * The standard set of measures that can be calculated by Strata.
 */
final class StandardMeasures {

  // present value, with currency conversion
  public static final Measure PRESENT_VALUE = ImmutableMeasure.of("PresentValue");
  // explain present value, with no currency conversion
  public static final Measure EXPLAIN_PRESENT_VALUE = ImmutableMeasure.of("ExplainPresentValue", false);

  // PV01 calibrated sum
  public static final Measure PV01_CALIBRATED_SUM = ImmutableMeasure.of("PV01CalibratedSum");
  // PV01 calibrated bucketed
  public static final Measure PV01_CALIBRATED_BUCKETED = ImmutableMeasure.of("PV01CalibratedBucketed");
  // PV01 market quote sum
  public static final Measure PV01_MARKET_QUOTE_SUM = ImmutableMeasure.of("PV01MarketQuoteSum");
  // PV01 market quote bucketed
  public static final Measure PV01_MARKET_QUOTE_BUCKETED = ImmutableMeasure.of("PV01MarketQuoteBucketed");

  //-------------------------------------------------------------------------
  // accrued interest
  public static final Measure ACCRUED_INTEREST = ImmutableMeasure.of("AccruedInterest");
  // cash flows
  public static final Measure CASH_FLOWS = ImmutableMeasure.of("CashFlows");
  // currency exposure, with no currency conversion
  public static final Measure CURRENCY_EXPOSURE = ImmutableMeasure.of("CurrencyExposure", false);
  // delta
  public static final Measure OPTION_DELTA = ImmutableMeasure.of("OptionDelta");
  // current cash
  public static final Measure CURRENT_CASH = ImmutableMeasure.of("CurrentCash");
  // forward FX rate
  public static final Measure FORWARD_FX_RATE = ImmutableMeasure.of("ForwardFxRate", false);
  // leg present value
  public static final Measure LEG_PRESENT_VALUE = ImmutableMeasure.of("LegPresentValue");
  // leg initial notional
  public static final Measure LEG_INITIAL_NOTIONAL = ImmutableMeasure.of("LegInitialNotional");
  // par rate, which is a decimal rate that does not need currency conversion
  public static final Measure PAR_RATE = ImmutableMeasure.of("ParRate", false);
  // par spread, which is a decimal rate that does not need currency conversion
  public static final Measure PAR_SPREAD = ImmutableMeasure.of("ParSpread", false);
  // the resolved target
  public static final Measure RESOLVED_TARGET = ImmutableMeasure.of("ResolvedTarget", false);
  // unit price, which is treated as a simple decimal number even if it refers to a currency
  public static final Measure UNIT_PRICE = ImmutableMeasure.of("UnitPrice", false);

  //-------------------------------------------------------------------------
  // semi-parallel gamma bucketed PV01
  public static final Measure PV01_SEMI_PARALLEL_GAMMA_BUCKETED = ImmutableMeasure.of("PV01SemiParallelGammaBucketed");
  // single-node gamma bucketed PV01
  public static final Measure PV01_SINGLE_NODE_GAMMA_BUCKETED = ImmutableMeasure.of("PV01SingleNodeGammaBucketed");

  //-------------------------------------------------------------------------
  // Vega market quote bucketed
  public static final Measure VEGA_MARKET_QUOTE_BUCKETED = ImmutableMeasure.of("VegaMarketQuoteBucketed");

  //-------------------------------------------------------------------------
  // restricted constructor
  private StandardMeasures() {
  }

}
