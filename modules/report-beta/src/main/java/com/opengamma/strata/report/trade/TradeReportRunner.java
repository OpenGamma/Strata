/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.report.trade;

import java.time.Instant;
import java.util.List;
import java.util.function.IntFunction;
import java.util.stream.Collectors;

import com.opengamma.strata.collect.result.FailureReason;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.engine.Column;
import com.opengamma.strata.engine.ColumnDefinition;
import com.opengamma.strata.engine.calculations.Results;
import com.opengamma.strata.engine.config.ReportingRules;
import com.opengamma.strata.report.ReportCalculationResults;
import com.opengamma.strata.report.ReportRequirements;
import com.opengamma.strata.report.ReportRunner;
import com.opengamma.strata.report.result.TokenPathEvaluator;

/**
 * Report runner for trade reports.
 * <p>
 * Trade reports are driven by a {@linkplain TradeReportTemplate trade report template}.
 * The resulting report is a table containing one row per trade, and the requested columns each
 * showing a value for that trade.
 */
public class TradeReportRunner implements ReportRunner<TradeReportTemplate> {
  
  private final TokenPathEvaluator pathEvaluator = new TokenPathEvaluator();
  
  @Override
  public ReportRequirements requirements(TradeReportTemplate reportTemplate) {
    List<Column> tradeMeasureColumns = reportTemplate.getColumns().stream()
    .map(this::toColumn)
    .collect(Collectors.toList());
    
    return ReportRequirements.builder()
        .tradeMeasureRequirements(tradeMeasureColumns)
        .build();
  }
  
  @Override
  public TradeReport runReport(ReportCalculationResults calculationResults, TradeReportTemplate reportTemplate) {
    String[] columnHeaders = reportTemplate.getColumns().stream()
        .map(c -> c.getHeader())
        .toArray(i -> new String[i]);
    
    Results results = calculationResults.getCalculationResults();
    Result<?>[][] resultsTable = new Result<?>[calculationResults.getCalculationResults().getRowCount()][reportTemplate.getColumns().size()];
    
    for (int reportColumnIdx = 0; reportColumnIdx < reportTemplate.getColumns().size(); reportColumnIdx++) {
      TradeReportColumn reportColumn = reportTemplate.getColumns().get(reportColumnIdx);
      IntFunction<Result<?>> resultFn;
      if (reportColumn instanceof TradeReportColumn) {
        TradeReportColumn measureColumn = (TradeReportColumn) reportColumn;
        Column calcColumn = toColumn(measureColumn);
        int calcColumnIndex = calculationResults.getColumns().indexOf(calcColumn);
        if (calcColumnIndex > -1) {
          resultFn = i -> {
            Result<?> result = results.get(i, calcColumnIndex);
            if (result.isFailure() || !measureColumn.getPath().isPresent()) {
              return result;
            } else {
              return evaluatePath(result.getValue(), measureColumn.getPath().get());
            }
          };
        } else {
          resultFn = i -> Result.failure(FailureReason.MISSING_DATA, "Missing engine result");
        }
      } else {
        throw new IllegalArgumentException("Unsupported report column type: " + reportColumn.getClass());
      }
      
      for (int i = 0; i < results.getRowCount(); i++) {
        resultsTable[i][reportColumnIdx] = resultFn.apply(i);
      }
    }
    
    return TradeReport.builder()
        .runInstant(Instant.now())
        .valuationDate(calculationResults.getValuationDate())
        .columnHeaders(columnHeaders)
        .results(resultsTable)
        .build();
  }

  private Column toColumn(TradeReportColumn column) {
    return Column.builder()
        .definition(ColumnDefinition.of(column.getMeasure()))
        .reportingRules(ReportingRules.empty())
        .build();
  }
  
  private Result<?> evaluatePath(Object resultValue, List<String> path) {
    Object resultObject = resultValue;
    try {
      return Result.success(pathEvaluator.evaluate(resultObject, path));
    } catch (Exception e) {
      return Result.failure(e);
    }
  }
  
}
