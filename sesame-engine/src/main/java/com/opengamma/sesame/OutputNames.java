/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

/**
 * Names of outputs available within the system.
 * <p>
 * These are used as map keys, thus they must be unique.
 */
public final class OutputNames {
  // In an ideal world, these would not be strings, however the Java programming
  // language offers few choices for annotation values
  // Alternative options were examined (SSM-130) but found to be worse

  /**
   * Output name when providing a description.
   */
  public static final String DESCRIPTION = "Description";
  /**
   * Output name when providing Present Value.
   */
  public static final String PRESENT_VALUE = "Present Value";
  /**
   * Output name when providing FX Present Value.
   */
  public static final String FX_PRESENT_VALUE = "FX Present Value";
  /**
   * Output name when providing Discounting Multicurve Bundle.
   */
  public static final String DISCOUNTING_MULTICURVE_BUNDLE = "Discounting Multicurve Bundle";
  /**
   * Output name when providing Issuer Provider Bundle.
   */
  public static final String ISSUER_PROVIDER_BUNDLE = "Issuer Provider Bundle";
  /**
   * Output name when providing P&L Series.
   */
  public static final String PNL_SERIES = "P&L Series";
  /**
   * Output name when providing YCNS P&L Series.
   */
  public static final String YCNS_PNL_SERIES = "YCNS P&L Series";
  /**
   * Output name when providing Yield Curve Node Sensitivities.
   */
  public static final String YIELD_CURVE_NODE_SENSITIVITIES = "Yield Curve Node Sensitivities";
  /**
   * Output name when providing Par Rate.
   */
  public static final String PAR_RATE = "Par Rate";
  /**
   * The PV01 of a cash-flow based fixed-income instrument.
   */
  public static final String PV01 = "PV01";
  /**
   * The implied volatility of an option contract.
   */
  public static final String IMPLIED_VOLATILITY = "Implied Volatility";
  /**
   * The bucketed PV01
   */
  public static final String BUCKETED_PV01 = "Bucketed PV01";
  /**
   * Bucketed SABR risk
   */
  public static final String BUCKETED_SABR_RISK = "Bucketed SABR Risk";

  /**
   * Restricted constructor.
   */
  private OutputNames() {
  }

}
