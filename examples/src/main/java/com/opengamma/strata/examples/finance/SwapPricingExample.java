/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.examples.finance;

import java.time.LocalDate;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.PayReceive;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.HolidayCalendars;
import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.value.ValueSchedule;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.engine.CalculationEngine;
import com.opengamma.strata.engine.CalculationRules;
import com.opengamma.strata.engine.Column;
import com.opengamma.strata.engine.calculations.Results;
import com.opengamma.strata.engine.config.Measure;
import com.opengamma.strata.engine.config.ReportingRules;
import com.opengamma.strata.engine.marketdata.BaseMarketData;
import com.opengamma.strata.examples.engine.ExampleEngine;
import com.opengamma.strata.examples.engine.ResultsFormatter;
import com.opengamma.strata.examples.marketdata.ExampleMarketData;
import com.opengamma.strata.finance.Trade;
import com.opengamma.strata.finance.TradeInfo;
import com.opengamma.strata.finance.rate.swap.FixedRateCalculation;
import com.opengamma.strata.finance.rate.swap.IborRateCalculation;
import com.opengamma.strata.finance.rate.swap.NotionalSchedule;
import com.opengamma.strata.finance.rate.swap.PaymentSchedule;
import com.opengamma.strata.finance.rate.swap.RateCalculationSwapLeg;
import com.opengamma.strata.finance.rate.swap.Swap;
import com.opengamma.strata.finance.rate.swap.SwapLeg;
import com.opengamma.strata.finance.rate.swap.SwapTrade;
import com.opengamma.strata.function.OpenGammaPricingRules;

/**
 * Example to illustrate using the engine to price a swap.
 * <p>
 * This makes use of the example engine which sources the required market data from
 * JSON resources.
 */
public class SwapPricingExample {

  public static void main(String[] args) {
    List<Trade> trades = ImmutableList.of(createTrade1());

    List<Column> columns = ImmutableList.of(
        Column.of(Measure.ID),
        Column.of(Measure.COUNTERPARTY),
        Column.of(Measure.SETTLEMENT_DATE),
        Column.of(Measure.MATURITY_DATE),
        Column.of(Measure.NOTIONAL),
        Column.of(Measure.PRESENT_VALUE),
        Column.of(Measure.PRESENT_VALUE_PAY_LEG),
        Column.of(Measure.PRESENT_VALUE_RECEIVE_LEG),
        Column.of(Measure.ACCRUED_INTEREST));

    CalculationRules rules = CalculationRules.builder()
        .pricingRules(OpenGammaPricingRules.standard())
        .marketDataRules(ExampleMarketData.rules())
        .reportingRules(ReportingRules.fixedCurrency(Currency.USD))
        .build();
    
    // Use an empty snapshot of market data, indicating only the valuation date.
    // The engine will attempt to source the data for us, which the example engine is
    // configured to load from JSON resources. We could alternatively populate the snapshot
    // with some or all of the required market data here.
    LocalDate valuationDate = LocalDate.of(2009, 7, 31);
    BaseMarketData baseMarketData = BaseMarketData.empty(valuationDate);

    CalculationEngine engine = ExampleEngine.create();
    Results results = engine.calculate(trades, columns, rules, baseMarketData);
    
    ResultsFormatter.print(results, columns);
	}

  //-----------------------------------------------------------------------  
  private static Trade createTrade1() {
    NotionalSchedule notional = NotionalSchedule.of(Currency.USD, 12_000_000);

    PeriodicSchedule accrual = PeriodicSchedule.builder()
        .startDate(LocalDate.of(2006, 2, 24))
        .endDate(LocalDate.of(2011, 2, 24))
        .frequency(Frequency.P3M)
        .businessDayAdjustment(BusinessDayAdjustment.of(BusinessDayConventions.MODIFIED_FOLLOWING, HolidayCalendars.USNY))
        .build();
    
    PaymentSchedule payment = PaymentSchedule.builder()
        .paymentFrequency(Frequency.P3M)
        .paymentOffset(DaysAdjustment.ofBusinessDays(2, HolidayCalendars.USNY))
        .build();
    
    SwapLeg payLeg = RateCalculationSwapLeg.builder()
        .payReceive(PayReceive.PAY)
        .accrualSchedule(accrual)
        .paymentSchedule(payment)
        .notionalSchedule(notional)
        .calculation(FixedRateCalculation.builder()
            .dayCount(DayCounts.ACT_360)
            .rate(ValueSchedule.of(0.05004))
            .build())
        .build();
    
    SwapLeg receiveLeg = RateCalculationSwapLeg.builder()
        .payReceive(PayReceive.RECEIVE)
        .accrualSchedule(accrual)
        .paymentSchedule(payment)
        .notionalSchedule(notional)
        .calculation(IborRateCalculation.builder()
            .dayCount(DayCounts.ACT_360)
            .index(IborIndices.USD_LIBOR_3M)
            .fixingOffset(DaysAdjustment.ofBusinessDays(-2, HolidayCalendars.USNY))
            .build())
        .build();
    
    return SwapTrade.builder()
        .standardId(StandardId.of("mn", "14248"))
        .product(Swap.builder()
            .legs(payLeg, receiveLeg)
            .build())
            .tradeInfo(TradeInfo.builder()
                .counterparty(StandardId.of("mn", "Dealer A"))
                .settlementDate(LocalDate.of(2006, 2, 24))
                .build())
        .build();
  }

}
