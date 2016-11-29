/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.report.trade;

import static com.opengamma.strata.collect.Guavate.toImmutableList;

import java.time.Instant;
import java.util.List;
import java.util.stream.IntStream;

import com.google.common.collect.ImmutableTable;
import com.opengamma.strata.calc.Column;
import com.opengamma.strata.collect.Guavate;
import com.opengamma.strata.collect.result.FailureReason;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.report.ReportCalculationResults;
import com.opengamma.strata.report.ReportRequirements;
import com.opengamma.strata.report.ReportRunner;
import com.opengamma.strata.report.framework.expression.ValuePathEvaluator;

/**
 * Report runner for trade reports.
 * <p>
 * Trade reports are driven by a {@linkplain TradeReportTemplate trade report template}.
 * The resulting report is a table containing one row per trade, and the requested columns each
 * showing a value for that trade.
 */
public final class TradeReportRunner
    implements ReportRunner<TradeReportTemplate> {

  /**
   * The single shared instance of this report runner.
   */
  public static final TradeReportRunner INSTANCE = new TradeReportRunner();

  // restricted constructor
  private TradeReportRunner() {
  }

  //-------------------------------------------------------------------------
  @Override
  public ReportRequirements requirements(TradeReportTemplate reportTemplate) {
    List<Column> measureRequirements = reportTemplate.getColumns().stream()
        .map(TradeReportColumn::getValue)
        .flatMap(Guavate::stream)
        .map(ValuePathEvaluator::measure)
        .flatMap(Guavate::stream)
        .map(Column::of)
        .collect(toImmutableList());

    return ReportRequirements.of(measureRequirements);
  }

  @Override
  public TradeReport runReport(ReportCalculationResults results, TradeReportTemplate reportTemplate) {
    ImmutableTable.Builder<Integer, Integer, Result<?>> resultTable = ImmutableTable.builder();

    for (int reportColumnIdx = 0; reportColumnIdx < reportTemplate.getColumns().size(); reportColumnIdx++) {
      TradeReportColumn reportColumn = reportTemplate.getColumns().get(reportColumnIdx);
      List<Result<?>> columnResults;

      if (reportColumn.getValue().isPresent()) {
        columnResults = ValuePathEvaluator.evaluate(reportColumn.getValue().get(), results);
      } else {
        columnResults = IntStream.range(0, results.getTargets().size())
            .mapToObj(i -> Result.failure(FailureReason.INVALID, "No value specified in report template"))
            .collect(toImmutableList());
      }
      int rowCount = results.getCalculationResults().getRowCount();

      for (int rowIdx = 0; rowIdx < rowCount; rowIdx++) {
        resultTable.put(rowIdx, reportColumnIdx, columnResults.get(rowIdx));
      }
    }

    return TradeReport.builder()
        .runInstant(Instant.now())
        .valuationDate(results.getValuationDate())
        .columns(reportTemplate.getColumns())
        .data(resultTable.build())
        .build();
  }

}
