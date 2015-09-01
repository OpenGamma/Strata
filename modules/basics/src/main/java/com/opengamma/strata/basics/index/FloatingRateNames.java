/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.index;

import com.opengamma.strata.collect.named.ExtendedEnum;

/**
 * Constants and implementations for standard Floating rate indices.
 * <p>
 * Each constant returns a standard definition of the specified index.
 */
public final class FloatingRateNames {
  // constants are indirected via ENUM_LOOKUP to allow them to be replaced by config

  /**
   * The extended enum lookup from name to instance.
   */
  static final ExtendedEnum<IborIndex> ENUM_LOOKUP = ExtendedEnum.of(IborIndex.class);

  //-------------------------------------------------------------------------
  /**
   * The FpML 'GBP-LIBOR-BBA' index of type 'Ibor'.
   */
  public static final FloatingRateName GBP_LIBOR_BBA = FloatingRateName.of("GBP-LIBOR-BBA");
  /**
   * The FpML 'CHF-LIBOR-BBA' index of type 'Ibor'.
   */
  public static final FloatingRateName CHF_LIBOR_BBA = FloatingRateName.of("CHF-LIBOR-BBA");
  /**
   * The FpML 'EUR-LIBOR-BBA' index of type 'Ibor'.
   */
  public static final FloatingRateName EUR_LIBOR_BBA = FloatingRateName.of("EUR-LIBOR-BBA");
  /**
   * The FpML 'JPY-LIBOR-BBA' index of type 'Ibor'.
   */
  public static final FloatingRateName JPY_LIBOR_BBA = FloatingRateName.of("JPY-LIBOR-BBA");
  /**
   * The FpML 'USD-LIBOR-BBA' index of type 'Ibor'.
   */
  public static final FloatingRateName USD_LIBOR_BBA = FloatingRateName.of("USD-LIBOR-BBA");
  /**
   * The FpML 'EUR-EURIBOR-Reuters' index of type 'Ibor'.
   */
  public static final FloatingRateName EUR_EURIBOR_REUTERS = FloatingRateName.of("EUR-EURIBOR-Reuters");
  /**
   * The FpML 'JPY-TIBOR-TIBM' index of type 'Ibor'.
   */
  public static final FloatingRateName JPY_TIBOR_TIBM = FloatingRateName.of("JPY-TIBOR-TIBM");

  /**
   * The FpML 'GBP-WMBA-SONIA-COMPOUND' index of type 'OvernightCompounded'.
   */
  public static final FloatingRateName GBP_WMBA_SONIA_COMPOUND = FloatingRateName.of("GBP-WMBA-SONIA-COMPOUND");
  /**
   * The FpML 'CHF-TOIS-OIS-COMPOUND' index of type 'OvernightCompounded'.
   */
  public static final FloatingRateName CHF_TOIS_OIS_COMPOUND = FloatingRateName.of("CHF-TOIS-OIS-COMPOUND");
  /**
   * The FpML 'EUR-EONIA-OIS-COMPOUND' index of type 'OvernightCompounded'.
   */
  public static final FloatingRateName EUR_EONIA_OIS_COMPOUND = FloatingRateName.of("EUR-EONIA-OIS-COMPOUND");
  /**
   * The FpML 'JPY-TONA-OIS-COMPOUND' index of type 'OvernightCompounded'.
   */
  public static final FloatingRateName JPY_TONA_OIS_COMPOUND = FloatingRateName.of("JPY-TONA-OIS-COMPOUND");
  /**
   * The FpML 'USD-Federal Funds-H.15-OIS-COMPOUND' index of type 'OvernightCompounded'.
   */
  public static final FloatingRateName USD_FEDERAL_FUNDS_H15_OIS_COMPOUND =
      FloatingRateName.of("USD-Federal Funds-H.15-OIS-COMPOUND");
  /**
   * The FpML 'USD-Federal Funds-H.15' index. of type 'OvernightAveraged'.
   */
  public static final FloatingRateName USD_FEDERAL_FUNDS_H15 = FloatingRateName.of("USD-Federal Funds-H.15");

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private FloatingRateNames() {
  }

}
