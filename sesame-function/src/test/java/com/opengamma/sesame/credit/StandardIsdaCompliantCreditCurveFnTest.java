/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.sesame.credit;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

import java.util.Map;
import java.util.SortedMap;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.threeten.bp.LocalDate;
import org.threeten.bp.Period;

import com.google.common.collect.ImmutableSortedMap;
import com.opengamma.analytics.financial.credit.isdastandardmodel.ISDACompliantCreditCurve;
import com.opengamma.analytics.financial.credit.isdastandardmodel.ISDACompliantYieldCurve;
import com.opengamma.analytics.financial.credit.isdastandardmodel.StubType;
import com.opengamma.analytics.util.time.TimeCalculator;
import com.opengamma.core.link.ConventionLink;
import com.opengamma.financial.analytics.isda.credit.CdsQuote;
import com.opengamma.financial.analytics.isda.credit.CreditCurveData;
import com.opengamma.financial.analytics.isda.credit.CreditCurveDataKey;
import com.opengamma.financial.analytics.isda.credit.ParSpreadQuote;
import com.opengamma.financial.convention.IsdaCreditCurveConvention;
import com.opengamma.financial.convention.businessday.BusinessDayConventions;
import com.opengamma.financial.convention.calendar.MondayToFridayCalendar;
import com.opengamma.financial.convention.daycount.DayCounts;
import com.opengamma.sesame.Environment;
import com.opengamma.sesame.credit.snapshot.CreditCurveDataProviderFn;
import com.opengamma.util.money.Currency;
import com.opengamma.util.result.FailureStatus;
import com.opengamma.util.result.Result;
import com.opengamma.util.time.Tenor;

/**
 * Credit curve build test. 
 * 
 * Test data from /sesame-function/src/test/resources/credit/YC Test Data.xls
 * 
 */
public class StandardIsdaCompliantCreditCurveFnTest {

  private static final double DELTA = 10e-6; //TODO this should be much tighter
  
  private static final ISDACompliantYieldCurve YIELD_CURVE = CreditTestData.createYieldCurve();
  private static final LocalDate VALUATION_DATE = LocalDate.of(2014, 3, 27);
  
  private StandardIsdaCompliantCreditCurveFn _fn;
  private Environment _env;
  
  private final CreditCurveDataKey _goodKey = CreditCurveDataKey.builder().currency(Currency.USD).build();
  private final CreditCurveDataKey _badKey = CreditCurveDataKey.builder().currency(Currency.GBP).build();
  private IsdaCompliantYieldCurveFn _yieldCurveFn;
  private CreditCurveDataProviderFn _curveDataProviderFn;
  
  private static final SortedMap<LocalDate, Double> EXPECTED;

  static {
    EXPECTED = ImmutableSortedMap.<LocalDate, Double> naturalOrder()
                  .put(LocalDate.of(2014, 6, 1), 0.997588127691401)
                  .put(LocalDate.of(2014, 7, 1), 0.996493745958187)
                  .put(LocalDate.of(2014, 8, 1), 0.995364146080812)
                  .put(LocalDate.of(2014, 9, 1), 0.994235826689028)
                  .put(LocalDate.of(2014, 10, 1), 0.993145122522617)
                  .put(LocalDate.of(2014, 11, 1), 0.992019318559303)
                  .put(LocalDate.of(2014, 12, 1), 0.990931045963539)
                  .put(LocalDate.of(2015, 1, 1), 0.989807751820904)
                  .put(LocalDate.of(2015, 2, 1), 0.988685731015839)
                  .put(LocalDate.of(2015, 3, 1), 0.987673386129469)
                  .put(LocalDate.of(2015, 4, 1), 0.986553784786878)
                  .build();
  }


  @BeforeMethod
  public void beforeMethod() {
    _env = mock(Environment.class);
    when(_env.getValuationDate()).thenReturn(VALUATION_DATE);
    
    _yieldCurveFn = mock(IsdaCompliantYieldCurveFn.class);
    when(_yieldCurveFn.buildISDACompliantCurve(_env, Currency.USD)).thenReturn(Result.success(YIELD_CURVE));

    when(_yieldCurveFn.buildISDACompliantCurve(_env, Currency.USD)).thenReturn(Result.success(YIELD_CURVE));

    _curveDataProviderFn = mock(CreditCurveDataProviderFn.class);
    
    _fn = new StandardIsdaCompliantCreditCurveFn(_yieldCurveFn, _curveDataProviderFn);
    
    IsdaCreditCurveConvention curveConvention = new IsdaCreditCurveConvention();
    curveConvention.setAccrualDayCount(DayCounts.ACT_360);
    curveConvention.setBusinessDayConvention(BusinessDayConventions.FOLLOWING);
    curveConvention.setCashSettle(3);
    curveConvention.setCouponInterval(Period.ofMonths(3));
    curveConvention.setCurveDayCount(DayCounts.ACT_365);
    curveConvention.setPayAccOnDefault(true);
    curveConvention.setProtectFromStartOfDay(true);
    curveConvention.setRegionCalendar(new MondayToFridayCalendar("test"));
    curveConvention.setStepIn(1);
    curveConvention.setStubType(StubType.FRONTSHORT);
    
    ConventionLink<IsdaCreditCurveConvention> conventionLink = ConventionLink.resolved(curveConvention);
    SortedMap<Tenor, CdsQuote> spreadData = ImmutableSortedMap.<Tenor, CdsQuote> naturalOrder()
                                                .put(Tenor.FIVE_YEARS, ParSpreadQuote.from(0.007928))
                                                .build();
    CreditCurveData curveData = CreditCurveData.builder()
                .curveConventionLink(conventionLink)
                .recoveryRate(0.4)
                .cdsQuotes(spreadData)
                .build();
    
    when(_curveDataProviderFn.retrieveCreditCurveData(_goodKey)).thenReturn(Result.success(curveData));
                
  }
  
  @Test
  public void testCurveBuild() {
    //curve successfully bootstrapped
    Result<ISDACompliantCreditCurve> result = _fn.buildISDACompliantCreditCurve(_env, _goodKey);
    
    assertTrue("Expected success result", result.isSuccess());
    
    ISDACompliantCreditCurve curve = result.getValue();
    
    for (Map.Entry<LocalDate, Double> entry : EXPECTED.entrySet()) {
      double t = TimeCalculator.getTimeBetween(VALUATION_DATE, entry.getKey());
      double discountFactor = curve.getDiscountFactor(t);
      System.out.println(entry.getKey() + " " + discountFactor + " " + (discountFactor - entry.getValue()));
      double diff = Math.abs(discountFactor - entry.getValue());
      assertEquals(0, diff, DELTA);
    }

  }
  
  @Test
  public void testMissingCreditCurveData() {
    //credit curve data missing but yc present
    when(_yieldCurveFn.buildISDACompliantCurve(_env, Currency.GBP)).thenReturn(Result.success(mock(ISDACompliantYieldCurve.class)));
    when(_curveDataProviderFn.retrieveCreditCurveData(_badKey)).thenReturn(Result.<CreditCurveData> failure(FailureStatus.ERROR, "Error"));
    
    Result<ISDACompliantCreditCurve> result = _fn.buildISDACompliantCreditCurve(_env, _badKey);
    
    assertFalse("Expected failure result", result.isSuccess());
  }
  
  @Test
  public void testMissingYieldCurveData() {
    //yc missing but credit curve data present
    when(_yieldCurveFn.buildISDACompliantCurve(_env, Currency.GBP)).thenReturn(Result.<ISDACompliantYieldCurve> failure(FailureStatus.ERROR, "Error"));
    when(_curveDataProviderFn.retrieveCreditCurveData(_badKey)).thenReturn(Result.success(mock(CreditCurveData.class)));
    
    Result<ISDACompliantCreditCurve> result = _fn.buildISDACompliantCreditCurve(_env, _badKey);
    
    assertFalse("Expected failure result", result.isSuccess());
  }
  
}
