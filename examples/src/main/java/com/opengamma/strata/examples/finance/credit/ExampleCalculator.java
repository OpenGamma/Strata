/*
 * *
 *  * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *  *
 *  * Please see distribution for license.
 *
 *
 */

package com.opengamma.strata.examples.finance.credit;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.market.FxRateId;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.engine.CalculationEngine;
import com.opengamma.strata.engine.CalculationRules;
import com.opengamma.strata.engine.Column;
import com.opengamma.strata.engine.calculations.Results;
import com.opengamma.strata.engine.config.Measure;
import com.opengamma.strata.engine.config.ReportingRules;
import com.opengamma.strata.engine.marketdata.BaseMarketData;
import com.opengamma.strata.examples.engine.ExampleEngine;
import com.opengamma.strata.examples.finance.credit.api.Calculator;
import com.opengamma.strata.examples.finance.credit.api.TradeSource;
import com.opengamma.strata.examples.marketdata.ExampleMarketData;
import com.opengamma.strata.examples.marketdata.MarketDataBuilder;
import com.opengamma.strata.function.OpenGammaPricingRules;
import com.opengamma.strata.market.sensitivity.CurveCurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.CurveCurrencyParameterSensitivity;
import com.opengamma.strata.report.ReportCalculationResults;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class ExampleCalculator implements Calculator {

  private ExampleCalculator() {
  }

  @Override
  public double calculateScalarValue(
      final LocalDate valuationDate,
      final TradeSource tradeSource,
      final Measure measure
  ) {
    Result<?> result = calculateResults(valuationDate, tradeSource, ImmutableList.of(measure)).getItems().get(0);
    Preconditions.checkArgument(
        result.getValue() instanceof CurrencyAmount,
        "Expecting a CurrencyAmount, found " + result.getValue()
    );
    CurrencyAmount value = (CurrencyAmount) result.getValue();
    return value.getAmount();
  }

  @Override
  public double[] calculateVectorValue(LocalDate valuationDate, TradeSource tradeSource, Measure measure) {
    Result<?> result = calculateResults(valuationDate, tradeSource, ImmutableList.of(measure)).getItems().get(0);
    Preconditions.checkArgument(
        result.getValue() instanceof CurveCurrencyParameterSensitivities,
        "Expecting a vector CurveCurrencyParameterSensitivities, found " + result.getValue()
    );
    CurveCurrencyParameterSensitivities value = (CurveCurrencyParameterSensitivities)result.getValue();
    Preconditions.checkArgument(value.getSensitivities().size() == 1);
    return value.getSensitivities().get(0).getSensitivity();
 }

  @Override
  public Results calculateResults(LocalDate valuationDate, TradeSource tradeSource, List<Measure> measures) {

    List<Column> columns = measures.stream().map(s -> Column.of(s)).collect(Collectors.toList());

    // the complete set of rules for calculating measures
    CalculationRules rules = CalculationRules.builder()
        .pricingRules(OpenGammaPricingRules.standard())
        .marketDataRules(ExampleMarketData.builder().rules())
        .reportingRules(ReportingRules.fixedCurrency(Currency.USD))
        .build();

    // Use an empty snapshot of market data, indicating only the valuation date.
    // The engine will attempt to source the data for us, which the example engine is
    // configured to load from JSON resources. We could alternatively populate the snapshot
    // with some or all of the required market data here.
    // TODO The rate is for automatic conversion to the reporting currency. Where should it come from?
    BaseMarketData baseMarketData = BaseMarketData.builder(valuationDate)
        .addValue(FxRateId.of(Currency.GBP, Currency.USD), FxRate.of(Currency.GBP, Currency.USD, 1.61))
        .build();

    // create the engine and calculate the results
    CalculationEngine engine = ExampleEngine.create();
    return engine.calculate(tradeSource.trades(), columns, rules, baseMarketData);

  }

  @Override
  public ReportCalculationResults calculateReportingResults(LocalDate valuationDate, TradeSource tradeSource, List<Measure> measures) {

    List<Column> columns = measures.stream().map(s -> Column.of(s)).collect(Collectors.toList());

    return ReportCalculationResults.of(
        valuationDate,
        tradeSource.trades(),
        columns,
        calculateResults(valuationDate, tradeSource, measures));
  }

  public static Calculator of() {
    return new ExampleCalculator();
  }

}
