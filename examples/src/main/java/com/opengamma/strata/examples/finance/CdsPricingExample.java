/*
 * *
 *  * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *  *
 *  * Please see distribution for license.
 *
 *
 */

package com.opengamma.strata.examples.finance;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.engine.config.Measure;
import com.opengamma.strata.examples.finance.credit.ExampleCalculator;
import com.opengamma.strata.examples.finance.credit.ExampleReporter;
import com.opengamma.strata.examples.finance.credit.ExampleTradeSource;
import com.opengamma.strata.examples.finance.credit.api.Calculator;
import com.opengamma.strata.examples.finance.credit.api.Reporter;
import com.opengamma.strata.examples.finance.credit.api.TradeSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;

public class CdsPricingExample {

  private static Logger logger = LoggerFactory.getLogger(CdsPricingExample.class);

  static final TradeSource trades = ExampleTradeSource.of();
  static final LocalDate valuationDate = LocalDate.of(2014, 10, 16);
  static final Measure pv = Measure.PRESENT_VALUE;
  static final Measure tradeInfo = Measure.TRADE_INFO;
  static final ImmutableList<Measure> measures = ImmutableList.of(
      pv,
      tradeInfo
  );
  static final Calculator calc = ExampleCalculator.of();
  static final Reporter reporter = ExampleReporter.of("cds-report-template");

  public static void main(String[] args) {
    logger.info("PV is " + calcPv());
    calcPvAndReportToAsciiToLogger();
  }

  public static double calcPv() {
    return calc.calculateSimpleValue(valuationDate, trades, pv).getAmount();
  }

  public static void calcPvAndReportToAsciiToLogger() {
    reporter.reportAsciiToLogger(calc.calculateReportingResults(valuationDate, trades, measures));
  }

}
