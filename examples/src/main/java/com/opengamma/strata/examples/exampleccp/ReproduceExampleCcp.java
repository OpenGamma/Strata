/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * <p>
 * Please see distribution for license.
 */
package com.opengamma.strata.examples.exampleccp;

import com.google.common.base.Preconditions;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.engine.CalculationEngine;
import com.opengamma.strata.engine.Column;
import com.opengamma.strata.engine.calculations.Results;
import com.opengamma.strata.examples.engine.ResultsFormatter;
import com.opengamma.strata.examples.exampleccp.marketdatarules.MyMarketDataRules;
import com.opengamma.strata.examples.exampleccp.trades.MyTrades;
import com.opengamma.strata.examples.exampleccp.uselessboilerplate.MyUselessBaseMarketData;

import java.time.LocalDate;
import java.util.List;

public class ReproduceExampleCcp {

  public static void main(String[] args) {

    // Use an empty snapshot of market data, indicating only the valuation date.
    // The engine will attempt to source the data for us, which the example engine is
    // configured to load from JSON resources. We could alternatively populate the snapshot
    // with some or all of the required market data here.
    LocalDate valuationDate = LocalDate.of(2015, 4, 23);

    // create the engine and calculate the results
    List<Column> columns = MyColumns.create();
    CalculationEngine engine = MyCalculationEngine.create();
    Results results = engine.calculate(
        MyTrades.create(),
        columns,
        MyCalculationRules.create(MyMarketDataRules.create()),
        MyUselessBaseMarketData.create(valuationDate));

    // produce an ASCII table of the results
    ResultsFormatter.print(results, columns);
    Result<CurrencyAmount> npvResult = (Result<CurrencyAmount>) results.get(0, 5);
    double npv = npvResult.getValue().getAmount();
    almostEquals(npv, -513.0392228654528);
  }

  private static void almostEquals(double x, double y) {
    double epsilon = 1e-9D;
    Preconditions.checkArgument(Math.abs(x - y) < epsilon);
  }

}
