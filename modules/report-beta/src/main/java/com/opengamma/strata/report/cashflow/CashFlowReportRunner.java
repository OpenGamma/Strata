/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.report.cashflow;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.engine.Column;
import com.opengamma.strata.engine.config.Measure;
import com.opengamma.strata.market.explain.ExplainKey;
import com.opengamma.strata.market.explain.ExplainMap;
import com.opengamma.strata.report.Report;
import com.opengamma.strata.report.ReportCalculationResults;
import com.opengamma.strata.report.ReportRequirements;
import com.opengamma.strata.report.ReportRunner;

/**
 * Report runner for cash flow reports.
 */
public class CashFlowReportRunner implements ReportRunner<CashFlowReportTemplate> {

  @Override
  public ReportRequirements requirements(CashFlowReportTemplate reportTemplate) {
    return ReportRequirements.builder()
        .tradeMeasureRequirements(Column.of(Measure.EXPLAIN_PRESENT_VALUE))
        .build();
  }

  @Override
  public Report runReport(ReportCalculationResults calculationResults, CashFlowReportTemplate reportTemplate) {
    Result<?> result = calculationResults.getCalculationResults().get(0, 0);
    ExplainMap explainMap = (ExplainMap) result.getValue();

    List<ExplainMap> flatMap = flatten(explainMap);

    List<ExplainKey<?>> keys = getKeys(flatMap);
    String[] headers = keys.stream()
        .map(k -> k.toString())
        .toArray(size -> new String[size]);

    Object[][] data = getData(flatMap, keys);

    return CashFlowReport.builder()
        .runInstant(Instant.now())
        .valuationDate(calculationResults.getValuationDate())
        .columnKeys(keys)
        .columnHeaders(headers)
        .data(data)
        .build();
  }

  private List<ExplainMap> flatten(ExplainMap explainMap) {
    List<ExplainMap> flattenedMap = new ArrayList<ExplainMap>();
    flatten(explainMap, ImmutableMap.of(), flattenedMap);
    return flattenedMap;
  }

  @SuppressWarnings("unchecked")
  private void flatten(ExplainMap explainMap, Map<ExplainKey<?>, Object> expandedParentRow, List<ExplainMap> accumulator) {
    Set<ExplainKey<List<ExplainMap>>> nestedListKeys = explainMap.getMap().keySet().stream()
        .filter(k -> List.class.isAssignableFrom(explainMap.get(k).get().getClass()))
        .map(k -> (ExplainKey<List<ExplainMap>>) k)
        .collect(Collectors.toSet());

    ImmutableMap.Builder<ExplainKey<?>, Object> rowBuilder = ImmutableMap.builder();
    rowBuilder.putAll(expandedParentRow);

    if (!nestedListKeys.isEmpty()) {
      if (nestedListKeys.size() > 1) {
        throw new IllegalArgumentException();
      }
      ExplainKey<List<ExplainMap>> nestedListKey = Iterables.getOnlyElement(nestedListKeys);
      rowBuilder.putAll(Maps.filterKeys(explainMap.getMap(), k -> !k.equals(nestedListKey)));
      List<ExplainMap> nestedList = explainMap.get(nestedListKey).get();
      Map<ExplainKey<?>, Object> currentRow = rowBuilder.build();
      for (ExplainMap nestedListItem : nestedList) {
        flatten(nestedListItem, currentRow, accumulator);
      }
    } else {
      rowBuilder.putAll(explainMap.getMap());
      accumulator.add(ExplainMap.of(rowBuilder.build()));
    }
  }

  private List<ExplainKey<?>> getKeys(List<ExplainMap> explainMap) {
    return explainMap.stream()
        .flatMap(m -> m.getMap().keySet().stream())
        .distinct()
        .collect(Collectors.toList());
  }

  private Object[][] getData(List<ExplainMap> flatMap, List<ExplainKey<?>> keys) {
    Object[][] data = new Object[flatMap.size()][keys.size()];
    for (int rowIdx = 0; rowIdx < data.length; rowIdx++) {
      ExplainMap rowMap = flatMap.get(rowIdx);
      for (int colIdx = 0; colIdx < keys.size(); colIdx++) {
        data[rowIdx][colIdx] = rowMap.get(keys.get(colIdx));
      }
    }
    return data;
  }

}
