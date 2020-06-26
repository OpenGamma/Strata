/*
 * Copyright (C) 2020 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.date;

import java.io.Serializable;
import java.time.Period;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.opengamma.strata.collect.ArgChecker;

/**
 * A code used in the market to indicate both the start date and tenor of a financial instrument.
 * <p>
 * In Strata, a {@link Tenor} is the actual tenor of an instrument, from start to end.
 * This class represents the code used in the market which also effectively describes the start date.
 * Four key dates are needed to understand how this code works.
 * <ul>
 * <li>Trade date, the date that the trade is agreed
 * <li>Spot date, the base for date calculations, typically 2 business days after the trade date, known as the spot lag
 * <li>Start date, the date on which accrual starts, generally the spot date unless forward starting
 * <li>End date, the date on which accrual ends
 * </ul>
 * <p>
 * The period from start date to end date is represented by {@link Tenor}.
 * {@code MarketTenor} includes the tenor, but also allows the market conventional spot lag to be overridden.
 * <p>
 * {@code MarketTenor} represents the 4 special cases used in the market as well as more normal tenors:
 * <ul>
 * <li>ON - Overnight, from today to tomorrow, spot lag of 0 and tenor of 1 day
 * <li>TN - Tomorrow-Next, from tomorrow to the next day, spot lag of 1 and tenor of 1 day
 * <li>SN - Spot-Next, from spot to the next day, market conventional spot lag and tenor of 1 day
 * <li>SW - Spot-Week, from spot for one week, market conventional spot lag and tenor of 1 week
 * <li>"Normal" tenors - 2W, 1M, 1Y etc - from spot for the specified period, market conventional spot lag
 * </ul>
 * <p>
 * Note that if the market conventional spot lag is 1 day, TN and SN would resolve to the same dates.
 * Note also that SN and SW exist for clarity, they might also be expressed as 1D and 1W with the spot implied.
 * Other date combinations are possible in theory but tend not to exist in the market, for example a three day
 * trade starting tomorrow, something which might require a code like T3D.
 */
public final class MarketTenor
    implements Comparable<MarketTenor>, Serializable {

  /**
   * Serialization version.
   */
  private static final long serialVersionUID = 1;
  /**
   * Constant used to indicate market convention swap lag.
   */
  private static final int MARKET_CONVENTION_LAG = Integer.MAX_VALUE;

  /**
   * A tenor code for Overnight, meaning from today to tomorrow.
   */
  public static final MarketTenor ON = new MarketTenor("ON", Tenor.ofDays(1), 0);
  /**
   * A tenor code for Tomorrow-Next, meaning from tomorrow to the next day.
   */
  public static final MarketTenor TN = new MarketTenor("TN", Tenor.ofDays(1), 1);
  /**
   * A tenor code for Spot-Next, meaning from the spot date to the next day.
   * The spot date is usually two working days after today, but this varies by currency.
   */
  public static final MarketTenor SN = new MarketTenor("SN", Tenor.ofDays(1), MARKET_CONVENTION_LAG);
  /**
   * A tenor code for Spot-Week, meaning one week starting from the spot date.
   * The spot date is usually two working days after today, but this varies by currency.
   */
  public static final MarketTenor SW = new MarketTenor("SW", Tenor.ofWeeks(1), MARKET_CONVENTION_LAG);

  /**
   * The code of the tenor.
   */
  private final String code;
  /**
   * The tenor.
   */
  private final Tenor tenor;
  /**
   * The spot lag or 0, 1 or MARKET_CONVENTION_LAG.
   */
  private final int spotLagIndicator;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from a {@code Tenor} with spot implied.
   * <p>
   * A tenor of 1D will return SN.
   * A tenor of 1W will return SW.
   * Other tenors will return a market tenor with the same code as the tenor.
   *
   * @param tenor  the tenor
   * @return the tenor
   */
  public static MarketTenor ofSpot(Tenor tenor) {
    if (tenor.getPeriod().equals(Period.ofDays(1))) {
      return MarketTenor.SN;
    }
    if (tenor.getPeriod().equals(Period.ofDays(7))) {
      return MarketTenor.SW;
    }
    return new MarketTenor(tenor.toString(), tenor, MARKET_CONVENTION_LAG);
  }

  //-------------------------------------------------------------------------
  /**
   * Parses a formatted string representing the market tenor.
   * <p>
   * This parses ON, TN, SN, SW and all formats accepted by {@link Tenor#parse(String)}.
   *
   * @param toParse  the string representing the tenor
   * @return the tenor
   * @throws IllegalArgumentException if the tenor cannot be parsed
   */
  @FromString
  public static MarketTenor parse(String toParse) {
    ArgChecker.notEmpty(toParse, "toParse");
    switch (toParse) {
      case "ON":
        return MarketTenor.ON;
      case "TN":
        return MarketTenor.TN;
      case "SN":
        return MarketTenor.SN;
      case "SW":
        return MarketTenor.SW;
      default:
        return MarketTenor.ofSpot(Tenor.parse(toParse));
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Creates a tenor.
   * @param code  the code
   * @param tenor  the tenor to represent
   * @param spotLagIndicator  the order, 0 for ON, 1 for TN and MARKET_CONVENTION_LAG for spot-starting
   */
  private MarketTenor(String code, Tenor tenor, int spotLagIndicator) {
    ArgChecker.notNull(code, "code");
    ArgChecker.notNull(tenor, "tenor");
    this.code = code;
    this.tenor = tenor;
    this.spotLagIndicator = spotLagIndicator;
  }

  // safe deserialization
  private Object readResolve() {
    return MarketTenor.parse(code);
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the market tenor code.
   *
   * @return the code
   */
  public String getCode() {
    return code;
  }

  /**
   * Gets the tenor of the instrument.
   * <p>
   * The tenor for ON, TN and SN is 1 day and for SW is 1 week.
   *
   * @return the tenor of the instrument
   */
  public Tenor getTenor() {
    return tenor;
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if the market tenor implies a non-standard spot lag.
   * <p>
   * This returns true for ON and TN as they need special date handling.
   * SN and SW return false as they imply market conventional spot.
   *
   * @return true if this market tenor is short
   */
  public boolean isNonStandardSpotLag() {
    return spotLagIndicator != MARKET_CONVENTION_LAG;
  }

  /**
   * Adjusts the market conventional spot lag to match the market tenor.
   * <p>
   * The resulting lag will be 0 days for ON and 1 day for TN.
   * Otherwise the input lag will be unchanged.
   *
   * @param marketConventionalSpotLag  the market conventional spot lag
   * @return true if this market tenor is short
   */
  public DaysAdjustment adjustSpotLag(DaysAdjustment marketConventionalSpotLag) {
    if (isNonStandardSpotLag()) {
      // this way ensures the result is a valid business day
      return DaysAdjustment.ofBusinessDays(spotLagIndicator, marketConventionalSpotLag.getResultCalendar());
    } else {
      return marketConventionalSpotLag;
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Compares this market tenor to another market tenor.
   * <p>
   * Comparing tenors is a hard problem in general, but for commonly used tenors the outcome is as expected.
   * This uses the same sort order as {@link Tenor} with ON and TN first.
   * 
   * @param other  the other tenor
   * @return negative if this is less than the other, zero if equal and positive if greater
   */
  @Override
  public int compareTo(MarketTenor other) {
    if (isNonStandardSpotLag() || other.isNonStandardSpotLag()) {
      return Integer.compare(spotLagIndicator, other.spotLagIndicator);
    }
    return tenor.compareTo(other.tenor);
  }

  /**
   * Checks if this market tenor equals another market tenor.
   * <p>
   * The comparison checks the code.
   * 
   * @param obj  the other market tenor, null returns false
   * @return true if equal
   */
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    MarketTenor other = (MarketTenor) obj;
    return code.equals(other.code);
  }

  /**
   * Returns a suitable hash code for the market tenor.
   * 
   * @return the hash code
   */
  @Override
  public int hashCode() {
    return code.hashCode();
  }

  /**
   * Returns a formatted string representing the market tenor.
   *
   * @return the formatted market tenor
   */
  @ToString
  @Override
  public String toString() {
    return code;
  }

}
