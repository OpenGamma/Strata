/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.basics.index;

import com.opengamma.basics.currency.Currency;
import com.opengamma.basics.location.Country;
import com.opengamma.basics.schedule.Frequency;

/**
 * Standard Price index implementations.
 * <p>
 * See {@link PriceIndices} for the description of each.
 */
final class StandardPriceIndices {

  private static final String GB_HICP_NAME = "GB-HICP";
  private static final String GB_RPI_NAME = "GB-RPI";
  private static final String GB_RPIX_NAME = "GB-RPIX";
  private static final String CH_CPI_NAME = "CH-CPI";
  private static final String EU_HICP_AI_NAME = "EU-HICP-AI";
  private static final String JP_CPI_EXF_NAME = "JP-CPI-EXF";
  private static final String US_CPI_U_NAME = "US-CPI-U";

  // GB HICP
  public static final PriceIndex GB_HICP = ImmutablePriceIndex.builder()
      .name(GB_HICP_NAME)
      .region(Country.GB)
      .currency(Currency.GBP)
      .publicationFrequency(Frequency.P1M)
      .build();
  // GB RPI
  public static final PriceIndex GB_RPI = ImmutablePriceIndex.builder()
      .name(GB_RPI_NAME)
      .region(Country.GB)
      .currency(Currency.GBP)
      .publicationFrequency(Frequency.P1M)
      .build();
  // GB RPI excluding mortgage payments
  public static final PriceIndex GB_RPIX = ImmutablePriceIndex.builder()
      .name(GB_RPIX_NAME)
      .region(Country.GB)
      .currency(Currency.GBP)
      .publicationFrequency(Frequency.P1M)
      .build();
  // Switzerland CPI
  public static final PriceIndex CH_CPI = ImmutablePriceIndex.builder()
      .name(CH_CPI_NAME)
      .region(Country.CH)
      .currency(Currency.CHF)
      .publicationFrequency(Frequency.P1M)
      .build();
  // Europe all items HICP
  public static final PriceIndex EU_HICP_AI = ImmutablePriceIndex.builder()
      .name(EU_HICP_AI_NAME)
      .region(Country.EU)
      .currency(Currency.EUR)
      .publicationFrequency(Frequency.P1M)
      .build();
  // Japan CPI excluding fresh food
  public static final PriceIndex JP_CPI_EXF = ImmutablePriceIndex.builder()
      .name(JP_CPI_EXF_NAME)
      .region(Country.JP)
      .currency(Currency.JPY)
      .publicationFrequency(Frequency.P1M)
      .build();
  // US Urban CPI
  public static final PriceIndex US_CPI_U = ImmutablePriceIndex.builder()
      .name(US_CPI_U_NAME)
      .region(Country.US)
      .currency(Currency.USD)
      .publicationFrequency(Frequency.P1M)
      .build();

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private StandardPriceIndices() {
  }

}
