/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.sesame.credit;

import java.util.Map;
import java.util.SortedMap;

import org.threeten.bp.LocalDate;

import com.opengamma.analytics.financial.credit.isdastandardmodel.CDSAnalytic;
import com.opengamma.analytics.financial.credit.isdastandardmodel.CDSAnalyticFactory;
import com.opengamma.analytics.financial.credit.isdastandardmodel.CDSQuoteConvention;
import com.opengamma.analytics.financial.credit.isdastandardmodel.FastCreditCurveBuilder;
import com.opengamma.analytics.financial.credit.isdastandardmodel.ISDACompliantCreditCurve;
import com.opengamma.analytics.financial.credit.isdastandardmodel.ISDACompliantYieldCurve;
import com.opengamma.financial.analytics.isda.credit.CdsQuote;
import com.opengamma.financial.analytics.isda.credit.CreditCurveData;
import com.opengamma.financial.analytics.isda.credit.CreditCurveDataKey;
import com.opengamma.financial.convention.IsdaCreditCurveConvention;
import com.opengamma.sesame.Environment;
import com.opengamma.sesame.credit.snapshot.CreditCurveDataProviderFn;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.result.Result;
import com.opengamma.util.time.Tenor;

/**
 * Builds ISDA compliant credit curves using standard cds contracts to define the curve instruments.
 */
public class StandardIsdaCompliantCreditCurveFn implements IsdaCompliantCreditCurveFn {
  
  private static final FastCreditCurveBuilder CREDIT_CURVE_BUILDER = new FastCreditCurveBuilder();
  
  private final IsdaCompliantYieldCurveFn _yieldCurveFn;
  
  private final CreditCurveDataProviderFn _curveDataProviderFn;
  
  /**
   * Constructor for function.
   * @param yieldCurveFn function for sourcing yield curves
   * @param curveDataProviderFn function for sourcing credit curve data
   */
  public StandardIsdaCompliantCreditCurveFn(IsdaCompliantYieldCurveFn yieldCurveFn, CreditCurveDataProviderFn curveDataProviderFn) {
    _yieldCurveFn = ArgumentChecker.notNull(yieldCurveFn, "yieldCurveFn");
    _curveDataProviderFn = ArgumentChecker.notNull(curveDataProviderFn, "curveDataProviderFn");
  }

  @Override
  public Result<ISDACompliantCreditCurve> buildISDACompliantCreditCurve(Environment env, CreditCurveDataKey creditCurveKey) {
    
    Result<ISDACompliantYieldCurve> yieldCurveResult = _yieldCurveFn.buildISDACompliantCurve(env, creditCurveKey.getCurrency());
    
    Result<CreditCurveData> creditCurveDataResult = _curveDataProviderFn.retrieveCreditCurveData(creditCurveKey);
    
    if (Result.anyFailures(creditCurveDataResult, yieldCurveResult)) {
      return Result.failure(creditCurveDataResult, yieldCurveResult);
    }
    
    ISDACompliantCreditCurve curve = buildWithResolvedData(env, yieldCurveResult.getValue(), creditCurveDataResult.getValue());
    
    return Result.success(curve);
  }

  private ISDACompliantCreditCurve buildWithResolvedData(Environment env, ISDACompliantYieldCurve yieldCurve, CreditCurveData creditCurveData) {
    
    IsdaCreditCurveConvention convention = creditCurveData.getCurveConventionLink().resolve();
    
    int curveLength = creditCurveData.getCdsQuotes().size();
    CDSAnalytic[] calibrationCDSs = new CDSAnalytic[curveLength];
    CDSQuoteConvention[] quotes = new CDSQuoteConvention[curveLength];
    
    SortedMap<Tenor, CdsQuote> spreadData = creditCurveData.getCdsQuotes();
    
    CDSAnalyticFactory cdsFactory = new CDSAnalyticFactory()
                                          .with(convention.getBusinessDayConvention())
                                          .with(convention.getRegionCalendar())
                                          .with(convention.getCouponInterval())
                                          .with(convention.getStubType())
                                          .withAccrualDCC(convention.getAccrualDayCount())
                                          .withCashSettle(convention.getCashSettle())
                                          .withCurveDCC(convention.getCurveDayCount())
                                          .withPayAccOnDefault(convention.isPayAccOnDefault())
                                          .withProtectionStart(convention.isProtectFromStartOfDay())
                                          .withRecoveryRate(creditCurveData.getRecoveryRate())
                                          .withStepIn(convention.getStepIn());
    int i = 0;
    
    LocalDate valuationDate = env.getValuationDate();
    
    for (Map.Entry<Tenor, CdsQuote> spreadEntry : spreadData.entrySet()) {
      
      calibrationCDSs[i] = cdsFactory.makeIMMCDS(valuationDate, spreadEntry.getKey().getPeriod(), false);
      
      quotes[i] = spreadEntry.getValue().toQuoteConvention();
      i++;
    }
    
    return CREDIT_CURVE_BUILDER.calibrateCreditCurve(calibrationCDSs, quotes, yieldCurve);
    
  }



}
