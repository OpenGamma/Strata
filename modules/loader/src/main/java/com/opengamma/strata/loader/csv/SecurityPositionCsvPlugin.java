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
import com.opengamma.strata.product.GenericSecurityPosition;
import com.opengamma.strata.product.Position;
import com.opengamma.strata.product.PositionInfo;
import com.opengamma.strata.product.SecurityPosition;

/**
 * Handles the CSV file format for security trades.
 */
final class SecurityPositionCsvPlugin implements PositionCsvParserPlugin {

  /**
   * The singleton instance of the plugin.
   */
  public static final SecurityPositionCsvPlugin INSTANCE = new SecurityPositionCsvPlugin();

  //-------------------------------------------------------------------------
  @Override
  public Set<String> positionTypeNames() {
    return ImmutableSet.of("SEC", "SECURITY");
  }

  @Override
  public Optional<Position> parsePosition(
      Class<?> requiredJavaType,
      CsvRow row,
      PositionInfo info,
      PositionCsvInfoResolver resolver) {

    if (requiredJavaType.isAssignableFrom(GenericSecurityPosition.class)) {
      return Optional.of(resolver.parseNonEtdPosition(row, info));
    }
    if (requiredJavaType.isAssignableFrom(SecurityPosition.class)) {
      return Optional.of(resolver.parseNonEtdSecurityPosition(row, info));
    }
    return Optional.empty();
  }

  @Override
  public String getName() {
    return "Security";
  }

}
