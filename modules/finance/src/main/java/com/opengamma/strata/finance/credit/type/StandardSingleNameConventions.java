package com.opengamma.strata.finance.credit.type;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.date.HolidayCalendars;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.StubConvention;

public class StandardSingleNameConventions {

  /**
   * http://www.cdsmodel.com/cdsmodel/assets/cds-model/docs/Standard%20CDS%20Contract%20Specification.pdf
   * <p>
   * CDS Dates: 20th of Mar/Jun/Sep/Dec
   * • Business Day Count Actual/360: see 2003 ISDA Credit Derivative Definitions
   * • Business Day Convention Following: see 2003 ISDA Credit Derivative Definitions
   * Contract Specification
   * With respect to trade date T:
   * • Maturity Date: A CDS Date, unadjusted
   * • Coupon Rate: 100bp or 500bp
   * • Protection Leg:
   * o Legal Protection Effective Date: today -60 days for credit events and today –
   * 90 days for succession events, unadjusted
   * o Protection Maturity Date: Maturity Date
   * o Protection Payoff: Par Minus Recovery
   * • Premium Leg:
   * o Payment Frequency: quarterly
   * o Daycount Basis: Actual/360
   * o Pay Accrued On Default: true
   * o Business Day Calendar: currency dependent
   * o Adjusted CDS Dates: CDS Dates, business day adjusted Following
   * o First Coupon Payment Date: earliest Adjusted CDS Date after T+1 calendar
   * o Accrual Begin Date: latest Adjusted CDS Date on or before T+1 calendar
   * o Accrual Dates: CDS dates business day adjusted Following except for the last
   * accrual date (Maturity Date) which remains unadjusted
   * o Accrual Periods: From previous accrual date, inclusive, to the next accrual
   * date, exclusive, except for the last accrual period where the accrual end date
   * (Maturity Date) is included
   * o Payment Dates: CDS dates, business day adjusted Following including the last
   * payment day (Maturity Date).
   */

  public static StandardSingleNameConvention northAmerican() {
    return StandardSingleNameConvention
        .builder()
        .currency(Currency.USD)
        .dayCount(DayCounts.ACT_360)
        .dayConvention(BusinessDayConventions.FOLLOWING)
        .paymentFrequency(Frequency.P3M)
        .payAccOnDefault(true)
        .calendar(HolidayCalendars.NO_HOLIDAYS)
        .stubConvention(StubConvention.SHORT_FINAL)
        .stepIn(1)
        .settleLag(3)
        .build();
  }

  /**
   * CDS Dates: 20th of Mar/Jun/Sep/Dec
   • Business Day Count Actual/360: see 2003 ISDA Credit Derivative Definitions
   * • Business Day Convention Following: see 2003 ISDA Credit Derivative Definitions
   * Contract Specification
   * With respect to trade date T:
   * • Maturity Date: A CDS Date, unadjusted
   * • Coupon Rate:
   * o Trading: 25bp, 100bp, 500bp or 1000bp
   * o Backloading: 25bp, 100bp, 300bp, 500bp, 750bp or 1000bp
   * • Protection Leg:
   * o Legal Protection Effective Date: T-60 days for credit events, T-90 days for
   * successions events, unadjusted
   * o Protection Maturity Date: Maturity Date
   * o Protection Payoff: Par Minus Recovery
   * • Premium Leg:
   * o Payment Frequency: quarterly
   * o Daycount Basis: Actual/360
   * o Pay Accrued On Default: true
   * o Business Day Calendar: currency dependent
   * o Adjusted CDS Dates: CDS Dates, business day adjusted Following
   * o First Coupon Payment Date: earliest Adjusted CDS Date after T+1 calendar
   * o Accrual Begin Date: latest Adjusted CDS Date on or before T+1 calendar
   * o Accrual Dates: CDS dates business day adjusted Following except for the last
   * accrual date (Maturity Date) which remains unadjusted
   * o Accrual Periods: From previous accrual date, inclusive, to the next accrual
   * date, exclusive, except for the last accrual period where the accrual end date
   * (Maturity Date) is included
   * o Payment Dates: CDS dates, business day adjusted Following including the last
   * payment day (Maturity Date)
   * 1
   * Version: May 5, 2009
   * Contacts: shuwie.chen@barcap.com, ozgur.kaya@barcap.com, marco.naldi@barcap.com,
   * claus.pedersen@barcap.com, ryan.mccorvie@gs.com, jacob.eliosoff@gs.com, keith.jia@jpmorgan.com,
   * marc.barrachin@markit.com, manish.mehra@markit.com, manfung.chow@markit.com,
   * kevin.krabbenhoeft@markit.com
   * 1
   * • Currencies: EUR, GBP, CHF, USD
   * • Business Calendars: determined by currency:
   * o EUR: London and TARGET Settlement Day
   * o GBP: London
   * o CHF: London and Zurich
   * o USD: London and New York 
   */

  public static StandardSingleNameConvention europeanEUR() {
    return StandardSingleNameConvention
        .builder()
        .currency(Currency.EUR)
        .dayCount(DayCounts.ACT_360)
        .dayConvention(BusinessDayConventions.FOLLOWING)
        .paymentFrequency(Frequency.P3M)
        .payAccOnDefault(true)
        .calendar(HolidayCalendars.GBLO.combineWith(HolidayCalendars.EUTA))
        .stubConvention(StubConvention.SHORT_FINAL)
        .stepIn(1)
        .settleLag(3)
        .build();
  }

  public static StandardSingleNameConvention europeanGBP() {
    return StandardSingleNameConvention
        .builder()
        .currency(Currency.GBP)
        .dayCount(DayCounts.ACT_360)
        .dayConvention(BusinessDayConventions.FOLLOWING)
        .paymentFrequency(Frequency.P3M)
        .payAccOnDefault(true)
        .calendar(HolidayCalendars.GBLO)
        .stubConvention(StubConvention.SHORT_FINAL)
        .stepIn(1)
        .settleLag(3)
        .build();
  }

  public static StandardSingleNameConvention europeanCHF() {
    return StandardSingleNameConvention
        .builder()
        .currency(Currency.CHF)
        .dayCount(DayCounts.ACT_360)
        .dayConvention(BusinessDayConventions.FOLLOWING)
        .paymentFrequency(Frequency.P3M)
        .payAccOnDefault(true)
        .calendar(HolidayCalendars.GBLO.combineWith(HolidayCalendars.CHZU))
        .stubConvention(StubConvention.SHORT_FINAL)
        .stepIn(1)
        .settleLag(3)
        .build();
  }

  public static StandardSingleNameConvention europeanUSD() {
    return StandardSingleNameConvention
        .builder()
        .currency(Currency.USD)
        .dayCount(DayCounts.ACT_360)
        .dayConvention(BusinessDayConventions.FOLLOWING)
        .paymentFrequency(Frequency.P3M)
        .payAccOnDefault(true)
        .calendar(HolidayCalendars.GBLO.combineWith(HolidayCalendars.USNY))
        .stubConvention(StubConvention.SHORT_FINAL)
        .stepIn(1)
        .settleLag(3)
        .build();
  }

}
