/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap.type;

import static com.opengamma.strata.basics.date.Tenor.TENOR_10Y;
import static com.opengamma.strata.basics.date.Tenor.TENOR_2Y;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_1M;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_6M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.product.common.BuySell.BUY;
import static com.opengamma.strata.product.common.PayReceive.PAY;
import static com.opengamma.strata.product.common.PayReceive.RECEIVE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDate;
import java.time.Period;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.product.swap.Swap;
import com.opengamma.strata.product.swap.SwapTrade;

/**
 * Test {@link IborIborSwapTemplate}.
 */
public class IborIborSwapTemplateTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final double NOTIONAL_2M = 2_000_000d;

  private static final IborRateSwapLegConvention IBOR1M = IborRateSwapLegConvention.of(USD_LIBOR_1M);
  private static final IborRateSwapLegConvention IBOR3M = IborRateSwapLegConvention.of(USD_LIBOR_3M);
  private static final IborRateSwapLegConvention IBOR6M = IborRateSwapLegConvention.of(USD_LIBOR_6M);
  private static final IborIborSwapConvention CONV = ImmutableIborIborSwapConvention.of("USD-Swap", IBOR3M, IBOR6M);
  private static final IborIborSwapConvention CONV2 = ImmutableIborIborSwapConvention.of("USD-Swap2", IBOR1M, IBOR3M);

  //-------------------------------------------------------------------------
  @Test
  public void test_of_spot() {
    IborIborSwapTemplate test = IborIborSwapTemplate.of(TENOR_10Y, CONV);
    assertThat(test.getPeriodToStart()).isEqualTo(Period.ZERO);
    assertThat(test.getTenor()).isEqualTo(TENOR_10Y);
    assertThat(test.getConvention()).isEqualTo(CONV);
  }

  @Test
  public void test_of() {
    IborIborSwapTemplate test = IborIborSwapTemplate.of(Period.ofMonths(3), TENOR_10Y, CONV);
    assertThat(test.getPeriodToStart()).isEqualTo(Period.ofMonths(3));
    assertThat(test.getTenor()).isEqualTo(TENOR_10Y);
    assertThat(test.getConvention()).isEqualTo(CONV);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_builder_notEnoughData() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> IborIborSwapTemplate.builder()
            .tenor(TENOR_2Y)
            .build());
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_createTrade() {
    IborIborSwapTemplate base = IborIborSwapTemplate.of(Period.ofMonths(3), TENOR_10Y, CONV);
    LocalDate tradeDate = LocalDate.of(2015, 5, 5);
    LocalDate startDate = date(2015, 8, 7);
    LocalDate endDate = date(2025, 8, 7);
    SwapTrade test = base.createTrade(tradeDate, BUY, NOTIONAL_2M, 0.25d, REF_DATA);
    Swap expected = Swap.of(
        IBOR3M.toLeg(startDate, endDate, PAY, NOTIONAL_2M, 0.25d),
        IBOR6M.toLeg(startDate, endDate, RECEIVE, NOTIONAL_2M));
    assertThat(test.getInfo().getTradeDate()).isEqualTo(Optional.of(tradeDate));
    assertThat(test.getProduct()).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    IborIborSwapTemplate test = IborIborSwapTemplate.of(Period.ofMonths(3), TENOR_10Y, CONV);
    coverImmutableBean(test);
    IborIborSwapTemplate test2 = IborIborSwapTemplate.of(Period.ofMonths(2), TENOR_2Y, CONV2);
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    IborIborSwapTemplate test = IborIborSwapTemplate.of(Period.ofMonths(3), TENOR_10Y, CONV);
    assertSerialization(test);
  }

}
