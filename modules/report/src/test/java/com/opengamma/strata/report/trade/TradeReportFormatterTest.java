/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.report.trade;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import org.testng.annotations.Test;

import com.google.common.collect.ArrayTable;
import com.google.common.collect.ImmutableList;
import com.opengamma.strata.collect.result.FailureReason;
import com.opengamma.strata.collect.result.Result;

/**
 * Test {@link TradeReportFormatter}.
 */
@Test
public class TradeReportFormatterTest {

  private static final ImmutableList<Integer> INDICES = ImmutableList.of(0, 1);

  public void getColumnTypes() {
    ArrayTable<Integer, Integer, Result<?>> table = ArrayTable.create(INDICES, INDICES);
    table.put(0, 0, Result.success(1));
    table.put(0, 1, Result.success("abc"));
    table.put(1, 0, Result.success(2));
    table.put(1, 1, Result.success("def"));

    List<Class<?>> columnTypes = TradeReportFormatter.INSTANCE.getColumnTypes(report(table));
    assertThat(columnTypes).isEqualTo(ImmutableList.of(Integer.class, String.class));
  }

  public void getColumnTypesWithSomeFailures() {
    ImmutableList<Integer> indices = ImmutableList.of(0, 1);
    ArrayTable<Integer, Integer, Result<?>> table = ArrayTable.create(indices, indices);
    table.put(0, 0, Result.failure(FailureReason.ERROR, "fail"));
    table.put(0, 1, Result.failure(FailureReason.ERROR, "fail"));
    table.put(1, 0, Result.success(2));
    table.put(1, 1, Result.success("def"));

    List<Class<?>> columnTypes = TradeReportFormatter.INSTANCE.getColumnTypes(report(table));
    assertThat(columnTypes).isEqualTo(ImmutableList.of(Integer.class, String.class));
  }

  public void getColumnTypesWithAllFailures() {
    ImmutableList<Integer> indices = ImmutableList.of(0, 1);
    ArrayTable<Integer, Integer, Result<?>> table = ArrayTable.create(indices, indices);
    table.put(0, 0, Result.failure(FailureReason.ERROR, "fail"));
    table.put(0, 1, Result.failure(FailureReason.ERROR, "fail"));
    table.put(1, 0, Result.failure(FailureReason.ERROR, "fail"));
    table.put(1, 1, Result.failure(FailureReason.ERROR, "fail"));

    List<Class<?>> columnTypes = TradeReportFormatter.INSTANCE.getColumnTypes(report(table));
    assertThat(columnTypes).isEqualTo(ImmutableList.of(Object.class, Object.class));
  }

  private TradeReport report(ArrayTable<Integer, Integer, Result<?>> table) {
    return TradeReport.builder()
        .columns(
            TradeReportColumn.builder().header("col0").build(),
            TradeReportColumn.builder().header("col1").build())
        .data(table)
        .valuationDate(LocalDate.now(ZoneOffset.UTC))
        .runInstant(Instant.now())
        .build();
  }
}
