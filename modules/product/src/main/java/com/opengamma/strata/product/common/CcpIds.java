/*
 * Copyright (C) 2019 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.common;

/**
 * Identifiers for common CCPs.
 */
public final class CcpIds {
  // alphabetical

  /** Australian Securities Exchange. */
  public static final CcpId ASX = CcpId.of("ASX");

  /** Bursa Malaysia Derivatives. */
  public static final CcpId BMD = CcpId.of("BMD");

  /** Canadian Derivatives Clearing Corporation. */
  public static final CcpId CDCC = CcpId.of("CDCC");

  /** Chicago Mercantile Exchange. */
  public static final CcpId CME = CcpId.of("CME");

  /** European Commodity Clearing. */
  public static final CcpId ECC = CcpId.of("ECC");

  /** Eurex. */
  public static final CcpId EUREX = CcpId.of("EUREX");

  /** Hong Kong Exchange. */
  public static final CcpId HKEX = CcpId.of("HKEX");

  /** Intercontinental Exchange (EU). */
  public static final CcpId ICE_EU = CcpId.of("ICE-EU");

  /** Intercontinental Exchange (US). */
  public static final CcpId ICE_US = CcpId.of("ICE-US");

  /** Japan Securities Clearing Corporation. */
  public static final CcpId JSCC = CcpId.of("JSCC");

  /** Japan Commodity Clearing House. */
  public static final CcpId JCCH = CcpId.of("JCCH");

  /** London Clearing House. */
  public static final CcpId LCH = CcpId.of("LCH");

  /** London Metal Exchange Clear. */
  public static final CcpId LME = CcpId.of("LME");

  /** Minneapolis Grain Exchange. */
  public static final CcpId MGEX = CcpId.of("MGEX");

  /** Options Clearing Corporation. */
  public static final CcpId OCC = CcpId.of("OCC");

  /** Singapore Exchange. */
  public static final CcpId SGX = CcpId.of("SGX");

  /** Tokyo Financial Exchange. */
  public static final CcpId TFX = CcpId.of("TFX");

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private CcpIds() {
  }

}
