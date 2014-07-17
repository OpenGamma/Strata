/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.sesame.credit.converter;

import com.opengamma.analytics.financial.credit.isdastandardmodel.CDSAnalytic;
import com.opengamma.analytics.financial.credit.isdastandardmodel.CDSAnalyticFactory;
import com.opengamma.core.holiday.HolidaySource;
import com.opengamma.core.region.RegionSource;
import com.opengamma.financial.analytics.conversion.CalendarUtils;
import com.opengamma.financial.analytics.isda.credit.CreditCurveData;
import com.opengamma.financial.convention.IsdaCreditCurveConvention;
import com.opengamma.financial.convention.calendar.Calendar;
import com.opengamma.financial.convention.calendar.MondayToFridayCalendar;
import com.opengamma.financial.security.credit.StandardCDSSecurity;
import com.opengamma.id.ExternalId;
import com.opengamma.sesame.Environment;
import com.opengamma.sesame.credit.IsdaCreditCurve;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.result.Result;

/**
 * Converts a standard cds to its analytics type, {@link CDSAnalytic}.
 */
public class DefaultStandardCdsConverterFn implements StandardCdsConverterFn {

  private final RegionSource _regionSource;
  private final HolidaySource _holidaySource;
  
  /**
   * Creates an instance
   * 
   * @param regionSource the region source for resolving holiday calendars
   * @param holidaySource the holiday source for resolving holiday calendars
   */
  public DefaultStandardCdsConverterFn(RegionSource regionSource, HolidaySource holidaySource) {
    _regionSource = ArgumentChecker.notNull(regionSource, "regionSource");
    _holidaySource = ArgumentChecker.notNull(holidaySource, "holidaySource");
  }

  @Override
  public Result<CDSAnalytic> toCdsAnalytic(Environment env, StandardCDSSecurity cds, IsdaCreditCurve curve) {
    CreditCurveData curveData = curve.getCurveData();
    IsdaCreditCurveConvention convention = curveData.getCurveConventionLink().resolve();
    ExternalId regionId = convention.getRegionId();
    Calendar calendar;
    if (regionId == null) {
      calendar = new MondayToFridayCalendar("weekday calendar");
    } else {
      calendar = CalendarUtils.getCalendar(_regionSource, _holidaySource, regionId);
    }
    
    CDSAnalyticFactory factory = new CDSAnalyticFactory()
                                      .with(convention.getBusinessDayConvention())
                                      .with(calendar)
                                      .with(convention.getCouponInterval())
                                      .with(convention.getStubType())
                                      .withAccrualDCC(convention.getAccrualDayCount())
                                      .withCashSettle(convention.getCashSettle())
                                      .withCurveDCC(convention.getCurveDayCount())
                                      .withPayAccOnDefault(convention.isPayAccOnDefault())
                                      .withProtectionStart(convention.isProtectFromStartOfDay())
                                      .withRecoveryRate(curveData.getRecoveryRate())
                                      .withStepIn(convention.getStepIn());
    CDSAnalytic cdsAnalytic = factory.makeIMMCDS(cds.getTradeDate(), cds.getTenor().getPeriod());
    return Result.success(cdsAnalytic);
  }

}
