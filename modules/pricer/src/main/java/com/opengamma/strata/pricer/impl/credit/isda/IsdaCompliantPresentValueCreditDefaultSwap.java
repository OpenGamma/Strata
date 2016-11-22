/**
 * Copyright (C) 2012 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.credit.isda;

import static com.opengamma.strata.pricer.impl.credit.isda.IsdaCompliantScheduleGenerator.getIntegrationNodesAsDates;
import static com.opengamma.strata.pricer.impl.credit.isda.IsdaCompliantScheduleGenerator.truncateList;

import java.time.LocalDate;
import java.time.Period;

import com.opengamma.strata.basics.date.BusinessDayConvention;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.date.HolidayCalendar;
import com.opengamma.strata.basics.date.HolidayCalendars;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.math.MathException;

/**
 *This prices a CDS using the ISDA methodology. The API of the public functions mimic as far a possible the ISDA high level ISDA c
 *functions. However this is NOT a line-by-line translation of the ISDA code. We find agreement with ISDA to better than 1 part in 10^12
 *on a test suit of 200 example.
 */
public class IsdaCompliantPresentValueCreditDefaultSwap {

  @SuppressWarnings("unused")
  private static final int DEFAULT_CASH_SETTLEMENT_DAYS = 3;
  private static final BusinessDayConvention FOLLOWING = BusinessDayConventions.FOLLOWING;
  private static final HolidayCalendar DEFAULT_CALENDAR = HolidayCalendars.SAT_SUN;

  private static final DayCount ACT_365 = DayCounts.ACT_365F;
  private static final DayCount ACT_360 = DayCounts.ACT_360;

  private final BusinessDayConvention _businessdayAdjustmentConvention;
  private final HolidayCalendar _calandar;
  private final DayCount _accuralDayCount;
  private final DayCount _curveDayCount;

  public IsdaCompliantPresentValueCreditDefaultSwap() {
    _businessdayAdjustmentConvention = FOLLOWING;
    _calandar = DEFAULT_CALENDAR;
    _accuralDayCount = ACT_360;
    _curveDayCount = ACT_365;
  }

  /**
   * This is the present value of the premium leg per unit of fractional spread.
   * <p>
   * It hence it is equal to 10,000 times the RPV01 (Risky PV01). The actual PV of the leg
   * is this multiplied by the notional and the fractional spread (i.e. spread in basis
   * points divided by 10,000).
   * <p>
   * This mimics the ISDA c function <b>JpmcdsCdsFeeLegPV</b>.
   * 
   * @param today  the 'current' date
   * @param stepinDate  the date when party assumes ownership. This is normally today + 1 (T+1).
   *  Aka assignment date or effective date.
   * @param valueDate  the valuation date. The date that values are PVed to. Is is normally today + 3 business days.  Aka cash-settle date.
   * @param startDate  the protection start date. If protectStart = true, then protections starts
   *  at the beginning of the day, otherwise it is at the end.
   * @param endDate  the protection end date (the protection ends at end of day)
   * @param payAccOnDefault  is the accrued premium paid in the event of a default
   * @param tenor  the nominal step between premium payments (e.g. 3 months, 6 months)
   * @param stubType  the stub convention
   * @param yieldCurve  the curve from which payments are discounted
   * @param hazardRateCurve  the curve giving survival probability
   * @param protectStart  whether protection starts at the beginning of the day
   * @param priceType  the Clean or Dirty price flag. The clean price removes the accrued premium
   *  if the trade is between payment times.
   * @return 10,000 times the RPV01 (on a notional of 1)
   */
  public double pvPremiumLegPerUnitSpread(
      LocalDate today,
      LocalDate stepinDate,
      LocalDate valueDate,
      LocalDate startDate,
      LocalDate endDate,
      boolean payAccOnDefault,
      Period tenor,
      StubConvention stubType,
      IsdaCompliantDateYieldCurve yieldCurve,
      IsdaCompliantDateCreditCurve hazardRateCurve,
      boolean protectStart,
      CdsPriceType priceType) {

    ArgChecker.notNull(today, "null today");
    ArgChecker.notNull(stepinDate, "null stepinDate");
    ArgChecker.notNull(valueDate, "null valueDate");
    ArgChecker.notNull(startDate, "null startDate");
    ArgChecker.notNull(endDate, "null endDate");
    ArgChecker.notNull(tenor, "null tenor");
    ArgChecker.notNull(stubType, "null stubType");
    ArgChecker.notNull(yieldCurve, "null yieldCurve");
    ArgChecker.notNull(hazardRateCurve, "null hazardRateCurve");
    ArgChecker.notNull(priceType, "null priceType");
    ArgChecker.isFalse(valueDate.isBefore(today), "Require valueDate >= today");
    ArgChecker.isFalse(stepinDate.isBefore(today), "Require stepin >= today");

    IsdaPremiumLegSchedule paymentSchedule = new IsdaPremiumLegSchedule(
        startDate, endDate, tenor, stubType, _businessdayAdjustmentConvention, _calandar, protectStart);
    int nPayments = paymentSchedule.getNumPayments();

    // these are potentially different from startDate and endDate
    LocalDate globalAccStart = paymentSchedule.getAccStartDate(0);
    LocalDate golobalAccEnd = paymentSchedule.getAccEndDate(nPayments - 1);

    // TODO this logic could be part of ISDAPremiumLegSchdule
    LocalDate matDate = protectStart ? golobalAccEnd.minusDays(1) : golobalAccEnd;

    if (today.isAfter(matDate) || stepinDate.isAfter(matDate)) {
      return 0.0; // trade has expired
    }

    LocalDate[] yieldCurveDates = yieldCurve.getCurveDates();
    LocalDate[] creditCurveDates = hazardRateCurve.getCurveDates();
    // This is common to the protection leg
    LocalDate[] integrationSchedule = payAccOnDefault ?
        getIntegrationNodesAsDates(globalAccStart, golobalAccEnd, yieldCurveDates, creditCurveDates) :
        null;
    int obsOffset = protectStart ? -1 : 0; // protection start at the beginning or end day

    double rpv01 = 0.0;
    for (int i = 0; i < nPayments; i++) {

      LocalDate accStart = paymentSchedule.getAccStartDate(i);
      LocalDate accEnd = paymentSchedule.getAccEndDate(i);
      LocalDate pay = paymentSchedule.getPaymentDate(i);

      if (!accEnd.isAfter(stepinDate)) {
        continue; // this cashflow has already been realised
      }

      double[] temp = calculateSinglePeriodRPV01(today, accStart, accEnd, pay, obsOffset, yieldCurve, hazardRateCurve);
      rpv01 += temp[0];

      if (payAccOnDefault) {
        LocalDate offsetStepinDate = stepinDate.plusDays(obsOffset);
        LocalDate offsetAccStartDate = accStart.plusDays(obsOffset);
        LocalDate offsetAccEndDate = accEnd.plusDays(obsOffset);
        rpv01 += calculateSinglePeriodAccrualOnDefault(
            today, offsetStepinDate, offsetAccStartDate, offsetAccEndDate,
            temp[1], yieldCurve, hazardRateCurve, integrationSchedule);
      }
    }

    // Compute the discount factor discounting the upfront payment made on the cash settlement date back to the valuation date
    double t = _curveDayCount.yearFraction(today, valueDate);
    double df = yieldCurve.getDiscountFactor(t);
    rpv01 /= df;

    // Do we want to calculate the clean price (includes the previously accrued portion of the premium)
    if (priceType == CdsPriceType.CLEAN) {
      rpv01 -= calculateAccruedInterest(paymentSchedule, stepinDate);
    }

    return rpv01;
  }

  /**
   * Computes the risky present value of a premium payment.
   * <p>
   * This mimics the ISDA c code function <b>FeePaymentPVWithTimeLine<b>.
   * 
   * @param today  the date today
   * @param accStartDate  the start date
   * @param accEndDate  the end date
   * @param paymentDate  the payment date
   * @param obsOffset  the offset
   * @param yieldCurve  the yield curve
   * @param hazardRateCurve  the hazard curve
   * @return PV
   */
  private double[] calculateSinglePeriodRPV01(
      LocalDate today,
      LocalDate accStartDate,
      LocalDate accEndDate,
      LocalDate paymentDate,
      int obsOffset,
      IsdaCompliantDateYieldCurve yieldCurve,
      IsdaCompliantDateCreditCurve hazardRateCurve) {

    double accTime = _accuralDayCount.yearFraction(accStartDate, accEndDate);
    double t = _curveDayCount.yearFraction(today, paymentDate);
    double tObsOffset = _curveDayCount.yearFraction(today, accEndDate.plusDays(obsOffset));

    // TODO Do we need this?
    // Compensate Java shortcoming
    if (Double.compare(t, -0.0) == 0) {
      t = 0;
    }
    if (Double.compare(tObsOffset, -0.0) == 0) {
      tObsOffset = 0;
    }

    double survival = hazardRateCurve.getSurvivalProbability(tObsOffset);
    double discount = yieldCurve.getDiscountFactor(t);
    return new double[] {accTime * discount * survival, accTime};
  }

  /**
   * This mimics the ISDA c JpmcdsAccrualOnDefaultPVWithTimeLine.
   * 
   * @param today  the date today
   * @param offsetStepinDate  the step in date
   * @param offsetAccStartDate  the start date
   * @param offsetAccEndDate  the end date
   * @param accTime  the time
   * @param yieldCurve  the yield curve
   * @param hazardRateCurve  the hazard rate curve
   * @param integrationSchedule  the schedule
   * @return the single period accrual on default
   */
  private double calculateSinglePeriodAccrualOnDefault(
      LocalDate today,
      LocalDate offsetStepinDate,
      LocalDate offsetAccStartDate,
      LocalDate offsetAccEndDate,
      double accTime,
      IsdaCompliantDateYieldCurve yieldCurve,
      IsdaCompliantDateCreditCurve hazardRateCurve,
      LocalDate[] integrationSchedule) {

    LocalDate[] truncatedDateList = truncateList(offsetAccStartDate, offsetAccEndDate, integrationSchedule);
    int nItems = truncatedDateList.length;

    // max(offsetStepinDate,offsetAccStartDate)
    LocalDate subStartDate = offsetStepinDate.isAfter(offsetAccStartDate) ? offsetStepinDate : offsetAccStartDate;

    double tAcc = ACT_365.yearFraction(offsetAccStartDate, offsetAccEndDate); // This is hardcoded to ACT/365 in ISDA code
    double accRate = accTime / tAcc;
    double t = ACT_365.yearFraction(today, subStartDate);

    // Compensate Java shortcoming
    if (Double.compare(t, -0.0) == 0) {
      t = 0;
    }
    double s0 = hazardRateCurve.getSurvivalProbability(t);
    double df0 = yieldCurve.getDiscountFactor(t);

    double myPV = 0.0;
    for (int j = 1; j < nItems; ++j) {

      if (!truncatedDateList[j].isAfter(offsetStepinDate)) {
        continue;
      }

      double thisAccPV = 0.0;
      t = ACT_365.yearFraction(today, truncatedDateList[j]);
      double s1 = hazardRateCurve.getSurvivalProbability(t);
      double df1 = yieldCurve.getDiscountFactor(t);

      double t0 = ACT_365.yearFraction(offsetAccStartDate, subStartDate) + 1 / 730.; // add on half a day
      double t1 = ACT_365.yearFraction(offsetAccStartDate, truncatedDateList[j]) + 1 / 730.;
      t = t1 - t0; // t repurposed

      // TODO check for s0 == s1 -> zero prob of default (and thus zero PV contribution) from this section

      double lambda = Math.log(s0 / s1) / t;
      double fwdRate = Math.log(df0 / df1) / t;
      double lambdafwdRate = lambda + fwdRate + 1.0e-50;

      thisAccPV = lambda * accRate * s0 * df0 *
          ((t0 + 1.0 / (lambdafwdRate)) / (lambdafwdRate) - (t1 + 1.0 / (lambdafwdRate)) / (lambdafwdRate) * s1 / s0 * df1 / df0);
      myPV += thisAccPV;
      s0 = s1;
      df0 = df1;
      subStartDate = truncatedDateList[j];
    }
    return myPV;
  }

  /**
   * Calculate the accrued premium at the start of a trade.
   * 
   * @param premiumLegSchedule  the schedule
   * @param stepinDate  the trade effective date
   * @return accrued premium
   */
  private double calculateAccruedInterest(IsdaPremiumLegSchedule premiumLegSchedule, LocalDate stepinDate) {

    int n = premiumLegSchedule.getNumPayments();

    // stepinDate is before first accStart or after last accEnd
    if (!stepinDate.isAfter(premiumLegSchedule.getAccStartDate(0)) ||
        !stepinDate.isBefore(premiumLegSchedule.getAccEndDate(n - 1))) {
      return 0.0;
    }

    int index = premiumLegSchedule.getAccStartDateIndex(stepinDate);
    if (index >= 0) {
      return 0.0; // on accrual start date
    }

    index = -(index + 1); // binary search notation
    if (index == 0) {
      throw new MathException("Error in calculateAccruedInterest - check logic"); // this should never be hit
    }

    return _accuralDayCount.yearFraction(premiumLegSchedule.getAccStartDate(index - 1), stepinDate);
  }

  /**
   * Get the value of the protection leg for unit notional<p>
   * This mimics the ISDA c function <b>JpmcdsCdsContingentLegPV</b>.
   *
   * @param today  the date today
   * @param stepinDate  the date when party assumes ownership. This is normally today
   *  + 1 (T+1). Aka assignment date or effective date.
   * @param valueDate  the valuation date. The date that values are PVed to. Is is
   *  normally today + 3 business days.  Aka cash-settle date.
   * @param startDate  the protection start date. If protectStart = true, then protections
   *  starts at the beginning of the day, otherwise it* is at the end.
   * @param endDate  the protection end date (the protection ends at end of day)
   * @param yieldCurve  the curve from which payments are discounted
   * @param hazardRateCurve  the curve giving survival probability
   * @param recoveryRate  the recovery rate of the protected debt
   * @param protectStart  whether protection starts at the beginning of the day
   * @return unit notional PV of protection (or contingent) leg
   */
  public double calculateProtectionLeg(
      LocalDate today,
      LocalDate stepinDate,
      LocalDate valueDate,
      LocalDate startDate,
      LocalDate endDate,
      IsdaCompliantDateYieldCurve yieldCurve,
      IsdaCompliantDateCreditCurve hazardRateCurve,
      double recoveryRate,
      boolean protectStart) {

    ArgChecker.notNull(today, "null today");
    ArgChecker.notNull(valueDate, "null valueDate");
    ArgChecker.notNull(startDate, "null startDate");
    ArgChecker.notNull(endDate, "null endDate");
    ArgChecker.notNull(yieldCurve, "null yieldCurve");
    ArgChecker.notNull(hazardRateCurve, "null hazardRateCurve");
    ArgChecker.inRangeInclusive(recoveryRate, 0d, 1d, "recoveryRate");
    ArgChecker.isFalse(valueDate.isBefore(today), "Require valueDate >= today");
    ArgChecker.isFalse(stepinDate.isBefore(today), "Require stepin >= today");

    if (recoveryRate == 1.0) {
      return 0.0;
    }

    LocalDate temp = stepinDate.isAfter(startDate) ? stepinDate : startDate;
    LocalDate effectiveStartDate = protectStart ? temp.minusDays(1) : temp;

    if (!endDate.isAfter(effectiveStartDate)) {
      return 0.0; // the protection has expired
    }

    LocalDate[] yieldCurveDates = yieldCurve.getCurveDates();
    LocalDate[] creditCurveDates = hazardRateCurve.getCurveDates();
    double[] integrationSchedule = IsdaCompliantScheduleGenerator.getIntegrationNodesAsTimes(today, effectiveStartDate,
        endDate, yieldCurveDates, creditCurveDates);

    double ht1 = hazardRateCurve.getRT(integrationSchedule[0]);
    double rt1 = yieldCurve.getRT(integrationSchedule[0]);
    double s1 = Math.exp(-ht1);
    double p1 = Math.exp(-rt1);
    double pv = 0.0;
    int n = integrationSchedule.length;
    for (int i = 1; i < n; ++i) {

      double ht0 = ht1;
      double rt0 = rt1;
      double p0 = p1;
      double s0 = s1;

      ht1 = hazardRateCurve.getRT(integrationSchedule[i]);
      rt1 = yieldCurve.getRT(integrationSchedule[i]);
      s1 = Math.exp(-ht1);
      p1 = Math.exp(-rt1);
      double dht = ht1 - ht0;
      double drt = rt1 - rt0;
      double dhrt = dht + drt;

      // this is equivalent to the ISDA code without explicitly calculating the time step - it also handles the limit
      double dPV;
      if (Math.abs(dhrt) < 1e-5) {
        dPV = dht * (1 - dhrt * (0.5 - dhrt / 6)) * p0 * s0;
      } else {
        dPV = dht / dhrt * (p0 * s0 - p1 * s1);
      }

      // ISDA code

      pv += dPV;

    }
    pv *= 1.0 - recoveryRate;

    // Compute the discount factor discounting the upfront payment made on the cash settlement date back to the valuation date
    double t = _curveDayCount.yearFraction(today, valueDate);
    double df = yieldCurve.getDiscountFactor(t);
    pv /= df;

    return pv;
  }

}
