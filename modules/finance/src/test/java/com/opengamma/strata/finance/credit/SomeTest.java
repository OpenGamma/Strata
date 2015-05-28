package com.opengamma.strata.finance.credit;

import com.opengamma.strata.basics.date.BusinessDayConvention;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.date.HolidayCalendar;
import com.opengamma.strata.basics.date.HolidayCalendars;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.StubConvention;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.time.LocalDate;

@Test
public class SomeTest {

  /**
   * http://www.cdsmodel.com/cdsmodel/assets/cds-model/docs/c-code%20Key%20Functions-v1.pdf
   * **************************************************************************
   * Computes the price (a.k.a. upfront charge) for a vanilla CDS
   * <p>
   * Risk starts at the end of today. The PV is computed for a given value date.
   * The CDS starts at startDate and ends at endDate. The last date is always
   * protected - the start date is also protected because internally protectStart
   * is set to True.
   * <p>
   * Interest accrues for the same number of days as there is protection.
   * Since protectStart is set to True you get one extra day of accrued interest in
   * comparison with an interest rate swap. This extra day is assumed to be
   * the last day of the CDS and means that the last period is one day longer
   * than for an interest rate swap.
   * **************************************************************************
   */
  private String CdsModelPriceInputs(
      /** Risk starts at the end of today */
      LocalDate today,
      /** Date for which the PV is calculated and cash settled */
      LocalDate valueDate,
      /** Date when step-in becomes effective */
      LocalDate stepinDate,
      /** Date when protection begins. Either at start or end of day (depends
       on protectStart) */
      LocalDate startDate,
      /** Date when protection ends (end of day) */
      LocalDate endDate,
      /** Fixed coupon rate (a.k.a. spread) for the fee leg */
      double couponRate,
      /** Should accrued interest be paid on default. Usually set to TRUE */
      boolean payAccOnDefault,
      /** Interval between coupon payments. Can be NULL when 3M is assumed */
      Frequency couponInterval,
      /** If the startDate and endDate are not on cycle, then this parameter
       determines location of coupon dates. */
      StubConvention stubType,
      /** Day count convention for coupon payment. Normal is ACT_360 */
      DayCount paymentDcc,
      /** Bad day convention for adjusting coupon payment dates. */
      BusinessDayConvention badDayConv,
      /** Calendar used when adjusting coupon dates. Can be null which equals
       a calendar with no holidays and including weekends. */
      HolidayCalendar calendar,
      /** Key to identify the discount curve, spread curve and recovery rate to use in pricing */
      String curveKey,
      /** Is the price expressed as a clean price (removing accrued interest) */
      boolean isPriceClean) {
    return String.format(
        "%s\n%s\n%s\n%s\n%s\n%f\n%s\n%s\n%s\n%s\n%s\n%s\n%s\n%s",
        today,
        valueDate,
        stepinDate,
        startDate,
        endDate,
        couponRate,
        payAccOnDefault,
        couponInterval,
        stubType,
        paymentDcc,
        badDayConv,
        calendar,
        curveKey,
        isPriceClean
    );
  }


  @Test
  public void test_something() {
    LocalDate tradeDate = LocalDate.now();
    Assert.assertEquals(
        CdsModelPriceInputs(
            tradeDate,
            tradeDate.plusDays(3L),
            tradeDate.plusDays(1L),
            tradeDate,
            tradeDate.plusYears(5L),
            100D,
            true,
            Frequency.P3M,
            StubConvention.NONE,
            DayCounts.ACT_360,
            BusinessDayConventions.FOLLOWING,
            HolidayCalendars.NO_HOLIDAYS,
            "key",
            true
        ),
        "2015-05-26\n" +
            "2015-05-29\n" +
            "2015-05-27\n" +
            "2015-05-26\n" +
            "2020-05-26\n" +
            "100\n" +
            "true\n" +
            "P3M\n" +
            "NONE\n" +
            "ACT/360\n" +
            "FOLLOWING\n" +
            "NO_HOLIDAYS\n" +
            "key\n",
        "true"
    );
  }

}
