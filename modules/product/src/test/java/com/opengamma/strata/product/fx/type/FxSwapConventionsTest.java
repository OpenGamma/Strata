package com.opengamma.strata.product.fx.type;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.EUTA;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.GBLO;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.USNY;
import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.date.HolidayCalendarId;

/**
 * Test {@link FxSwapConventions}.
 */
@Test
public class FxSwapConventionsTest {

  private static final HolidayCalendarId EUTA_USNY = EUTA.combinedWith(USNY);
  private static final HolidayCalendarId GBLO_EUTA = GBLO.combinedWith(EUTA);
  private static final HolidayCalendarId GBLO_USNY = GBLO.combinedWith(USNY);

  @DataProvider(name = "spotLag")
  static Object[][] data_spot_lag() {
    return new Object[][] {
        {FxSwapConventions.EUR_USD, 2},
        {FxSwapConventions.GBP_EUR, 2},
        {FxSwapConventions.GBP_USD, 2}
    };
  }

  @Test(dataProvider = "spotLag")
  public void test_spot_lag(ImmutableFxSwapConvention convention, int lag) {
    assertEquals(convention.getSpotDateOffset().getDays(), lag);
  }

  @DataProvider(name = "currencyPair")
  static Object[][] data_currency_pair() {
    return new Object[][] {
        {FxSwapConventions.EUR_USD, CurrencyPair.of(EUR, USD)},
        {FxSwapConventions.GBP_EUR, CurrencyPair.of(GBP, EUR)},
        {FxSwapConventions.GBP_USD, CurrencyPair.of(GBP, USD)}
    };
  }

  @Test(dataProvider = "currencyPair")
  public void test_currency_pair(ImmutableFxSwapConvention convention, CurrencyPair ccys) {
    assertEquals(convention.getCurrencyPair(), ccys);
  }

  @DataProvider(name = "calendar")
  static Object[][] data_calendar() {
    return new Object[][] {
        {FxSwapConventions.EUR_USD, EUTA_USNY},
        {FxSwapConventions.GBP_EUR, GBLO_EUTA},
        {FxSwapConventions.GBP_USD, GBLO_USNY}
    };
  }

  @Test(dataProvider = "calendar")
  public void test_calendar(ImmutableFxSwapConvention convention, HolidayCalendarId cal) {
    assertEquals(convention.getSpotDateOffset().getCalendar(), cal);
    assertEquals(convention.getBusinessDayAdjustment().getCalendar(), cal);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverPrivateConstructor(FxSwapConventions.class);
    coverPrivateConstructor(StandardFxSwapConventions.class);
  }

}
