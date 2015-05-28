package com.opengamma.strata.examples.finance.credit;


import com.opengamma.analytics.financial.credit.isdastandardmodel.CDSAnalytic;
import com.opengamma.analytics.financial.credit.isdastandardmodel.StubType;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConvention;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.date.HolidayCalendar;
import com.opengamma.strata.basics.date.HolidayCalendars;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.finance.TradeInfo;
import com.opengamma.strata.finance.credit.CreditDefaultSwap;
import com.opengamma.strata.finance.credit.CreditDefaultSwapTrade;
import com.opengamma.strata.finance.credit.common.RedCode;
import com.opengamma.strata.finance.credit.fee.FeeLeg;
import com.opengamma.strata.finance.credit.general.GeneralTerms;
import com.opengamma.strata.finance.credit.general.reference.SeniorityLevel;
import com.opengamma.strata.finance.credit.general.reference.SingleNameReferenceInformation;
import com.opengamma.strata.finance.credit.protection.ProtectionTerms;
import com.opengamma.strata.finance.credit.protection.RestructuringClause;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.time.LocalDate;
import java.time.Period;

@Test
public class CdsTradeModelToAnalyticsTest {

  // normally will come from a curve, just hard code for testing purpose
  private final static double recoveryRate = 0.40D;

  @Test
  public void test_Raytheon() {
    CDSAnalytic result = toAnalytic(trade);
    CDSAnalytic anticipated = expected;
    Assert.assertEquals(result, anticipated);
  }

  private CDSAnalytic toAnalytic(CreditDefaultSwapTrade trade) {
    // some of these values come from trade, some from curve?
    LocalDate tradeDate = trade.getTradeInfo().getTradeDate().get();
    LocalDate valueDate = tradeDate;

    CreditDefaultSwap cds = trade.getProduct();

    GeneralTerms generalTerms = cds.getGeneralTerms();
    LocalDate startDate = generalTerms.getEffectiveDate();
    LocalDate maturityDate = generalTerms.getScheduledTerminationDate();
    BusinessDayConvention businessdayAdjustmentConvention = generalTerms.getDateAdjustments().getConvention();
    HolidayCalendar calendar = generalTerms.getDateAdjustments().getCalendar();

    FeeLeg feeLeg = cds.getFeeLeg();
    LocalDate stepInDate = feeLeg.getStepInDate();
    boolean payAccOnDefault = feeLeg.isPayAccOnDefault();
    Frequency frequency = feeLeg.getFrequency();

    // Need to map these to analytics enums
    StubConvention stubConvention = feeLeg.getStubConvention();
    DayCount accrualDayCount = feeLeg.getDayCount();

    boolean protectStart = true;

    /**
     * tradeDate The trade date
     * stepinDate (aka Protection Effective sate or assignment date). Date when party assumes ownership. This is usually T+1. This is when protection
     * (and risk) starts in terms of the model. Note, this is sometimes just called the Effective Date, however this can cause
     * confusion with the legal effective date which is T-60 or T-90.
     * valueDate The valuation date. The date that values are PVed to. Is is normally today + 3 business days.  Aka cash-settle date.
     * accStartDate This is when the CDS nominally starts in terms of premium payments.  i.e. the number of days in the first
     * period (and thus the amount of the first premium payment) is counted from this date.
     * endDate (aka maturity date) This is when the contract expires and protection ends - any default after this date does not
     *  trigger a payment. (the protection ends at end of day)
     * payAccOnDefault Is the accrued premium paid in the event of a default
     * paymentInterval The nominal step between premium payments (e.g. 3 months, 6 months).
     * stubType stubType Options are FRONTSHORT, FRONTLONG, BACKSHORT, BACKLONG or NONE
     *  - <b>Note</b> in this code NONE is not allowed
     * protectStart  If protectStart = true, then protections starts at the beginning of the day, otherwise it is at the end.
     * recoveryRate The recovery rate
     * businessdayAdjustmentConvention How are adjustments for non-business days made
     * calendar HolidayCalendar defining what is a non-business day
     * accrualDayCount Day count used for accrual
     */
    return new CDSAnalytic(
        tradeDate,
        stepInDate,
        valueDate,
        startDate,
        maturityDate,
        payAccOnDefault,
        frequency.getPeriod(),
        StubType.FRONTSHORT,
        protectStart,
        recoveryRate,
        businessdayAdjustmentConvention,
        calendar,
        com.opengamma.analytics.convention.daycount.DayCounts.ACT_360
    );

  }

  final static CDSAnalytic expected = new CDSAnalytic(
      LocalDate.of(2014, 9, 20),
      LocalDate.of(2014, 9, 21),
      LocalDate.of(2014, 9, 24),
      LocalDate.of(2014, 6, 20),
      LocalDate.of(2019, 12, 20),
      true,
      Period.ofMonths(3),
      StubType.FRONTSHORT,
      true,
      recoveryRate,
      BusinessDayConventions.FOLLOWING,
      HolidayCalendars.NO_HOLIDAYS,
      com.opengamma.analytics.convention.daycount.DayCounts.ACT_360
  );
  final static LocalDate tradeDate = LocalDate.of(2014, 9, 20);
  final static CreditDefaultSwapTrade trade = CreditDefaultSwapTrade
      .builder()
      .standardId(StandardId.of("trade", "673676"))
      .tradeInfo(
          TradeInfo
              .builder()
              .counterparty(StandardId.of("cpty", "Counterparty"))
              .tradeDate(tradeDate)
              .settlementDate(tradeDate.plusDays(3))
              .build()
      )
      .product(
          CreditDefaultSwap
              .builder()
              .generalTerms(
                  GeneralTerms
                      .builder()
                      .effectiveDate(LocalDate.of(2014, 6, 20))
                      .scheduledTerminationDate(LocalDate.of(2019, 12, 20))
                      .dateAdjustments(
                          BusinessDayAdjustment.of(
                              BusinessDayConventions.FOLLOWING,
                              HolidayCalendars.NO_HOLIDAYS
                          )
                      )
                      .referenceInformation(
                          SingleNameReferenceInformation
                              .builder()
                              .referenceEntityName("Pepsico Inc")
                              .referenceEntityId(RedCode.of("123456"))
                              .currency(Currency.USD)
                              .seniority(SeniorityLevel.SeniorUnSec)
                              .build()
                      )
                      .build()
              )
              .feeLeg(
                  FeeLeg
                      .builder()
                      .frequency(Frequency.P3M)
                      .payAccOnDefault(true)
                      .dayCount(DayCounts.ACT_360)
                      .stubConvention(StubConvention.SHORT_FINAL)
                      .calculationAmount(1_000_000D)
                      .stepInDate(tradeDate.plusDays(1))
                      .build()
              )
              .protectionTerms(
                  ProtectionTerms.of(
                      1_000_000D,
                      RestructuringClause.XR
                  )
              )
              .build()
      )
      .build();

}
