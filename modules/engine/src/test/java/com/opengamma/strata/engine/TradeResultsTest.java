/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine;

import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.collect.result.FailureReason;
import com.opengamma.strata.collect.result.Result;

@Test
public class TradeResultsTest {

  public void testTradeIndexMustNotBeNegative() {
    assertThrowsIllegalArg(() -> TradeResults.of(-1, ImmutableList.of()));
  }

  public void testColumnsMustNotBeNull() {
    assertThrowsIllegalArg(() -> TradeResults.of(0, null));
  }

  public void testSize() {
    ImmutableList<Result<?>> columns =
        ImmutableList.of(Result.success(true), Result.failure(FailureReason.ERROR, "oops"));
    TradeResults results = TradeResults.of(0, columns);

    assertThat(results.size()).isEqualTo(2);
  }

  public void testGetColumn() {
    Result<Boolean> r1 = Result.success(true);
    Result<Object> r2 = Result.failure(FailureReason.ERROR, "oops");
    ImmutableList<Result<?>> columns =
        ImmutableList.of(r1, r2);
    TradeResults results = TradeResults.of(0, columns);

    assertThat(results.getColumn(0)).isEqualTo(r1);
    assertThat(results.getColumn(1)).isEqualTo(r2);
  }

  public void testStream() {
    ImmutableList<Result<?>> columns =
        ImmutableList.of(Result.success(true), Result.failure(FailureReason.ERROR, "oops"));
    TradeResults results = TradeResults.of(0, columns);

    assertThat(results.stream().filter(Result::isSuccess).findFirst().get().getValue())
        .isEqualTo(true);
  }

  public void testBuilder() {

    ImmutableList<Result<?>> columns =
        ImmutableList.of(Result.success(true), Result.failure(FailureReason.ERROR, "oops"));
    TradeResults results = TradeResults.builder()
        .tradeIndex(1)
        .columns(columns)
        .build();

    assertThat(results.getTradeIndex()).isEqualTo(1);
    assertThat(results.getColumns()).isEqualTo(columns);

  }

  public void coverage() {

    ImmutableList<Result<?>> columns =
        ImmutableList.of(Result.success(true), Result.failure(FailureReason.ERROR, "oops"));
    TradeResults test = TradeResults.of(0, columns);
    coverImmutableBean(test);

    ImmutableList<Result<?>> columns2 =
        ImmutableList.of(Result.success(true), Result.failure(FailureReason.ERROR, "bother"));
    TradeResults test2 = TradeResults.of(1, columns2);
    coverBeanEquals(test, test2);
  }

}
