/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.examples.finance.credit.harness;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.MoreExecutors;
import com.opengamma.strata.basics.Trade;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.calc.CalculationRules;
import com.opengamma.strata.calc.CalculationRunner;
import com.opengamma.strata.calc.Column;
import com.opengamma.strata.calc.config.Measure;
import com.opengamma.strata.calc.config.ReportingCurrency;
import com.opengamma.strata.calc.marketdata.MarketEnvironment;
import com.opengamma.strata.calc.runner.Results;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.examples.marketdata.ExampleMarketData;
import com.opengamma.strata.examples.marketdata.ExampleMarketDataBuilder;
import com.opengamma.strata.function.StandardComponents;
import com.opengamma.strata.market.curve.CurveCurrencyParameterSensitivities;

public class TestCalculator implements Calculator {

  private TestCalculator() {
  }

  @Override
  public double calculateScalarValue(LocalDate valuationDate, TradeSource tradeSource, Measure measure) {
    Result<?> result = calculateResults(valuationDate, tradeSource, ImmutableList.of(measure)).getItems().get(0);
    if (result.getValue() instanceof CurrencyAmount) {
      CurrencyAmount value = (CurrencyAmount) result.getValue();
      return value.getAmount();
    } else if (result.getValue() instanceof Double) {
      Double value = (Double) result.getValue();
      return value;
    } else {
      throw new IllegalStateException("Expecting a CurrencyAmount, found " + result.getValue());
    }
  }

  @Override
  public DoubleArray calculateVectorValue(
      LocalDate valuationDate, TradeSource tradeSource, Measure measure) {

    Result<?> result = calculateResults(valuationDate, tradeSource, ImmutableList.of(measure)).getItems().get(0);
    Preconditions.checkArgument(
        result.getValue() instanceof CurveCurrencyParameterSensitivities,
        "Expecting a vector CurveCurrencyParameterSensitivities, found " + result.getValue());
    CurveCurrencyParameterSensitivities value = (CurveCurrencyParameterSensitivities) result.getValue();
    Preconditions.checkArgument(value.getSensitivities().size() == 1);
    return value.getSensitivities().get(0).getSensitivity();
  }

  @Override
  public Results calculateResults(LocalDate valuationDate, TradeSource tradeSource, List<Measure> measures) {

    // use the built-in example market data
    ExampleMarketDataBuilder marketDataBuilder = ExampleMarketData.builder();

    // the complete set of rules for calculating measures
    CalculationRules rules = CalculationRules.builder()
        .pricingRules(StandardComponents.pricingRules())
        .marketDataRules(marketDataBuilder.rules())
        .reportingCurrency(ReportingCurrency.of(Currency.USD))
        .build();

    MarketEnvironment marketSnapshot = marketDataBuilder.buildSnapshot(valuationDate);

    List<Column> columns = measures.stream().map(Column::of).collect(Collectors.toList());

    // create the engine and calculate the results
    ImmutableList<Trade> trades = ImmutableList.of(tradeSource.apply());
    // using the direct executor means there is no need to close/shutdown the runner
    CalculationRunner runner = CalculationRunner.of(MoreExecutors.newDirectExecutorService());
    return runner.calculateSingleScenario(rules, trades, columns, marketSnapshot);
  }

  public static Calculator of() {
    return new TestCalculator();
  }

}
