/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.cms;

import static com.opengamma.strata.basics.currency.Currency.CHF;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.BuySell;
import com.opengamma.strata.product.swap.SwapIndex;
import com.opengamma.strata.product.swap.SwapIndices;

/**
 * Test {@link CmsPeriod}.
 */
@Test
public class CmsPeriodTest {
  private static final SwapIndex INDEX = SwapIndices.GBP_LIBOR_1100_15Y;
  private static final LocalDate START = LocalDate.of(2015, 10, 22);
  private static final LocalDate END = LocalDate.of(2016, 10, 24);
  private static final LocalDate START_UNADJUSTED = LocalDate.of(2015, 10, 22);
  private static final LocalDate END_UNADJUSTED = LocalDate.of(2016, 10, 22);  // SAT
  private static final LocalDate PAYMENT = LocalDate.of(2016, 10, 26);
  private static final LocalDate FIXING = LocalDate.of(2015, 10, 19);
  private static final double STRIKE = 0.015;
  private static final double NOTIONAL = 1.0e6;
  private static final double YEAR_FRACTION = 1.005;

  public void test_builder_full() {
    CmsPeriod testCaplet = CmsPeriod.builder()
        .caplet(STRIKE)
        .currency(GBP)
        .startDate(START)
        .endDate(END)
        .unadjustedStartDate(START_UNADJUSTED)
        .unadjustedEndDate(END_UNADJUSTED)
        .fixingDate(FIXING)
        .paymentDate(PAYMENT)
        .index(INDEX)
        .notional(NOTIONAL)
        .yearFraction(YEAR_FRACTION)
        .build();
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
    assertEquals(testCaplet.getUnderlyingSwap(), INDEX.getTemplate().getConvention()
        .toTrade(START, START, START.plus(INDEX.getTemplate().getTenor()), BuySell.BUY, 1d, 1d).getProduct());
    CmsPeriod testFloorlet = CmsPeriod.builder()
        .floorlet(STRIKE)
        .currency(GBP)
        .startDate(START)
        .endDate(END)
        .unadjustedStartDate(START_UNADJUSTED)
        .unadjustedEndDate(END_UNADJUSTED)
        .fixingDate(FIXING)
        .paymentDate(PAYMENT)
        .index(INDEX)
        .notional(NOTIONAL)
        .yearFraction(YEAR_FRACTION)
        .build();
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
    assertEquals(testFloorlet.getUnderlyingSwap(), INDEX.getTemplate().getConvention()
        .toTrade(START, START, START.plus(INDEX.getTemplate().getTenor()), BuySell.BUY, 1d, 1d).getProduct());
    CmsPeriod testCoupon = CmsPeriod.builder()
        .currency(GBP)
        .startDate(START)
        .endDate(END)
        .unadjustedStartDate(START_UNADJUSTED)
        .unadjustedEndDate(END_UNADJUSTED)
        .fixingDate(FIXING)
        .paymentDate(PAYMENT)
        .index(INDEX)
        .notional(NOTIONAL)
        .yearFraction(YEAR_FRACTION)
        .build();
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
    assertEquals(testCoupon.getUnderlyingSwap(), INDEX.getTemplate().getConvention()
        .toTrade(START, START, START.plus(INDEX.getTemplate().getTenor()), BuySell.BUY, 1d, 1d).getProduct());
  }

  public void test_builder_min() {
    CmsPeriod testCaplet = CmsPeriod.builder()
        .caplet(STRIKE)
        .startDate(START)
        .endDate(END)
        .index(INDEX)
        .notional(NOTIONAL)
        .yearFraction(YEAR_FRACTION)
        .build();
    assertEquals(testCaplet.getCaplet().getAsDouble(), STRIKE);
    assertFalse(testCaplet.getFloorlet().isPresent());
    assertEquals(testCaplet.getCmsPeriodType(), CmsPeriodType.CAPLET);
    assertEquals(testCaplet.getCurrency(), GBP);
    assertEquals(testCaplet.getStartDate(), START);
    assertEquals(testCaplet.getEndDate(), END);
    assertEquals(testCaplet.getUnadjustedStartDate(), START);
    assertEquals(testCaplet.getUnadjustedEndDate(), END);
    assertEquals(testCaplet.getFixingDate(), START);
    assertEquals(testCaplet.getPaymentDate(), END);
    assertEquals(testCaplet.getIndex(), INDEX);
    assertEquals(testCaplet.getNotional(), NOTIONAL);
    assertEquals(testCaplet.getYearFraction(), YEAR_FRACTION);
    assertEquals(testCaplet.getUnderlyingSwap(), INDEX.getTemplate().getConvention()
        .toTrade(START, START, START.plus(INDEX.getTemplate().getTenor()), BuySell.BUY, 1d, 1d).getProduct());
    CmsPeriod testFloorlet = CmsPeriod.builder()
        .floorlet(STRIKE)
        .startDate(START)
        .endDate(END)
        .index(INDEX)
        .notional(NOTIONAL)
        .yearFraction(YEAR_FRACTION)
        .build();
    assertFalse(testFloorlet.getCaplet().isPresent());
    assertEquals(testFloorlet.getFloorlet().getAsDouble(), STRIKE);
    assertEquals(testFloorlet.getCmsPeriodType(), CmsPeriodType.FLOORLET);
    assertEquals(testFloorlet.getCurrency(), GBP);
    assertEquals(testFloorlet.getStartDate(), START);
    assertEquals(testFloorlet.getEndDate(), END);
    assertEquals(testFloorlet.getUnadjustedStartDate(), START);
    assertEquals(testFloorlet.getUnadjustedEndDate(), END);
    assertEquals(testFloorlet.getFixingDate(), START);
    assertEquals(testFloorlet.getPaymentDate(), END);
    assertEquals(testFloorlet.getIndex(), INDEX);
    assertEquals(testFloorlet.getNotional(), NOTIONAL);
    assertEquals(testFloorlet.getYearFraction(), YEAR_FRACTION);
    assertEquals(testFloorlet.getUnderlyingSwap(), INDEX.getTemplate().getConvention()
        .toTrade(START, START, START.plus(INDEX.getTemplate().getTenor()), BuySell.BUY, 1d, 1d).getProduct());
    CmsPeriod testCoupon = CmsPeriod.builder()
        .currency(GBP)
        .startDate(START)
        .endDate(END)
        .index(INDEX)
        .notional(NOTIONAL)
        .yearFraction(YEAR_FRACTION)
        .build();
    assertFalse(testCoupon.getCaplet().isPresent());
    assertFalse(testCoupon.getFloorlet().isPresent());
    assertEquals(testCoupon.getCmsPeriodType(), CmsPeriodType.COUPON);
    assertEquals(testCoupon.getCurrency(), GBP);
    assertEquals(testCoupon.getStartDate(), START);
    assertEquals(testCoupon.getEndDate(), END);
    assertEquals(testCoupon.getUnadjustedStartDate(), START);
    assertEquals(testCoupon.getUnadjustedEndDate(), END);
    assertEquals(testCoupon.getFixingDate(), START);
    assertEquals(testCoupon.getPaymentDate(), END);
    assertEquals(testCoupon.getIndex(), INDEX);
    assertEquals(testCoupon.getNotional(), NOTIONAL);
    assertEquals(testCoupon.getYearFraction(), YEAR_FRACTION);
    assertEquals(testCoupon.getUnderlyingSwap(), INDEX.getTemplate().getConvention()
        .toTrade(START, START, START.plus(INDEX.getTemplate().getTenor()), BuySell.BUY, 1d, 1d).getProduct());
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
        .build());
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    CmsPeriod test1 = CmsPeriod.builder()
        .caplet(STRIKE)
        .startDate(START)
        .endDate(END)
        .index(INDEX)
        .notional(NOTIONAL)
        .yearFraction(YEAR_FRACTION)
        .build();
    coverImmutableBean(test1);
    CmsPeriod test2 = CmsPeriod.builder()
        .floorlet(STRIKE)
        .currency(CHF)
        .startDate(LocalDate.of(2014, 11, 22))
        .endDate(LocalDate.of(2015, 11, 22))
        .index(SwapIndices.CHF_LIBOR_1100_5Y)
        .notional(1.0e7)
        .yearFraction(0.51)
        .build();
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    CmsPeriod test = CmsPeriod.builder()
        .caplet(STRIKE)
        .startDate(START)
        .endDate(END)
        .index(INDEX)
        .notional(NOTIONAL)
        .yearFraction(YEAR_FRACTION)
        .build();
    assertSerialization(test);
  }

}
