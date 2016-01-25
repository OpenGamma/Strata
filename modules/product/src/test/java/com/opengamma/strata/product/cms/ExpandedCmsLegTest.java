/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.cms;

import static com.opengamma.strata.basics.PayReceive.PAY;
import static com.opengamma.strata.basics.PayReceive.RECEIVE;
import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.product.swap.SwapIndex;
import com.opengamma.strata.product.swap.SwapIndices;

/**
 * Test {@link ExpandedCmsLeg}.
 */
@Test
public class ExpandedCmsLegTest {
  private static final SwapIndex INDEX = SwapIndices.GBP_LIBOR_1100_15Y;
  private static final LocalDate DATE_1 = LocalDate.of(2015, 10, 22);
  private static final LocalDate DATE_2 = LocalDate.of(2016, 10, 24);
  private static final LocalDate DATE_3 = LocalDate.of(2017, 10, 23);
  private static final double STRIKE = 0.015;
  private static final double NOTIONAL = 1.0e6;
  private static final double YEAR_FRACTION_1 = 1.005;
  private static final double YEAR_FRACTION_2 = 0.998;

  private static final CmsPeriod PERIOD_1 = CmsPeriod.builder()
      .caplet(STRIKE)
      .index(INDEX)
      .startDate(DATE_1)
      .endDate(DATE_2)
      .notional(NOTIONAL)
      .yearFraction(YEAR_FRACTION_1)
      .dayCount(ACT_360)
      .build();
  private static final CmsPeriod PERIOD_2 = CmsPeriod.builder()
      .caplet(STRIKE).index(INDEX)
      .startDate(DATE_2)
      .endDate(DATE_3)
      .notional(NOTIONAL)
      .yearFraction(YEAR_FRACTION_2)
      .dayCount(ACT_360)
      .build();

  public void test_builder() {
    ExpandedCmsLeg test = ExpandedCmsLeg.builder()
        .payReceive(RECEIVE)
        .cmsPeriods(PERIOD_1, PERIOD_2)
        .build();
    assertEquals(test.getCmsPeriods().size(), 2);
    assertEquals(test.getCmsPeriods().get(0), PERIOD_1);
    assertEquals(test.getCmsPeriods().get(1), PERIOD_2);
    assertEquals(test.getCurrency(), GBP);
    assertEquals(test.getStartDate(), DATE_1);
    assertEquals(test.getEndDate(), DATE_3);
    assertEquals(test.getIndex(), INDEX);
    assertEquals(test.getPayReceive(), RECEIVE);
  }

  public void test_builder_multiCurrencyIndex() {
    CmsPeriod period3 = CmsPeriod.builder()
        .caplet(STRIKE)
        .index(SwapIndices.GBP_LIBOR_1100_10Y)
        .startDate(DATE_2)
        .endDate(DATE_3)
        .notional(NOTIONAL)
        .yearFraction(YEAR_FRACTION_2)
        .dayCount(ACT_360)
        .build();
    assertThrowsIllegalArg(() -> ExpandedCmsLeg.builder().payReceive(RECEIVE).cmsPeriods(PERIOD_1, period3).build());
    CmsPeriod period4 = CmsPeriod.builder()
        .caplet(STRIKE)
        .index(INDEX)
        .startDate(DATE_2)
        .endDate(DATE_3)
        .notional(NOTIONAL)
        .yearFraction(YEAR_FRACTION_2)
        .dayCount(ACT_360)
        .currency(EUR).build();
    assertThrowsIllegalArg(() -> ExpandedCmsLeg.builder().payReceive(RECEIVE).cmsPeriods(PERIOD_1, period4).build());
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    ExpandedCmsLeg test1 = ExpandedCmsLeg.builder()
        .payReceive(RECEIVE)
        .cmsPeriods(PERIOD_1, PERIOD_2)
        .build();
    coverImmutableBean(test1);
    CmsPeriod p1 = CmsPeriod.builder()
        .caplet(STRIKE)
        .index(INDEX)
        .startDate(DATE_1)
        .endDate(DATE_2)
        .notional(-NOTIONAL)
        .yearFraction(YEAR_FRACTION_1)
        .dayCount(ACT_360)
        .build();
    CmsPeriod p2 = CmsPeriod.builder()
        .caplet(STRIKE).index(INDEX)
        .startDate(DATE_2)
        .endDate(DATE_3)
        .notional(-NOTIONAL)
        .yearFraction(YEAR_FRACTION_2)
        .dayCount(ACT_360)
        .build();
    ExpandedCmsLeg test2 = ExpandedCmsLeg.builder()
        .payReceive(PAY)
        .cmsPeriods(p1, p2)
        .build();
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    ExpandedCmsLeg test = ExpandedCmsLeg.builder()
        .payReceive(RECEIVE)
        .cmsPeriods(PERIOD_1, PERIOD_2)
        .build();
    assertSerialization(test);
  }

}
