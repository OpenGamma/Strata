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
import com.opengamma.strata.product.etd.EtdFuturePosition;

/**
 * Handles the CSV file format for ETD future trades.
 */
final class EtdFuturePositionCsvPlugin implements PositionCsvParserPlugin {

  /**
   * The singleton instance of the plugin.
   */
  public static final EtdFuturePositionCsvPlugin INSTANCE = new EtdFuturePositionCsvPlugin();

  //-------------------------------------------------------------------------
  @Override
  public Set<String> positionTypeNames() {
    return ImmutableSet.of("FUT", "FUTURE");
  }

  @Override
  public Optional<Position> parsePosition(
      Class<?> requiredJavaType,
      CsvRow row,
      PositionInfo info,
      PositionCsvInfoResolver resolver) {

    if (requiredJavaType.isAssignableFrom(EtdFuturePosition.class)) {
      return Optional.of(resolver.parseEtdFuturePosition(row, info));
    }
    if (requiredJavaType.isAssignableFrom(SecurityPosition.class)) {
      return Optional.of(resolver.parseEtdFutureSecurityPosition(row, info));
    }
    return Optional.empty();
  }

  @Override
  public String getName() {
    return "EtdFuture";
  }

}
