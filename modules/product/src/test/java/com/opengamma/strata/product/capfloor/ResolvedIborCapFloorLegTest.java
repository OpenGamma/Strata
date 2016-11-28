/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.capfloor;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.index.IborIndices.EUR_EURIBOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.product.common.PayReceive.PAY;
import static com.opengamma.strata.product.common.PayReceive.RECEIVE;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.product.rate.IborRateComputation;

/**
 * Test {@link ResolvedIborCapFloorLeg}.
 */
@Test
public class ResolvedIborCapFloorLegTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final double STRIKE = 0.0125;
  private static final double NOTIONAL = 1.0e6;
  private static final IborCapletFloorletPeriod PERIOD_1 = IborCapletFloorletPeriod.builder()
      .caplet(STRIKE)
      .notional(NOTIONAL)
      .currency(EUR)
      .startDate(LocalDate.of(2011, 3, 17))
      .endDate(LocalDate.of(2011, 6, 17))
      .unadjustedStartDate(LocalDate.of(2011, 3, 17))
      .unadjustedEndDate(LocalDate.of(2011, 6, 17))
      .paymentDate(LocalDate.of(2011, 6, 21))
      .iborRate(IborRateComputation.of(EUR_EURIBOR_3M, LocalDate.of(2011, 6, 15), REF_DATA))
      .yearFraction(0.2556)
      .build();
  private static final IborCapletFloorletPeriod PERIOD_2 = IborCapletFloorletPeriod.builder()
      .caplet(STRIKE)
      .notional(NOTIONAL)
      .currency(EUR)
      .startDate(LocalDate.of(2011, 6, 17))
      .endDate(LocalDate.of(2011, 9, 19))
      .unadjustedStartDate(LocalDate.of(2011, 6, 17))
      .unadjustedEndDate(LocalDate.of(2011, 9, 17))
      .paymentDate(LocalDate.of(2011, 9, 21))
      .iborRate(IborRateComputation.of(EUR_EURIBOR_3M, LocalDate.of(2011, 9, 15), REF_DATA))
      .yearFraction(0.2611)
      .build();
  private static final IborCapletFloorletPeriod PERIOD_3 = IborCapletFloorletPeriod.builder()
      .caplet(STRIKE)
      .notional(NOTIONAL)
      .currency(EUR)
      .startDate(LocalDate.of(2011, 9, 19))
      .endDate(LocalDate.of(2011, 12, 19))
      .unadjustedStartDate(LocalDate.of(2011, 9, 17))
      .unadjustedEndDate(LocalDate.of(2011, 12, 17))
      .paymentDate(LocalDate.of(2011, 12, 21))
      .iborRate(IborRateComputation.of(EUR_EURIBOR_3M, LocalDate.of(2011, 12, 15), REF_DATA))
      .yearFraction(0.2528)
      .build();
  private static final IborCapletFloorletPeriod PERIOD_4 = IborCapletFloorletPeriod.builder()
      .caplet(STRIKE)
      .notional(NOTIONAL)
      .currency(EUR)
      .startDate(LocalDate.of(2011, 12, 19))
      .endDate(LocalDate.of(2012, 3, 19))
      .unadjustedStartDate(LocalDate.of(2011, 12, 17))
      .unadjustedEndDate(LocalDate.of(2012, 3, 17))
      .paymentDate(LocalDate.of(2012, 3, 21))
      .iborRate(IborRateComputation.of(EUR_EURIBOR_3M, LocalDate.of(2012, 3, 15), REF_DATA))
      .yearFraction(0.2528)
      .build();

  public void test_builder() {
    ResolvedIborCapFloorLeg test = ResolvedIborCapFloorLeg.builder()
        .capletFloorletPeriods(PERIOD_1, PERIOD_2, PERIOD_3, PERIOD_4)
        .payReceive(RECEIVE)
        .build();
    assertEquals(test.getCapletFloorletPeriods(), ImmutableList.of(PERIOD_1, PERIOD_2, PERIOD_3, PERIOD_4));
    assertEquals(test.getPayReceive(), RECEIVE);
    assertEquals(test.getStartDate(), PERIOD_1.getStartDate());
    assertEquals(test.getEndDate(), PERIOD_4.getEndDate());
    assertEquals(test.getFinalFixingDateTime(), PERIOD_4.getFixingDateTime());
    assertEquals(test.getCurrency(), EUR);
    assertEquals(test.getIndex(), EUR_EURIBOR_3M);
  }

  public void test_builder_fail() {
    // two currencies
    IborCapletFloorletPeriod periodGbp = IborCapletFloorletPeriod.builder()
        .caplet(STRIKE)
        .notional(NOTIONAL)
        .currency(GBP)
        .startDate(LocalDate.of(2011, 6, 17))
        .endDate(LocalDate.of(2011, 9, 19))
        .unadjustedStartDate(LocalDate.of(2011, 6, 17))
        .unadjustedEndDate(LocalDate.of(2011, 9, 17))
        .paymentDate(LocalDate.of(2011, 9, 21))
        .iborRate(IborRateComputation.of(EUR_EURIBOR_3M, LocalDate.of(2011, 9, 15), REF_DATA))
        .yearFraction(0.2611)
        .build();
    assertThrowsIllegalArg(() -> ResolvedIborCapFloorLeg.builder()
        .capletFloorletPeriods(PERIOD_1, periodGbp)
        .payReceive(RECEIVE)
        .build());
    // two indices
    IborCapletFloorletPeriod periodLibor = IborCapletFloorletPeriod.builder()
        .caplet(STRIKE)
        .notional(NOTIONAL)
        .currency(EUR)
        .startDate(LocalDate.of(2011, 6, 17))
        .endDate(LocalDate.of(2011, 9, 19))
        .unadjustedStartDate(LocalDate.of(2011, 6, 17))
        .unadjustedEndDate(LocalDate.of(2011, 9, 17))
        .paymentDate(LocalDate.of(2011, 9, 21))
        .iborRate(IborRateComputation.of(GBP_LIBOR_3M, LocalDate.of(2011, 9, 15), REF_DATA))
        .yearFraction(0.2611)
        .build();
    assertThrowsIllegalArg(() -> ResolvedIborCapFloorLeg.builder()
        .capletFloorletPeriods(PERIOD_1, periodLibor)
        .payReceive(RECEIVE)
        .build());
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    ResolvedIborCapFloorLeg test1 = ResolvedIborCapFloorLeg.builder()
        .capletFloorletPeriods(PERIOD_1, PERIOD_2, PERIOD_3, PERIOD_4)
        .payReceive(RECEIVE)
        .build();
    coverImmutableBean(test1);
    ResolvedIborCapFloorLeg test2 = ResolvedIborCapFloorLeg.builder()
        .capletFloorletPeriods(PERIOD_2, PERIOD_3)
        .payReceive(PAY)
        .build();
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    ResolvedIborCapFloorLeg test = ResolvedIborCapFloorLeg.builder()
        .capletFloorletPeriods(PERIOD_1, PERIOD_2, PERIOD_3, PERIOD_4)
        .payReceive(RECEIVE)
        .build();
    assertSerialization(test);
  }

}
