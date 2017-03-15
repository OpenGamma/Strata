/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.capfloor;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.index.IborIndices.EUR_EURIBOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_6M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.time.ZonedDateTime;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.product.common.PutCall;
import com.opengamma.strata.product.rate.IborRateComputation;

/**
 * Test {@link IborCapletFloorletPeriod}.
 */
@Test
public class IborCapletFloorletPeriodTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate FIXING = LocalDate.of(2011, 1, 4);
  private static final ZonedDateTime FIXING_TIME_ZONE = EUR_EURIBOR_3M.calculateFixingDateTime(FIXING);
  private static final double STRIKE = 0.04;
  private static final LocalDate START_UNADJ = LocalDate.of(2010, 10, 8);
  private static final LocalDate END_UNADJ = LocalDate.of(2011, 1, 8);
  private static final LocalDate START = LocalDate.of(2010, 10, 8);
  private static final LocalDate END = LocalDate.of(2011, 1, 10);
  private static final LocalDate PAYMENT = LocalDate.of(2011, 1, 13);
  private static final double NOTIONAL = 1.e6;
  private static final IborRateComputation RATE_COMP = IborRateComputation.of(EUR_EURIBOR_3M, FIXING, REF_DATA);
  private static final double YEAR_FRACTION = 0.251d;

  public void test_builder_min() {
    IborCapletFloorletPeriod test = IborCapletFloorletPeriod.builder()
        .notional(NOTIONAL)
        .startDate(START)
        .endDate(END)
        .yearFraction(YEAR_FRACTION)
        .caplet(STRIKE)
        .iborRate(RATE_COMP)
        .build();
    assertEquals(test.getCaplet().getAsDouble(), STRIKE);
    assertEquals(test.getFloorlet().isPresent(), false);
    assertEquals(test.getStrike(), STRIKE);
    assertEquals(test.getStartDate(), START);
    assertEquals(test.getEndDate(), END);
    assertEquals(test.getPaymentDate(), test.getEndDate());
    assertEquals(test.getCurrency(), EUR);
    assertEquals(test.getNotional(), NOTIONAL);
    assertEquals(test.getIborRate(), RATE_COMP);
    assertEquals(test.getIndex(), EUR_EURIBOR_3M);
    assertEquals(test.getFixingDate(), FIXING_TIME_ZONE.toLocalDate());
    assertEquals(test.getFixingDateTime(), FIXING_TIME_ZONE);
    assertEquals(test.getPutCall(), PutCall.CALL);
    assertEquals(test.getUnadjustedStartDate(), START);
    assertEquals(test.getUnadjustedEndDate(), END);
    assertEquals(test.getYearFraction(), YEAR_FRACTION);
  }

  public void test_builder_full() {
    IborCapletFloorletPeriod test = IborCapletFloorletPeriod.builder()
        .notional(NOTIONAL)
        .startDate(START)
        .endDate(END)
        .unadjustedStartDate(START_UNADJ)
        .unadjustedEndDate(END_UNADJ)
        .paymentDate(PAYMENT)
        .yearFraction(YEAR_FRACTION)
        .currency(GBP)
        .floorlet(STRIKE)
        .iborRate(RATE_COMP)
        .build();
    assertEquals(test.getFloorlet().getAsDouble(), STRIKE);
    assertEquals(test.getCaplet().isPresent(), false);
    assertEquals(test.getStrike(), STRIKE);
    assertEquals(test.getStartDate(), START);
    assertEquals(test.getEndDate(), END);
    assertEquals(test.getUnadjustedStartDate(), START_UNADJ);
    assertEquals(test.getUnadjustedEndDate(), END_UNADJ);
    assertEquals(test.getPaymentDate(), PAYMENT);
    assertEquals(test.getCurrency(), GBP);
    assertEquals(test.getNotional(), NOTIONAL);
    assertEquals(test.getIborRate(), RATE_COMP);
    assertEquals(test.getIndex(), EUR_EURIBOR_3M);
    assertEquals(test.getFixingDateTime(), FIXING_TIME_ZONE);
    assertEquals(test.getPutCall(), PutCall.PUT);
    assertEquals(test.getYearFraction(), YEAR_FRACTION);
  }

  public void test_builder_fail() {
    // rate observation missing
    assertThrowsIllegalArg(() -> IborCapletFloorletPeriod.builder()
        .notional(NOTIONAL)
        .caplet(STRIKE)
        .build());
    // cap and floor missing
    assertThrowsIllegalArg(() -> IborCapletFloorletPeriod.builder()
        .notional(NOTIONAL)
        .iborRate(RATE_COMP)
        .build());
    // cap and floor present
    assertThrowsIllegalArg(() -> IborCapletFloorletPeriod.builder()
        .notional(NOTIONAL)
        .caplet(STRIKE)
        .floorlet(STRIKE)
        .iborRate(RATE_COMP)
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
  static IborCapletFloorletPeriod sut() {
    return IborCapletFloorletPeriod.builder()
        .notional(NOTIONAL)
        .startDate(START)
        .endDate(END)
        .caplet(STRIKE)
        .iborRate(RATE_COMP)
        .build();
  }

  static IborCapletFloorletPeriod sut2() {
    return IborCapletFloorletPeriod.builder()
        .notional(-NOTIONAL)
        .startDate(START.plusDays(1))
        .endDate(END.plusDays(1))
        .floorlet(STRIKE)
        .iborRate(IborRateComputation.of(USD_LIBOR_6M, LocalDate.of(2013, 2, 15), REF_DATA))
        .build();
  }

}
