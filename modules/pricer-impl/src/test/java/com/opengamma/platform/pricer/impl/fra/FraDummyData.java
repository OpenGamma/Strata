package com.opengamma.platform.pricer.impl.fra;

import static com.opengamma.basics.BuySell.BUY;
import static com.opengamma.basics.BuySell.SELL;
import static com.opengamma.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.basics.date.HolidayCalendars.GBLO;
import static com.opengamma.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.collect.TestHelper.date;

import com.opengamma.basics.currency.Currency;
import com.opengamma.basics.date.AdjustableDate;
import com.opengamma.basics.date.BusinessDayAdjustment;
import com.opengamma.basics.date.DaysAdjustment;
import com.opengamma.collect.id.StandardId;
import com.opengamma.platform.finance.fra.Fra;
import com.opengamma.platform.finance.fra.FraDiscountingMethod;
import com.opengamma.platform.finance.fra.FraTrade;

/**
 * Basic dummy objects used when the data within is not important.
 */
public class FraDummyData {

  /**
   * The notional.
   */
  public static final double NOTIONAL = 1_000_000d;
  /**
   *  Business day convention: modified following
   */
  private static final BusinessDayAdjustment BDA_MOD_FOLLOW = BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO);
  /**
   * Fixing offset: -2
   */
  private static final DaysAdjustment MINUS_TWO_DAYS = DaysAdjustment.ofBusinessDays(-2, GBLO);

  /**
   * Fra, default discounting method. 
   */
  public static final Fra FRA = Fra.builder()
      .buySell(BUY)
      .notional(NOTIONAL)
      .paymentDate(AdjustableDate.of(date(2014, 12, 12), BDA_MOD_FOLLOW))
      .startDate(date(2014, 9, 12))
      .endDate(date(2014, 12, 12))
      .index(GBP_LIBOR_3M)
      .fixedRate(0.0125)
      .currency(Currency.GBP)
      .fixingOffset(MINUS_TWO_DAYS)
      .build();

  /**
   * Fra, AFMA discounting method. 
   */
  public static final Fra FRA_AFMA = Fra.builder()
      .buySell(SELL)
      .notional(NOTIONAL)
      .paymentDate(AdjustableDate.of(date(2014, 12, 12), BDA_MOD_FOLLOW))
      .startDate(date(2014, 9, 12))
      .endDate(date(2014, 12, 12))
      .index(GBP_LIBOR_3M)
      .fixedRate(0.0125)
      .currency(Currency.GBP)
      .fixingOffset(MINUS_TWO_DAYS)
      .discounting(FraDiscountingMethod.AFMA)
      .build();

  /**
   * FraTrade, default discounting method. 
   */
  public static final FraTrade FRA_TRADE = FraTrade.builder()
      .standardId(StandardId.of("OG-Trade", "1"))
      .tradeDate(date(2014, 6, 30))
      .fra(FRA)
      .build();
}
