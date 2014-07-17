package com.opengamma.sesame.credit;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;

import java.util.Map;
import java.util.SortedMap;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.threeten.bp.LocalDate;

import com.google.common.collect.ImmutableSortedMap;
import com.opengamma.analytics.financial.credit.isdastandardmodel.ISDACompliantYieldCurve;
import com.opengamma.analytics.util.time.TimeCalculator;
import com.opengamma.core.holiday.HolidaySource;
import com.opengamma.core.region.RegionSource;
import com.opengamma.financial.analytics.isda.credit.YieldCurveData;
import com.opengamma.sesame.Environment;
import com.opengamma.sesame.credit.snapshot.YieldCurveDataProviderFn;
import com.opengamma.util.money.Currency;
import com.opengamma.util.result.FailureStatus;
import com.opengamma.util.result.Result;
import com.opengamma.util.test.TestGroup;

/**
 * YC build test. Test data from /sesame-function/src/test/resources/credit/YC Test Data.xls
 * 
 * Tests construction of a simple yield curve and a failure case where the yc data is missing.
 */
@Test(groups = TestGroup.UNIT)
public class DefaultIsdaCompliantYieldCurveFnTest {
  
  private static final double DELTA = 10e-15;

  private static final LocalDate VALUATION_DATE = LocalDate.of(2014,  3, 27);

  private IsdaCompliantYieldCurveFn _fn;

  private Environment _env;

  private static final SortedMap<LocalDate, Double> EXPECTED;
  
  static {
    //important note - discount factors are shifted back to trade date
    //by dividing by P(spot date, trade date). i.e. DF of spot date to
    //trade date. (This is >1 since trade date is before spot date.)
    //this step is necessary because ISDA model requires that future 
    //values are discounted back to spot date, not the trade date.
    //'valuation date' in OpenGamma refers to the trade date of the 
    //ISDA model so it is important to make the distinction.
    EXPECTED = ImmutableSortedMap.<LocalDate, Double> naturalOrder()
                  .put(LocalDate.of(2014, 6, 1), 0.998346299747064)
                  .put(LocalDate.of(2014, 7, 1), 0.996829575396936)
                  .put(LocalDate.of(2014, 8, 1), 0.994858240378622)
                  .put(LocalDate.of(2014, 9, 1), 0.992890803882032)
                  .put(LocalDate.of(2014, 10, 1), 0.990990537728945)
                  .put(LocalDate.of(2014, 11, 1), 0.989094157713670)
                  .put(LocalDate.of(2014, 12, 1), 0.987262406564133)
                  .put(LocalDate.of(2015, 1, 1), 0.985373160777861)
                  .put(LocalDate.of(2015, 2, 1), 0.983259485394869)
                  .put(LocalDate.of(2015, 3, 1), 0.981354256531180)
                  .put(LocalDate.of(2015, 4, 1), 0.979249201901534)
                  .build();
  }
  
  @BeforeMethod
  public void beforeMethod() {
    
    YieldCurveDataProviderFn providerFn = mock(YieldCurveDataProviderFn.class);
    
    when(providerFn.retrieveYieldCurveData(Currency.GBP)).
        thenReturn(Result.<YieldCurveData> failure(FailureStatus.ERROR, "test"));
    
    _fn = new DefaultIsdaCompliantYieldCurveFn(providerFn, 
                                               mock(RegionSource.class), 
                                               mock(HolidaySource.class));
    
    YieldCurveData ycData = CreditTestData.createYieldCurveData();
    
    when(providerFn.retrieveYieldCurveData(Currency.USD)).thenReturn(Result.success(ycData));
    
    _env = mock(Environment.class);
    
    when(_env.getValuationDate()).thenReturn(VALUATION_DATE);
    
  }

  @Test
  public void testProviderFailure() {
    Result<IsdaYieldCurve> result = _fn.buildIsdaCompliantCurve(_env, Currency.GBP);
    assertFalse("GBP curve data lookup failed so this call should too.", result.isSuccess());
  }
  
  @Test
  public void testBuildUSD() {
    Result<IsdaYieldCurve> usdCurve = _fn.buildIsdaCompliantCurve(_env, Currency.USD);
    
    ISDACompliantYieldCurve curve = usdCurve.getValue().getCalibratedCurve();
    
    assertNotNull(curve);
    
    for (Map.Entry<LocalDate, Double> entry : EXPECTED.entrySet()) {
      double t = TimeCalculator.getTimeBetween(VALUATION_DATE, entry.getKey());
      double discountFactor = curve.getDiscountFactor(t);
      System.out.println(entry.getKey() + " " + discountFactor + " " + (discountFactor - entry.getValue()));
      assertEquals(entry.getValue(), discountFactor, DELTA);
    }
  }
}
