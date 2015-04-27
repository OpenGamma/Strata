/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.config;

import java.util.regex.Pattern;

import org.joda.convert.FromString;

import com.opengamma.strata.collect.type.TypedString;

/**
 * Identifies a measure that can be produced by the system.
 * <p>
 * A measure identifies the calculation result that is required.
 * For example present value, par rate or spread.
 * <p>
 * Some measures represent aspects of the calculation target rather than a calculation.
 * For example, the target identifier, counterparty and trade date.
 * <p>
 * Note that not all measures will be available for all targets.
 */
public final class Measure
    extends TypedString<Measure> {

  /** Serialization version. */
  private static final long serialVersionUID = 1L;
  /**
   * Pattern for checking the name.
   * It must only contains the characters A-Z, a-z, 0-9 and -.
   */
  private static final Pattern NAME_PATTERN = Pattern.compile("[A-Za-z0-9-]+");

  //-------------------------------------------------------------------------
  /** Measure representing the accrued interest of the calculation target. */
  public static final Measure ACCRUED_INTEREST = Measure.of("AccruedInterest");
  
  /** Measure representing the counterparty of the calculation target. */
  public static final Measure COUNTERPARTY = Measure.of("Counterparty");

  /** Measure representing the ID of the calculation target. */
  public static final Measure ID = Measure.of("Id");

  /** Measure representing the maturity date of the calculation target. */
  public static final Measure MATURITY_DATE = Measure.of("MaturityDate");

  /** Measure representing the notional amount of the calculation target. */
  public static final Measure NOTIONAL = Measure.of("Notional");
  
  /** Measure representing the par rate of the calculation target. */
  public static final Measure PAR_RATE = Measure.of("ParRate");
  
  /** Measure representing the par spread of the calculation target. */
  public static final Measure PAR_SPREAD = Measure.of("ParSpread");

  /** Measure representing the present value of the calculation target. */
  public static final Measure PRESENT_VALUE = Measure.of("PresentValue");

  /** Measure representing the present value of the pay leg of the calculation target. */
  public static final Measure PRESENT_VALUE_PAY_LEG = Measure.of("PresentValuePayLeg");

  /** Measure representing the present value of the receive leg of the calculation target. */
  public static final Measure PRESENT_VALUE_RECEIVE_LEG = Measure.of("PresentValueReceiveLeg");

  /** Measure representing the PV01 of the calculation target. */
  public static final Measure PV01 = Measure.of("PV01");
  
  /** Measure representing the settlement date of the calculation target. */
  public static final Measure SETTLEMENT_DATE = Measure.of("SettlementDate");
  
  /** Measure representing the trade date of the calculation target. */
  public static final Measure TRADE_DATE = Measure.of("TradeDate");

  //-------------------------------------------------------------------------
  /**
   * Obtains a {@code Measure} by name.
   * <p>
   * Measure names must only contains the characters A-Z, a-z, 0-9 and -.
   *
   * @param name  the name of the measure
   * @return a measure with the specified name
   */
  @FromString
  public static Measure of(String name) {
    return new Measure(name);
  }

  /**
   * Creates an instance.
   * 
   * @param name  the name of the measure
   */
  private Measure(String name) {
    super(name, NAME_PATTERN, "Measure name must only contain the characters A-Z, a-z, 0-9 and -");
  }

}
