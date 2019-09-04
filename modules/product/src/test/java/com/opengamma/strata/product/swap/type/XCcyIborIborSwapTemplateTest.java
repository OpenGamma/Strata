/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap.type;

import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.EUTA;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.USNY;
import static com.opengamma.strata.basics.date.Tenor.TENOR_10Y;
import static com.opengamma.strata.basics.date.Tenor.TENOR_2Y;
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
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.HolidayCalendarId;
import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.product.swap.Swap;
import com.opengamma.strata.product.swap.SwapTrade;

/**
 * Test {@link XCcyIborIborSwapTemplate}.
 */
public class XCcyIborIborSwapTemplateTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final HolidayCalendarId EUTA_USNY = EUTA.combinedWith(USNY);

  private static final double NOTIONAL_2M = 2_000_000d;
  private static final CurrencyPair EUR_USD = CurrencyPair.of(Currency.EUR, Currency.USD);
  private static final double FX_EUR_USD = 1.15d;
  private static final DaysAdjustment PLUS_TWO_DAY = DaysAdjustment.ofBusinessDays(2, EUTA_USNY);
  private static final IborRateSwapLegConvention EUR3M = IborRateSwapLegConvention.builder()
      .index(IborIndices.EUR_EURIBOR_3M)
      .accrualBusinessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, EUTA_USNY))
      .build();
  private static final IborRateSwapLegConvention USD3M = IborRateSwapLegConvention.builder()
      .index(IborIndices.USD_LIBOR_3M)
      .accrualBusinessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, EUTA_USNY))
      .build();
  private static final XCcyIborIborSwapConvention CONV =
      ImmutableXCcyIborIborSwapConvention.of("EUR-EURIBOR-3M-USD-LIBOR-3M", EUR3M, USD3M, PLUS_TWO_DAY);

  //-------------------------------------------------------------------------
  @Test
  public void test_of_spot() {
    XCcyIborIborSwapTemplate test = XCcyIborIborSwapTemplate.of(TENOR_10Y, CONV);
    assertThat(test.getPeriodToStart()).isEqualTo(Period.ZERO);
    assertThat(test.getTenor()).isEqualTo(TENOR_10Y);
    assertThat(test.getConvention()).isEqualTo(CONV);
    assertThat(test.getCurrencyPair()).isEqualTo(EUR_USD);
  }

  @Test
  public void test_of() {
    XCcyIborIborSwapTemplate test = XCcyIborIborSwapTemplate.of(Period.ofMonths(3), TENOR_10Y, CONV);
    assertThat(test.getPeriodToStart()).isEqualTo(Period.ofMonths(3));
    assertThat(test.getTenor()).isEqualTo(TENOR_10Y);
    assertThat(test.getConvention()).isEqualTo(CONV);
    assertThat(test.getCurrencyPair()).isEqualTo(EUR_USD);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_builder_notEnoughData() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> XCcyIborIborSwapTemplate.builder()
            .tenor(TENOR_2Y)
            .build());
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_createTrade() {
    XCcyIborIborSwapTemplate base = XCcyIborIborSwapTemplate.of(Period.ofMonths(3), TENOR_10Y, CONV);
    LocalDate tradeDate = LocalDate.of(2015, 5, 5);
    LocalDate startDate = date(2015, 8, 7);
    LocalDate endDate = date(2025, 8, 7);
    SwapTrade test = base.createTrade(tradeDate, BUY, NOTIONAL_2M, NOTIONAL_2M * FX_EUR_USD, 0.25d, REF_DATA);
    Swap expected = Swap.of(
        EUR3M.toLeg(startDate, endDate, PAY, NOTIONAL_2M, 0.25d),
        USD3M.toLeg(startDate, endDate, RECEIVE, NOTIONAL_2M * FX_EUR_USD));
    assertThat(test.getInfo().getTradeDate()).isEqualTo(Optional.of(tradeDate));
    assertThat(test.getProduct()).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    XCcyIborIborSwapTemplate test = XCcyIborIborSwapTemplate.of(Period.ofMonths(3), TENOR_10Y, CONV);
    coverImmutableBean(test);
    DaysAdjustment bda2 = DaysAdjustment.ofBusinessDays(1, EUTA);
    XCcyIborIborSwapConvention conv2 =
        ImmutableXCcyIborIborSwapConvention.of("XXX", USD3M, EUR3M, bda2);
    XCcyIborIborSwapTemplate test2 = XCcyIborIborSwapTemplate.of(Period.ofMonths(2), TENOR_2Y, conv2);
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    XCcyIborIborSwapTemplate test = XCcyIborIborSwapTemplate.of(Period.ofMonths(3), TENOR_10Y, CONV);
    assertSerialization(test);
  }

}
