/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.amount;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.util.List;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.currency.FxRate;

/**
 * Test {@link CashFlows}.
 */
@Test
public class CashFlowsTest {

  private static final double TOLERANCE = 1e-8;
  private static final LocalDate PAYMENT_DATE_1 = LocalDate.of(2015, 6, 22);
  private static final LocalDate PAYMENT_DATE_2 = LocalDate.of(2015, 12, 21);
  private static final double FORECAST_VALUE_1 = 0.0132;
  private static final double FORECAST_VALUE_2 = -0.0108;
  private static final double FORECAST_VALUE_3 = 0.0126;
  private static final double DISCOUNT_FACTOR_1 = 0.96d;
  private static final double DISCOUNT_FACTOR_2 = 0.9d;

  private static final CashFlow CASH_FLOW_1 = CashFlow.ofForecastValue(PAYMENT_DATE_1, USD, FORECAST_VALUE_1, DISCOUNT_FACTOR_1);
  private static final CashFlow CASH_FLOW_2 = CashFlow.ofForecastValue(PAYMENT_DATE_1, GBP, FORECAST_VALUE_2, DISCOUNT_FACTOR_1);
  private static final CashFlow CASH_FLOW_3 = CashFlow.ofForecastValue(PAYMENT_DATE_2, USD, FORECAST_VALUE_3, DISCOUNT_FACTOR_2);

  //-------------------------------------------------------------------------
  public void test_of_singleFlow() {
    CashFlows test = CashFlows.of(CASH_FLOW_1);
    assertEquals(test.getCashFlows().size(), 1);
    assertEquals(test.getCashFlows().get(0), CASH_FLOW_1);
    assertEquals(test.getCashFlow(0), CASH_FLOW_1);
  }

  public void test_of_listFlows() {
    List<CashFlow> list = ImmutableList.<CashFlow>builder().add(CASH_FLOW_1, CASH_FLOW_2).build();
    CashFlows test = CashFlows.of(list);
    assertEquals(test.getCashFlows(), list);
    assertEquals(test.getCashFlow(0), CASH_FLOW_1);
    assertEquals(test.getCashFlow(1), CASH_FLOW_2);
  }

  //-------------------------------------------------------------------------
  public void test_combinedWith_singleFlow() {
    CashFlows base = CashFlows.of(CASH_FLOW_1);
    CashFlows test = base.combinedWith(CASH_FLOW_2);
    CashFlows expected = CashFlows.of(ImmutableList.of(CASH_FLOW_1, CASH_FLOW_2));
    assertEquals(test, expected);
  }

  public void test_combinedWith_listFlows() {
    CashFlows base = CashFlows.of(CASH_FLOW_1);
    CashFlows other = CashFlows.of(ImmutableList.of(CASH_FLOW_2, CASH_FLOW_3));
    CashFlows test = base.combinedWith(other);
    CashFlows expected = CashFlows.of(ImmutableList.of(CASH_FLOW_1, CASH_FLOW_2, CASH_FLOW_3));
    assertEquals(test, expected);
  }

  //-------------------------------------------------------------------------
  public void test_sorted_listFlows() {
    CashFlows base = CashFlows.of(ImmutableList.of(CASH_FLOW_1, CASH_FLOW_2, CASH_FLOW_3));
    CashFlows test = base.sorted();
    CashFlows expected = CashFlows.of(ImmutableList.of(CASH_FLOW_2, CASH_FLOW_1, CASH_FLOW_3));
    assertEquals(test, expected);
    assertEquals(test.sorted(), test);
  }

  //-------------------------------------------------------------------------
  public void test_convertedTo() {
    CashFlows base = CashFlows.of(ImmutableList.of(CASH_FLOW_1, CASH_FLOW_2));
    CashFlows test = base.convertedTo(USD, FxRate.of(GBP, USD, 1.5));
    assertEquals(test.getCashFlow(0), CASH_FLOW_1);
    CashFlow converted = test.getCashFlow(1);
    assertEquals(converted.getPaymentDate(), CASH_FLOW_2.getPaymentDate());
    assertEquals(converted.getDiscountFactor(), CASH_FLOW_2.getDiscountFactor(), TOLERANCE);
    assertEquals(converted.getPresentValue().getCurrency(), USD);
    assertEquals(converted.getPresentValue().getAmount(), CASH_FLOW_2.getPresentValue().getAmount() * 1.5, TOLERANCE);
    assertEquals(converted.getForecastValue().getCurrency(), USD);
    assertEquals(converted.getForecastValue().getAmount(), CASH_FLOW_2.getForecastValue().getAmount() * 1.5, TOLERANCE);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    CashFlows test1 = CashFlows.of(CASH_FLOW_1);
    coverImmutableBean(test1);
    CashFlows test2 = CashFlows.of(ImmutableList.of(CASH_FLOW_2, CASH_FLOW_3));
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    CashFlows test = CashFlows.of(ImmutableList.of(CASH_FLOW_1, CASH_FLOW_2, CASH_FLOW_3));
    assertSerialization(test);
  }

}
