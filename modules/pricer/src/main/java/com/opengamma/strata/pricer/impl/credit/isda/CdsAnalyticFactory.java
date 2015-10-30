/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.credit.isda;

import java.time.LocalDate;
import java.time.Period;

import com.opengamma.strata.basics.date.BusinessDayConvention;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.date.HolidayCalendar;
import com.opengamma.strata.basics.date.HolidayCalendars;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.collect.ArgChecker;

/**
 * 
 */
public class CdsAnalyticFactory {

  private static final int DEFAULT_STEPIN = 1;
  private static final int DEFAULT_CASH_SETTLE = 3;
  private static final boolean DEFAULT_PAY_ACC = true;
  private static final Period DEFAULT_COUPON_INT = Period.ofMonths(3);
  private static final StubConvention DEFAULT_STUB_TYPE = StubConvention.SHORT_INITIAL;
  private static final boolean PROT_START = true;
  private static final double DEFAULT_RR = 0.4;
  private static final HolidayCalendar DEFAULT_CALENDAR = HolidayCalendars.SAT_SUN;
  private static final BusinessDayConvention FOLLOWING = BusinessDayConventions.FOLLOWING;
  /** Curve daycount generally fixed to Act/365 in ISDA */
  private static final DayCount ACT_365 = DayCounts.ACT_365F;
  private static final DayCount ACT_360 = DayCounts.ACT_360;

  private final int _stepIn;
  private final int _cashSettle;
  private final boolean _payAccOnDefault;
  private final Period _couponInterval;
  private final Tenor _couponIntervalTenor;
  private final StubConvention _stubType;
  private final boolean _protectStart;
  private final double _recoveryRate;
  private final BusinessDayConvention _businessdayAdjustmentConvention;
  private final HolidayCalendar _calendar;
  private final DayCount _accrualDayCount;
  private final DayCount _curveDayCount;

  /**
   * Produce CDSs with the following default values:<P>
   * Step-in: T+1<br>
   * Cash-Settle: T+3 working days<br>
   * Pay accrual on Default: true<br>
   * CouponInterval: 3M<br>
   * Stub type: front-short<br>
   * Protection from start of day: true<br>
   * Recovery rate: 40%<br>
   * Business-day Adjustment: Following<br>
   * HolidayCalendar: weekend only<br>
   * Accrual day count: ACT/360<br>
   * Curve day count: ACT/365 (fixed)<p>
   * These defaults can be overridden using the with methods 
   */
  public CdsAnalyticFactory() {
    _stepIn = DEFAULT_STEPIN;
    _cashSettle = DEFAULT_CASH_SETTLE;
    _payAccOnDefault = DEFAULT_PAY_ACC;
    _couponInterval = DEFAULT_COUPON_INT;
    _stubType = DEFAULT_STUB_TYPE;
    _protectStart = PROT_START;
    _recoveryRate = DEFAULT_RR;
    _businessdayAdjustmentConvention = FOLLOWING;
    _calendar = DEFAULT_CALENDAR;
    _accrualDayCount = ACT_360;
    _curveDayCount = ACT_365;
    _couponIntervalTenor = Tenor.of(_couponInterval);
  }

  /**
  * Produce CDSs with the following default values and a supplied recovery rate:<P>
   * Step-in: T+1<br>
   * Cash-Settle: T+3 working days<br>
   * Pay accrual on Default: true<br>
   * CouponInterval: 3M<br>
   * Stub type: front-short<br>
   * Protection from start of day: true<br>
   * Business-day Adjustment: Following<br>
   * HolidayCalendar: weekend only<br>
   * Accrual day count: ACT/360<br>
   * Curve day count: ACT/365 (fixed)
   * @param recoveryRate The recovery rate
   */
  public CdsAnalyticFactory(double recoveryRate) {
    _stepIn = DEFAULT_STEPIN;
    _cashSettle = DEFAULT_CASH_SETTLE;
    _payAccOnDefault = DEFAULT_PAY_ACC;
    _couponInterval = DEFAULT_COUPON_INT;
    _stubType = DEFAULT_STUB_TYPE;
    _protectStart = PROT_START;
    _recoveryRate = recoveryRate;
    _businessdayAdjustmentConvention = FOLLOWING;
    _calendar = DEFAULT_CALENDAR;
    _accrualDayCount = ACT_360;
    _curveDayCount = ACT_365;
    _couponIntervalTenor = Tenor.of(_couponInterval);
  }

  /**
  * Produce CDSs with the following default values and a supplied coupon interval:<P> 
   * Step-in: T+1<br>
   * Cash-Settle: T+3 working days<br>
   * Pay accrual on Default: true<br>
   * Stub type: front-short<br>
   * Protection from start of day: true<br>
   * Recovery rate: 40%<br>
   * Business-day Adjustment: Following<br>
   * HolidayCalendar: weekend only<br>
   * Accrual day count: ACT/360<br>
   * Curve day count: ACT/365 (fixed) 
   * @param couponInterval The coupon interval
   */
  public CdsAnalyticFactory(Period couponInterval) {
    ArgChecker.notNull(couponInterval, "couponInterval");
    _stepIn = DEFAULT_STEPIN;
    _cashSettle = DEFAULT_CASH_SETTLE;
    _payAccOnDefault = DEFAULT_PAY_ACC;
    _couponInterval = couponInterval;
    _stubType = DEFAULT_STUB_TYPE;
    _protectStart = PROT_START;
    _recoveryRate = DEFAULT_RR;
    _businessdayAdjustmentConvention = FOLLOWING;
    _calendar = DEFAULT_CALENDAR;
    _accrualDayCount = ACT_360;
    _curveDayCount = ACT_365;
    _couponIntervalTenor = Tenor.of(_couponInterval);
  }

  /**
  * Produce CDSs with the following default values and a supplied recovery rate and coupon interval:<P> 
   * Step-in: T+1<br>
   * Cash-Settle: T+3 working days<br>
   * Pay accrual on Default: true<br>
   * Stub type: front-short<br>
   * Protection from start of day: true<br>
   * Business-day Adjustment: Following<br>
   * HolidayCalendar: weekend only<br>
   * Accrual day count: ACT/360<br>
   * Curve day count: ACT/365 (fixed) 
   * @param recoveryRate The recovery rate
   * @param couponInterval The coupon interval
   */
  public CdsAnalyticFactory(double recoveryRate, Period couponInterval) {
    ArgChecker.notNull(couponInterval, "couponInterval");
    _stepIn = DEFAULT_STEPIN;
    _cashSettle = DEFAULT_CASH_SETTLE;
    _payAccOnDefault = DEFAULT_PAY_ACC;
    _couponInterval = couponInterval;
    _stubType = DEFAULT_STUB_TYPE;
    _protectStart = PROT_START;
    _recoveryRate = recoveryRate;
    _businessdayAdjustmentConvention = FOLLOWING;
    _calendar = DEFAULT_CALENDAR;
    _accrualDayCount = ACT_360;
    _curveDayCount = ACT_365;
    _couponIntervalTenor = Tenor.of(_couponInterval);
  }

  /**
   * Copy constructor 
   * @param other The factory to copy
   */
  public CdsAnalyticFactory(CdsAnalyticFactory other) {
    ArgChecker.notNull(other, "other");
    _stepIn = other._stepIn;
    _cashSettle = other._cashSettle;
    _payAccOnDefault = other._payAccOnDefault;
    _couponInterval = other._couponInterval;
    _stubType = other._stubType;
    _protectStart = other._protectStart;
    _recoveryRate = other._recoveryRate;
    _businessdayAdjustmentConvention = other._businessdayAdjustmentConvention;
    _calendar = other._calendar;
    _accrualDayCount = other._accrualDayCount;
    _curveDayCount = other._curveDayCount;
    _couponIntervalTenor = Tenor.of(_couponInterval);
  }

  protected CdsAnalyticFactory(
      int stepIn,
      int cashSettle,
      boolean payAccOnDefault,
      Period couponInterval,
      StubConvention stubType,
      boolean protectStart,
      double recoveryRate,
      BusinessDayConvention businessdayAdjustmentConvention,
      HolidayCalendar calendar,
      DayCount accrualDayCount,
      DayCount curveDayCount) {

    _stepIn = stepIn;
    _cashSettle = cashSettle;
    _payAccOnDefault = payAccOnDefault;
    _couponInterval = couponInterval;
    _stubType = stubType;
    _protectStart = protectStart;
    _recoveryRate = recoveryRate;
    _businessdayAdjustmentConvention = businessdayAdjustmentConvention;
    _calendar = calendar;
    _accrualDayCount = accrualDayCount;
    _curveDayCount = curveDayCount;
    _couponIntervalTenor = Tenor.of(_couponInterval);
  }

  //************************************************************************************************************************
  // with methods - use these to override defaults
  //************************************************************************************************************************

  /**
   * The Step-in (Protection Effective Date or sometimes just Effective Date) is usually T+1.
   * This is when protection (and risk) starts in terms of the model.
   * @param stepIn Zero or more days (after trade day)
   * @return A new factory with the step-in days set. 
   */
  public CdsAnalyticFactory withStepIn(int stepIn) {
    ArgChecker.notNegative(stepIn, "stepIn");
    return new CdsAnalyticFactory(
        stepIn, _cashSettle, _payAccOnDefault, _couponInterval, _stubType, _protectStart,
        _recoveryRate, _businessdayAdjustmentConvention, _calendar, _accrualDayCount, _curveDayCount);
  }

  /**
   * Valuation or Cash-settle Date. This is the date for which the present value (PV) of
   * the CDS is calculated. It is usually three working dates after the trade date. 
   * @param cashSettle Zero or more days (after trade day)
   * @return A new factory with the cash-settle days set.
   */
  public CdsAnalyticFactory withCashSettle(int cashSettle) {
    return new CdsAnalyticFactory(
        _stepIn, cashSettle, _payAccOnDefault, _couponInterval, _stubType, _protectStart, _recoveryRate,
        _businessdayAdjustmentConvention, _calendar, _accrualDayCount, _curveDayCount);
  }

  /**
   *  Is the accrued premium paid in the event of a default (default value is true)
   * @param payAcc Set to true to pay accrued on default 
   * @return A new factory with the payAccOnDefault set
   */
  public CdsAnalyticFactory withPayAccOnDefault(boolean payAcc) {
    return new CdsAnalyticFactory(
        _stepIn, _cashSettle, payAcc, _couponInterval, _stubType, _protectStart, _recoveryRate,
        _businessdayAdjustmentConvention, _calendar, _accrualDayCount, _curveDayCount);
  }

  /**
   * Set the coupon interval (default is 3M)
   * @param couponInterval The coupon interval
   * @return  A new factory with the coupon interval set
   */
  public CdsAnalyticFactory with(Period couponInterval) {
    return new CdsAnalyticFactory(
        _stepIn, _cashSettle, _payAccOnDefault, couponInterval, _stubType, _protectStart, _recoveryRate,
        _businessdayAdjustmentConvention, _calendar, _accrualDayCount, _curveDayCount);
  }

  /**
   * Sets the stub convention.
   * @param stubType The stub type 
   * @return  A new factory with the stub-type interval set
   */
  public CdsAnalyticFactory with(StubConvention stubType) {
    return new CdsAnalyticFactory(
        _stepIn, _cashSettle, _payAccOnDefault, _couponInterval, stubType, _protectStart, _recoveryRate,
        _businessdayAdjustmentConvention, _calendar, _accrualDayCount, _curveDayCount);
  }

  /**
   * If protectStart = true, then protections starts at the beginning of the day, otherwise it is at the end.
   * @param protectionStart Protected from start of day?
   * @return A new factory with protectStart set
   */
  public CdsAnalyticFactory withProtectionStart(boolean protectionStart) {
    return new CdsAnalyticFactory(
        _stepIn, _cashSettle, _payAccOnDefault, _couponInterval, _stubType, protectionStart, _recoveryRate,
        _businessdayAdjustmentConvention, _calendar, _accrualDayCount, _curveDayCount);
  }

  /**
   * Set the recovery rate (default is 40%)
   * @param recovery The recovery rate
   * @return  A new factory with recovery rate set
   */
  public CdsAnalyticFactory withRecoveryRate(double recovery) {
    return new CdsAnalyticFactory(
        _stepIn, _cashSettle, _payAccOnDefault, _couponInterval, _stubType, _protectStart, recovery,
        _businessdayAdjustmentConvention, _calendar, _accrualDayCount, _curveDayCount);
  }

  /**
   * Set how adjustments for non-business are days made. Default is following.
   * @param busDay business-day adjustment convention
   * @return A new factory with business-day adjustment convention set
   */
  public CdsAnalyticFactory with(BusinessDayConvention busDay) {
    return new CdsAnalyticFactory(
        _stepIn, _cashSettle, _payAccOnDefault, _couponInterval, _stubType, _protectStart, _recoveryRate,
        busDay, _calendar, _accrualDayCount, _curveDayCount);
  }

  /**
   * Set the calendar. Default is weekend-only 
   * @param calendar HolidayCalendar defining what is a non-business day
   * @return A new factory with calendar set
   */
  public CdsAnalyticFactory with(HolidayCalendar calendar) {
    return new CdsAnalyticFactory(
        _stepIn, _cashSettle, _payAccOnDefault, _couponInterval, _stubType, _protectStart, _recoveryRate,
        _businessdayAdjustmentConvention, calendar, _accrualDayCount, _curveDayCount);
  }

  /**
   * Set the day count used for accrual calculations (i.e. premium payments). Default is ACT/360
   * @param accDCC Day count used for accrual
   * @return A new factory with accDCC set
   */
  public CdsAnalyticFactory withAccrualDCC(DayCount accDCC) {
    return new CdsAnalyticFactory(
        _stepIn, _cashSettle, _payAccOnDefault, _couponInterval, _stubType, _protectStart, _recoveryRate,
        _businessdayAdjustmentConvention, _calendar, accDCC, _curveDayCount);
  }

  /**
   * Set the day count used on curve
   * @param curveDCC Day count used on curve (NOTE ISDA uses ACT/365 (fixed) and it is not recommended to change this)
   * @return A new factory with curveDCC set
   */
  public CdsAnalyticFactory withCurveDCC(DayCount curveDCC) {
    return new CdsAnalyticFactory(
        _stepIn, _cashSettle, _payAccOnDefault, _couponInterval, _stubType, _protectStart, _recoveryRate,
        _businessdayAdjustmentConvention, _calendar, _accrualDayCount, curveDCC);
  }

  //************************************************************************************************************************
  // Make CdsAnalytic  
  //************************************************************************************************************************

  /**
   * Set up an on-the-run index represented as a single name CDS (i.e. by CdsAnalytic).
   * The index roll dates (when new indices are issued) are 20 Mar & Sep,
   * and the index is defined to have a maturity that is its nominal tenor plus 3M on issuance,
   * so a 5Y index on the 6-Feb-2014 will have a maturity of 20-Dec-2018 (5Y3M on the issue date of 20-Sep-2013). 
   * The accrual start date will be the previous IMM date (before the trade date), business-day adjusted.
   * <b>Note</b> it payment interval is changed from the
   * default of 3M, this will produce a (possibly incorrect) non-standard first coupon.    
   * 
   * @param tradeDate  the trade date
   * @param tenor  the nominal length of the index 
   * @return a CDS analytic description 
   */
  public CdsAnalytic makeCdx(LocalDate tradeDate, Period tenor) {
    ArgChecker.notNull(tradeDate, "tradeDate");
    ArgChecker.notNull(tenor, "tenor");
    LocalDate effectiveDate = _businessdayAdjustmentConvention.adjust(ImmDateLogic.getPrevIMMDate(tradeDate), _calendar);
    LocalDate roll = ImmDateLogic.getNextIndexRollDate(tradeDate);
    LocalDate maturity = roll.plus(tenor).minusMonths(3);
    return makeCds(tradeDate, effectiveDate, maturity);
  }

  /**
   * Set up a strip of on-the-run indexes represented as a single name CDSs (i.e. by CdsAnalytic).
   * The index roll dates (when new indices are issued) are 20 Mar & Sep,
   * and the index is defined to have a maturity that is its nominal tenor plus 3M on issuance,
   * so a 5Y index on the 6-Feb-2014 will have a maturity of
   * 20-Dec-2018 (5Y3M on the issue date of 20-Sep-2013). 
   * The accrual start date will be the previous IMM date (before the trade date), business-day adjusted.
   * <b>Note</b> it payment interval is changed from the
   * default of 3M, this will produce a (possibly incorrect) non-standard first coupon.    
   * 
   * @param tradeDate  the trade date
   * @param tenors  the nominal lengths of the indexes
   * @return an array of CDS analytic descriptions 
   */
  public CdsAnalytic[] makeCdx(LocalDate tradeDate, Period[] tenors) {
    ArgChecker.notNull(tradeDate, "tradeDate");
    ArgChecker.noNulls(tenors, "tenors");
    LocalDate effectiveDate = _businessdayAdjustmentConvention.adjust(ImmDateLogic.getPrevIMMDate(tradeDate), _calendar);
    LocalDate mid = ImmDateLogic.getNextIndexRollDate(tradeDate).minusMonths(3);
    LocalDate[] maturities = ImmDateLogic.getIMMDateSet(mid, tenors);
    return makeCds(tradeDate, effectiveDate, maturities);
  }

  //-------------------------------------------------------------------------
  /**
   * Make a CDS with a maturity date the given period on from the next IMM date after the trade-date.
   * The accrual start date will be the previous IMM date (before the trade date), business-day adjusted.
   * <b>Note</b> it payment interval is changed from the
   * default of 3M, this will produce a (possibly incorrect) non-standard first coupon.   
   * 
   * @param tradeDate  the trade date
   * @param tenor  the tenor (length) of the CDS
   * @return a CDS analytic description 
   */
  public CdsAnalytic makeImmCds(LocalDate tradeDate, Period tenor) {
    return makeImmCds(tradeDate, tenor, true);
  }

  /**
   * Make a CDS with a maturity date the given period on from the next IMM date after the trade-date.
   * The accrual start date will be the previous IMM date (before the trade date).
   * <b>Note</b> it payment interval is changed from the
   * default of 3M, this will produce a (possibly incorrect) non-standard first coupon.
   * 
   * @param tradeDate  the trade date
   * @param tenor  the tenor (length) of the CDS
   * @param makeEffBusDay  is the accrual start day business-day adjusted.
   * @return a CDS analytic description 
   */
  public CdsAnalytic makeImmCds(LocalDate tradeDate, Period tenor, boolean makeEffBusDay) {
    ArgChecker.notNull(tradeDate, "tradeDate");
    ArgChecker.notNull(tenor, "tenor");
    LocalDate effectiveDate = makeEffBusDay ?
        _businessdayAdjustmentConvention.adjust(ImmDateLogic.getPrevIMMDate(tradeDate), _calendar) :
        ImmDateLogic.getPrevIMMDate(tradeDate);
    LocalDate nextIMM = ImmDateLogic.getNextIMMDate(tradeDate);
    LocalDate maturity = nextIMM.plus(tenor);
    return makeCds(tradeDate, effectiveDate, maturity);
  }

  /**
   * Make a set of CDSs with a common trade date and maturities dates the given periods after the
   * next IMM date (after the trade-date).
   * The accrual start date will  be the previous IMM date (before the trade date), business-day adjusted. 
   * <b>Note</b> it payment interval is changed from the default of 3M, this will produce a
   * (possibly incorrect) non-standard first coupon.
   * 
   * @param tradeDate  the trade date
   * @param tenors  the tenors (lengths) of the CDSs
   * @return an array of CDS analytic descriptions 
   */
  public CdsAnalytic[] makeImmCds(LocalDate tradeDate, Period[] tenors) {
    return makeImmCds(tradeDate, tenors, true);
  }

  /**
   * Make a set of CDSs with a common trade date and maturities dates the given periods after
   * the next IMM date (after the trade-date).
   * The accrual start date will  be the previous IMM date (before the trade date).
   * <b>Note</b> it payment interval is changed from the default of 3M, this will produce a
   * (possibly incorrect) non-standard first coupon.
   * 
   * @param tradeDate  the trade date
   * @param tenors  the tenors (lengths) of the CDSs
   * @param makeEffBusDay  is the accrual start day business-day adjusted.
   * @return an array of CDS analytic descriptions 
   */
  public CdsAnalytic[] makeImmCds(LocalDate tradeDate, Period[] tenors, boolean makeEffBusDay) {
    LocalDate effectiveDate = makeEffBusDay ?
        _businessdayAdjustmentConvention.adjust(ImmDateLogic.getPrevIMMDate(tradeDate), _calendar) :
        ImmDateLogic.getPrevIMMDate(tradeDate);
    return makeImmCds(tradeDate, effectiveDate, tenors);
  }

  /**
   * Make a set of CDSs with a common trade date and maturities dates the given periods after
   * the next IMM date (after the trade-date).
   * 
   * @param tradeDate  the trade date
   * @param accStartDate  this is when the CDS nominally starts in terms of premium payments.
   *  For a standard CDS this is  the previous IMM date, and for a `legacy' CDS it is T+1
   * @param tenors  the tenors (lengths) of the CDSs
   * @return an array of CDS analytic descriptions 
   */
  public CdsAnalytic[] makeImmCds(LocalDate tradeDate, LocalDate accStartDate, Period[] tenors) {
    ArgChecker.notNull(tradeDate, "tradeDate");
    ArgChecker.notNull(accStartDate, "effectiveDate");
    ArgChecker.noNulls(tenors, "tenors");
    LocalDate nextIMM = ImmDateLogic.getNextIMMDate(tradeDate);
    LocalDate[] maturities = ImmDateLogic.getIMMDateSet(nextIMM, tenors);
    return makeCds(tradeDate, accStartDate, maturities);
  }

  //-------------------------------------------------------------------------
  /**
   * Make a CDS by specifying key dates.
   * 
   * @param tradeDate  the trade date
   * @param accStartDate  this is when the CDS nominally starts in terms of premium payments.
   *  For a standard CDS this is  the previous IMM date, and for a `legacy' CDS it is T+1
   * @param maturity  the maturity. For a standard CDS this is an IMM  date
   * @return a CDS analytic description 
   */
  public CdsAnalytic makeCds(LocalDate tradeDate, LocalDate accStartDate, LocalDate maturity) {
    LocalDate stepinDate = tradeDate.plusDays(_stepIn);
    LocalDate valueDate = addWorkDays(tradeDate, _cashSettle, _calendar);
    return new CdsAnalytic(tradeDate, stepinDate, valueDate, accStartDate, maturity,
        _payAccOnDefault, _couponInterval, _stubType, _protectStart, _recoveryRate, _businessdayAdjustmentConvention,
        _calendar, _accrualDayCount, _curveDayCount);
  }

  /**
   * Make a CDS by specifying all dates.
   * 
   * @param tradeDate  the trade date
   * @param stepinDate  (aka Protection Effective sate or assignment date). Date when party assumes ownership.
   *  This is usually T+1. This is when protection (and risk) starts in terms of the model.
   *  Note, this is sometimes just called the Effective Date, however this can cause confusion
   *  with the legal effective date which is T-60 or T-90.
   * @param cashSettlementDate The valuation date. The date that values are PVed to.
   *  Is is normally today + 3 business days.  Aka cash-settle date.
   * @param accStartDate  this is when the CDS nominally starts in terms of premium payments.
   *  For a standard CDS this is  the previous IMM date, and for a `legacy' CDS it is T+1
   * @param maturity  (aka end date) This is when the contract expires and protection ends - any
   *  default after this date does not trigger a payment. (the protection ends at end of day)
   * @return a CDS analytic description 
   */
  public CdsAnalytic makeCds(
      LocalDate tradeDate,
      LocalDate stepinDate,
      LocalDate cashSettlementDate,
      LocalDate accStartDate,
      LocalDate maturity) {

    return new CdsAnalytic(tradeDate, stepinDate, cashSettlementDate, accStartDate, maturity,
        _payAccOnDefault, _couponInterval, _stubType, _protectStart, _recoveryRate,
        _businessdayAdjustmentConvention, _calendar, _accrualDayCount, _curveDayCount);
  }

  /**
   * Make a set of CDS by specifying key dates.
   * 
   * @param tradeDate  the trade date
   * @param accStartDate  this is when the CDS nominally starts in terms of premium payments.
   *  For a standard CDS this is  the previous IMM date, and for a `legacy' CDS it is T+1
   * @param maturities The maturities of the CDSs. For a standard CDS these are IMM  dates
   * @return an array of CDS analytic descriptions 
   */
  public CdsAnalytic[] makeCds(LocalDate tradeDate, LocalDate accStartDate, LocalDate[] maturities) {
    LocalDate stepinDate = tradeDate.plusDays(_stepIn);
    LocalDate valueDate = addWorkDays(tradeDate, _cashSettle, _calendar);
    return makeCds(tradeDate, stepinDate, valueDate, accStartDate, maturities);
  }

  /**
   * Make a set of CDS by specifying all dates.
   * 
   * @param tradeDate  the trade date
   * @param stepinDate  (aka Protection Effective sate or assignment date). Date when party assumes ownership.
   *  This is usually T+1. This is when protection (and risk) starts in terms of the model.
   *  Note, this is sometimes just called the Effective Date, however this can cause
   *  confusion with the legal effective date which is T-60 or T-90.
   * @param valueDate  the valuation date. The date that values are PVed to.
   *  Is is normally today + 3 business days.  Aka cash-settle date.
   * @param accStartDate  this is when the CDS nominally starts in terms of premium payments. i.e. the number
   *  of days in the first period (and thus the amount of the first premium payment) is counted from this date.
   * @param maturities  The maturities of the CDSs. For a standard CDS these are IMM  dates
   * @return an array of CDS analytic descriptions 
   */
  public CdsAnalytic[] makeCds(
      LocalDate tradeDate,
      LocalDate stepinDate,
      LocalDate valueDate,
      LocalDate accStartDate,
      LocalDate[] maturities) {

    ArgChecker.noNulls(maturities, "maturities");
    int n = maturities.length;
    CdsAnalytic[] cds = new CdsAnalytic[n];
    for (int i = 0; i < n; i++) {
      cds[i] = new CdsAnalytic(tradeDate, stepinDate, valueDate, accStartDate, maturities[i], _payAccOnDefault,
          _couponInterval, _stubType, _protectStart, _recoveryRate,
          _businessdayAdjustmentConvention, _calendar, _accrualDayCount, _curveDayCount);
    }
    return cds;
  }

  //************************************************************************************************************************
  // Make forward starting CDS 
  //************************************************************************************************************************

  /**
   * A forward starting CDS starts on some date after today (the trade date).
   * The stepin date and cash settlement date are taken from the forward start date
   * (1 day and 3 working days by default).
   * 
   * @param tradeDate  the trade date (i.e. today)
   * @param forwardStartDate  the forward start date
   * @param maturity  the maturity of the CDS 
   * @return a CDS analytic description for a forward starting CDS
   */
  public CdsAnalytic makeForwardStartingCds(LocalDate tradeDate, LocalDate forwardStartDate, LocalDate maturity) {
    ArgChecker.isFalse(
        forwardStartDate.isBefore(tradeDate),
        "forwardStartDate of {} is before trade date of {}", forwardStartDate, tradeDate);
    LocalDate stepinDate = forwardStartDate.plusDays(_stepIn);
    LocalDate valueDate = addWorkDays(forwardStartDate, _cashSettle, _calendar);
    LocalDate accStartDate = _businessdayAdjustmentConvention.adjust(ImmDateLogic.getPrevIMMDate(forwardStartDate), _calendar);
    return makeCds(tradeDate, stepinDate, valueDate, accStartDate, maturity);
  }

  /**
   * A forward starting CDS starts on some date after today (the trade date).
   * The accrual start must be specified (would normally use this for T+1 accrual atart).
   * The stepin date and cash settlement date are taken from the forward start date 
   * (1 day and 3 working days by default).
   * 
   * @param tradeDate  the trade date (i.e. today)
   * @param forwardStartDate  the forward start date
   * @param accStartDate  the accrual start date 
   * @param maturity  the maturity of the CDS 
   * @return a CDS analytic description for a forward starting CDS
   */
  public CdsAnalytic makeForwardStartingCds(
      LocalDate tradeDate,
      LocalDate forwardStartDate,
      LocalDate accStartDate,
      LocalDate maturity) {

    ArgChecker.isFalse(
        forwardStartDate.isBefore(tradeDate),
        "forwardStartDate of {} is before trade date of {}", forwardStartDate, tradeDate);
    LocalDate stepinDate = forwardStartDate.plusDays(_stepIn);
    LocalDate valueDate = addWorkDays(forwardStartDate, _cashSettle, _calendar);
    return makeCds(tradeDate, stepinDate, valueDate, accStartDate, maturity);
  }

  /** A forward starting CDS starts on some date after today (the trade date). 
   * The stepin date and cash settlement date are taken from the forward start date
   * (1 day and 3 working days by default). The period is from the next IMM date after the 
   * forward-start-date, so for a trade-date of 13-Feb-2014, a forward-start-date
   * of 25-Mar-2014 and a tenor of 1Y, the maturity will be 20-Jun-2015. 
   * 
   * @param tradeDate  the trade date (i.e. today)
   * @param forwardStartDate  the forward start date
   * @param tenor  the tenor (length) of the CDS at the forwardStartDate
   * @return a CDS analytic description for a forward starting CDS
   */
  public CdsAnalytic makeForwardStartingImmCds(LocalDate tradeDate, LocalDate forwardStartDate, Period tenor) {
    LocalDate nextIMM = ImmDateLogic.getNextIMMDate(forwardStartDate);
    LocalDate maturity = nextIMM.plus(tenor);
    return makeForwardStartingCds(tradeDate, forwardStartDate, maturity);
  }

  /**
   * /** A forward starting index starts on some date after today (the trade date).
   * The stepin date and cash settlement date are taken from the forward start date
   * (1 day and 3 working days by default). 
   * The maturity (of the index) is taken from the forward-start-date.
   * The index roll dates (when new indices are issued) are 20 Mar & Sep,
   * and the index is defined to have a maturity that is its nominal tenor plus 3M on issuance,
   * so a 5Y index on the 6-Feb-2014 will have a maturity of
   * 20-Dec-2018 (5Y3M on the issue date of 20-Sep-2013).  However for a trade-date of 6-Feb-2014, a forward-start-date
   * of 25-Mar-2014 and a tenor of 5Y, the maturity will be 20-Jun-2019. 
   * 
   * @param tradeDate  the trade date (i.e. today)
   * @param forwardStartDate  the forward start date
   * @param tenor  the tenor (nominal length) of the index at the forwardStartDate
   * @return a CDS analytic description for a forward starting index
   */
  public CdsAnalytic makeForwardStartingCdx(LocalDate tradeDate, LocalDate forwardStartDate, Period tenor) {
    LocalDate roll = ImmDateLogic.getNextIndexRollDate(forwardStartDate);
    LocalDate maturity = roll.plus(tenor).minusMonths(3);
    return makeForwardStartingCds(tradeDate, forwardStartDate, maturity);
  }

  //************************************************************************************************************************
  // Make MultiCdsAnalytic
  //************************************************************************************************************************

  /**
   * Make a CDS represented as a MultiCdsAnalytic instance. Note, this is mainly for testing,
   * since if you want only a single CDS should use a method that returns a {@link CdsAnalytic}.
   * 
   * @param tradeDate  the trade date
   * @param maturityReferanceDate  a reference date that maturities are measured from.
   *  For standard CDSSs, this is the next IMM  date after
   *  the trade date, so the actually maturities will be some fixed periods after this.  
   * @param termMatIndex  the maturities are fixed integer multiples of the payment interval, so  2Y tenor with a 3M 
   *  payment interval, this would be 8
   * @return a a CDS represented as a MultiCdsAnalytic
   */
  public MultiCdsAnalytic makeMultiCds(LocalDate tradeDate, LocalDate maturityReferanceDate, int termMatIndex) {
    int[] maturityIndexes = new int[termMatIndex + 1];
    for (int i = 0; i <= termMatIndex; i++) {
      maturityIndexes[i] = i;
    }
    LocalDate accStartDate = _businessdayAdjustmentConvention.adjust(ImmDateLogic.getPrevIMMDate(tradeDate), _calendar);
    return makeMultiCds(tradeDate, accStartDate, maturityReferanceDate, maturityIndexes);
  }

  /**
   * Make a set of CDS represented as a MultiCdsAnalytic instance.
   * 
   * @param tradeDate  the trade date
   * @param accStartDate This is when the CDS nominally starts in terms of the accrual calculation for premium payments.
   *  For a standard CDS this is  the previous IMM date, and for a `legacy' CDS it is T+1
   * @param maturityReferanceDate A reference date that maturities are measured from.
   *  For standard CDSSs, this is the next IMM  date after the trade date, so the actually maturities
   *  will be some fixed periods after this.  
   * @param maturityIndexes  the maturities are fixed integer multiples of the payment interval,
   *  so for 6M, 1Y and 2Y tenors with a 3M payment interval, would require 2, 4, and 8 as the indices 
   * @return Make a set of CDS represented as a MultiCdsAnalytic
   */
  public MultiCdsAnalytic makeMultiCds(
      LocalDate tradeDate,
      LocalDate accStartDate,
      LocalDate maturityReferanceDate,
      int[] maturityIndexes) {
    LocalDate stepinDate = tradeDate.plusDays(_stepIn);

    LocalDate valueDate = addWorkDays(tradeDate, _cashSettle, _calendar);
    return makeMultiCds(tradeDate, stepinDate, valueDate, accStartDate, maturityReferanceDate, maturityIndexes);
  }

  /**
   * Make a set of CDS represented as a MultiCdsAnalytic instance. 
   * 
   * @param tradeDate  the trade date
   * @param stepinDate  (aka Protection Effective sate or assignment date). Date when party assumes ownership.
   *  This is usually T+1. This is when protection (and risk) starts in terms of the model.
   *  Note, this is sometimes just called the Effective Date, however this can cause
   *  confusion with the legal effective date which is T-60 or T-90.
   * @param cashSettlementDate  the valuation date. The date that values are PVed to.
   *  Is is normally today + 3 business days.  Aka cash-settle date.
   * @param accStartDate  this is when the CDS nominally starts in terms of premium payments.  i.e. the number
   *  of days in the first period (and thus the amount of the first premium payment) is counted from this date.
   * @param maturityReferanceDate  a reference date that maturities are measured from.
   *  For standard CDSSs, this is the next IMM  date after
   *  the trade date, so the actually maturities will be some fixed periods after this.  
   * @param maturityIndexes  the maturities are fixed integer multiples of the payment interval,
   *  so for 6M, 1Y and 2Y tenors with a 3M payment interval, would require 2, 4, and 8 as the indices 
   * @return a set of CDS represented as a MultiCdsAnalytic
   */
  public MultiCdsAnalytic makeMultiCds(
      LocalDate tradeDate,
      LocalDate stepinDate,
      LocalDate cashSettlementDate,
      LocalDate accStartDate,
      LocalDate maturityReferanceDate,
      int[] maturityIndexes) {

    return new MultiCdsAnalytic(
        tradeDate, stepinDate, cashSettlementDate, accStartDate, maturityReferanceDate,
        maturityIndexes, _payAccOnDefault, _couponIntervalTenor, _stubType, _protectStart,
        _recoveryRate, _businessdayAdjustmentConvention, DEFAULT_CALENDAR, _accrualDayCount, _curveDayCount);
  }

  /**
   * Make a set of standard CDS represented as a MultiCdsAnalytic instance. 
   * 
   * @param tradeDate  the trade date
   * @param tenors  the tenors (length) of the CDS
   * @return a set of CDS represented as a MultiCdsAnalytic
   */
  public MultiCdsAnalytic makeMultiImmCds(LocalDate tradeDate, Period[] tenors) {
    LocalDate accStartDate = _businessdayAdjustmentConvention.adjust(ImmDateLogic.getPrevIMMDate(tradeDate), _calendar);
    return makeMultiImmCds(tradeDate, accStartDate, tenors);
  }

  /**
   * Make a set of standard CDS represented as a MultiCdsAnalytic instance. 
   *  
   * @param tradeDate  the trade date
   * @param accStartDate  the accrual start date 
   * @param tenors  the tenors (length) of the CDS
   * @return a set of CDS represented as a MultiCdsAnalytic
   */
  public MultiCdsAnalytic makeMultiImmCds(LocalDate tradeDate, LocalDate accStartDate, Period[] tenors) {

    ArgChecker.noNulls(tenors, "tenors");
    int n = tenors.length;

    int immNMonths = (int) DEFAULT_COUPON_INT.toTotalMonths();
    int[] matIndices = new int[n];
    for (int i = 0; i < n; i++) {
      int months = (int) tenors[i].toTotalMonths();
      if (months % immNMonths != 0) {
        throw new IllegalArgumentException("tenors index " + i + " is not a multiple of " + DEFAULT_COUPON_INT.toString());
      }
      matIndices[i] = months / immNMonths;
    }

    return makeMultiImmCds(tradeDate, accStartDate, matIndices);
  }

  /**
   * Make a set of standard CDS represented as a MultiCdsAnalytic instance.
   * The first CDS with have a tenor of firstTenor, while the last CDS will have a tenor of lastTenor;
   * the remaining CDS will consist of all the (multiple of 3 month) tenors between the first and
   * last tenor, e.g. if firstTenor = 6M and lastTenor = 5Y, there will be a total
   * of 22 CDS with tenors of 6M, 9M, 1Y,....4Y9M, 5Y.
   * 
   * @param tradeDate  the trade date
   * @param firstTenor  the first tenor 
   * @param lastTenor  the last tenor
   * @return a set of CDS represented as a MultiCdsAnalytic
   */
  public MultiCdsAnalytic makeMultiImmCds(LocalDate tradeDate, Period firstTenor, Period lastTenor) {
    ArgChecker.notNull(firstTenor, "firstTenor");
    ArgChecker.notNull(lastTenor, "lastTenor");
    int immNMonths = (int) DEFAULT_COUPON_INT.toTotalMonths();
    int m1 = (int) firstTenor.toTotalMonths();
    int m2 = (int) lastTenor.toTotalMonths();
    if (m1 % immNMonths != 0 || m2 % immNMonths != 0) {
      throw new IllegalArgumentException("tenors is not a multiple of " + DEFAULT_COUPON_INT.toString());
    }
    int firstIndex = m1 / immNMonths;
    int lastIndex = m2 / immNMonths;
    return makeMultiImmCds(tradeDate, firstIndex, lastIndex);
  }

  /**
   * Make a set of standard CDS represented as a MultiCdsAnalytic instance.
   * The maturities of the CDS are measured from the next IMM date (after the trade date), and 
   * the first and last tenors are the firstIndex and lastIndex multiplied by the coupon interval
   * (3 months), which the remaining tenors being everything in between.
   * 
   * @param tradeDate  the trade date 
   * @param firstIndex  the First index
   * @param lastIndex  the last index  
   * @return a set of CDS represented as a MultiCdsAnalytic
   */
  public MultiCdsAnalytic makeMultiImmCds(LocalDate tradeDate, int firstIndex, int lastIndex) {
    ArgChecker.isTrue(lastIndex > firstIndex, "Require lastIndex>firstIndex");
    ArgChecker.isTrue(firstIndex >= 0, "Require positive indices");
    int n = lastIndex - firstIndex + 1;
    int[] matIndices = new int[n];
    for (int i = 0; i < n; i++) {
      matIndices[i] = i + firstIndex;
    }
    return makeMultiImmCds(tradeDate, matIndices);
  }

  /**
   * Make a set of standard CDS represented as a MultiCdsAnalytic instance.
   * The maturities of the CDS are measured from the next IMM date (after the trade date), and 
   * the tenors are the given matIndices multiplied by the coupon interval (3 months).
   * 
   * @param tradeDate  the trade date
   * @param matIndices  the CDS tenors are these multiplied by the coupon interval (3 months)
   * @return a set of CDS represented as a MultiCdsAnalytic
   */
  public MultiCdsAnalytic makeMultiImmCds(LocalDate tradeDate, int[] matIndices) {
    LocalDate accStartDate = _businessdayAdjustmentConvention.adjust(ImmDateLogic.getPrevIMMDate(tradeDate), _calendar);
    return makeMultiImmCds(tradeDate, accStartDate, matIndices);
  }

  /**
   * Make a set of standard CDS represented as a MultiCdsAnalytic instance.
   * The maturities of the CDS are measured from the next IMM date (after the trade date), and 
   * the tenors are the given matIndices multiplied by the coupon interval (3 months).
   * 
   * @param tradeDate  the trade date
   * @param accStartDate  the accrual start date 
   * @param matIndices  the CDS tenors are these multiplied by the coupon interval (3 months)
   * @return a set of CDS represented as a MultiCdsAnalytic
   */
  public MultiCdsAnalytic makeMultiImmCds(LocalDate tradeDate, LocalDate accStartDate, int[] matIndices) {
    if (!_couponInterval.equals(DEFAULT_COUPON_INT)) {
      throw new IllegalArgumentException(
          "coupon interval must be 3M for this method. However it is set to " + _couponInterval.toString());
    }
    ArgChecker.notNull(tradeDate, "tradeDate");
    ArgChecker.notEmpty(matIndices, "matIndicies");

    LocalDate nextIMM = ImmDateLogic.getNextIMMDate(tradeDate);
    LocalDate stepinDate = tradeDate.plusDays(_stepIn);
    LocalDate valueDate = addWorkDays(tradeDate, _cashSettle, _calendar);

    return new MultiCdsAnalytic(
        tradeDate, stepinDate, valueDate, accStartDate, nextIMM, matIndices, _payAccOnDefault,
        _couponIntervalTenor, _stubType, _protectStart, _recoveryRate,
        _businessdayAdjustmentConvention, DEFAULT_CALENDAR, _accrualDayCount, _curveDayCount);
  }

  /**
   * Add a certain number of working days (defined by the holidayCalendar) to a date.
   * 
   * @param startDate  the start date
   * @param workingDaysToAdd  working days to add
   * @param calendar  the calendar of holidays
   * @return a working day
   */
  private static LocalDate addWorkDays(LocalDate startDate, int workingDaysToAdd, HolidayCalendar calendar) {
    ArgChecker.notNull(startDate, "startDate");
    ArgChecker.notNull(calendar, "calendar");

    int daysLeft = workingDaysToAdd;
    LocalDate temp = startDate;
    while (daysLeft > 0) {
      temp = temp.plusDays(1);
      if (calendar.isBusinessDay(temp)) {
        daysLeft--;
      }
    }
    return temp;
  }

}
