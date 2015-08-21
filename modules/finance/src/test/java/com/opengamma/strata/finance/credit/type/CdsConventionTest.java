/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * <p>
 * Please see distribution for license.
 */
package com.opengamma.strata.finance.credit.type;

import static com.opengamma.strata.basics.BuySell.BUY;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.BusinessDayConventions.FOLLOWING;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.HolidayCalendars.USNY;
import static com.opengamma.strata.basics.schedule.Frequency.P3M;
import static com.opengamma.strata.basics.schedule.RollConventions.DAY_20;
import static com.opengamma.strata.basics.schedule.StubConvention.SHORT_INITIAL;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.Period;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.finance.credit.CdsTestUtils;
import com.opengamma.strata.finance.credit.RestructuringClause;
import com.opengamma.strata.finance.credit.SeniorityLevel;

/**
 * Test {@link CdsConvention}.
 */
@Test
public class CdsConventionTest {

  //-------------------------------------------------------------------------
  public void test_of() {
    CdsConvention sut = CdsConventions.NORTH_AMERICAN_USD;
    assertEquals(sut, CdsConvention.of("NorthAmericanUsd"));
    assertEquals(sut.getName(), "NorthAmericanUsd");
    assertEquals(sut.getCurrency(), USD);
    assertEquals(sut.getDayCount(), ACT_360);
    assertEquals(sut.getBusinessDayAdjustment(), BusinessDayAdjustment.of(FOLLOWING, USNY));
    assertEquals(sut.getPaymentFrequency(), P3M);
    assertEquals(sut.getRollConvention(), DAY_20);
    assertTrue(sut.getPayAccruedOnDefault());
    assertEquals(sut.getStubConvention(), SHORT_INITIAL);
    assertEquals(sut.getStepIn(), 1);
    assertEquals(sut.getSettleLag(), 3);
  }

  //-------------------------------------------------------------------------
  public void test_unadjusted_maturity_date_from_valuation_date() {
    CdsConvention sut = CdsConventions.NORTH_AMERICAN_USD;
    assertEquals(sut.getUnadjustedMaturityDateFromValuationDate(date(2014, 9, 19), Period.ofYears(5)), date(2019, 9, 20));
    assertEquals(sut.getUnadjustedMaturityDateFromValuationDate(date(2014, 9, 20), Period.ofYears(5)), date(2019, 9, 20));
    assertEquals(sut.getUnadjustedMaturityDateFromValuationDate(date(2014, 9, 21), Period.ofYears(5)), date(2019, 12, 20));
    assertEquals(sut.getUnadjustedMaturityDateFromValuationDate(date(2014, 10, 16), Period.ofYears(5)), date(2019, 12, 20));
    assertEquals(sut.getUnadjustedMaturityDateFromValuationDate(date(2015, 10, 16), Period.ofYears(5)), date(2020, 12, 20));
  }

  public void test_unadjusted_maturity_date() {
    assertEquals(CdsConvention.getUnadjustedMaturityDate(date(2014, 9, 19), P3M, Period.ofYears(5)), date(2019, 9, 20));
    assertEquals(CdsConvention.getUnadjustedMaturityDate(date(2014, 9, 20), P3M, Period.ofYears(5)), date(2019, 9, 20));
    assertEquals(CdsConvention.getUnadjustedMaturityDate(date(2014, 9, 21), P3M, Period.ofYears(5)), date(2019, 12, 20));
    assertEquals(CdsConvention.getUnadjustedMaturityDate(date(2014, 10, 16), P3M, Period.ofYears(5)), date(2019, 12, 20));
  }

  public void test_unadjusted_start_date() {
    assertEquals(CdsConvention.getUnadjustedAccrualStartDate(date(2014, 9, 19)), date(2014, 6, 20));
    assertEquals(CdsConvention.getUnadjustedAccrualStartDate(date(2014, 9, 20)), date(2014, 6, 20));
    assertEquals(CdsConvention.getUnadjustedAccrualStartDate(date(2014, 9, 21)), date(2014, 9, 20));
    assertEquals(CdsConvention.getUnadjustedAccrualStartDate(date(2014, 10, 16)), date(2014, 9, 20));
  }

  public void test_adjusted_start_date() {
    CdsConvention sut = CdsConventions.NORTH_AMERICAN_USD;
    assertEquals(sut.getAdjustedStartDate(date(2014, 9, 19)), date(2014, 6, 20));
    assertEquals(sut.getAdjustedStartDate(date(2014, 9, 20)), date(2014, 6, 20));
    assertEquals(sut.getAdjustedStartDate(date(2014, 9, 21)), date(2014, 9, 22));
    assertEquals(sut.getAdjustedStartDate(date(2014, 10, 16)), date(2014, 9, 22));
  }

  public void test_adjusted_settle_date() {
    CdsConvention sut = CdsConventions.NORTH_AMERICAN_USD;
    assertEquals(sut.getAdjustedSettleDate(date(2014, 9, 19)), date(2014, 9, 24));
    assertEquals(sut.getAdjustedSettleDate(date(2014, 9, 20)), date(2014, 9, 24));
    assertEquals(sut.getAdjustedSettleDate(date(2014, 9, 21)), date(2014, 9, 24));
    assertEquals(sut.getAdjustedSettleDate(date(2014, 9, 22)), date(2014, 9, 25));
    assertEquals(sut.getAdjustedSettleDate(date(2014, 10, 16)), date(2014, 10, 21));
  }

  public void test_unadjusted_step_in_date() {
    CdsConvention sut = CdsConventions.NORTH_AMERICAN_USD;
    assertEquals(sut.getUnadjustedStepInDate(date(2014, 9, 19)), date(2014, 9, 20));
    assertEquals(sut.getUnadjustedStepInDate(date(2014, 9, 20)), date(2014, 9, 21));
    assertEquals(sut.getUnadjustedStepInDate(date(2014, 9, 21)), date(2014, 9, 22));
    assertEquals(sut.getUnadjustedStepInDate(date(2014, 10, 16)), date(2014, 10, 17));
  }

  //-------------------------------------------------------------------------
  public void test_single_name() {
    CdsConvention sut = CdsConventions.NORTH_AMERICAN_USD;
    assertEquals(
        sut.toSingleNameTrade(
            date(2014, 3, 20),
            date(2019, 6, 20),
            BUY,
            100_000_000d,
            0.00100,
            StandardId.of("Test", "Test1"),
            SeniorityLevel.SENIOR_UNSECURED_FOREIGN,
            RestructuringClause.NO_RESTRUCTURING_2014,
            1_000_000d,
            date(2014, 3, 23)),
        CdsTestUtils.singleNameTrade());
  }

  public void test_index() {
    CdsConvention sut = CdsConventions.NORTH_AMERICAN_USD;
    assertEquals(
        sut.toIndexTrade(
            date(2014, 3, 20),
            date(2019, 6, 20),
            BUY,
            100_000_000d,
            0.00100,
            StandardId.of("Test", "Test1"),
            32,
            8,
            1_000_000d,
            date(2014, 3, 23)),
        CdsTestUtils.indexTrade());
  }

}
