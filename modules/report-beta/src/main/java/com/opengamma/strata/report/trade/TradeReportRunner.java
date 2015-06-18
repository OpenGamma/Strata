/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.report.trade;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.opengamma.strata.collect.result.FailureReason;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.engine.Column;
import com.opengamma.strata.report.ReportCalculationResults;
import com.opengamma.strata.report.ReportRequirements;
import com.opengamma.strata.report.ReportRunner;
import com.opengamma.strata.report.result.ValuePathEvaluator;

/**
 * Report runner for trade reports.
 * <p>
 * Trade reports are driven by a {@linkplain TradeReportTemplate trade report template}.
 * The resulting report is a table containing one row per trade, and the requested columns each
 * showing a value for that trade.
 */
public class TradeReportRunner implements ReportRunner<TradeReportTemplate> {
  
  /** The evaluator for the report column's value field */
  private final ValuePathEvaluator valuePathEvaluator = new ValuePathEvaluator();

  @Override
  public ReportRequirements requirements(TradeReportTemplate reportTemplate) {
    List<Column> measureRequirements = reportTemplate.getColumns().stream()
        .filter(c -> c.getValue().isPresent())
        .map(c -> valuePathEvaluator.measure(c.getValue().get()))
        .filter(m -> m.isPresent())
        .map(m -> Column.of(m.get()))
        .collect(Collectors.toList());
    
    return ReportRequirements.builder()
        .tradeMeasureRequirements(measureRequirements)
        .build();
  }

  @Override
  public TradeReport runReport(ReportCalculationResults results, TradeReportTemplate reportTemplate) {
    String[] columnHeaders = reportTemplate.getColumns().stream()
        .map(c -> c.getHeader())
        .toArray(i -> new String[i]);
    
    Result<?>[][] dataTable = new Result<?>[results.getCalculationResults().getRowCount()][reportTemplate
        .getColumns().size()];

    for (int reportColumnIdx = 0; reportColumnIdx < reportTemplate.getColumns().size(); reportColumnIdx++) {
      TradeReportColumn reportColumn = reportTemplate.getColumns().get(reportColumnIdx);
      List<Result<?>> columnResults;
      if (reportColumn.getValue().isPresent()) {
        columnResults = valuePathEvaluator.evaluate(reportColumn.getValue().get(), results);
      } else {
        columnResults = IntStream.range(0, results.getTrades().size())
            .mapToObj(i -> Result.failure(FailureReason.INVALID_INPUT, "No value specified in report template"))
            .collect(Collectors.toList());
      }
      for (int rowIdx = 0; rowIdx < dataTable.length; rowIdx++) {
        dataTable[rowIdx][reportColumnIdx] = columnResults.get(rowIdx);
      }
    }

    return TradeReport.builder()
        .runInstant(Instant.now())
        .valuationDate(results.getValuationDate())
        .columns(reportTemplate.getColumns())
        .columnHeaders(columnHeaders)
        .data(dataTable)
        .build();
  }

}
