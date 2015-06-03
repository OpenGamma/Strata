package com.opengamma.strata.examples.exampleccp.trades;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.PayReceive;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.HolidayCalendars;
import com.opengamma.strata.basics.index.OvernightIndices;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.finance.Trade;
import com.opengamma.strata.finance.TradeInfo;
import com.opengamma.strata.finance.rate.swap.FixedRateCalculation;
import com.opengamma.strata.finance.rate.swap.NotionalSchedule;
import com.opengamma.strata.finance.rate.swap.OvernightRateCalculation;
import com.opengamma.strata.finance.rate.swap.PaymentSchedule;
import com.opengamma.strata.finance.rate.swap.RateCalculationSwapLeg;
import com.opengamma.strata.finance.rate.swap.Swap;
import com.opengamma.strata.finance.rate.swap.SwapTrade;

import java.time.LocalDate;
import java.util.List;

import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;

public class MyTrades {

  public static List<Trade> create() {
    return ImmutableList.of(
        createOisSwap()
    );
  }

  // Create a fixed vs overnight swap with fixing
  private static Trade createOisSwap() {
    NotionalSchedule notional = NotionalSchedule.of(Currency.USD, 1_000_000);

    RateCalculationSwapLeg payLeg = RateCalculationSwapLeg.builder()
        .payReceive(PayReceive.PAY)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(LocalDate.of(2015, 4, 27))
            .endDate(LocalDate.of(2021, 4, 27))
            .frequency(Frequency.ofYears(1))
            .businessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, HolidayCalendars.USNY))
            .stubConvention(StubConvention.NONE)
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(Frequency.ofYears(1))
            .paymentDateOffset(DaysAdjustment.ofBusinessDays(2, HolidayCalendars.USNY, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, HolidayCalendars.USNY)))
            .build())
        .notionalSchedule(notional)
        .calculation(FixedRateCalculation.of(0.0305, DayCounts.ACT_360))
        .build();

    RateCalculationSwapLeg receiveLeg = RateCalculationSwapLeg.builder()
        .payReceive(PayReceive.RECEIVE)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(LocalDate.of(2015, 4, 27))
            .endDate(LocalDate.of(2021, 4, 27))
            .frequency(Frequency.ofYears(1))
            .businessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, HolidayCalendars.USNY))
            .stubConvention(StubConvention.NONE)
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(Frequency.ofYears(1))
            .paymentDateOffset(DaysAdjustment.ofBusinessDays(2, HolidayCalendars.USNY, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, HolidayCalendars.USNY)))
            .build())
        .notionalSchedule(notional)
        .calculation(OvernightRateCalculation.builder()
            .dayCount(DayCounts.ACT_360)
            .index(OvernightIndices.USD_FED_FUND)
            .build())
        .build();

    return SwapTrade.builder()
        .product(Swap.of(payLeg, receiveLeg))
        .tradeInfo(TradeInfo.builder()
            .id(StandardId.of("swap", "Fixed vs ON (with fixing)"))
            .counterparty(StandardId.of("example", "A"))
            .settlementDate(LocalDate.of(2015, 4, 27))
            .attributes(ImmutableMap.of("description", "Fixed vs ON (with fixing)"))
            .build())
        .build();
  }
}
