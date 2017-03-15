/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.report.framework.expression;

import static com.opengamma.strata.collect.Guavate.toImmutableList;

import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.calc.Column;
import com.opengamma.strata.calc.Measure;
import com.opengamma.strata.calc.runner.CalculationFunctions;
import com.opengamma.strata.collect.result.FailureReason;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.product.GenericSecurityTrade;
import com.opengamma.strata.product.Position;
import com.opengamma.strata.product.Product;
import com.opengamma.strata.product.ProductTrade;
import com.opengamma.strata.product.Security;
import com.opengamma.strata.product.SecurityTrade;
import com.opengamma.strata.product.Trade;
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

  //-------------------------------------------------------------------------
  /**
   * Returns the target from the row.
   *
   * @return the target from the row
   */
  CalculationTarget getTarget() {
    return results.getTargets().get(rowIndex);
  }

  /**
   * Returns the position from the row.
   *
   * @return the position from the row
   */
  Result<Position> getPosition() {
    CalculationTarget target = getTarget();
    if (target instanceof Position) {
      return Result.success((Position) target);
    }
    return Result.failure(FailureReason.INVALID, "Calculaton target is not a position");
  }

  /**
   * Returns the trade from the row.
   *
   * @return the trade from the row
   */
  Result<Trade> getTrade() {
    CalculationTarget target = getTarget();
    if (target instanceof Trade) {
      return Result.success((Trade) target);
    }
    return Result.failure(FailureReason.INVALID, "Calculaton target is not a trade");
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
    CalculationTarget target = getTarget();
    if (target instanceof SecurityTrade) {
      SecurityTrade idTrade = (SecurityTrade) target;
      target = idTrade.resolveSecurity(results.getReferenceData());
    }
    if (target instanceof ProductTrade) {
      return Result.success(((ProductTrade) target).getProduct());
    }
    if (target instanceof Trade) {
      return Result.failure(FailureReason.INVALID, "Trade does not contain a product");
    }
    return Result.failure(FailureReason.INVALID, "Calculaton target is not a trade");
  }

  /**
   * Returns the security from the row.
   * <p>
   * This returns a successful result where the trade associated with the row
   * implements {@link GenericSecurityTrade}.
   *
   * @return the security from the row
   */
  Result<Security> getSecurity() {
    CalculationTarget target = getTarget();
    if (target instanceof SecurityTrade) {
      SecurityTrade secTrade = (SecurityTrade) target;
      Security security = results.getReferenceData().getValue(secTrade.getSecurityId());
      return Result.success(security);
    }
    if (target instanceof GenericSecurityTrade) {
      GenericSecurityTrade secTrade = (GenericSecurityTrade) target;
      return Result.success(secTrade.getSecurity());
    }
    if (target instanceof Trade) {
      return Result.failure(FailureReason.INVALID, "Trade does not contain a security");
    }
    return Result.failure(FailureReason.INVALID, "Calculaton target is not a trade");
  }

  //-------------------------------------------------------------------------
  /**
   * Returns the result of calculating the named measure for the trade in the row.
   *
   * @param measureName  the name of the measure
   * @return the result of calculating the named measure for the trade in the row
   */
  Result<?> getResult(String measureName) {
    List<String> validMeasureNames = measureNames(results.getTargets().get(rowIndex), results.getCalculationFunctions());
    if (!validMeasureNames.contains(measureName)) {
      return Result.failure(
          FailureReason.INVALID,
          "Invalid measure name: {}. Valid measure names: {}",
          measureName,
          validMeasureNames);
    }
    try {
      Column column = Column.of(Measure.of(measureName));
      int columnIndex = results.getColumns().indexOf(column);
      if (columnIndex == -1) {
        return Result.failure(
            FailureReason.INVALID,
            "Measure not found in results: '{}'. Valid measure names: {}",
            measureName,
            validMeasureNames);
      }
      Result<?> result = results.getCalculationResults().get(rowIndex, columnIndex);
      if (result.isFailure() && result.getFailure().getReason() == FailureReason.ERROR) {
        return Result.failure(
            FailureReason.INVALID,
            "Unable to calculate measure '{}'. Reason: {}",
            measureName,
            validMeasureNames,
            result.getFailure().getMessage());
      }
      return result;

    } catch (IllegalArgumentException ex) {
      return Result.failure(
          FailureReason.INVALID,
          "Unable to calculate measure '{}'. Reason: {}. Valid measure names: {}",
          measureName,
          ex.getMessage(),
          validMeasureNames);
    }
  }

  // determine the available measures
  static List<String> measureNames(CalculationTarget target, CalculationFunctions calculationFunctions) {
    Set<Measure> validMeasures = calculationFunctions.findFunction(target)
        .map(fn -> fn.supportedMeasures())
        .orElse(ImmutableSet.of());
    return validMeasures.stream()
        .map(Measure::getName)
        .sorted()
        .collect(toImmutableList());
  }

}
