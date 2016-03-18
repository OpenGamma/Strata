/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * <p>
 * Please see distribution for license.
 */
package com.opengamma.strata.product.credit.type;

import static com.opengamma.strata.basics.BuySell.BUY;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.BusinessDayConventions.FOLLOWING;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.USNY;
import static com.opengamma.strata.basics.schedule.Frequency.P3M;
import static com.opengamma.strata.basics.schedule.RollConventions.DAY_20;
import static com.opengamma.strata.basics.schedule.StubConvention.SHORT_INITIAL;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.time.Period;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.market.ReferenceData;
import com.opengamma.strata.basics.market.StandardId;
import com.opengamma.strata.product.credit.CdsTestUtils;
import com.opengamma.strata.product.credit.IndexReferenceInformation;
import com.opengamma.strata.product.credit.RestructuringClause;
import com.opengamma.strata.product.credit.SeniorityLevel;
import com.opengamma.strata.product.credit.SingleNameReferenceInformation;

/**
 * Test {@link CdsConvention}.
 */
@Test
public class CdsConventionTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();

  //-------------------------------------------------------------------------
  public void test_of() {
    CdsConvention sut = CdsConventions.USD_NORTH_AMERICAN;
    assertEquals(sut, CdsConvention.of("USD-NorthAmerican"));
    assertEquals(sut.getName(), "USD-NorthAmerican");
    assertEquals(sut.getCurrency(), USD);
    assertEquals(sut.getDayCount(), ACT_360);
    assertEquals(sut.getBusinessDayAdjustment(), BusinessDayAdjustment.of(FOLLOWING, USNY));
    assertEquals(sut.getPaymentFrequency(), P3M);
    assertEquals(sut.getRollConvention(), DAY_20);
    assertEquals(sut.isPayAccruedOnDefault(), true);
    assertEquals(sut.getStubConvention(), SHORT_INITIAL);
    assertEquals(sut.getStepInDays(), 1);
    assertEquals(sut.getSettleLagDays(), 3);
  }

  //-------------------------------------------------------------------------
  public void test_unadjusted_maturity_date_from_valuation_date() {
    CdsConvention sut = CdsConventions.USD_NORTH_AMERICAN;
    assertEquals(sut.calculateUnadjustedMaturityDateFromValuationDate(date(2014, 9, 19), Period.ofYears(5)), date(2019, 9, 20));
    assertEquals(sut.calculateUnadjustedMaturityDateFromValuationDate(date(2014, 9, 20), Period.ofYears(5)), date(2019, 9, 20));
    assertEquals(sut.calculateUnadjustedMaturityDateFromValuationDate(date(2014, 9, 21), Period.ofYears(5)), date(2019, 12, 20));
    assertEquals(sut.calculateUnadjustedMaturityDateFromValuationDate(date(2014, 10, 16), Period.ofYears(5)), date(2019, 12, 20));
    assertEquals(sut.calculateUnadjustedMaturityDateFromValuationDate(date(2015, 10, 16), Period.ofYears(5)), date(2020, 12, 20));
  }

  public void test_unadjusted_maturity_date() {
    assertEquals(CdsConvention.calculateUnadjustedMaturityDate(date(2014, 9, 19), P3M, Period.ofYears(5)), date(2019, 9, 20));
    assertEquals(CdsConvention.calculateUnadjustedMaturityDate(date(2014, 9, 20), P3M, Period.ofYears(5)), date(2019, 9, 20));
    assertEquals(CdsConvention.calculateUnadjustedMaturityDate(date(2014, 9, 21), P3M, Period.ofYears(5)), date(2019, 12, 20));
    assertEquals(CdsConvention.calculateUnadjustedMaturityDate(date(2014, 10, 16), P3M, Period.ofYears(5)), date(2019, 12, 20));
  }

  public void test_unadjusted_start_date() {
    assertEquals(CdsConvention.calculateUnadjustedAccrualStartDate(date(2014, 9, 19)), date(2014, 6, 20));
    assertEquals(CdsConvention.calculateUnadjustedAccrualStartDate(date(2014, 9, 20)), date(2014, 6, 20));
    assertEquals(CdsConvention.calculateUnadjustedAccrualStartDate(date(2014, 9, 21)), date(2014, 9, 20));
    assertEquals(CdsConvention.calculateUnadjustedAccrualStartDate(date(2014, 10, 16)), date(2014, 9, 20));
  }

  public void test_adjusted_start_date() {
    CdsConvention sut = CdsConventions.USD_NORTH_AMERICAN;
    assertEquals(sut.calculateAdjustedStartDate(date(2014, 9, 19), REF_DATA), date(2014, 6, 20));
    assertEquals(sut.calculateAdjustedStartDate(date(2014, 9, 20), REF_DATA), date(2014, 6, 20));
    assertEquals(sut.calculateAdjustedStartDate(date(2014, 9, 21), REF_DATA), date(2014, 9, 22));
    assertEquals(sut.calculateAdjustedStartDate(date(2014, 10, 16), REF_DATA), date(2014, 9, 22));
  }

  public void test_adjusted_settle_date() {
    CdsConvention sut = CdsConventions.USD_NORTH_AMERICAN;
    assertEquals(sut.calculateAdjustedSettleDate(date(2014, 9, 19), REF_DATA), date(2014, 9, 24));
    assertEquals(sut.calculateAdjustedSettleDate(date(2014, 9, 20), REF_DATA), date(2014, 9, 24));
    assertEquals(sut.calculateAdjustedSettleDate(date(2014, 9, 21), REF_DATA), date(2014, 9, 24));
    assertEquals(sut.calculateAdjustedSettleDate(date(2014, 9, 22), REF_DATA), date(2014, 9, 25));
    assertEquals(sut.calculateAdjustedSettleDate(date(2014, 10, 16), REF_DATA), date(2014, 10, 21));
  }

  public void test_unadjusted_step_in_date() {
    CdsConvention sut = CdsConventions.USD_NORTH_AMERICAN;
    assertEquals(sut.calculateUnadjustedStepInDate(date(2014, 9, 19)), date(2014, 9, 20));
    assertEquals(sut.calculateUnadjustedStepInDate(date(2014, 9, 20)), date(2014, 9, 21));
    assertEquals(sut.calculateUnadjustedStepInDate(date(2014, 9, 21)), date(2014, 9, 22));
    assertEquals(sut.calculateUnadjustedStepInDate(date(2014, 10, 16)), date(2014, 10, 17));
  }

  //-------------------------------------------------------------------------
  public void test_toTrade_singleName() {
    CdsConvention sut = CdsConventions.USD_NORTH_AMERICAN;
    assertEquals(
        sut.toTrade(
            date(2014, 3, 20),
            date(2019, 6, 20),
            BUY,
            100_000_000d,
            0.00100,
            SingleNameReferenceInformation.of(
                StandardId.of("Test", "Test1"),
                SeniorityLevel.SENIOR_UNSECURED_FOREIGN,
                USD,
                RestructuringClause.NO_RESTRUCTURING_2014),
            1_000_000d,
            date(2014, 3, 23)),
        CdsTestUtils.singleNameTrade());
  }

  public void test_toTrade_index() {
    CdsConvention sut = CdsConventions.USD_NORTH_AMERICAN;
    assertEquals(
        sut.toTrade(
            date(2014, 3, 20),
            date(2019, 6, 20),
            BUY,
            100_000_000d,
            0.00100,
            IndexReferenceInformation.of(StandardId.of("Test", "Test1"), 32, 8),
            1_000_000d,
            date(2014, 3, 23)),
        CdsTestUtils.indexTrade());
  }

}
