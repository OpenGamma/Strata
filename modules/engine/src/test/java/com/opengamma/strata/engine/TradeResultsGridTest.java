/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine;

import static com.opengamma.strata.collect.CollectProjectAssertions.assertThat;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.collect.result.FailureReason;
import com.opengamma.strata.collect.result.Result;

@Test
public class TradeResultsGridTest {

  public void testNullRowsDisallowed() {
    assertThrowsIllegalArg(() -> TradeResultsGrid.of(null));
  }

  public void testRowColumnAccess() {

    TradeResultsGrid grid = TradeResultsGrid.of(
        ImmutableList.of(
            TradeResults.of(0, ImmutableList.of(Result.success(123), Result.failure(FailureReason.ERROR, "oops"))),
            TradeResults.of(1, ImmutableList.<Result<?>>of(Result.success(456), Result.success(89.123)))));

    assertThat(grid.getTradeColumnCell(0, 0)).isSuccess().hasValue(123);
    assertThat(grid.getTradeColumnCell(0, 1)).isFailure(FailureReason.ERROR).hasFailureMessageMatching("oops");
    assertThat(grid.getTradeColumnCell(1, 0)).isSuccess().hasValue(456);
    assertThat(grid.getTradeColumnCell(1, 1)).isSuccess().hasValue(89.123);
  }

  public void testSize() {

    TradeResultsGrid grid = TradeResultsGrid.of(
        ImmutableList.of(
            TradeResults.of(0, ImmutableList.of(Result.success(123), Result.failure(FailureReason.ERROR, "oops"))),
            TradeResults.of(1, ImmutableList.<Result<?>>of(Result.success(456), Result.success(89.123)))));

    assertThat(grid.size()).isEqualTo(2);
  }

  public void testStream() {

    TradeResults tr1 = TradeResults.of(0, ImmutableList.of(Result.success(123), Result.failure(FailureReason.ERROR, "oops")));
    TradeResults tr2 = TradeResults.of(1, ImmutableList.<Result<?>>of(Result.success(456), Result.success(89.123)));
    TradeResultsGrid grid = TradeResultsGrid.of(ImmutableList.of(tr1, tr2));

    assertThat(grid.stream().filter(tr -> tr.getTradeIndex() == 1).findFirst().get()).isEqualTo(tr2);
  }

  public void testBuilder() {

    TradeResults results = TradeResults.of(0, ImmutableList.of(Result.success(true)));
    TradeResultsGrid grid = TradeResultsGrid.builder().rows(results).build();

    assertThat(grid.getRows()).isEqualTo(ImmutableList.of(results));
  }

  public void coverage() {
    coverImmutableBean(TradeResultsGrid.of(
        ImmutableList.of(TradeResults.of(0, ImmutableList.of(Result.success(true))))));
  }

}
