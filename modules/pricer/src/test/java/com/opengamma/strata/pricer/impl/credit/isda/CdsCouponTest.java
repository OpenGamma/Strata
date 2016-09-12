/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.credit.isda;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import java.time.LocalDate;
import java.time.Period;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.date.BusinessDayConvention;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.date.HolidayCalendar;
import com.opengamma.strata.basics.date.HolidayCalendars;
import com.opengamma.strata.basics.schedule.StubConvention;

/**
 * 
 */
@Test
public class CdsCouponTest {

  /**
   * 
   */
  @SuppressWarnings("unused")
  @Test
  public void yearFractionTest() {
    final DayCount curveDcc = DayCounts.ACT_365F;
    final DayCount accrDcc = DayCounts.ACT_360;
    final HolidayCalendar calender = HolidayCalendars.SAT_SUN;
    final BusinessDayConvention following = BusinessDayConventions.FOLLOWING;

    final LocalDate tradeDate = LocalDate.of(2011, 4, 22);

    {
      final LocalDate accStart = ImmDateLogic.getPrevIMMDate(tradeDate);
      final LocalDate accEnd = following.adjust(accStart.plusMonths(3), calender);
      final LocalDate settle = accEnd;
      final boolean protectStart = true;

      final double toStart = -curveDcc.yearFraction(accStart.minusDays(1), tradeDate);
      final double toEnd = curveDcc.yearFraction(tradeDate, accEnd.minusDays(1));
      final double toSettle = curveDcc.yearFraction(tradeDate, settle);
      final double fromStartToEnd = accrDcc.yearFraction(accStart, accEnd);
      final double yFracRatio = fromStartToEnd / curveDcc.yearFraction(accStart, accEnd);

      final CdsCoupon cp = new CdsCoupon(tradeDate, accStart, accEnd, settle, protectStart);
      assertEquals(toStart, cp.getEffStart());
      assertEquals(toEnd, cp.getEffEnd());
      assertEquals(toSettle, cp.getPaymentTime());
      assertEquals(fromStartToEnd, cp.getYearFrac());
      assertEquals(yFracRatio, cp.getYFRatio());

      final CdsCoupon cpFromTriplet = new CdsCoupon(tradeDate, accStart, accEnd, settle);
      final CdsCoupon cpFromTripletWithDcc = new CdsCoupon(tradeDate, accStart, accEnd, settle, protectStart, accrDcc, curveDcc);
      final CdsCoupon cpFromArrayWithDcc = new CdsCoupon(tradeDate, new LocalDate[] {accStart, accEnd, settle }, protectStart, accrDcc, curveDcc);

      assertEquals(cp, cpFromTriplet);
      assertEquals(cp, cpFromTripletWithDcc);
      assertEquals(cp, cpFromArrayWithDcc);

      final LocalDate mat = accStart.plusYears(5);
      final IsdaPremiumLegSchedule sch = new IsdaPremiumLegSchedule(accStart, mat, Period.ofMonths(3), StubConvention.SHORT_INITIAL, following, calender, protectStart);
      final CdsCoupon[] coupons = CdsCoupon.makeCoupons(tradeDate, sch, protectStart, accrDcc, curveDcc);
      assertEquals(cp.getEffStart(), coupons[0].getEffStart());
      assertEquals(cp.getEffEnd(), coupons[0].getEffEnd());
      assertEquals(cp.getPaymentTime(), coupons[0].getPaymentTime());
      assertEquals(cp.getYearFrac(), coupons[0].getYearFrac());
      assertEquals(cp.getYFRatio(), coupons[0].getYFRatio());

      /*
       * Error expected
       */
      try {
        new CdsCoupon(tradeDate, new LocalDate[] {accStart, accEnd }, protectStart, accrDcc, curveDcc);
        throw new RuntimeException();
      } catch (Exception e) {
        assertTrue(e instanceof IllegalArgumentException);
      }

      /*
       * hashCode & equals
       */

      final CdsCoupon cp1 = new CdsCoupon(tradeDate, accStart, accEnd, settle, protectStart, curveDcc, curveDcc);
      final CdsCoupon cp2 = new CdsCoupon(tradeDate, accStart.plusDays(4), accEnd, settle, protectStart);
      final CdsCoupon cp3 = new CdsCoupon(tradeDate, accStart, accEnd.plusDays(4), settle, protectStart);
      final CdsCoupon cp4 = new CdsCoupon(tradeDate, accStart, accEnd, settle.plusDays(5), protectStart);

      assertTrue(cp.equals(cpFromTriplet));
      assertEquals(cp.hashCode(), cpFromTriplet.hashCode());

      assertTrue(cp.equals(cp));
      assertTrue(!(cp.equals(null)));
      assertTrue(!(cp.equals(new double[] {})));

      assertTrue(cp.hashCode() != cp1.hashCode());
      assertTrue(!(cp.equals(cp1)));

      assertTrue(cp.hashCode() != cp2.hashCode());
      assertTrue(!(cp.equals(cp2)));

      assertTrue(cp.hashCode() != cp3.hashCode());
      assertTrue(!(cp.equals(cp3)));

      assertTrue(cp.hashCode() != cp4.hashCode());
      assertTrue(!(cp.equals(cp4)));
    }

    {
      final LocalDate accStart = ImmDateLogic.getNextIMMDate(tradeDate);
      final LocalDate accEnd = following.adjust(accStart.plusMonths(3), calender);
      final LocalDate settle = accEnd;
      final boolean protectStart = false;

      final double toStart = curveDcc.yearFraction(tradeDate, accStart);
      final double toEnd = curveDcc.yearFraction(tradeDate, accEnd);
      final double toSettle = curveDcc.yearFraction(tradeDate, settle);
      final double fromStartToEnd = accrDcc.yearFraction(accStart, accEnd);
      final double yFracRatio = fromStartToEnd / curveDcc.yearFraction(accStart, accEnd);

      final CdsCoupon cp = new CdsCoupon(tradeDate, accStart, accEnd, settle, protectStart);
      assertEquals(toStart, cp.getEffStart());
      assertEquals(toEnd, cp.getEffEnd());
      assertEquals(toSettle, cp.getPaymentTime());
      assertEquals(fromStartToEnd, cp.getYearFrac());
      assertEquals(yFracRatio, cp.getYFRatio());
    }
  }
}
