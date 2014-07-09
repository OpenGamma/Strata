/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.sesame.credit.converter;

import java.util.Set;

import org.threeten.bp.Period;

import com.opengamma.analytics.financial.credit.isdastandardmodel.CDSAnalytic;
import com.opengamma.analytics.financial.credit.isdastandardmodel.CDSAnalyticFactory;
import com.opengamma.core.holiday.HolidaySource;
import com.opengamma.financial.analytics.isda.credit.CreditCurveData;
import com.opengamma.financial.convention.HolidaySourceCalendarAdapter;
import com.opengamma.financial.convention.IsdaCreditCurveConvention;
import com.opengamma.financial.convention.calendar.Calendar;
import com.opengamma.financial.convention.frequency.PeriodFrequency;
import com.opengamma.financial.security.credit.LegacyCDSSecurity;
import com.opengamma.id.ExternalId;
import com.opengamma.sesame.Environment;
import com.opengamma.sesame.credit.IsdaCreditCurve;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.result.Result;

/**
 * Converts a legacy cds security to its analytics type.
 */
public class DefaultLegacyCdsConverterFn implements LegacyCdsConverterFn {

  private final HolidaySource _holidaySource;
  
  public DefaultLegacyCdsConverterFn(HolidaySource holidaySource) {
    _holidaySource = ArgumentChecker.notNull(holidaySource, "holidaySource");
  }

  @Override
  public Result<CDSAnalytic> toCdsAnalytic(Environment env, LegacyCDSSecurity legacyCds, IsdaCreditCurve curve) {
    CreditCurveData curveData = curve.getCurveData();
    IsdaCreditCurveConvention convention = curveData.getCurveConventionLink().resolve();
    Set<ExternalId> calendarIds = legacyCds.getCalendars();
    Calendar calendar = new HolidaySourceCalendarAdapter(_holidaySource, 
                                                         calendarIds.toArray(new ExternalId[calendarIds.size()]));
    Period couponFreq = PeriodFrequency.convertToPeriodFrequency(legacyCds.getCouponFrequency()).getPeriod();
    double recoveryRate;
    if (legacyCds.getFixedRecovery() != null) {
      recoveryRate = legacyCds.getFixedRecovery();
    } else {
      recoveryRate = curveData.getRecoveryRate();
    }
    CDSAnalyticFactory cdsAnalyticFactory = new CDSAnalyticFactory()
                                                .with(legacyCds.getBusinessDayConvention())
                                                .with(calendar)
                                                .with(couponFreq)
                                                //note - could equally drive this off 30 day IMM rule
                                                //if (first coupon date - trade date) < 30 days, FRONTLONG
                                                //else FRONTSHORT
                                                .with(convention.getStubType())
                                                .withAccrualDCC(legacyCds.getDayCount())
                                                .withCashSettle(convention.getCashSettle())
                                                .withCurveDCC(convention.getCurveDayCount())
                                                .withPayAccOnDefault(legacyCds.isAccruedOnDefault())
                                                .withProtectionStart(convention.isProtectFromStartOfDay()) 
                                                .withRecoveryRate(recoveryRate)
                                                .withStepIn(convention.getStepIn());
    
    CDSAnalytic cdsAnalytic = cdsAnalyticFactory.makeCDS(legacyCds.getTradeDate(), 
                                                         legacyCds.getStartDate(), 
                                                         legacyCds.getMaturityDate());
    return Result.success(cdsAnalytic);
  }

}
