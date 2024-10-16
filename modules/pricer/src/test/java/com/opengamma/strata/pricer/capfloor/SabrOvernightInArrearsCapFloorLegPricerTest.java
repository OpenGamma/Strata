/*
 * Copyright (C) 2024 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.capfloor;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.EUTA;
import static com.opengamma.strata.basics.index.OvernightIndices.EUR_ESTR;
import static com.opengamma.strata.collect.TestHelper.dateUtc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.HolidayCalendar;
import com.opengamma.strata.basics.date.PeriodAdditionConventions;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.basics.date.TenorAdjustment;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.index.ImmutableIborIndex;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.schedule.RollConventions;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.basics.value.ValueSchedule;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.product.capfloor.OvernightInArrearsCapFloorLeg;
import com.opengamma.strata.product.common.PayReceive;
import com.opengamma.strata.product.swap.OvernightRateCalculation;

public class SabrOvernightInArrearsCapFloorLegPricerTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final IborIndex EUR_ESTRTERM_3M = ImmutableIborIndex.builder()
      .name("EUR-ESTRTERM-3M")
      .currency(EUR)
      .dayCount(DayCounts.ACT_360)
      .fixingCalendar(EUTA)
      .fixingTime(LocalTime.of(11, 0))
      .fixingZone(ZoneId.of("Europe/Brussels"))
      .fixingDateOffset(DaysAdjustment.ofBusinessDays(-2, EUTA))
      .effectiveDateOffset(DaysAdjustment.ofBusinessDays(2, EUTA))
      .maturityDateOffset(TenorAdjustment.of(
          Tenor.TENOR_3M,
          PeriodAdditionConventions.LAST_BUSINESS_DAY,
          BusinessDayAdjustment.of(BusinessDayConventions.MODIFIED_FOLLOWING, EUTA)))
      .build();
  private static final HolidayCalendar EUTA_IMPL = REF_DATA.getValue(EUTA);
  private static final ZonedDateTime VALUATION = dateUtc(2021, 12, 20);
  private static final ZonedDateTime VALUATION_AFTER_START = dateUtc(2022, 8, 18);
  private static final ZonedDateTime VALUATION_AFTER_PAY = dateUtc(2022, 11, 18);
  private static final ZonedDateTime VALUATION_AFTER_END = dateUtc(2022, 9, 29);
  private static final LocalDate START_DATE = LocalDate.of(2022, 6, 22);
  private static final LocalDate END_DATE = LocalDate.of(2026, 6, 22);
  private static final double NOTIONAL_VALUE = 1_000_000.0d;
  private static final ValueSchedule NOTIONAL = ValueSchedule.of(NOTIONAL_VALUE);

  private static final double STRIKE = 0.0155;
  private static final ValueSchedule STRIKE_SCHEDULE = ValueSchedule.of(STRIKE);
  private static final PeriodicSchedule PAY_SCHEDULE =
      PeriodicSchedule.of(START_DATE, END_DATE, Frequency.P12M, BusinessDayAdjustment.of(BusinessDayConventions.MODIFIED_FOLLOWING, EUTA), StubConvention.NONE, RollConventions.NONE);
  private static final OvernightInArrearsCapFloorLeg CAP_LEG = OvernightInArrearsCapFloorLeg.builder()
      .capSchedule(STRIKE_SCHEDULE)
      .calculation(OvernightRateCalculation.of(EUR_ESTR))
      .currency(EUR)
      .notional(NOTIONAL)
      .paymentSchedule(PAY_SCHEDULE)
      .payReceive(PayReceive.RECEIVE)
      .build();

  // valuation date before start date
  private static final ImmutableRatesProvider RATES = IborCapletFloorletSabrRateVolatilityDataSet.getRatesProvider(
      VALUATION.toLocalDate(), EUR_ESTRTERM_3M, LocalDateDoubleTimeSeries.empty());
  private static final SabrParametersIborCapletFloorletVolatilities VOLS = IborCapletFloorletSabrRateVolatilityDataSet
      .getVolatilities(VALUATION, EUR_ESTRTERM_3M);

  private static final SabrOvernightInArrearsCapFloorLegPricer LEG_PRICER = SabrOvernightInArrearsCapFloorLegPricer.DEFAULT;

  /* The present value of the in-arrears option is higher than the in-advance. */
  @Test
  public void test() {
    System.out.println(LEG_PRICER.presentValue(CAP_LEG.resolve(REF_DATA), RATES, VOLS));
    System.out.println(LEG_PRICER.presentValueSensitivityModelParamsSabr(CAP_LEG.resolve(REF_DATA), RATES, VOLS));
    System.out.println(LEG_PRICER.presentValueSensitivityRatesStickyModel(CAP_LEG.resolve(REF_DATA), RATES, VOLS));
    System.out.println(LEG_PRICER.impliedVolatilities(CAP_LEG.resolve(REF_DATA), RATES, VOLS));
    System.out.println(LEG_PRICER.forwardRates(CAP_LEG.resolve(REF_DATA), RATES));

  }

}
