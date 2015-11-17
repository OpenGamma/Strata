/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.report.framework.format;

import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.market.curve.CurveCurrencyParameterSensitivity;

/**
 * Provides and caches format settings across types.
 */
public class FormatSettingsProvider {
  // TODO extensibility - perhaps drive from properties file

  /**
   * The default instance.
   */
  public static final FormatSettingsProvider INSTANCE = new FormatSettingsProvider();

  /**
   * The map of settings by type.
   */
  private static final Map<Class<?>, FormatSettings<?>> TYPE_SETTINGS =
      ImmutableMap
          .<Class<?>, FormatSettings<?>>builder()
          .put(String.class, FormatSettings.of(FormatCategory.TEXT, ValueFormatters.TO_STRING))
          .put(Currency.class, FormatSettings.of(FormatCategory.TEXT, ValueFormatters.TO_STRING))
          .put(StandardId.class, FormatSettings.of(FormatCategory.TEXT, ValueFormatters.TO_STRING))
          .put(LocalDate.class, FormatSettings.of(FormatCategory.DATE, ValueFormatters.TO_STRING))
          .put(CurrencyAmount.class, FormatSettings.of(FormatCategory.NUMERIC, ValueFormatters.CURRENCY_AMOUNT))
          .put(CurveCurrencyParameterSensitivity.class,
              FormatSettings.of(FormatCategory.TEXT, ValueFormatters.CURVE_CURRENCY_PARAMETER_SENSITIVITY))
          .put(Double.class, FormatSettings.of(FormatCategory.NUMERIC, ValueFormatters.DOUBLE))
          .put(Short.class, FormatSettings.of(FormatCategory.NUMERIC, ValueFormatters.TO_STRING))
          .put(Integer.class, FormatSettings.of(FormatCategory.NUMERIC, ValueFormatters.TO_STRING))
          .put(Long.class, FormatSettings.of(FormatCategory.NUMERIC, ValueFormatters.TO_STRING))
          .put(double[].class, FormatSettings.of(FormatCategory.NUMERIC, ValueFormatters.DOUBLE_ARRAY))
          .build();

  /**
   * The settings cache.
   */
  private final Map<Class<?>, FormatSettings<?>> settingsCache = new ConcurrentHashMap<>();

  /**
   * Creates an instance.
   */
  protected FormatSettingsProvider() {
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains the format settings for a given type.
   * 
   * @param clazz  the type to format
   * @param defaultSettings  the default settings, used if no settings are found for the type
   * @return the format settings
   */
  @SuppressWarnings("unchecked")
  public <T> FormatSettings<T> settings(Class<? extends T> clazz, FormatSettings<Object> defaultSettings) {
    FormatSettings<T> settings = (FormatSettings<T>) settingsCache.get(clazz);

    if (settings == null) {
      settings = (FormatSettings<T>) TYPE_SETTINGS.get(clazz);
      if (settings == null) {
        settings = (FormatSettings<T>) defaultSettings;
      }
      settingsCache.put(clazz, settings);
    }
    return settings;
  }

}
