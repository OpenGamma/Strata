/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.cms;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.swap.ResolvedSwap;
import com.opengamma.strata.product.swap.SwapIndex;
import com.opengamma.strata.product.swap.SwapIndices;
import com.opengamma.strata.product.swap.type.FixedFloatSwapConvention;

/**
 * Test {@link CmsPeriod}.
 */
public class CmsPeriodTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final SwapIndex INDEX = SwapIndices.GBP_LIBOR_1100_15Y;
  private static final LocalDate FIXING = LocalDate.of(2015, 10, 16);
  private static final LocalDate START = LocalDate.of(2015, 10, 22);
  private static final LocalDate END = LocalDate.of(2016, 10, 24);
  private static final LocalDate START_UNADJUSTED = LocalDate.of(2015, 10, 22);
  private static final LocalDate END_UNADJUSTED = LocalDate.of(2016, 10, 22);  // SAT
  private static final LocalDate PAYMENT = LocalDate.of(2016, 10, 26);
  private static final double STRIKE = 0.015;
  private static final double NOTIONAL = 1.0e6;
  private static final double YEAR_FRACTION = 1.005;

  @Test
  public void test_builder_cap() {
    CmsPeriod testCaplet = sutCap();
    assertThat(testCaplet.getCaplet().getAsDouble()).isEqualTo(STRIKE);
    assertThat(testCaplet.getFloorlet().isPresent()).isFalse();
    assertThat(testCaplet.getCmsPeriodType()).isEqualTo(CmsPeriodType.CAPLET);
    assertThat(testCaplet.getCurrency()).isEqualTo(GBP);
    assertThat(testCaplet.getStartDate()).isEqualTo(START);
    assertThat(testCaplet.getEndDate()).isEqualTo(END);
    assertThat(testCaplet.getUnadjustedStartDate()).isEqualTo(START_UNADJUSTED);
    assertThat(testCaplet.getUnadjustedEndDate()).isEqualTo(END_UNADJUSTED);
    assertThat(testCaplet.getFixingDate()).isEqualTo(FIXING);
    assertThat(testCaplet.getPaymentDate()).isEqualTo(PAYMENT);
    assertThat(testCaplet.getIndex()).isEqualTo(INDEX);
    assertThat(testCaplet.getNotional()).isEqualTo(NOTIONAL);
    assertThat(testCaplet.getYearFraction()).isEqualTo(YEAR_FRACTION);
    assertThat(testCaplet.getDayCount()).isEqualTo(ACT_360);
    assertThat(testCaplet.getStrike()).isEqualTo(STRIKE);
  }

  @Test
  public void test_builder_floor() {
    CmsPeriod testFloorlet = sutFloor();
    assertThat(testFloorlet.getCaplet().isPresent()).isFalse();
    assertThat(testFloorlet.getFloorlet().getAsDouble()).isEqualTo(STRIKE);
    assertThat(testFloorlet.getCmsPeriodType()).isEqualTo(CmsPeriodType.FLOORLET);
    assertThat(testFloorlet.getCurrency()).isEqualTo(GBP);
    assertThat(testFloorlet.getStartDate()).isEqualTo(START);
    assertThat(testFloorlet.getEndDate()).isEqualTo(END);
    assertThat(testFloorlet.getUnadjustedStartDate()).isEqualTo(START_UNADJUSTED);
    assertThat(testFloorlet.getUnadjustedEndDate()).isEqualTo(END_UNADJUSTED);
    assertThat(testFloorlet.getFixingDate()).isEqualTo(FIXING);
    assertThat(testFloorlet.getPaymentDate()).isEqualTo(PAYMENT);
    assertThat(testFloorlet.getIndex()).isEqualTo(INDEX);
    assertThat(testFloorlet.getNotional()).isEqualTo(NOTIONAL);
    assertThat(testFloorlet.getYearFraction()).isEqualTo(YEAR_FRACTION);
    assertThat(testFloorlet.getDayCount()).isEqualTo(ACT_360);
    assertThat(testFloorlet.getStrike()).isEqualTo(STRIKE);
  }

  @Test
  public void test_builder_coupon() {
    CmsPeriod testCoupon = sutCoupon();
    assertThat(testCoupon.getCaplet().isPresent()).isFalse();
    assertThat(testCoupon.getFloorlet().isPresent()).isFalse();
    assertThat(testCoupon.getCmsPeriodType()).isEqualTo(CmsPeriodType.COUPON);
    assertThat(testCoupon.getCurrency()).isEqualTo(GBP);
    assertThat(testCoupon.getStartDate()).isEqualTo(START);
    assertThat(testCoupon.getEndDate()).isEqualTo(END);
    assertThat(testCoupon.getUnadjustedStartDate()).isEqualTo(START_UNADJUSTED);
    assertThat(testCoupon.getUnadjustedEndDate()).isEqualTo(END_UNADJUSTED);
    assertThat(testCoupon.getFixingDate()).isEqualTo(FIXING);
    assertThat(testCoupon.getPaymentDate()).isEqualTo(PAYMENT);
    assertThat(testCoupon.getIndex()).isEqualTo(INDEX);
    assertThat(testCoupon.getNotional()).isEqualTo(NOTIONAL);
    assertThat(testCoupon.getYearFraction()).isEqualTo(YEAR_FRACTION);
    assertThat(testCoupon.getDayCount()).isEqualTo(ACT_360);
    assertThat(testCoupon.getStrike()).isEqualTo(0d);
  }

  @Test
  public void test_builder_nonNullCapFloor() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> CmsPeriod.builder()
            .caplet(STRIKE)
            .floorlet(STRIKE)
            .startDate(START)
            .endDate(END)
            .index(INDEX)
            .notional(NOTIONAL)
            .yearFraction(YEAR_FRACTION)
            .dayCount(ACT_360)
            .build());
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverImmutableBean(sutCap());
    coverBeanEquals(sutCap(), sut2());
  }

  @Test
  public void test_serialization() {
    assertSerialization(sutCap());
  }

  @Test
  public void test_toCouponEquivalent() {
    CmsPeriod caplet = sutCap();
    CmsPeriod cpnEquivalent = caplet.toCouponEquivalent();

    assertThat(cpnEquivalent.getCmsPeriodType()).isEqualTo(CmsPeriodType.COUPON);
    assertThat(caplet.getCurrency()).isEqualTo(cpnEquivalent.getCurrency());
    assertThat(caplet.getStartDate()).isEqualTo(cpnEquivalent.getStartDate());
    assertThat(caplet.getEndDate()).isEqualTo(cpnEquivalent.getEndDate());
    assertThat(caplet.getUnadjustedStartDate()).isEqualTo(cpnEquivalent.getUnadjustedStartDate());
    assertThat(caplet.getUnadjustedEndDate()).isEqualTo(cpnEquivalent.getUnadjustedEndDate());
    assertThat(caplet.getFixingDate()).isEqualTo(cpnEquivalent.getFixingDate());
    assertThat(caplet.getPaymentDate()).isEqualTo(cpnEquivalent.getPaymentDate());
    assertThat(caplet.getIndex()).isEqualTo(cpnEquivalent.getIndex());
    assertThat(caplet.getNotional()).isEqualTo(cpnEquivalent.getNotional());
    assertThat(caplet.getYearFraction()).isEqualTo(cpnEquivalent.getYearFraction());
    assertThat(caplet.getDayCount()).isEqualTo(cpnEquivalent.getDayCount());
  }

  //-------------------------------------------------------------------------
  static CmsPeriod sutCap() {
    FixedFloatSwapConvention conv = INDEX.getTemplate().getConvention();
    ResolvedSwap swap = conv.toTrade(FIXING, START, END, BuySell.BUY, 1d, 0.01).getProduct().resolve(REF_DATA);
    return CmsPeriod.builder()
        .currency(GBP)
        .notional(NOTIONAL)
        .startDate(START)
        .endDate(END)
        .unadjustedStartDate(START_UNADJUSTED)
        .unadjustedEndDate(END_UNADJUSTED)
        .yearFraction(YEAR_FRACTION)
        .paymentDate(PAYMENT)
        .fixingDate(FIXING)
        .caplet(STRIKE)
        .dayCount(ACT_360)
        .index(INDEX)
        .underlyingSwap(swap)
        .build();
  }

  static CmsPeriod sutFloor() {
    FixedFloatSwapConvention conv = INDEX.getTemplate().getConvention();
    ResolvedSwap swap = conv.toTrade(FIXING, START, END, BuySell.BUY, 1d, 0.01).getProduct().resolve(REF_DATA);
    return CmsPeriod.builder()
        .currency(GBP)
        .notional(NOTIONAL)
        .startDate(START)
        .endDate(END)
        .unadjustedStartDate(START_UNADJUSTED)
        .unadjustedEndDate(END_UNADJUSTED)
        .yearFraction(YEAR_FRACTION)
        .paymentDate(PAYMENT)
        .fixingDate(FIXING)
        .floorlet(STRIKE)
        .dayCount(ACT_360)
        .index(INDEX)
        .underlyingSwap(swap)
        .build();
  }

  static CmsPeriod sutCoupon() {
    FixedFloatSwapConvention conv = INDEX.getTemplate().getConvention();
    ResolvedSwap swap = conv.toTrade(FIXING, START, END, BuySell.BUY, 1d, 0.01).getProduct().resolve(REF_DATA);
    return CmsPeriod.builder()
        .currency(GBP)
        .notional(NOTIONAL)
        .startDate(START)
        .endDate(END)
        .unadjustedStartDate(START_UNADJUSTED)
        .unadjustedEndDate(END_UNADJUSTED)
        .yearFraction(YEAR_FRACTION)
        .paymentDate(PAYMENT)
        .fixingDate(FIXING)
        .dayCount(ACT_360)
        .index(INDEX)
        .underlyingSwap(swap)
        .build();
  }

  static CmsPeriod sut2() {
    FixedFloatSwapConvention conv = INDEX.getTemplate().getConvention();
    ResolvedSwap swap = conv.toTrade(FIXING.plusDays(1), START.plusDays(1), END.plusDays(1), BuySell.BUY, 1d, 1d)
        .getProduct().resolve(REF_DATA);
    return CmsPeriod.builder()
        .currency(EUR)
        .notional(NOTIONAL + 1)
        .startDate(START.plusDays(1))
        .endDate(END.plusDays(1))
        .unadjustedStartDate(START_UNADJUSTED.plusDays(1))
        .unadjustedEndDate(END_UNADJUSTED.plusDays(1))
        .yearFraction(YEAR_FRACTION + 0.01)
        .paymentDate(PAYMENT.plusDays(1))
        .fixingDate(FIXING.plusDays(1))
        .floorlet(STRIKE)
        .dayCount(ACT_365F)
        .index(SwapIndices.EUR_EURIBOR_1100_5Y)
        .underlyingSwap(swap)
        .build();
  }

}
