/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.sensitivity;

import static com.opengamma.strata.collect.Guavate.toImmutableList;
import static com.opengamma.strata.collect.Guavate.toImmutableSet;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.MapStream;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.param.CurrencyParameterSensitivity;
import com.opengamma.strata.market.param.LabelDateParameterMetadata;
import com.opengamma.strata.market.param.ParameterMetadata;
import com.opengamma.strata.market.param.TenorDateParameterMetadata;
import com.opengamma.strata.market.param.TenorParameterMetadata;
import com.opengamma.strata.market.param.TenoredParameterMetadata;
import com.opengamma.strata.product.PortfolioItemInfo;

/**
 * Builder for {@code CurveSensitivities}
 */
public final class CurveSensitivitiesBuilder {

  /**
   * The info.
   */
  private final PortfolioItemInfo info;
  /**
   * The map of sensitivity data.
   */
  private final Map<CurveSensitivitiesType, SortedMap<CurveName, SensitivityMapBuilder>> data = new TreeMap<>();

  //-------------------------------------------------------------------------
  // restricted constructor
  CurveSensitivitiesBuilder(PortfolioItemInfo info) {
    this.info = info;
  }

  //-------------------------------------------------------------------------
  /**
   * Adds a single sensitivity to the builder using a tenor.
   * <p>
   * If the key already exists, the sensitivity value will be merged.
   * If all the values added are tenor-based, the resulting sensitivity will have the tenors sorted.
   * 
   * @param type  the sensitivity type
   * @param curveName  the curve name
   * @param tenor  the sensitivity tenor
   * @param sensitivityValue  the sensitivity value
   * @return this, for chaining
   * @throws IllegalArgumentException if the sensitivity currencies mismatch when merging
   */
  public CurveSensitivitiesBuilder add(
      CurveSensitivitiesType type,
      CurveName curveName,
      Tenor tenor,
      CurrencyAmount sensitivityValue) {

    return add(type, curveName, TenorParameterMetadata.of(tenor), sensitivityValue);
  }

  /**
   * Adds a single sensitivity to the builder using a date.
   * <p>
   * If the key already exists, the sensitivity value will be merged.
   * 
   * @param type  the sensitivity type
   * @param curveName  the curve name
   * @param date  the sensitivity date
   * @param sensitivityValue  the sensitivity value
   * @return this, for chaining
   * @throws IllegalArgumentException if the sensitivity currencies mismatch when merging
   */
  public CurveSensitivitiesBuilder add(
      CurveSensitivitiesType type,
      CurveName curveName,
      LocalDate date,
      CurrencyAmount sensitivityValue) {

    return add(type, curveName, LabelDateParameterMetadata.of(date, date.toString()), sensitivityValue);
  }

  /**
   * Adds a single sensitivity to the builder using a tenor and date.
   * <p>
   * If the key already exists, the sensitivity value will be merged.
   * If all the values added are tenor-based, the resulting sensitivity will have the tenors sorted.
   * 
   * @param type  the sensitivity type
   * @param curveName  the curve name
   * @param tenor  the sensitivity tenor
   * @param date  the sensitivity date
   * @param sensitivityValue  the sensitivity value
   * @return this, for chaining
   * @throws IllegalArgumentException if the sensitivity currencies mismatch when merging
   */
  public CurveSensitivitiesBuilder add(
      CurveSensitivitiesType type,
      CurveName curveName,
      Tenor tenor,
      LocalDate date,
      CurrencyAmount sensitivityValue) {

    return add(type, curveName, TenorDateParameterMetadata.of(date, tenor), sensitivityValue);
  }

  /**
   * Adds a single sensitivity to the builder using metadata.
   * <p>
   * If the key already exists, the sensitivity value will be merged.
   * If all the values added are tenor-based, the resulting sensitivity will have the tenors sorted.
   * 
   * @param type  the sensitivity type
   * @param curveName  the curve name
   * @param metadata  the sensitivity metadata
   * @param sensitivityValue  the sensitivity value
   * @return this, for chaining
   * @throws IllegalArgumentException if the sensitivity currencies mismatch when merging
   */
  public CurveSensitivitiesBuilder add(
      CurveSensitivitiesType type,
      CurveName curveName,
      ParameterMetadata metadata,
      CurrencyAmount sensitivityValue) {

    data.computeIfAbsent(type, t -> new TreeMap<>())
        .computeIfAbsent(curveName, t -> new SensitivityMapBuilder(curveName, sensitivityValue.getCurrency()))
        .add(metadata, sensitivityValue);
    return this;
  }

  //-------------------------------------------------------------------------
  /**
   * Builds the sensitivity from the provided data.
   * 
   * @return the sensitivities instance
   */
  public CurveSensitivities build() {
    Map<CurveSensitivitiesType, CurrencyParameterSensitivities> typedSensitivities = new TreeMap<>();
    for (CurveSensitivitiesType type : data.keySet()) {
      CurrencyParameterSensitivities sens = CurrencyParameterSensitivities.of(data.get(type).values().stream()
          .map(SensitivityMapBuilder::toCurrencyParameterSensitivity)
          .collect(toImmutableList()));
      typedSensitivities.put(type, sens);
    }
    return CurveSensitivities.of(info, typedSensitivities);
  }

  //-------------------------------------------------------------------------
  // helper class
  private static class SensitivityMapBuilder {
    private final CurveName curveName;
    private final Currency currency;
    private final Map<ParameterMetadata, Double> sensitivity = new LinkedHashMap<>();

    private SensitivityMapBuilder(CurveName curveName, Currency currency) {
      this.curveName = ArgChecker.notNull(curveName, "curveName");
      this.currency = ArgChecker.notNull(currency, "currency");
    }

    private void add(ParameterMetadata metadata, CurrencyAmount value) {
      if (!currency.equals(value.getCurrency())) {
        throw new IllegalArgumentException(
            "Cannot create sensitivities with different currencies: " + currency + " and " + value.getCurrency());
      }
      this.sensitivity.merge(metadata, value.getAmount(), Double::sum);
    }

    private CurrencyParameterSensitivity toCurrencyParameterSensitivity() {
      ImmutableSet<Class<?>> metadataTypes =
          sensitivity.keySet().stream().map(Object::getClass).collect(toImmutableSet());
      if (metadataTypes.size() == 1) {
        if (TenoredParameterMetadata.class.isAssignableFrom(metadataTypes.iterator().next())) {
          Map<ParameterMetadata, Double> sorted = MapStream.of(sensitivity)
              .sortedKeys(Comparator.comparing(k -> ((TenoredParameterMetadata) k).getTenor()))
              .toMap();
          return CurrencyParameterSensitivity.of(curveName, currency, sorted);
        }
      }
      return CurrencyParameterSensitivity.of(curveName, currency, sensitivity);
    }
  }

}
