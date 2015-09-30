/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.report.framework.format;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.market.sensitivity.CurveCurrencyParameterSensitivity;

/**
 * Provides and caches format settings across types.
 */
public class FormatSettingsProvider {

  // TODO extensibility - perhaps drive from properties file
  private static final Map<Class<?>, FormatSettings<?>> TYPE_SETTINGS =
      ImmutableMap.<Class<?>, FormatSettings<?>>builder()
          .put(String.class, FormatSettings.of(FormatCategory.TEXT, ValueFormatter.defaultToString()))
          .put(Currency.class, FormatSettings.of(FormatCategory.TEXT, ValueFormatter.defaultToString()))
          .put(StandardId.class, FormatSettings.of(FormatCategory.TEXT, ValueFormatter.defaultToString()))
          .put(LocalDate.class, FormatSettings.of(FormatCategory.DATE, ValueFormatter.defaultToString()))
          .put(CurrencyAmount.class, FormatSettings.of(FormatCategory.NUMERIC, CurrencyAmountValueFormatter.INSTANCE))
          .put(CurveCurrencyParameterSensitivity.class, FormatSettings.of(FormatCategory.TEXT, CurveCurrencyParameterSensitivityValueFormatter.INSTANCE))
          .put(Double.class, FormatSettings.of(FormatCategory.NUMERIC, new DoubleValueFormatter()))
          .put(Short.class, FormatSettings.of(FormatCategory.NUMERIC, ValueFormatter.defaultToString()))
          .put(Integer.class, FormatSettings.of(FormatCategory.NUMERIC, ValueFormatter.defaultToString()))
          .put(Long.class, FormatSettings.of(FormatCategory.NUMERIC, ValueFormatter.defaultToString()))
          .put(double[].class, FormatSettings.of(FormatCategory.NUMERIC, DoubleArrayValueFormatter.INSTANCE))
          .build();

  private final Map<Class<?>, FormatSettings<?>> settingsCache = new HashMap<>();

  /**
   * Obtains the format settings for a given type.
   * 
   * @param clazz  the type to format
   * @param defaultSettings  the default settings, used if no settings are found for the type
   * @return the format settings
   */
  @SuppressWarnings("unchecked")
  public <T> FormatSettings<T> getSettings(Class<? extends T> clazz, FormatSettings<Object> defaultSettings) {
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
