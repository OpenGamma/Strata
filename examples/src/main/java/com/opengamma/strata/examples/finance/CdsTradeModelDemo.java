/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * <p>
 * Please see distribution for license.
 */
package com.opengamma.strata.examples.finance;

import com.opengamma.analytics.financial.credit.isdastandardmodel.StubType;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConvention;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.date.HolidayCalendar;
import com.opengamma.strata.basics.date.HolidayCalendars;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.finance.TradeInfo;
import com.opengamma.strata.finance.credit.CreditDefaultSwap;
import com.opengamma.strata.finance.credit.CreditDefaultSwapTrade;
import com.opengamma.strata.finance.credit.fee.FeeLeg;
import com.opengamma.strata.finance.credit.general.BuyerConvention;
import com.opengamma.strata.finance.credit.general.GeneralTerms;
import com.opengamma.strata.finance.credit.general.reference.IndexReferenceInformation;
import com.opengamma.strata.finance.credit.general.reference.SingleNameReferenceInformation;
import com.opengamma.strata.finance.credit.protection.ProtectionTerms;
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
    demo.simpleSingleName();
    demo.simpleIndex();
  }

  //-----------------------------------------------------------------------
  public void simpleSingleName() {
    FeeLeg feeLeg = null;
    ProtectionTerms protectionTerms = null;

    CreditDefaultSwapTrade trade = CreditDefaultSwapTrade
        .builder()
        .standardId(StandardId.of("trade", "673676"))
        .tradeInfo(
            TradeInfo
                .builder()
                .counterparty(StandardId.of("cpty", "Counterparty"))
                .tradeDate(LocalDate.of(2014, 1, 1))
                .settlementDate(LocalDate.of(2014, 1, 3))
                .build()
        )
        .product(
            CreditDefaultSwap
                .builder()
                .standardId(StandardId.of("product", "17616"))
                .generalTerms(
                    GeneralTerms
                        .builder()
                        .effectiveDate(LocalDate.of(2014, 6, 20))
                        .scheduledTerminationDate(LocalDate.of(2019, 12, 20))
                        .buyerConvention(BuyerConvention.PROTECTION)
                        .dateAdjustments(
                            BusinessDayAdjustment.of(
                                BusinessDayConventions.FOLLOWING,
                                HolidayCalendars.USNY.combineWith(HolidayCalendars.GBLO)
                            )
                        )
                        .referenceInformation(
                            SingleNameReferenceInformation
                                .builder()
                                .referenceEntityName("Ford Motor Company")
                                .referenceEntityId(StandardId.of("http://www.ext.org/entity-id-RED-1-0", "H98A7"))
                                .referenceObligationId(StandardId.of("http://www.ext.org/instrument-id-ISIN-1-0", "US345370BX76"))
                                .build()
                        )
                        .build()
                )
                .feeLeg(feeLeg)
                .protectionTerms(protectionTerms)
                .build()
        )
        .build();

    checkValues(trade);

    System.out.println("===== Trade =====");
    System.out.println(JodaBeanSer.PRETTY.jsonWriter().write(trade));
    System.out.println();
    System.out.println("===== Expanded =====");
    System.out.println(JodaBeanSer.PRETTY.jsonWriter().write(trade.getProduct().expand()));
    System.out.println();
  }


  public void simpleIndex() {
    FeeLeg feeLeg = null;
    ProtectionTerms protectionTerms = null;

    CreditDefaultSwapTrade trade = CreditDefaultSwapTrade
        .builder()
        .standardId(StandardId.of("trade", "673676"))
        .tradeInfo(
            TradeInfo
                .builder()
                .counterparty(StandardId.of("cpty", "Counterparty"))
                .tradeDate(LocalDate.of(2014, 1, 1))
                .settlementDate(LocalDate.of(2014, 1, 3))
                .build()
        )
        .product(
            CreditDefaultSwap
                .builder()
                .standardId(StandardId.of("product", "17616"))
                .generalTerms(
                    GeneralTerms
                        .builder()
                        .effectiveDate(LocalDate.of(2014, 6, 20))
                        .scheduledTerminationDate(LocalDate.of(2019, 12, 20))
                        .buyerConvention(BuyerConvention.PROTECTION)
                        .dateAdjustments(
                            BusinessDayAdjustment.of(
                                BusinessDayConventions.FOLLOWING,
                                HolidayCalendars.NO_HOLIDAYS
                            )
                        )
                        .referenceInformation(
                            IndexReferenceInformation
                                .builder()
                                .indexName("CDX.NA.IG.15")
                                .indexId(StandardId.of("http://www.ext.org/entity-id-RED-pair-1-0", "2I65BYCL7"))
                                .indexSeries(15)
                                .indexAnnexVersion(1)
                                .build()
                        )
                        .build()
                )
                .feeLeg(feeLeg)
                .protectionTerms(protectionTerms)
                .build()
        )
        .build();

    checkValues(trade);

    System.out.println("===== Trade =====");
    System.out.println(JodaBeanSer.PRETTY.jsonWriter().write(trade));
    System.out.println();
    System.out.println("===== Expanded =====");
    System.out.println(JodaBeanSer.PRETTY.jsonWriter().write(trade.getProduct().expand()));
    System.out.println();
  }

  private void checkValues(CreditDefaultSwapTrade trade) {
    // some of these values come from trade, some from curve?
    LocalDate tradeDate = LocalDate.of(2014, 1, 1);
    LocalDate startDate = LocalDate.of(2014, 1, 1);
    LocalDate maturityDate = LocalDate.of(2014, 1, 1);
    int stepIn = 0;
    int cashSettle = 0;
    boolean payAccOnDefault = true;
    Period couponInterval = Period.ofDays(0);
    Tenor couponIntervalTenor = Tenor.TENOR_1D;
    StubType stubType = StubType.NONE;
    boolean protectStart = true;
    double recoveryRate = 0.40D;
    BusinessDayConvention businessdayAdjustmentConvention = BusinessDayConventions.MODIFIED_FOLLOWING;
    HolidayCalendar calendar = HolidayCalendars.USNY;
    DayCount accrualDayCount = DayCounts.ACT_360;
    DayCount curveDayCount = DayCounts.ACT_360;

    assert (trade.getTradeInfo().getTradeDate().equals(tradeDate));
  }

}
