/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.report.trade;

import static com.opengamma.strata.collect.Guavate.toImmutableList;

import java.util.List;
import java.util.stream.IntStream;

import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.report.framework.format.FormatCategory;
import com.opengamma.strata.report.framework.format.FormatSettings;
import com.opengamma.strata.report.framework.format.ReportFormatter;
import com.opengamma.strata.report.framework.format.ReportOutputFormat;
import com.opengamma.strata.report.framework.format.ValueFormatters;

/**
 * Formatter for trade reports.
 */
public final class TradeReportFormatter
    extends ReportFormatter<TradeReport> {

  /**
   * The single shared instance of this report formatter.
   */
  public static final TradeReportFormatter INSTANCE = new TradeReportFormatter();

  // restricted constructor
  private TradeReportFormatter() {
    super(FormatSettings.of(FormatCategory.TEXT, ValueFormatters.UNSUPPORTED));
  }

  //-------------------------------------------------------------------------
  @Override
  protected List<Class<?>> getColumnTypes(TradeReport report) {
    return IntStream.range(0, report.getColumnCount())
        .mapToObj(columnIndex -> columnType(report, columnIndex))
        .collect(toImmutableList());
  }

  // TODO This would be unnecessary if measures had a data type
  /**
   * Returns the data type for the values in a column of a trade report.
   * <p>
   * The results in the column are examined and the type of the first successful value is returned. If all values
   * are failures then {@code Object.class} is returned.
   *
   * @param report  a trade report
   * @param columnIndex  the index of a column in the report
   * @return the data type of the values in the column or {@code Object.class} if all results are failures
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  private static Class<?> columnType(TradeReport report, int columnIndex) {
    return report.getData().rowKeySet().stream()
        .map(rowIndex -> report.getData().get(rowIndex, columnIndex))
        .filter(Result::isSuccess)
        .map(Result::getValue)
        .map(Object::getClass)
        .findFirst()
        .orElse((Class) Object.class);  // raw type needed for Eclipse
  }

  @Override
  protected String formatData(TradeReport report, int rowIdx, int colIdx, ReportOutputFormat format) {
    TradeReportColumn templateColumn = report.getColumns().get(colIdx);
    Result<?> result = report.getData().get(rowIdx, colIdx);

    if (result.isFailure()) {
      return templateColumn.isIgnoreFailures() ? "" : Messages.format("FAIL: {}", result.getFailure().getMessage());
    }
    Object value = result.getValue();
    return formatValue(value, format);
  }

}
