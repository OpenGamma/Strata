package com.opengamma.strata.finance.credit.fee;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.date.HolidayCalendars;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.schedule.RollConventions;
import com.opengamma.strata.basics.schedule.StubConvention;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Test
public class FeeLegTest {

  @Test
  public void test_schedule() {
    LocalDate unadjustedStart = LocalDate.of(2014, 9, 20);
    LocalDate unadjustedEnd = unadjustedStart.plusYears(5);
    FeeLeg sut = FeeLeg.of(
        PeriodicPayments.of(
            PeriodicSchedule.of(
                unadjustedStart,
                unadjustedEnd,
                Frequency.P3M,
                BusinessDayAdjustment.of(
                    BusinessDayConventions.MODIFIED_FOLLOWING,
                    HolidayCalendars.SAT_SUN
                ),
                StubConvention.SHORT_FINAL,
                RollConventions.DAY_20
            ),
            CurrencyAmount.of(Currency.USD, 100_000_000D),
            0.0100,
            DayCounts.ACT_360
        )
    );

    ImmutableList<LocalDate> dd = sut.getPeriodicPayments().getPeriodicSchedule().createAdjustedDates();
    check(dd);
  }

  private void check(ImmutableList<LocalDate> dates) {
    List<LocalDate> expected = ImmutableList.of(
        "2014-09-20",
        "2014-12-20",
        "2014-09-20",
        "2014-09-20",
        "2014-09-20",
        "2014-09-20",
        "2014-09-20",
        "2014-09-20",
        "2014-09-20",
        "2014-09-20",
        "2014-09-20",
        "2014-09-20",
        "2014-09-20",
        "2014-09-20",
        "2014-09-20",
        "2014-09-20",
        "2014-09-20",
        "2014-09-20",
        "2014-09-20",
        "2014-09-20",
        "2015-09-20"
    ).stream()
        .map(x -> LocalDate.parse(x))
        .collect(Collectors.toList());
    Assert.assertEquals(dates, expected);
  }

}
