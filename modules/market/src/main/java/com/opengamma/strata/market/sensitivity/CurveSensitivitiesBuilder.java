/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.sensitivity;

import java.util.Map;
import java.util.TreeMap;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.collect.MapStream;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.param.CurrencyParameterSensitivitiesBuilder;
import com.opengamma.strata.market.param.CurrencyParameterSensitivity;
import com.opengamma.strata.market.param.ParameterMetadata;
import com.opengamma.strata.product.PortfolioItemInfo;

/**
 * Builder for {@code CurveSensitivities}.
 */
public final class CurveSensitivitiesBuilder {

  /**
   * The info.
   */
  private PortfolioItemInfo info;
  /**
   * The map of sensitivity data.
   */
  private final Map<CurveSensitivitiesType, CurrencyParameterSensitivitiesBuilder> data = new TreeMap<>();

  //-------------------------------------------------------------------------
  // restricted constructor
  CurveSensitivitiesBuilder(PortfolioItemInfo info) {
    this.info = info;
  }

  //-------------------------------------------------------------------------
  /**
   * Adds another set of sensitivities to the builder.
   * <p>
   * Values with the same market data name and currency will be merged.
   * 
   * @param other  the other sensitivities
   * @return this, for chaining
   */
  CurveSensitivitiesBuilder combine(CurveSensitivitiesBuilder other) {
    this.info = this.info.combinedWith(other.info);
    other.data.forEach((type, map) -> add(type, map.build()));
    return this;
  }

  /**
   * Adds another set of sensitivities to the builder.
   * <p>
   * Values with the same market data name and currency will be merged.
   * 
   * @param other  the other sensitivities
   * @return this, for chaining
   */
  public CurveSensitivitiesBuilder add(CurveSensitivities other) {
    this.info = this.info.combinedWith(other.getInfo());
    other.getTypedSensitivities().forEach(this::add);
    return this;
  }

  /**
   * Adds sensitivities to the builder.
   * <p>
   * Values with the same market data name and currency will be merged.
   * 
   * @param type  the sensitivity type
   * @param sensitivities  the sensitivities to add
   * @return this, for chaining
   */
  public CurveSensitivitiesBuilder add(
      CurveSensitivitiesType type,
      CurrencyParameterSensitivities sensitivities) {

    data.computeIfAbsent(type, t -> CurrencyParameterSensitivities.builder())
        .add(sensitivities);
    return this;
  }

  /**
   * Adds a sensitivity to the builder.
   * <p>
   * Values with the same market data name and currency will be merged.
   * 
   * @param type  the sensitivity type
   * @param sensitivity  the sensitivity to add
   * @return this, for chaining
   */
  public CurveSensitivitiesBuilder add(
      CurveSensitivitiesType type,
      CurrencyParameterSensitivity sensitivity) {

    data.computeIfAbsent(type, t -> CurrencyParameterSensitivities.builder())
        .add(sensitivity);
    return this;
  }

  /**
   * Adds a single sensitivity to the builder.
   * <p>
   * Values with the same market data name and currency will be merged.
   * 
   * @param type  the sensitivity type
   * @param curveName  the curve name
   * @param currency  the currency of the sensitivity
   * @param metadata  the sensitivity metadata, not empty
   * @param sensitivityValue  the sensitivity value
   * @return this, for chaining
   */
  public CurveSensitivitiesBuilder add(
      CurveSensitivitiesType type,
      CurveName curveName,
      Currency currency,
      ParameterMetadata metadata,
      double sensitivityValue) {

    data.computeIfAbsent(type, t -> CurrencyParameterSensitivities.builder())
        .add(curveName, currency, metadata, sensitivityValue);
    return this;
  }

  //-------------------------------------------------------------------------
  /**
   * Builds the sensitivity from the provided data.
   * <p>
   * If all the values for a single sensitivity are tenor-based, or all are date-based,
   * then the resulting sensitivity will have the tenors sorted.
   * 
   * @return the sensitivities instance
   */
  public CurveSensitivities build() {
    return CurveSensitivities.of(info, MapStream.of(data)
        .mapValues(CurrencyParameterSensitivitiesBuilder::build)
        .toMap());
  }

}
