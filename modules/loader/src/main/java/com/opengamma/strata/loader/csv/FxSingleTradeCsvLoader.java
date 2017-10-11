package com.opengamma.strata.loader.csv;

import static com.opengamma.strata.product.common.PayReceive.PAY;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.collect.io.CsvRow;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.common.PayReceive;
import com.opengamma.strata.product.fx.FxSingle;
import com.opengamma.strata.product.fx.FxSingleTrade;

/**
 * Loads FX Trades from CSV files. So far, support exists for FX Forwards.
 */
class FxSingleTradeCsvLoader {
  private static final String PAYMENT_DATE_HEADER = "Payment Date";
  private static final String LEG_1_DIRECTION_HEADER = "Leg 1 Direction";
  private static final String LEG_1_CURRENCY_HEADER = "Leg 1 Currency";
  private static final String LEG_1_NOTIONAL_HEADER = "Leg 1 Notional";
  private static final String LEG_2_DIRECTION_HEADER = "Leg 2 Direction";
  private static final String LEG_2_CURRENCY_HEADER = "Leg 2 Currency";
  private static final String LEG_2_NOTIONAL_HEADER = "Leg 2 Notional";
  private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

  /**
   * Parses the data from a CSV row.
   *
   * @param row  the CSV row object
   * @param info  the trade info object
   * @param resolver  the resolver used to parse additional information. This is not currently used in this method.
   * @return the parsed trade, as an instance of {@link FxSingleTrade}
   */
  static FxSingleTrade parse(CsvRow row, TradeInfo info, TradeCsvInfoResolver resolver) {
    FxSingleTrade trade = parseRow(row, info, resolver);
    return resolver.completeTrade(row, trade);
  }

  /**
   * Parses the data from a CSV row.
   *
   * @param row  the CSV row object
   * @param info  the trade info object
   * @param resolver  the resolver used to parse additional information. This is not currently used in this method.
   * @return the parsed trade, as an instance of {@link FxSingleTrade}
   */
  private static FxSingleTrade parseRow(CsvRow row, TradeInfo info, TradeCsvInfoResolver resolver) {
    LocalDate paymentDate = LocalDate.parse(row.getField(PAYMENT_DATE_HEADER),
        DATE_TIME_FORMATTER);
    PayReceive leg1Direction = PayReceive.of(row.getField(LEG_1_DIRECTION_HEADER));
    Currency leg1Currency = Currency.of(row.getField(LEG_1_CURRENCY_HEADER));
    double leg1Notional = Double.parseDouble(row.getField(LEG_1_NOTIONAL_HEADER));
    PayReceive leg2Direction = PayReceive.of(row.getField(LEG_2_DIRECTION_HEADER));
    Currency leg2Currency = Currency.of(row.getField(LEG_2_CURRENCY_HEADER));
    double leg2Notional = Double.parseDouble(row.getField(LEG_2_NOTIONAL_HEADER));

    int leg1DirectionMultiplier = leg1Direction.equals(PAY) ? 1 : -1;
    int leg2DirectionMultiplier = leg2Direction.equals(PAY) ? 1 : -1;
    CurrencyAmount firstLeg = CurrencyAmount.of(leg1Currency, leg1DirectionMultiplier * leg1Notional);
    CurrencyAmount secondLeg = CurrencyAmount.of(leg2Currency, leg2DirectionMultiplier * leg2Notional);

    return FxSingleTrade.builder()
        .info(info)
        .product(FxSingle.of(firstLeg, secondLeg, paymentDate))
        .build();
  }
}
