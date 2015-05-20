/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.examples.finance;

import java.time.LocalDate;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.BuySell;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.engine.CalculationEngine;
import com.opengamma.strata.engine.CalculationRules;
import com.opengamma.strata.engine.Column;
import com.opengamma.strata.engine.calculations.Results;
import com.opengamma.strata.engine.config.Measure;
import com.opengamma.strata.engine.config.ReportingRules;
import com.opengamma.strata.engine.marketdata.BaseMarketData;
import com.opengamma.strata.examples.engine.ExampleEngine;
import com.opengamma.strata.examples.engine.ResultsFormatter;
import com.opengamma.strata.examples.marketdata.ExampleMarketData;
import com.opengamma.strata.finance.Trade;
import com.opengamma.strata.finance.TradeInfo;
import com.opengamma.strata.finance.rate.fra.Fra;
import com.opengamma.strata.finance.rate.fra.FraTrade;
import com.opengamma.strata.function.OpenGammaPricingRules;

/**
 * Example to illustrate using the engine to price a FRA.
 * <p>
 * This makes use of the example engine which sources the required market data from
 * JSON resources.
 */
public class FraPricingExample {

  /**
   * Runs the example, pricing the instruments, producing the output as an ASCII table.
   * 
   * @param args  ignored
   */
  public static void main(String[] args) {
    // the trades that will have measures calculated
    List<Trade> trades = ImmutableList.of(createTrade1());

    // the columns, specifying the measures to be calculated
    List<Column> columns = ImmutableList.of(
        Column.of(Measure.ID),
        Column.of(Measure.COUNTERPARTY),
        Column.of(Measure.SETTLEMENT_DATE),
        Column.of(Measure.PRESENT_VALUE),
        Column.of(Measure.PV01),
        Column.of(Measure.PAR_RATE),
        Column.of(Measure.PAR_SPREAD));

    // the complete set of rules for calculating measures
    CalculationRules rules = CalculationRules.builder()
        .pricingRules(OpenGammaPricingRules.standard())
        .marketDataRules(ExampleMarketData.rules())
        .reportingRules(ReportingRules.fixedCurrency(Currency.USD))
        .build();

    // Use an empty snapshot of market data, indicating only the valuation date.
    // The engine will attempt to source the data for us, which the example engine is
    // configured to load from JSON resources. We could alternatively populate the snapshot
    // with some or all of the required market data here.
    LocalDate valuationDate = LocalDate.of(2014, 1, 22);
    BaseMarketData baseMarketData = BaseMarketData.empty(valuationDate);

    // create the engine and calculate the results
    CalculationEngine engine = ExampleEngine.create();
    Results results = engine.calculate(trades, columns, rules, baseMarketData);

    // produce an ASCII table of the results
    ResultsFormatter.print(results, columns);
  }

  //-----------------------------------------------------------------------  
  // create a FRA trade
  private static Trade createTrade1() {
    Fra fra = Fra.builder()
        .buySell(BuySell.SELL)
        .index(IborIndices.USD_LIBOR_3M)
        .startDate(LocalDate.of(2014, 9, 12))
        .endDate(LocalDate.of(2014, 12, 12))
        .fixedRate(0.0125)
        .notional(10_000_000)
        .build();

    return FraTrade.builder()
        .standardId(StandardId.of("example", "1"))
        .product(fra)
        .tradeInfo(TradeInfo.builder()
            .counterparty(StandardId.of("mn", "Dealer B"))
            .settlementDate(LocalDate.of(2014, 9, 14))
            .build())
        .build();
  }

}
