/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * <p>
 * Please see distribution for license.
 */
package com.opengamma.strata.examples.finance.credit.harness;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.engine.CalculationEngine;
import com.opengamma.strata.engine.CalculationRules;
import com.opengamma.strata.engine.Column;
import com.opengamma.strata.engine.calculations.Results;
import com.opengamma.strata.engine.config.Measure;
import com.opengamma.strata.engine.config.ReportingRules;
import com.opengamma.strata.engine.marketdata.BaseMarketData;
import com.opengamma.strata.examples.engine.ExampleEngine;
import com.opengamma.strata.examples.marketdata.ExampleMarketData;
import com.opengamma.strata.examples.marketdata.MarketDataBuilder;
import com.opengamma.strata.function.OpenGammaPricingRules;
import com.opengamma.strata.market.sensitivity.CurveCurrencyParameterSensitivities;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

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
  public double[] calculateVectorValue(
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
    MarketDataBuilder marketDataBuilder = ExampleMarketData.builder();

    // the complete set of rules for calculating measures
    CalculationRules rules = CalculationRules.builder()
        .pricingRules(OpenGammaPricingRules.standard())
        .marketDataRules(marketDataBuilder.rules())
        .reportingRules(ReportingRules.fixedCurrency(Currency.USD))
        .build();

    BaseMarketData baseMarketData = marketDataBuilder.buildSnapshot(valuationDate);

    List<Column> columns = measures.stream().map(s -> Column.of(s)).collect(Collectors.toList());

    // create the engine and calculate the results
    CalculationEngine engine = ExampleEngine.create();
    return engine.calculate(ImmutableList.of(tradeSource.apply()), columns, rules, baseMarketData);

  }

  public static Calculator of() {
    return new TestCalculator();
  }

}
