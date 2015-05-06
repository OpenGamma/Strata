/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.index;

import static com.opengamma.strata.basics.currency.Currency.CHF;
import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.JPY;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.BusinessDayConventions.FOLLOWING;
import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.strata.basics.date.BusinessDayConventions.PRECEDING;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.date.HolidayCalendars.CHZU;
import static com.opengamma.strata.basics.date.HolidayCalendars.EUTA;
import static com.opengamma.strata.basics.date.HolidayCalendars.GBLO;
import static com.opengamma.strata.basics.date.HolidayCalendars.JPTO;
import static com.opengamma.strata.basics.date.HolidayCalendars.USNY;
import static com.opengamma.strata.basics.date.Tenor.TENOR_12M;
import static com.opengamma.strata.basics.date.Tenor.TENOR_1M;
import static com.opengamma.strata.basics.date.Tenor.TENOR_1W;
import static com.opengamma.strata.basics.date.Tenor.TENOR_2M;
import static com.opengamma.strata.basics.date.Tenor.TENOR_2W;
import static com.opengamma.strata.basics.date.Tenor.TENOR_3M;
import static com.opengamma.strata.basics.date.Tenor.TENOR_6M;
import static com.opengamma.strata.basics.date.Tenor.TENOR_9M;

import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.HolidayCalendar;
import com.opengamma.strata.basics.date.PeriodAdditionConventions;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.basics.date.TenorAdjustment;

/**
 * Standard IBOR index implementations.
 * <p>
 * See {@link IborIndices} for the description of each.
 */
final class StandardIborIndices {
  // http://www.opengamma.com/sites/default/files/interest-rate-instruments-and-market-conventions.pdf
  // LIBOR - http://www.bbalibor.com/technical-aspects/fixing-value-and-maturity
  // different rules for overnight
  // EURIBOR - http://www.bbalibor.com/technical-aspects/fixing-value-and-maturity
  // EURIBOR - http://www.emmi-benchmarks.eu/assets/files/Euribor_code_conduct.pdf
  // TIBOR - http://www.jbatibor.or.jp/english/public/

  // GBP libor
  public static final IborIndex GBP_LIBOR_1W = gbpLibor(TENOR_1W);
  public static final IborIndex GBP_LIBOR_1M = gbpLibor(TENOR_1M);
  public static final IborIndex GBP_LIBOR_2M = gbpLibor(TENOR_2M);
  public static final IborIndex GBP_LIBOR_3M = gbpLibor(TENOR_3M);
  public static final IborIndex GBP_LIBOR_6M = gbpLibor(TENOR_6M);
  public static final IborIndex GBP_LIBOR_12M = gbpLibor(TENOR_12M);

  // conventions for GBP libor
  // settlement date equals fixing date
  // maturity date last business day rule, modified following, GBLO
  private static IborIndex gbpLibor(Tenor tenor) {
    return ImmutableIborIndex.builder()
        .name("GBP-LIBOR-" + tenor)
        .currency(GBP)
        .fixingCalendar(GBLO)
        .fixingDateOffset(DaysAdjustment.ofCalendarDays(0, BusinessDayAdjustment.of(PRECEDING, GBLO)))
        .effectiveDateOffset(DaysAdjustment.ofCalendarDays(0, BusinessDayAdjustment.of(FOLLOWING, GBLO)))
        .maturityDateOffset(maturity(tenor, GBLO))
        .dayCount(ACT_365F)
        .build();
  }

  //-------------------------------------------------------------------------
  // CHF libor
  public static final IborIndex CHF_LIBOR_1W = chfLibor(TENOR_1W);
  public static final IborIndex CHF_LIBOR_1M = chfLibor(TENOR_1M);
  public static final IborIndex CHF_LIBOR_2M = chfLibor(TENOR_2M);
  public static final IborIndex CHF_LIBOR_3M = chfLibor(TENOR_3M);
  public static final IborIndex CHF_LIBOR_6M = chfLibor(TENOR_6M);
  public static final IborIndex CHF_LIBOR_12M = chfLibor(TENOR_12M);

  // conventions for CHF libor
  // settlement date two GBLO business days after fixing date, adjusted to GBLO+CHZU
  // maturity date last business day rule, modified following, GBLO+CHZU
  private static IborIndex chfLibor(Tenor tenor) {
    HolidayCalendar cal = GBLO.combineWith(CHZU);
    return ImmutableIborIndex.builder()
        .name("CHF-LIBOR-" + tenor)
        .currency(CHF)
        .fixingCalendar(GBLO)
        .fixingDateOffset(DaysAdjustment.ofBusinessDays(-2, GBLO))
        .effectiveDateOffset(DaysAdjustment.ofBusinessDays(2, GBLO, BusinessDayAdjustment.of(FOLLOWING, cal)))
        .maturityDateOffset(maturity(tenor, cal))
        .dayCount(ACT_360)
        .build();
  }

  //-------------------------------------------------------------------------
  // EUR libor
  public static final IborIndex EUR_LIBOR_1W = eurLibor(TENOR_1W);
  public static final IborIndex EUR_LIBOR_1M = eurLibor(TENOR_1M);
  public static final IborIndex EUR_LIBOR_2M = eurLibor(TENOR_2M);
  public static final IborIndex EUR_LIBOR_3M = eurLibor(TENOR_3M);
  public static final IborIndex EUR_LIBOR_6M = eurLibor(TENOR_6M);
  public static final IborIndex EUR_LIBOR_12M = eurLibor(TENOR_12M);

  // conventions for EUR libor
  // settlement date two EUTA business days after fixing date
  // maturity date last business day rule, modified following, EUTA
  private static IborIndex eurLibor(Tenor tenor) {
    return ImmutableIborIndex.builder()
        .name("EUR-LIBOR-" + tenor)
        .currency(EUR)
        .fixingCalendar(GBLO)
        .fixingDateOffset(DaysAdjustment.ofBusinessDays(-2, EUTA, BusinessDayAdjustment.of(PRECEDING, GBLO)))
        .effectiveDateOffset(DaysAdjustment.ofBusinessDays(2, EUTA))
        .maturityDateOffset(maturity(tenor, EUTA))
        .dayCount(ACT_360)
        .build();
  }

  //-------------------------------------------------------------------------
  // JPY libor
  public static final IborIndex JPY_LIBOR_1W = jpyLibor(TENOR_1W);
  public static final IborIndex JPY_LIBOR_1M = jpyLibor(TENOR_1M);
  public static final IborIndex JPY_LIBOR_2M = jpyLibor(TENOR_2M);
  public static final IborIndex JPY_LIBOR_3M = jpyLibor(TENOR_3M);
  public static final IborIndex JPY_LIBOR_6M = jpyLibor(TENOR_6M);
  public static final IborIndex JPY_LIBOR_12M = jpyLibor(TENOR_12M);

  // conventions for JPY libor
  // settlement date two GBLO business days after fixing date, adjusted to GBLO+JPTO
  // maturity date last business day rule, modified following, GBLO+JPTO
  private static IborIndex jpyLibor(Tenor tenor) {
    HolidayCalendar cal = GBLO.combineWith(JPTO);
    return ImmutableIborIndex.builder()
        .name("JPY-LIBOR-" + tenor)
        .currency(JPY)
        .fixingCalendar(GBLO)
        .fixingDateOffset(DaysAdjustment.ofBusinessDays(-2, GBLO))
        .effectiveDateOffset(DaysAdjustment.ofBusinessDays(2, GBLO, BusinessDayAdjustment.of(FOLLOWING, cal)))
        .maturityDateOffset(maturity(tenor, cal))
        .dayCount(ACT_360)
        .build();
  }

  //-------------------------------------------------------------------------
  // USD libor
  public static final IborIndex USD_LIBOR_1W = usdLibor(TENOR_1W);
  public static final IborIndex USD_LIBOR_1M = usdLibor(TENOR_1M);
  public static final IborIndex USD_LIBOR_2M = usdLibor(TENOR_2M);
  public static final IborIndex USD_LIBOR_3M = usdLibor(TENOR_3M);
  public static final IborIndex USD_LIBOR_6M = usdLibor(TENOR_6M);
  public static final IborIndex USD_LIBOR_12M = usdLibor(TENOR_12M);

  // conventions for USD libor
  // settlement date two GBLO business days after fixing date, adjusted to GBLO+USNY
  // maturity date last business day rule, modified following, GBLO+USNY
  private static IborIndex usdLibor(Tenor tenor) {
    HolidayCalendar cal = GBLO.combineWith(USNY);
    return ImmutableIborIndex.builder()
        .name("USD-LIBOR-" + tenor)
        .currency(USD)
        .fixingCalendar(GBLO)
        .fixingDateOffset(DaysAdjustment.ofBusinessDays(-2, GBLO))
        .effectiveDateOffset(DaysAdjustment.ofBusinessDays(2, GBLO, BusinessDayAdjustment.of(FOLLOWING, cal)))
        .maturityDateOffset(maturity(tenor, cal))
        .dayCount(ACT_360)
        .build();
  }

  //-------------------------------------------------------------------------
  // Euribor
  public static final IborIndex EUR_EURIBOR_1W = euribor(TENOR_1W);
  public static final IborIndex EUR_EURIBOR_2W = euribor(TENOR_2W);
  public static final IborIndex EUR_EURIBOR_1M = euribor(TENOR_1M);
  public static final IborIndex EUR_EURIBOR_2M = euribor(TENOR_2M);
  public static final IborIndex EUR_EURIBOR_3M = euribor(TENOR_3M);
  public static final IborIndex EUR_EURIBOR_6M = euribor(TENOR_6M);
  public static final IborIndex EUR_EURIBOR_9M = euribor(TENOR_9M);
  public static final IborIndex EUR_EURIBOR_12M = euribor(TENOR_12M);

  // conventions for euribor
  private static IborIndex euribor(Tenor tenor) {
    return ImmutableIborIndex.builder()
        .name("EUR-EURIBOR-" + tenor)
        .currency(EUR)
        .fixingCalendar(EUTA)
        .fixingDateOffset(DaysAdjustment.ofBusinessDays(-2, EUTA))
        .effectiveDateOffset(DaysAdjustment.ofBusinessDays(2, EUTA))
        .maturityDateOffset(maturity(tenor, EUTA))
        .dayCount(ACT_360)
        .build();
  }

  //-------------------------------------------------------------------------
  // JPY Tibor (Japan)
  // only include some tenors here
  public static final IborIndex JPY_TIBOR_JAPAN_1W = tiborJapan(TENOR_1W);
  public static final IborIndex JPY_TIBOR_JAPAN_1M = tiborJapan(TENOR_1M);
  public static final IborIndex JPY_TIBOR_JAPAN_2M = tiborJapan(TENOR_2M);
  public static final IborIndex JPY_TIBOR_JAPAN_3M = tiborJapan(TENOR_3M);
  public static final IborIndex JPY_TIBOR_JAPAN_6M = tiborJapan(TENOR_6M);
  public static final IborIndex JPY_TIBOR_JAPAN_12M = tiborJapan(TENOR_12M);

  // conventions for tibor
  private static IborIndex tiborJapan(Tenor tenor) {
    return ImmutableIborIndex.builder()
        .name("JPY-TIBOR-JAPAN-" + tenor)
        .currency(JPY)
        .fixingCalendar(JPTO)
        .fixingDateOffset(DaysAdjustment.ofBusinessDays(-2, JPTO))
        .effectiveDateOffset(DaysAdjustment.ofBusinessDays(2, JPTO))
        .maturityDateOffset(maturity(tenor, JPTO))
        .dayCount(ACT_365F)
        .build();
  }

  //-------------------------------------------------------------------------
  // JPY Tibor (Euroyen)
  // only include some tenors here
  public static final IborIndex JPY_TIBOR_EUROYEN_1W = tiborEuroyen(TENOR_1W);
  public static final IborIndex JPY_TIBOR_EUROYEN_1M = tiborEuroyen(TENOR_1M);
  public static final IborIndex JPY_TIBOR_EUROYEN_2M = tiborEuroyen(TENOR_2M);
  public static final IborIndex JPY_TIBOR_EUROYEN_3M = tiborEuroyen(TENOR_3M);
  public static final IborIndex JPY_TIBOR_EUROYEN_6M = tiborEuroyen(TENOR_6M);
  public static final IborIndex JPY_TIBOR_EUROYEN_12M = tiborEuroyen(TENOR_12M);

  // conventions for tibor
  private static IborIndex tiborEuroyen(Tenor tenor) {
    return ImmutableIborIndex.builder()
        .name("JPY-TIBOR-EUROYEN-" + tenor)
        .currency(JPY)
        .fixingCalendar(JPTO)
        .fixingDateOffset(DaysAdjustment.ofBusinessDays(-2, JPTO))
        .effectiveDateOffset(DaysAdjustment.ofBusinessDays(2, JPTO))
        .maturityDateOffset(maturity(tenor, JPTO))
        .dayCount(ACT_360)
        .build();
  }

  // maturity rule for LIBOR
  private static TenorAdjustment maturity(Tenor tenor, HolidayCalendar calendar) {
    TenorAdjustment maturity = tenor.isMonthBased() ?
        TenorAdjustment.ofLastBusinessDay(tenor, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, calendar)) :
        TenorAdjustment.of(tenor, PeriodAdditionConventions.NONE, BusinessDayAdjustment.of(FOLLOWING, calendar));
    return maturity;
  }

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private StandardIborIndices() {
  }

}
