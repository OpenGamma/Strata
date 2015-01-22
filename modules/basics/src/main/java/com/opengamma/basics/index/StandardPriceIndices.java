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

  // GB HICP
  public static final PriceIndex GB_HICP = ImmutablePriceIndex.builder()
      .name("GB-HICP")
      .region(Country.GB)
      .currency(Currency.GBP)
      .publicationFrequency(Frequency.P1M)
      .build();
  // GB RPI
  public static final PriceIndex GB_RPI = ImmutablePriceIndex.builder()
      .name("GB-RPI")
      .region(Country.GB)
      .currency(Currency.GBP)
      .publicationFrequency(Frequency.P1M)
      .build();
  // GB RPI excluding mortgage payments
  public static final PriceIndex GB_RPIX = ImmutablePriceIndex.builder()
      .name("GB-RPIX")
      .region(Country.GB)
      .currency(Currency.GBP)
      .publicationFrequency(Frequency.P1M)
      .build();
  // Switzerland CPI
  public static final PriceIndex CH_CPI = ImmutablePriceIndex.builder()
      .name("CH-CPI")
      .region(Country.CH)
      .currency(Currency.CHF)
      .publicationFrequency(Frequency.P1M)
      .build();
  // Europe all items HICP
  public static final PriceIndex EU_HICP_AI = ImmutablePriceIndex.builder()
      .name("EU-HICP-AI")
      .region(Country.EU)
      .currency(Currency.EUR)
      .publicationFrequency(Frequency.P1M)
      .build();
  // Japan CPI excluding fresh food
  public static final PriceIndex JP_CPI_EXF = ImmutablePriceIndex.builder()
      .name("JP-CPI-EXF")
      .region(Country.JP)
      .currency(Currency.JPY)
      .publicationFrequency(Frequency.P1M)
      .build();
  // US Urban CPI
  public static final PriceIndex US_CPI_U = ImmutablePriceIndex.builder()
      .name("US-CPI-U")
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
