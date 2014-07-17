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
import com.opengamma.analytics.financial.credit.isdastandardmodel.StubType;
import com.opengamma.analytics.util.time.TimeCalculator;
import com.opengamma.core.holiday.HolidaySource;
import com.opengamma.core.link.ConventionLink;
import com.opengamma.core.region.RegionSource;
import com.opengamma.financial.analytics.isda.credit.CdsQuote;
import com.opengamma.financial.analytics.isda.credit.CreditCurveData;
import com.opengamma.financial.analytics.isda.credit.CreditCurveDataKey;
import com.opengamma.financial.analytics.isda.credit.ParSpreadQuote;
import com.opengamma.financial.convention.IsdaCreditCurveConvention;
import com.opengamma.financial.convention.businessday.BusinessDayConventions;
import com.opengamma.financial.convention.daycount.DayCounts;
import com.opengamma.sesame.Environment;
import com.opengamma.sesame.credit.snapshot.CreditCurveDataProviderFn;
import com.opengamma.util.money.Currency;
import com.opengamma.util.result.FailureStatus;
import com.opengamma.util.result.Result;
import com.opengamma.util.test.TestGroup;
import com.opengamma.util.time.Tenor;

/**
 * Credit curve build test. 
 * 
 * Test data from /sesame-function/src/test/resources/credit/YC Test Data.xls
 * 
 */
@Test(groups = TestGroup.UNIT)
public class StandardIsdaCompliantCreditCurveFnTest {

  private static final double DELTA = 10e-15; 
  
  private static final IsdaYieldCurve YIELD_CURVE = IsdaYieldCurve.builder()
                                                                          .calibratedCurve(CreditTestData.createYieldCurve())
                                                                          .curveData(CreditTestData.createYieldCurveData())
                                                                          .build();
  private static final LocalDate VALUATION_DATE = LocalDate.of(2014, 3, 27);
  
  private StandardIsdaCompliantCreditCurveFn _fn;
  private Environment _env;
  
  private final CreditCurveDataKey _goodKey = CreditCurveDataKey.builder().curveName("USD").currency(Currency.USD).build();
  private final CreditCurveDataKey _badKey = CreditCurveDataKey.builder().curveName("GBP").currency(Currency.GBP).build();
  private IsdaCompliantYieldCurveFn _yieldCurveFn;
  private CreditCurveDataProviderFn _curveDataProviderFn;
  
  private static final SortedMap<LocalDate, Double> EXPECTED;

  static {
    EXPECTED = ImmutableSortedMap.<LocalDate, Double> naturalOrder()
                  .put(LocalDate.of(2014, 6, 1), 0.997588273438233)
                  .put(LocalDate.of(2014, 7, 1), 0.996493957721021)
                  .put(LocalDate.of(2014, 8, 1), 0.995364425907840)
                  .put(LocalDate.of(2014, 9, 1), 0.994236174425670)
                  .put(LocalDate.of(2014, 10, 1), 0.993145535831308)
                  .put(LocalDate.of(2014, 11, 1), 0.992019799474205)
                  .put(LocalDate.of(2014, 12, 1), 0.990931592157365)
                  .put(LocalDate.of(2015, 1, 1), 0.989808365318550)
                  .put(LocalDate.of(2015, 2, 1), 0.988686411664022)
                  .put(LocalDate.of(2015, 3, 1), 0.987674127298215)
                  .put(LocalDate.of(2015, 4, 1), 0.986554592815145)
                  .build();
  }


  @BeforeMethod
  public void beforeMethod() {
    _env = mock(Environment.class);
    when(_env.getValuationDate()).thenReturn(VALUATION_DATE);
    
    _yieldCurveFn = mock(IsdaCompliantYieldCurveFn.class);
    when(_yieldCurveFn.buildIsdaCompliantCurve(_env, Currency.USD)).thenReturn(Result.success(YIELD_CURVE));

    when(_yieldCurveFn.buildIsdaCompliantCurve(_env, Currency.USD)).thenReturn(Result.success(YIELD_CURVE));

    _curveDataProviderFn = mock(CreditCurveDataProviderFn.class);
    
    HolidaySource holidaySource = mock(HolidaySource.class);
    RegionSource regionSource = mock(RegionSource.class);
    
    _fn = new StandardIsdaCompliantCreditCurveFn(_yieldCurveFn, _curveDataProviderFn, holidaySource, regionSource);    
    IsdaCreditCurveConvention curveConvention = new IsdaCreditCurveConvention();
    curveConvention.setAccrualDayCount(DayCounts.ACT_360);
    curveConvention.setBusinessDayConvention(BusinessDayConventions.FOLLOWING);
    curveConvention.setCashSettle(3);
    curveConvention.setCouponInterval(Period.ofMonths(3));
    curveConvention.setCurveDayCount(DayCounts.ACT_365);
    curveConvention.setPayAccOnDefault(true);
    curveConvention.setProtectFromStartOfDay(true);
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
    Result<IsdaCreditCurve> result = _fn.buildIsdaCompliantCreditCurve(_env, _goodKey);
    
    assertTrue("Expected success result", result.isSuccess());
    
    ISDACompliantCreditCurve curve = result.getValue().getCalibratedCurve();
    
    for (Map.Entry<LocalDate, Double> entry : EXPECTED.entrySet()) {
      double t = TimeCalculator.getTimeBetween(VALUATION_DATE, entry.getKey());
      double discountFactor = curve.getDiscountFactor(t);
      System.out.println(entry.getKey() + " " + discountFactor + " " + (discountFactor - entry.getValue()));
      assertEquals(entry.getValue(), discountFactor, DELTA);
    }

  }
  
  @Test
  public void testMissingCreditCurveData() {
    //credit curve data missing but yc present
    when(_yieldCurveFn.buildIsdaCompliantCurve(_env, Currency.GBP)).thenReturn(Result.success(YIELD_CURVE));
    when(_curveDataProviderFn.retrieveCreditCurveData(_badKey))
        .thenReturn(Result.<CreditCurveData> failure(FailureStatus.ERROR, "Error"));
    
    Result<IsdaCreditCurve> result = _fn.buildIsdaCompliantCreditCurve(_env, _badKey);
    
    assertFalse("Expected failure result", result.isSuccess());
  }
  
  @Test
  public void testMissingYieldCurveData() {
    //yc missing but credit curve data present
    when(_yieldCurveFn.buildIsdaCompliantCurve(_env, Currency.GBP))
        .thenReturn(Result.<IsdaYieldCurve> failure(FailureStatus.ERROR, "Error"));
    when(_curveDataProviderFn.retrieveCreditCurveData(_badKey))
        .thenReturn(Result.success(mock(CreditCurveData.class)));
    
    Result<IsdaCreditCurve> result = _fn.buildIsdaCompliantCreditCurve(_env, _badKey);
    
    assertFalse("Expected failure result", result.isSuccess());
  }
  
}
