/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.common;

import static java.time.temporal.ChronoUnit.MONTHS;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.product.PortfolioItemSummary;
import com.opengamma.strata.product.PortfolioItemType;
import com.opengamma.strata.product.Position;
import com.opengamma.strata.product.ProductType;
import com.opengamma.strata.product.Trade;

/**
 * Utilities to support summarizing portfolio items.
 * <p>
 * This class provides a central place for description logic.
 */
public final class SummarizerUtils {

  /** Date format. */
  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dMMMuu", Locale.UK);

  // restricted constructor
  private SummarizerUtils() {
  }

  //-------------------------------------------------------------------------
  /**
   * Converts a date to a string.
   * 
   * @param date  the date
   * @return the string form
   */
  public static String date(LocalDate date) {
    return date.format(DATE_FORMAT);
  }

  /**
   * Converts a date range to a string.
   * 
   * @param start  the start date
   * @param end  the end date
   * @return the string form
   */
  public static String dateRange(LocalDate start, LocalDate end) {
    return date(start) + "-" + date(end);
  }

  /**
   * Converts a date range to a period string.
   * 
   * @param start  the start date
   * @param end  the end date
   * @return the string form
   */
  public static String datePeriod(LocalDate start, LocalDate end) {
    int months = Math.toIntExact(MONTHS.between(start, end.plusDays(3)));
    if (months > 0) {
      return Tenor.of(Period.ofMonths((int) months)).normalized().toString();
    } else {
      return Tenor.of(Period.ofDays((int) start.until(end, ChronoUnit.DAYS))).toString();
    }
  }

  /**
   * Converts an amount to a string.
   * 
   * @param currencyAmount  the amount
   * @return the string form
   */
  public static String amount(CurrencyAmount currencyAmount) {
    return amount(currencyAmount.getCurrency(), currencyAmount.getAmount());
  }

  /**
   * Converts an amount to a string.
   * 
   * @param currency  the currency
   * @param value  the value
   * @return the string form
   */
  public static String amount(Currency currency, double value) {
    BigDecimal dec = BigDecimal.valueOf(value).stripTrailingZeros();
    String symbol = currency.getCode() + " ";
    if (dec.scale() <= -3) {
      if (Math.abs(dec.longValue()) >= 1_000_000L) {
        dec = dec.movePointLeft(6);
        return symbol + dec.toPlainString() + "mm";
      } else {
        dec = dec.movePointLeft(3);
        return symbol + dec.toPlainString() + "k";
      }
    }
    if (dec.scale() > currency.getMinorUnitDigits()) {
      dec = dec.setScale(currency.getMinorUnitDigits(), RoundingMode.HALF_UP);
    }
    DecimalFormat formatter = new DecimalFormat("###,###.###", new DecimalFormatSymbols(Locale.UK));
    return symbol + formatter.format(dec);
  }

  /**
   * Converts a value to a string.
   * 
   * @param value  the value
   * @return the string form
   */
  public static String value(double value) {
    BigDecimal dec = BigDecimal.valueOf(value).stripTrailingZeros();
    if (dec.scale() > 6) {
      dec = dec.setScale(6, RoundingMode.HALF_UP);
    }
    return dec.toPlainString();
  }

  /**
   * Converts a value to a percentage string.
   * 
   * @param value  the value
   * @return the string form
   */
  public static String percent(double value) {
    BigDecimal dec = BigDecimal.valueOf(value);
    dec = dec.multiply(BigDecimal.valueOf(100)).stripTrailingZeros();
    if (dec.scale() > 4) {
      dec = dec.setScale(4, RoundingMode.HALF_UP);
    }
    return dec.toPlainString() + "%";
  }

  //-------------------------------------------------------------------------
  /**
   * Converts pay/receive to a string.
   * 
   * @param payReceive  the value
   * @return the string form
   */
  public static String payReceive(PayReceive payReceive) {
    return payReceive.toString().substring(0, 3);
  }

  /**
   * Converts an FX exchange to a string.
   * 
   * @param base  the base currency amount
   * @param counter  the counter currency amount
   * @return the string form
   */
  public static String fx(CurrencyAmount base, CurrencyAmount counter) {
    BigDecimal rateDec = BigDecimal.valueOf(
        counter.getAmount() / base.getAmount()).setScale(base.getCurrency().getMinorUnitDigits() + 2, RoundingMode.HALF_UP).abs();
    FxRate rate = FxRate.of(base.getCurrency(), counter.getCurrency(), rateDec.doubleValue());
    BigDecimal baseDec = BigDecimal.valueOf(base.getAmount()).stripTrailingZeros();
    BigDecimal counterDec = BigDecimal.valueOf(counter.getAmount()).stripTrailingZeros();
    boolean roundBase = baseDec.scale() < counterDec.scale();
    CurrencyAmount round = roundBase ? base : counter;
    return (round.getAmount() < 0 ? "Pay " : "Rec ") +
        SummarizerUtils.amount(round.mapAmount(a -> Math.abs(a))) + " " + "@ " + rate;
  }

  //-------------------------------------------------------------------------
  /**
   * Creates a summary instance for a position.
   * 
   * @param position  the position
   * @param type  the type
   * @param description  the description
   * @param currencies  the currencies, may be empty
   * @return the string form
   */
  public static PortfolioItemSummary summary(Position position, ProductType type, String description, Currency... currencies) {
    return PortfolioItemSummary.of(
        position.getId().orElse(null), PortfolioItemType.POSITION, type, ImmutableSet.copyOf(currencies), description);
  }

  /**
   * Creates a summary instance for a trade.
   * 
   * @param trade  the trade
   * @param type  the type
   * @param description  the description
   * @param currencies  the currencies, may be empty
   * @return the string form
   */
  public static PortfolioItemSummary summary(Trade trade, ProductType type, String description, Currency... currencies) {
    return PortfolioItemSummary.of(
        trade.getId().orElse(null), PortfolioItemType.TRADE, type, ImmutableSet.copyOf(currencies), description);
  }

}
