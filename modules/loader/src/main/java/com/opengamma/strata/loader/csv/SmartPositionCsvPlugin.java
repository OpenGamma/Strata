/*
 * Copyright (C) 2021 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.csv;

import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.collect.io.CsvRow;
import com.opengamma.strata.product.Position;
import com.opengamma.strata.product.PositionInfo;
import com.opengamma.strata.product.SecurityPosition;

/**
 * Handles the CSV file format for security trades interpretted from the data.
 */
final class SmartPositionCsvPlugin implements PositionCsvParserPlugin {

  /**
   * The singleton instance of the plugin.
   */
  public static final SmartPositionCsvPlugin INSTANCE = new SmartPositionCsvPlugin();

  //-------------------------------------------------------------------------
  @Override
  public Set<String> positionTypeNames() {
    return ImmutableSet.of("SMART");
  }

  @Override
  public Optional<Position> parsePosition(
      Class<?> requiredJavaType,
      CsvRow row,
      PositionInfo info,
      PositionCsvInfoResolver resolver) {

    if (requiredJavaType == SecurityPosition.class) {
      return Optional.of(SecurityTradeCsvPlugin.parsePositionLightweight(row, info, resolver));
    } else {
      return Optional.of(SecurityTradeCsvPlugin.parsePosition(row, info, resolver));
    }
  }

  @Override
  public String getName() {
    return "Smart";
  }

}
