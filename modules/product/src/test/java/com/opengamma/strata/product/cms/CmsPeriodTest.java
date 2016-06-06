/**
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
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.swap.ResolvedSwap;
import com.opengamma.strata.product.swap.SwapIndex;
import com.opengamma.strata.product.swap.SwapIndices;
import com.opengamma.strata.product.swap.type.FixedIborSwapConvention;

/**
 * Test {@link CmsPeriod}.
 */
@Test
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

  public void test_builder_cap() {
    CmsPeriod testCaplet = sutCap();
    assertEquals(testCaplet.getCaplet().getAsDouble(), STRIKE);
    assertFalse(testCaplet.getFloorlet().isPresent());
    assertEquals(testCaplet.getCmsPeriodType(), CmsPeriodType.CAPLET);
    assertEquals(testCaplet.getCurrency(), GBP);
    assertEquals(testCaplet.getStartDate(), START);
    assertEquals(testCaplet.getEndDate(), END);
    assertEquals(testCaplet.getUnadjustedStartDate(), START_UNADJUSTED);
    assertEquals(testCaplet.getUnadjustedEndDate(), END_UNADJUSTED);
    assertEquals(testCaplet.getFixingDate(), FIXING);
    assertEquals(testCaplet.getPaymentDate(), PAYMENT);
    assertEquals(testCaplet.getIndex(), INDEX);
    assertEquals(testCaplet.getNotional(), NOTIONAL);
    assertEquals(testCaplet.getYearFraction(), YEAR_FRACTION);
    assertEquals(testCaplet.getDayCount(), ACT_360);
    assertEquals(testCaplet.getStrike(), STRIKE);
  }

  public void test_builder_floor() {
    CmsPeriod testFloorlet = sutFloor();
    assertFalse(testFloorlet.getCaplet().isPresent());
    assertEquals(testFloorlet.getFloorlet().getAsDouble(), STRIKE);
    assertEquals(testFloorlet.getCmsPeriodType(), CmsPeriodType.FLOORLET);
    assertEquals(testFloorlet.getCurrency(), GBP);
    assertEquals(testFloorlet.getStartDate(), START);
    assertEquals(testFloorlet.getEndDate(), END);
    assertEquals(testFloorlet.getUnadjustedStartDate(), START_UNADJUSTED);
    assertEquals(testFloorlet.getUnadjustedEndDate(), END_UNADJUSTED);
    assertEquals(testFloorlet.getFixingDate(), FIXING);
    assertEquals(testFloorlet.getPaymentDate(), PAYMENT);
    assertEquals(testFloorlet.getIndex(), INDEX);
    assertEquals(testFloorlet.getNotional(), NOTIONAL);
    assertEquals(testFloorlet.getYearFraction(), YEAR_FRACTION);
    assertEquals(testFloorlet.getDayCount(), ACT_360);
    assertEquals(testFloorlet.getStrike(), STRIKE);
  }

  public void test_builder_coupon() {
    CmsPeriod testCoupon = sutCoupon();
    assertFalse(testCoupon.getCaplet().isPresent());
    assertFalse(testCoupon.getFloorlet().isPresent());
    assertEquals(testCoupon.getCmsPeriodType(), CmsPeriodType.COUPON);
    assertEquals(testCoupon.getCurrency(), GBP);
    assertEquals(testCoupon.getStartDate(), START);
    assertEquals(testCoupon.getEndDate(), END);
    assertEquals(testCoupon.getUnadjustedStartDate(), START_UNADJUSTED);
    assertEquals(testCoupon.getUnadjustedEndDate(), END_UNADJUSTED);
    assertEquals(testCoupon.getFixingDate(), FIXING);
    assertEquals(testCoupon.getPaymentDate(), PAYMENT);
    assertEquals(testCoupon.getIndex(), INDEX);
    assertEquals(testCoupon.getNotional(), NOTIONAL);
    assertEquals(testCoupon.getYearFraction(), YEAR_FRACTION);
    assertEquals(testCoupon.getDayCount(), ACT_360);
    assertEquals(testCoupon.getStrike(), 0d);
  }

  public void test_builder_nonNullCapFloor() {
    assertThrowsIllegalArg(() -> CmsPeriod.builder()
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
  public void coverage() {
    coverImmutableBean(sutCap());
    coverBeanEquals(sutCap(), sut2());
  }

  public void test_serialization() {
    assertSerialization(sutCap());
  }

  public void test_toCouponEquivalent() {
    CmsPeriod caplet = sutCap();
    CmsPeriod cpnEquivalent = caplet.toCouponEquivalent();

    assertEquals(cpnEquivalent.getCmsPeriodType(), CmsPeriodType.COUPON);
    assertEquals(caplet.getCurrency(), cpnEquivalent.getCurrency());
    assertEquals(caplet.getStartDate(), cpnEquivalent.getStartDate());
    assertEquals(caplet.getEndDate(), cpnEquivalent.getEndDate());
    assertEquals(caplet.getUnadjustedStartDate(), cpnEquivalent.getUnadjustedStartDate());
    assertEquals(caplet.getUnadjustedEndDate(), cpnEquivalent.getUnadjustedEndDate());
    assertEquals(caplet.getFixingDate(), cpnEquivalent.getFixingDate());
    assertEquals(caplet.getPaymentDate(), cpnEquivalent.getPaymentDate());
    assertEquals(caplet.getIndex(), cpnEquivalent.getIndex());
    assertEquals(caplet.getNotional(), cpnEquivalent.getNotional());
    assertEquals(caplet.getYearFraction(), cpnEquivalent.getYearFraction());
    assertEquals(caplet.getDayCount(), cpnEquivalent.getDayCount());
  }

  //-------------------------------------------------------------------------
  static CmsPeriod sutCap() {
    FixedIborSwapConvention conv = INDEX.getTemplate().getConvention();
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
    FixedIborSwapConvention conv = INDEX.getTemplate().getConvention();
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
    FixedIborSwapConvention conv = INDEX.getTemplate().getConvention();
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
    FixedIborSwapConvention conv = INDEX.getTemplate().getConvention();
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
