/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * <p>
 * Please see distribution for license.
 */
package com.opengamma.strata.report.framework.expression;

import static com.opengamma.strata.collect.Guavate.toImmutableList;

import java.util.List;
import java.util.Set;

import com.opengamma.strata.basics.Trade;
import com.opengamma.strata.calc.Column;
import com.opengamma.strata.calc.config.Measure;
import com.opengamma.strata.collect.result.FailureReason;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.function.StandardComponents;
import com.opengamma.strata.product.Product;
import com.opengamma.strata.product.ProductTrade;
import com.opengamma.strata.product.Security;
import com.opengamma.strata.product.SecurityTrade;
import com.opengamma.strata.report.ReportCalculationResults;

/**
 * Wraps a set of {@link ReportCalculationResults} and exposes the contents of a single row.
 */
class ResultsRow {

  /** The results used to generate a report. */
  private final ReportCalculationResults results;

  /** The index of the row in the result whose data is exposed by this object. */
  private final int rowIndex;

  /**
   * Returns a new instance exposing the data from a single row in the results.
   *
   * @param results  the results used to generate a report
   * @param rowIndex  the index of the row in the result whose data is exposed by this object
   */
  ResultsRow(ReportCalculationResults results, int rowIndex) {
    this.results = results;
    this.rowIndex = rowIndex;
  }

  /**
   * Returns the trade from the row.
   *
   * @return the trade from the row
   */
  Trade getTrade() {
    return results.getTrades().get(rowIndex);
  }

  /**
   * Returns the product from the row.
   * <p>
   * This returns a successful result where the trade associated with the row
   * implements {@link ProductTrade}.
   *
   * @return the product from the row
   */
  Result<Product> getProduct() {
    Trade trade = getTrade();
    if (trade instanceof ProductTrade) {
      return Result.success(((ProductTrade) trade).getProduct());
    }
    return Result.failure(FailureReason.INVALID_INPUT, "Trade does not contain a product");
  }

  /**
   * Returns the security from the row.
   * <p>
   * This returns a successful result where the trade associated with the row
   * implements {@link SecurityTrade}.
   *
   * @return the security from the row
   */
  Result<Security<?>> getSecurity() {
    Trade trade = getTrade();
    if (trade instanceof SecurityTrade) {
      return Result.success(((SecurityTrade<?>) trade).getSecurity());
    }
    return Result.failure(FailureReason.INVALID_INPUT, "Trade does not contain a security");
  }

  /**
   * Returns the result of calculating the named measure for the trade in the row.
   *
   * @param measureName  the name of the measure
   * @return the result of calculating the named measure for the trade in the row
   */
  Result<?> getResult(String measureName) {
    List<String> validMeasureNames = measureNames(results.getTrades().get(rowIndex));
    if (!validMeasureNames.contains(measureName)) {
      return Result.failure(
          FailureReason.INVALID_INPUT,
          "Invalid measure name: {}. Valid measure names: {}",
          measureName,
          validMeasureNames);
    }
    try {
      Column column = Column.of(Measure.of(measureName));
      int columnIndex = results.getColumns().indexOf(column);
      if (columnIndex == -1) {
        return Result.failure(
            FailureReason.INVALID_INPUT,
            "Measure not found in results: '{}'. Valid measure names: {}",
            measureName,
            validMeasureNames);
      }
      Result<?> result = results.getCalculationResults().get(rowIndex, columnIndex);
      if (result.isFailure() && result.getFailure().getReason() == FailureReason.ERROR) {
        return Result.failure(
            FailureReason.INVALID_INPUT,
            "Unable to calculate measure '{}'. Reason: {}",
            measureName,
            validMeasureNames,
            result.getFailure().getMessage());
      }
      return result;

    } catch (IllegalArgumentException ex) {
      return Result.failure(
          FailureReason.INVALID_INPUT,
          "Unable to calculate measure '{}'. Reason: {}. Valid measure names: {}",
          measureName,
          ex.getMessage(),
          validMeasureNames);
    }
  }

  // determine the available measures
  static List<String> measureNames(Trade trade) {
    // TODO The pricing rules should be an argument, not hard-coded to be the standard rules
    Set<Measure> validMeasures = StandardComponents.pricingRules().configuredMeasures(trade);
    return validMeasures.stream()
        .map(Measure::getName)
        .sorted()
        .collect(toImmutableList());
  }

}
