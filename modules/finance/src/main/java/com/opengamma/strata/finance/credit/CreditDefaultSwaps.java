package com.opengamma.strata.finance.credit;


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
import com.opengamma.strata.finance.credit.general.BuyerConvention;
import com.opengamma.strata.finance.credit.general.GeneralTerms;
import com.opengamma.strata.finance.credit.general.reference.SeniorityLevel;
import com.opengamma.strata.finance.credit.protection.ProtectionTerms;
import com.opengamma.strata.finance.credit.protection.RestructuringClause;

import java.time.LocalDate;

/**
 * https://products.markit.com/warehouse/news/Markit%20Credit%20Default%20Swap%20Calculator%20User%20Guide.pdf
 */
public class CreditDefaultSwaps {

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

  public void test() {

    CreditDefaultSwaps.northAmericanSingleName100bps5Y(
        10_000_000D,
        LocalDate.of(2015, 6, 1),
        "Ford Motor Company",
        REDCode.of("3H98A7"),
        SeniorityLevel.SeniorUnSec
    );
  }

  public static CreditDefaultSwap northAmericanSingleName100bps5Y(
      double notional,
      LocalDate tradeDate,
      String refEntityName,
      REDCode refEntityRedCode,
      SeniorityLevel seniorityLevel
  ) {
    HolidayCalendar calendar = HolidayCalendars.NO_HOLIDAYS;
    BusinessDayConvention convention = BusinessDayConventions.FOLLOWING;
    BusinessDayAdjustment businessDayAdjustment = BusinessDayAdjustment.of(convention, calendar);
    LocalDate settleDate = calendar.shift(tradeDate, 3);
    LocalDate endDate = settleDate.plusYears(5);
    LocalDate stepInDate = tradeDate.plusDays(1);
    LocalDate startDate = tradeDate;
    DayCount dayCount = DayCounts.ACT_360;
    StubConvention stubConvention = StubConvention.SHORT_FINAL;
    Frequency frequency = Frequency.P3M;
    boolean payAccOnDefault = true;

    return CreditDefaultSwap.of(
        StandardId.of("test", "11"),
        GeneralTerms.singleName(
            startDate,
            endDate,
            BuyerConvention.PROTECTION,
            businessDayAdjustment,
            refEntityRedCode,
            refEntityName,
            Currency.USD,
            seniorityLevel
        ),
        null,
        ProtectionTerms.of(
            notional,
            RestructuringClause.XR
        )
    );

  }
}
