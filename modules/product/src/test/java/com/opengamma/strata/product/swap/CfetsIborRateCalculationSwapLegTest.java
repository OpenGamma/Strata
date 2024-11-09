package com.opengamma.strata.product.swap;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.util.Arrays;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.HolidayCalendar;
import com.opengamma.strata.basics.date.HolidayCalendarId;
import com.opengamma.strata.basics.date.ImmutableHolidayCalendar;
import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.basics.value.ValueSchedule;
import com.opengamma.strata.product.common.PayReceive;
import org.junit.jupiter.api.Test;

public class CfetsIborRateCalculationSwapLegTest {
  private static final List<LocalDate> holidays = Arrays.asList(LocalDate.of(2024, Month.JANUARY, 1),
      LocalDate.of(2024, Month.FEBRUARY, 12), LocalDate.of(2024, Month.FEBRUARY, 13),
      LocalDate.of(2024, Month.FEBRUARY, 14), LocalDate.of(2024, Month.FEBRUARY, 15),
      LocalDate.of(2024, Month.FEBRUARY, 16), LocalDate.of(2024, Month.APRIL, 4),
      LocalDate.of(2024, Month.APRIL, 5), LocalDate.of(2024, Month.MAY, 1), LocalDate.of(2024, Month.MAY, 2),
      LocalDate.of(2024, Month.MAY, 3), LocalDate.of(2024, Month.JUNE, 10),
      LocalDate.of(2024, Month.SEPTEMBER, 16), LocalDate.of(2024, Month.SEPTEMBER, 17),
      LocalDate.of(2024, Month.OCTOBER, 1), LocalDate.of(2024, Month.OCTOBER, 2),
      LocalDate.of(2024, Month.OCTOBER, 3), LocalDate.of(2024, Month.OCTOBER, 4),
      LocalDate.of(2024, Month.OCTOBER, 7));

  private static final List<LocalDate> workingWeekends = Arrays.asList(LocalDate.of(2024, Month.FEBRUARY, 4),
      LocalDate.of(2024, Month.FEBRUARY, 18), LocalDate.of(2024, Month.APRIL, 7),
      LocalDate.of(2024, Month.APRIL, 28), LocalDate.of(2024, Month.MAY, 11),
      LocalDate.of(2024, Month.SEPTEMBER, 14), LocalDate.of(2024, Month.SEPTEMBER, 29),
      LocalDate.of(2024, Month.OCTOBER, 12));

  private static final HolidayCalendarId CNBE_CALENDAR = HolidayCalendarId.of("CNBE");

  private static final LocalDate startDate = LocalDate.of(2024, Month.JULY, 3),
      matureDate = LocalDate.of(2025, Month.APRIL, 3);

  private static final double notional = 10000000;

  @Test
  public void testFR007() {
    HolidayCalendar cnbe = ImmutableHolidayCalendar.of(CNBE_CALENDAR, holidays,
        Arrays.asList(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY), workingWeekends);
    ReferenceData refData = ReferenceData
                                .of(ImmutableMap.<HolidayCalendarId, HolidayCalendar>builder().put(cnbe.getId(), cnbe).build());

    RateCalculationSwapLeg leg = RateCalculationSwapLeg.builder()
                                     .accrualSchedule(PeriodicSchedule.builder().startDate(startDate).endDate(matureDate)
                                                          .businessDayAdjustment(
                                                              BusinessDayAdjustment.of(BusinessDayConventions.FOLLOWING, CNBE_CALENDAR)).frequency( Frequency.P3M)
                                                          .stubConvention(StubConvention.SHORT_FINAL).endDateBusinessDayAdjustment(BusinessDayAdjustment.of(BusinessDayConventions.FOLLOWING, CNBE_CALENDAR)).build())
                                     .paymentSchedule(PaymentSchedule.builder().paymentRelativeTo(PaymentRelativeTo.PERIOD_END).paymentFrequency(Frequency.P3M)
                                                          .businessDayAdjustment(BusinessDayAdjustment.of(BusinessDayConventions.FOLLOWING, CNBE_CALENDAR)).compoundingMethod(CompoundingMethod.STRAIGHT).paymentDateOffset(
                                             DaysAdjustment.NONE).build())
                                     .payReceive(PayReceive.RECEIVE)
                                     .calculation(CfetsIborRateCalculation.builder().dayCount(DayCounts.ACT_365F).index(
                                             IborIndices.CNY_REPO_1W).fixingDateOffset(DaysAdjustment.ofBusinessDays(-1, CNBE_CALENDAR, BusinessDayAdjustment.of(BusinessDayConventions.PRECEDING, CNBE_CALENDAR)))
                                                      .resetPeriods(ResetSchedule.builder().businessDayAdjustment(BusinessDayAdjustment.NONE).resetFrequency(Frequency.P1W).build()).build())
                                     .notionalSchedule(
                                         NotionalSchedule.builder().currency(Currency.CNY).finalExchange(false)
                                             .initialExchange(false).amount(ValueSchedule.of(notional)).build())
                                     .build();
    ResolvedSwapLeg resolve = leg.resolve(refData);
    ImmutableList<SwapPaymentPeriod> paymentPeriods = resolve.getPaymentPeriods();
  }
}
