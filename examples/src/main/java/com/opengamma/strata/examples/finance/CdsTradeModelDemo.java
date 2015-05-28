/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * <p>
 * Please see distribution for license.
 */
package com.opengamma.strata.examples.finance;

import com.opengamma.analytics.financial.credit.isdastandardmodel.CDSAnalytic;
import com.opengamma.analytics.financial.credit.isdastandardmodel.StubType;
import com.opengamma.strata.basics.BuySell;
import com.opengamma.strata.basics.date.BusinessDayConvention;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.HolidayCalendar;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.finance.credit.CreditDefaultSwap;
import com.opengamma.strata.finance.credit.CreditDefaultSwapTrade;
import com.opengamma.strata.finance.credit.common.RedCode;
import com.opengamma.strata.finance.credit.fee.FeeLeg;
import com.opengamma.strata.finance.credit.general.GeneralTerms;
import com.opengamma.strata.finance.credit.general.reference.SeniorityLevel;
import com.opengamma.strata.finance.credit.protection.RestructuringClause;
import com.opengamma.strata.finance.credit.type.StandardSingleNameConventions;
import com.opengamma.strata.finance.credit.type.StandardSingleNameTemplate;
import org.joda.beans.ser.JodaBeanSer;

import java.time.LocalDate;
import java.time.Period;

/**
 * Demonstrate use of the API for credit default swaps.
 * <p>
 * This class exists for demonstration purposes to aid with understanding credit default swaps.
 * It is not intended to be used in a production environment.
 */
public class CdsTradeModelDemo {

  /**
   * Launch demo, no arguments needed.
   *
   * @param args no arguments needed
   */
  public static void main(String[] args) {
    CdsTradeModelDemo demo = new CdsTradeModelDemo();
    demo.simpleSingleNameUsd();
    demo.simpleSingleNameEur();
    demo.simpleIndex();
  }

  //-----------------------------------------------------------------------
  public void simpleSingleNameUsd() {
    CreditDefaultSwapTrade swapTrade = StandardSingleNameTemplate
        .of(StandardSingleNameConventions.northAmerican())
        .toTrade(
            StandardId.of("tradeid", "62726762"),
            LocalDate.of(2014, 1, 1),
            Period.ofYears(5),
            BuySell.BUY,
            1_000_000D,
            0.0100,
            RedCode.of("H98A7"),
            "Ford Motor Company",
            SeniorityLevel.SeniorUnSec,
            RestructuringClause.XR
        );
  }

  public void simpleSingleNameEur() {
    CreditDefaultSwapTrade trade = StandardSingleNameTemplate
        .of(StandardSingleNameConventions.europeanEUR())
        .toTrade(
            StandardId.of("tradeid", "62726762"),
            LocalDate.of(2014, 1, 1),
            Period.ofYears(5),
            BuySell.BUY,
            1_000_000D,
            0.0050,
            RedCode.of("D28123"),
            "Addidas",
            SeniorityLevel.SeniorUnSec,
            RestructuringClause.XR
        );

    checkValues(trade);

    System.out.println("===== Trade =====");
    System.out.println(JodaBeanSer.PRETTY.jsonWriter().write(trade));
    System.out.println();
    System.out.println("===== Expanded =====");
    System.out.println(JodaBeanSer.PRETTY.jsonWriter().write(trade.getProduct().expand()));
    System.out.println();
  }

  public void simpleIndex() {
//    LocalDate tradeDate = LocalDate.of(2014, 1, 1);
//    BusinessDayAdjustment businessDayAdjustment = BusinessDayAdjustment.of(
//        BusinessDayConventions.FOLLOWING,
//        HolidayCalendars.NO_HOLIDAYS
//    );
//    CreditDefaultSwapTrade trade = CreditDefaultSwapTrade.of(
//        StandardId.of("trade", "673676"),
//        TradeInfo
//            .builder()
//            .counterparty(StandardId.of("cpty", "Counterparty"))
//            .tradeDate(LocalDate.of(2014, 1, 1))
//            .build(),
//        CreditDefaultSwap.of(
//            GeneralTerms.index(
//                LocalDate.of(2014, 6, 20),
//                LocalDate.of(2019, 12, 20),
//                BuySell.BUY,
//                businessDayAdjustment,
//                RedCode.of("2I65BYCL7"),
//                "CDX.NA.IG.15",
//                15,
//                1
//            ),
//            FeeLeg.of(
//                1_000_000D,
//                PeriodicSchedule.of(
//                    tradeDate,
//                    tradeDate.plusYears(5)
//                )
//    true,
//                LocalDate.of(2014, 1, 1).plusDays(1),
//                DayCounts.ACT_360,
//                Frequency.P3M,
//                StubConvention.SHORT_FINAL
//            ),
//            ProtectionTerms.of(
//                1_000_000D,
//                RestructuringClause.XR
//            )
//        )
//    );
//
//
//    checkValues(trade);
//
//    System.out.println("===== Trade =====");
//    System.out.println(JodaBeanSer.PRETTY.jsonWriter().write(trade));
//    System.out.println();
//    System.out.println("===== Expanded =====");
//    System.out.println(JodaBeanSer.PRETTY.jsonWriter().write(trade.getProduct().expand()));
//    System.out.println();
  }

  private void checkValues(CreditDefaultSwapTrade trade) {
    // some of these values come from trade, some from curve?
    LocalDate tradeDate = trade.getTradeInfo().getTradeDate().get();
    LocalDate valueDate = tradeDate;
    LocalDate stepInDate = tradeDate.plusDays(1);

    CreditDefaultSwap cds = trade.getProduct();

    GeneralTerms generalTerms = cds.getGeneralTerms();
    LocalDate startDate = generalTerms.getEffectiveDate();
    LocalDate maturityDate = generalTerms.getScheduledTerminationDate();
    BusinessDayConvention businessdayAdjustmentConvention = generalTerms.getDateAdjustments().getConvention();
    HolidayCalendar calendar = generalTerms.getDateAdjustments().getCalendar();

    FeeLeg feeLeg = cds.getFeeLeg();
    PeriodicSchedule periodicPayments = feeLeg.getPeriodicPayments();
    Frequency frequency = periodicPayments.getFrequency();
    StubConvention stubType = periodicPayments.getStubConvention().get();

    boolean payAccOnDefault = true;
    DayCount accrualDayCount = null;
    boolean protectStart = true;
    double recoveryRate = 0.40D;

    assert (trade.getTradeInfo().getTradeDate().equals(tradeDate));

    CDSAnalytic guy = new CDSAnalytic(
        tradeDate,
        stepInDate,
        valueDate,
        startDate,
        maturityDate,
        payAccOnDefault,
        frequency.getPeriod(),
        StubType.NONE,
        protectStart,
        recoveryRate,
        businessdayAdjustmentConvention,
        calendar,
        com.opengamma.analytics.convention.daycount.DayCounts.ACT_360
    );

    assert (guy.isPayAccOnDefault());
  }

}
