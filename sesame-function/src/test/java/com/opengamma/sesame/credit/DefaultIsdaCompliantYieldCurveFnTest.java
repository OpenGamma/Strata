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
import org.threeten.bp.Period;

import com.google.common.collect.ImmutableSortedMap;
import com.opengamma.analytics.financial.credit.isdastandardmodel.ISDACompliantYieldCurve;
import com.opengamma.analytics.util.time.TimeCalculator;
import com.opengamma.financial.analytics.isda.credit.YieldCurveData;
import com.opengamma.financial.convention.businessday.BusinessDayConventions;
import com.opengamma.financial.convention.calendar.MondayToFridayCalendar;
import com.opengamma.financial.convention.daycount.DayCounts;
import com.opengamma.sesame.Environment;
import com.opengamma.sesame.credit.snapshot.YieldCurveDataProviderFn;
import com.opengamma.util.money.Currency;
import com.opengamma.util.result.FailureStatus;
import com.opengamma.util.result.Result;
import com.opengamma.util.test.TestGroup;
import com.opengamma.util.time.Tenor;

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
    EXPECTED = ImmutableSortedMap.<LocalDate, Double> naturalOrder()
                  .put(LocalDate.of(2014, 6, 1), 0.998146703581420)
                  .put(LocalDate.of(2014, 7, 1), 0.996603172154409)
                  .put(LocalDate.of(2014, 8, 1), 0.994637268984417)
                  .put(LocalDate.of(2014, 9, 1), 0.992675243762419)
                  .put(LocalDate.of(2014, 10, 1), 0.990783514347689)
                  .put(LocalDate.of(2014, 11, 1), 0.988880444056390)
                  .put(LocalDate.of(2014, 12, 1), 0.987042243596708)
                  .put(LocalDate.of(2015, 1, 1), 0.985123839033025)
                  .put(LocalDate.of(2015, 2, 1), 0.982999405917283)
                  .put(LocalDate.of(2015, 3, 1), 0.981084501161757)
                  .put(LocalDate.of(2015, 4, 1), 0.979151846830480)
                  .build();
  }
  
  @BeforeMethod
  public void beforeMethod() {
    
    YieldCurveDataProviderFn providerFn = mock(YieldCurveDataProviderFn.class);
    
    when(providerFn.retrieveYieldCurveData(Currency.GBP)).
        thenReturn(Result.<YieldCurveData> failure(FailureStatus.ERROR, "test"));
    
    _fn = new DefaultIsdaCompliantYieldCurveFn(providerFn);
    
    SortedMap<Tenor, Double> cashData = ImmutableSortedMap.<Tenor, Double> naturalOrder()
                                          .put(Tenor.ONE_MONTH, 0.00445)
                                          .put(Tenor.TWO_MONTHS, 0.009488)
                                          .put(Tenor.THREE_MONTHS, 0.012337)
                                          .put(Tenor.SIX_MONTHS, 0.017762)
                                          .put(Tenor.NINE_MONTHS, 0.01935)
                                          .put(Tenor.ONE_YEAR, 0.020838)
                                          .build();
    
    @SuppressWarnings("deprecation")
    SortedMap<Tenor, Double> swapData = ImmutableSortedMap.<Tenor, Double> naturalOrder()
                                          .put(Tenor.TWO_YEARS, 0.01652)
                                          .put(Tenor.THREE_YEARS, 0.02018)
                                          .put(Tenor.FOUR_YEARS, 0.023033)
                                          .put(Tenor.FIVE_YEARS, 0.02525)
                                          .put(Tenor.SIX_YEARS, 0.02696)
                                          .put(Tenor.SEVEN_YEARS, 0.02825)
                                          .put(Tenor.EIGHT_YEARS, 0.02931)
                                          .put(Tenor.NINE_YEARS, 0.03017)
                                          .put(Tenor.TEN_YEARS, 0.03092)
                                          .put(new Tenor(Period.ofYears(11)), 0.0316)
                                          .put(new Tenor(Period.ofYears(12)), 0.03231)
                                          .put(new Tenor(Period.ofYears(15)), 0.03367)
                                          .put(new Tenor(Period.ofYears(20)), 0.03419)
                                          .put(new Tenor(Period.ofYears(25)), 0.03411)
                                          .put(new Tenor(Period.ofYears(30)), 0.03412)
                                          .build();
    
    YieldCurveData ycData = YieldCurveData.builder()
                      .cashData(cashData)
                      .swapData(swapData)
                      .calendar(new MondayToFridayCalendar("test"))
                      .cashDayCount(DayCounts.ACT_360)
                      .currency(Currency.USD)
                      .curveBusinessDayConvention(BusinessDayConventions.MODIFIED_FOLLOWING)
                      .curveDayCount(DayCounts.ACT_365)
                      //note - spot would normally be T+3 but
                      //use valuation date to be consistent with sheet
                      .spotDate(VALUATION_DATE)
                      .swapDayCount(DayCounts.THIRTY_360)
                      .swapFixedLegInterval(Tenor.ONE_YEAR)
                      .build();
    
    when(providerFn.retrieveYieldCurveData(Currency.USD)).thenReturn(Result.success(ycData));
    
    _env = mock(Environment.class);
    
    when(_env.getValuationDate()).thenReturn(VALUATION_DATE);
    
  }

  @Test
  public void testProviderFailure() {
    Result<ISDACompliantYieldCurve> result = _fn.buildIsdaCompliantCurve(_env, Currency.GBP);
    assertFalse("GBP curve data lookup failed so this call should too.", result.isSuccess());
  }
  
  @Test
  public void testBuildUSD() {
    Result<ISDACompliantYieldCurve> usdCurve = _fn.buildIsdaCompliantCurve(_env, Currency.USD);
    
    ISDACompliantYieldCurve curve = usdCurve.getValue();
    
    assertNotNull(curve);
    
    for (Map.Entry<LocalDate, Double> entry : EXPECTED.entrySet()) {
      double t = TimeCalculator.getTimeBetween(VALUATION_DATE, entry.getKey());
      double discountFactor = curve.getDiscountFactor(t);
      System.out.println(entry.getKey() + " " + discountFactor + " " + (discountFactor - entry.getValue()));
      double diff = Math.abs(discountFactor - entry.getValue());
      assertEquals(0, diff, DELTA);
    }
  }
}
