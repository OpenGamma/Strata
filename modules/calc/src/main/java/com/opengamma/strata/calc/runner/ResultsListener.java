/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.runner;

import static com.opengamma.strata.collect.Guavate.toImmutableList;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.calc.Column;
import com.opengamma.strata.calc.ColumnHeader;
import com.opengamma.strata.calc.Results;
import com.opengamma.strata.collect.result.Result;

/**
 * Calculation listener that receives the results of individual calculations and builds a set of {@link Results}.
 */
public final class ResultsListener extends AggregatingCalculationListener<Results> {

  /** Comparator for sorting the results by row and then column. */
  private static final Comparator<CalculationResult> COMPARATOR =
      Comparator.comparingInt(CalculationResult::getRowIndex)
          .thenComparingInt(CalculationResult::getColumnIndex);

  /** List that is populated with the results as they arrive. */
  private final List<CalculationResult> results = new ArrayList<>();

  /** The columns that define what values are calculated. */
  private List<Column> columns;

  /**
   * Creates a new instance.
   */
  public ResultsListener() {
  }

  @Override
  public void calculationsStarted(List<CalculationTarget> targets, List<Column> columns) {
    this.columns = ImmutableList.copyOf(columns);
  }

  @Override
  public void resultReceived(CalculationTarget target, CalculationResult result) {
    results.add(result);
  }

  @Override
  protected Results createAggregateResult() {
    results.sort(COMPARATOR);
    return buildResults(results, columns);
  }

  /**
   * Builds a set of results from the results of the individual calculations.
   *
   * @param calculationResults the results of the individual calculations
   * @param columns the columns that define what values are calculated
   * @return the results
   */
  private static Results buildResults(List<CalculationResult> calculationResults, List<Column> columns) {
    List<Result<?>> results = calculationResults.stream()
        .map(CalculationResult::getResult)
        .collect(toImmutableList());

    List<ColumnHeader> headers = columns.stream()
        .map(Column::toHeader)
        .collect(toImmutableList());

    return Results.of(headers, results);
  }
}
