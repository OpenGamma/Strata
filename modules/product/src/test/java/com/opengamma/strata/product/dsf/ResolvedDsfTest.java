/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.dsf;

import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.strata.basics.date.BusinessDayConventions.PRECEDING;
import static com.opengamma.strata.basics.date.DayCounts.THIRTY_U_360;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.SAT_SUN;
import static com.opengamma.strata.basics.schedule.Frequency.P1M;
import static com.opengamma.strata.basics.schedule.Frequency.P3M;
import static com.opengamma.strata.basics.schedule.Frequency.P6M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.product.common.PayReceive.PAY;
import static com.opengamma.strata.product.common.PayReceive.RECEIVE;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.basics.value.ValueSchedule;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.swap.FixedRateCalculation;
import com.opengamma.strata.product.swap.IborRateCalculation;
import com.opengamma.strata.product.swap.NotionalSchedule;
import com.opengamma.strata.product.swap.PaymentSchedule;
import com.opengamma.strata.product.swap.RateCalculationSwapLeg;
import com.opengamma.strata.product.swap.ResolvedSwap;
import com.opengamma.strata.product.swap.Swap;
import com.opengamma.strata.product.swap.SwapLeg;
import com.opengamma.strata.product.swap.type.FixedIborSwapConventions;

/**
 * Test {@link ResolvedDsf}.
 */
@Test
public class ResolvedDsfTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final Dsf PRODUCT = DsfTest.sut();
  private static final IborIndex INDEX = IborIndices.USD_LIBOR_3M;
  private static final BusinessDayAdjustment BDA_MF = BusinessDayAdjustment.of(MODIFIED_FOLLOWING, SAT_SUN);
  private static final BusinessDayAdjustment BDA_P = BusinessDayAdjustment.of(PRECEDING, SAT_SUN);
  private static final LocalDate START_DATE = LocalDate.of(2014, 9, 12);
  private static final Swap SWAP = FixedIborSwapConventions.USD_FIXED_6M_LIBOR_3M
      .createTrade(START_DATE, Tenor.TENOR_10Y, BuySell.SELL, 1d, 0.015, REF_DATA).getProduct();
  private static final ResolvedSwap RSWAP = PRODUCT.getUnderlyingSwap().resolve(REF_DATA);
  private static final LocalDate LAST_TRADE_DATE = PRODUCT.getLastTradeDate();
  private static final LocalDate DELIVERY_DATE = PRODUCT.getDeliveryDate();
  private static final double NOTIONAL = PRODUCT.getNotional();

  //-------------------------------------------------------------------------
  public void test_builder() {
    ResolvedDsf test = sut();
    assertEquals(test.getDeliveryDate(), DELIVERY_DATE);
    assertEquals(test.getLastTradeDate(), LAST_TRADE_DATE);
    assertEquals(test.getNotional(), NOTIONAL);
    assertEquals(test.getCurrency(), USD);
    assertEquals(test.getUnderlyingSwap(), RSWAP);
  }

  public void test_builder_deliveryAfterStart() {
    assertThrowsIllegalArg(() -> ResolvedDsf.builder()
        .notional(NOTIONAL)
        .deliveryDate(LocalDate.of(2014, 9, 19))
        .lastTradeDate(LAST_TRADE_DATE)
        .underlyingSwap(RSWAP)
        .build());
  }

  public void test_builder_tradeAfterdelivery() {
    assertThrowsIllegalArg(() -> ResolvedDsf.builder()
        .notional(NOTIONAL)
        .deliveryDate(DELIVERY_DATE)
        .lastTradeDate(LocalDate.of(2014, 9, 11))
        .underlyingSwap(RSWAP)
        .build());
  }

  public void test_builder_notUnitNotional() {
    SwapLeg fixedLeg10 = RateCalculationSwapLeg.builder()
        .payReceive(RECEIVE)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(LocalDate.of(2014, 9, 12))
            .endDate(LocalDate.of(2016, 9, 12))
            .frequency(P6M)
            .businessDayAdjustment(BDA_MF)
            .stubConvention(StubConvention.SHORT_INITIAL)
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(P6M)
            .paymentDateOffset(DaysAdjustment.NONE)
            .build())
        .notionalSchedule(NotionalSchedule.of(USD, 10d))
        .calculation(FixedRateCalculation.builder()
            .dayCount(THIRTY_U_360)
            .rate(ValueSchedule.of(0.015))
            .build())
        .build();
    SwapLeg iborLeg500 = RateCalculationSwapLeg.builder()
        .payReceive(PAY)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(LocalDate.of(2014, 9, 12))
            .endDate(LocalDate.of(2016, 9, 12))
            .frequency(P1M)
            .businessDayAdjustment(BDA_MF)
            .stubConvention(StubConvention.SHORT_INITIAL)
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(P3M)
            .paymentDateOffset(DaysAdjustment.NONE)
            .build())
        .notionalSchedule(NotionalSchedule.builder()
            .currency(USD)
            .amount(ValueSchedule.of(500d))
            .finalExchange(true)
            .initialExchange(true)
            .build())
        .calculation(IborRateCalculation.builder()
            .index(INDEX)
            .fixingDateOffset(DaysAdjustment.ofBusinessDays(-2, SAT_SUN, BDA_P))
            .build())
        .build();
    Swap swap1 = Swap.of(fixedLeg10, SWAP.getLeg(PAY).get());
    Swap swap2 = Swap.of(SWAP.getLeg(RECEIVE).get(), iborLeg500);
    assertThrowsIllegalArg(() -> ResolvedDsf.builder()
        .securityId(PRODUCT.getSecurityId())
        .notional(NOTIONAL)
        .deliveryDate(DELIVERY_DATE)
        .lastTradeDate(LAST_TRADE_DATE)
        .underlyingSwap(swap1.resolve(REF_DATA))
        .build());
    assertThrowsIllegalArg(() -> ResolvedDsf.builder()
        .securityId(PRODUCT.getSecurityId())
        .notional(NOTIONAL)
        .deliveryDate(DELIVERY_DATE)
        .lastTradeDate(LAST_TRADE_DATE)
        .underlyingSwap(swap2.resolve(REF_DATA))
        .build());
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverImmutableBean(sut());
    coverBeanEquals(sut(), sut2());
  }

  public void test_serialization() {
    assertSerialization(sut());
  }

  //-------------------------------------------------------------------------
  static ResolvedDsf sut() {
    return PRODUCT.resolve(REF_DATA);
  }

  static ResolvedDsf sut2() {
    return DsfTest.sut2().resolve(REF_DATA);
  }

}
