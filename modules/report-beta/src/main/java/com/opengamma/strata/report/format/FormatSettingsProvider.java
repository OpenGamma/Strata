/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.report.format;

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
  private static final Map<Class<?>, FormatSettings> TYPE_SETTINGS = ImmutableMap.<Class<?>, FormatSettings>builder()
      .put(String.class, FormatSettings.of(FormatCategory.TEXT, ValueFormatter.defaultToString()))
      .put(Currency.class, FormatSettings.of(FormatCategory.TEXT, ValueFormatter.defaultToString()))
      .put(StandardId.class, FormatSettings.of(FormatCategory.TEXT, ValueFormatter.defaultToString()))
      .put(LocalDate.class, FormatSettings.of(FormatCategory.DATE, ValueFormatter.defaultToString()))
      .put(CurrencyAmount.class, FormatSettings.of(FormatCategory.NUMERIC, new CurrencyAmountValueFormatter()))
      .put(CurveCurrencyParameterSensitivity.class, FormatSettings.of(FormatCategory.TEXT, new CurveCurrencyParameterSensitivityValueFormatter()))
      .put(Double.class, FormatSettings.of(FormatCategory.NUMERIC, new DoubleValueFormatter()))
      .put(Integer.class, FormatSettings.of(FormatCategory.NUMERIC, ValueFormatter.defaultToString()))
      .build();

  private final Map<Class<?>, FormatSettings> settingsCache = new HashMap<Class<?>, FormatSettings>();

  /**
   * Obtains the format settings for a given type.
   * 
   * @param clazz  the type to format
   * @return the format settings
   */
  public FormatSettings getSettings(Class<?> clazz, FormatSettings fallbackSettings) {
    FormatSettings settings = settingsCache.get(clazz);
    if (settings == null) {
      settings = TYPE_SETTINGS.get(clazz);
      if (settings == null) {
        settings = fallbackSettings;
      }
      settingsCache.put(clazz, settings);
    }
    return settings;
  }

}
