/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.sesame.credit.converter;

import com.opengamma.analytics.financial.credit.isdastandardmodel.CDSAnalytic;
import com.opengamma.analytics.financial.credit.isdastandardmodel.CDSAnalyticFactory;
import com.opengamma.financial.analytics.isda.credit.CreditCurveData;
import com.opengamma.financial.convention.IsdaCreditCurveConvention;
import com.opengamma.financial.security.credit.StandardCDSSecurity;
import com.opengamma.sesame.Environment;
import com.opengamma.sesame.credit.IsdaCreditCurve;
import com.opengamma.util.result.Result;

/**
 * Converts a standard cds to its analytics type, {@link CDSAnalytic}.
 */
public class DefaultStandardCdsConverterFn implements StandardCdsConverterFn {

  @Override
  public Result<CDSAnalytic> toCdsAnalytic(Environment env, StandardCDSSecurity cds, IsdaCreditCurve curve) {
    CreditCurveData curveData = curve.getCurveData();
    IsdaCreditCurveConvention convention = curveData.getCurveConventionLink().resolve();
    CDSAnalyticFactory factory = new CDSAnalyticFactory()
                                      .with(convention.getBusinessDayConvention())
                                      .with(convention.getRegionCalendar())
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
