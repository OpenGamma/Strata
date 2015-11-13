/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.credit.type;

import static com.opengamma.strata.basics.date.BusinessDayConventions.FOLLOWING;
import static com.opengamma.strata.basics.date.HolidayCalendars.CHZU;
import static com.opengamma.strata.basics.date.HolidayCalendars.EUTA;
import static com.opengamma.strata.basics.date.HolidayCalendars.GBLO;
import static com.opengamma.strata.basics.date.HolidayCalendars.USNY;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.RollConventions;
import com.opengamma.strata.basics.schedule.StubConvention;

/**
 * Market standard CDS conventions.
 * <p>
 * See ISDA CDS documentation for more details.
 */
final class StandardCdsConventions {

  /**
   * The North-American USD convention.
   */
  public static final CdsConvention USD_NORTH_AMERICAN = ImmutableCdsConvention.builder()
      .name("USD-NorthAmerican")
      .currency(Currency.USD)
      .dayCount(DayCounts.ACT_360)
      .businessDayAdjustment(BusinessDayAdjustment.of(FOLLOWING, USNY))
      .paymentFrequency(Frequency.P3M)
      .rollConvention(RollConventions.DAY_20)
      .payAccruedOnDefault(true)
      .stubConvention(StubConvention.SHORT_INITIAL)
      .stepInDays(1)
      .settleLagDays(3)
      .build();

  /**
   * The European EUR convention.
   */
  public static final CdsConvention EUR_EUROPEAN = ImmutableCdsConvention.builder()
      .name("EUR-European")
      .currency(Currency.EUR)
      .dayCount(DayCounts.ACT_360)
      .businessDayAdjustment(BusinessDayAdjustment.of(FOLLOWING, EUTA))
      .paymentFrequency(Frequency.P3M)
      .rollConvention(RollConventions.DAY_20)
      .payAccruedOnDefault(true)
      .stubConvention(StubConvention.SHORT_INITIAL)
      .stepInDays(1)
      .settleLagDays(3)
      .build();

  /**
   * The European GBP convention.
   */
  public static final CdsConvention GBP_EUROPEAN = ImmutableCdsConvention.builder()
      .name("GBP-European")
      .currency(Currency.GBP)
      .dayCount(DayCounts.ACT_360)
      .businessDayAdjustment(BusinessDayAdjustment.of(FOLLOWING, GBLO))
      .paymentFrequency(Frequency.P3M)
      .rollConvention(RollConventions.DAY_20)
      .payAccruedOnDefault(true)
      .stubConvention(StubConvention.SHORT_INITIAL)
      .stepInDays(1)
      .settleLagDays(3)
      .build();

  /**
   * The European CHF convention.
   */
  public static final CdsConvention CHF_EUROPEAN = ImmutableCdsConvention.builder()
      .name("CHF-European")
      .currency(Currency.CHF)
      .dayCount(DayCounts.ACT_360)
      .businessDayAdjustment(BusinessDayAdjustment.of(FOLLOWING, CHZU))
      .paymentFrequency(Frequency.P3M)
      .rollConvention(RollConventions.DAY_20)
      .payAccruedOnDefault(true)
      .stubConvention(StubConvention.SHORT_INITIAL)
      .stepInDays(1)
      .settleLagDays(3)
      .build();

  /**
   * The European USD convention.
   */
  public static final CdsConvention USD_EUROPEAN = ImmutableCdsConvention.builder()
      .name("USD-European")
      .currency(Currency.USD)
      .dayCount(DayCounts.ACT_360)
      .businessDayAdjustment(BusinessDayAdjustment.of(FOLLOWING, GBLO.combineWith(USNY)))
      .paymentFrequency(Frequency.P3M)
      .rollConvention(RollConventions.DAY_20)
      .payAccruedOnDefault(true)
      .stubConvention(StubConvention.SHORT_INITIAL)
      .stepInDays(1)
      .settleLagDays(3)
      .build();

}
