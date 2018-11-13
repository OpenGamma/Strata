/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.csv;

import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.util.List;

import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.MapStream;
import com.opengamma.strata.collect.io.CsvOutput;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.market.param.CurrencyParameterSensitivity;
import com.opengamma.strata.market.param.DatedParameterMetadata;
import com.opengamma.strata.market.param.ParameterMetadata;
import com.opengamma.strata.market.param.TenoredParameterMetadata;
import com.opengamma.strata.market.sensitivity.CurveSensitivities;
import com.opengamma.strata.market.sensitivity.CurveSensitivitiesType;

/**
 * Writes sensitivities to a CSV file.
 * <p>
 * This takes a Strata {@link CurveSensitivities} instance and creates a matching CSV file.
 * The output is written in standard format, with no identifier columns.
 * The parameter metadata must contain tenors.
 */
public final class SensitivityCsvWriter {

  /**
   * The supplier, providing additional information.
   */
  private final SensitivityCsvInfoSupplier supplier;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance that uses the standard set of reference data.
   * 
   * @return the loader
   */
  public static SensitivityCsvWriter standard() {
    return new SensitivityCsvWriter(SensitivityCsvInfoSupplier.standard());
  }

  /**
   * Obtains an instance that uses the specified supplier for additional information.
   * 
   * @param supplier  the supplier used to extract additional information to output
   * @return the loader
   */
  public static SensitivityCsvWriter of(SensitivityCsvInfoSupplier supplier) {
    return new SensitivityCsvWriter(supplier);
  }

  // restricted constructor
  private SensitivityCsvWriter(SensitivityCsvInfoSupplier supplier) {
    this.supplier = ArgChecker.notNull(supplier, "supplier");
  }

  //-------------------------------------------------------------------------
  /**
   * Write sensitivities to an appendable in the standard sensitivities format.
   * <p>
   * The output is written in standard format, with no identifier columns.
   * The parameter metadata must contain tenors.
   *
   * @param curveSens  the curve sensitivities to write
   * @param output  the appendable to write to
   * @throws IllegalArgumentException if the metadata does not contain tenors
   * @throws UncheckedIOException if an IO error occurs
   */
  public void write(CurveSensitivities curveSens, Appendable output) {
    CsvOutput csv = CsvOutput.standard(output, "\n");
    List<String> additionalHeaders = supplier.headers(curveSens);

    // check for dates
    if (curveSens.getTypedSensitivities().values().stream()
        .flatMap(allParamSens -> allParamSens.getSensitivities().stream())
        .flatMap(paramSens -> paramSens.getParameterMetadata().stream())
        .anyMatch(pmd -> !(pmd instanceof TenoredParameterMetadata))) {
      throw new IllegalArgumentException("Parameter metadata must contain tenors");
    }
    boolean containsDates = curveSens.getTypedSensitivities().values().stream()
        .flatMap(allParamSens -> allParamSens.getSensitivities().stream())
        .flatMap(paramSens -> paramSens.getParameterMetadata().stream())
        .anyMatch(pmd -> pmd instanceof DatedParameterMetadata);

    // headers
    csv.writeCell(SensitivityCsvLoader.REFERENCE_HEADER);
    csv.writeCell(SensitivityCsvLoader.TYPE_HEADER);
    csv.writeCell(SensitivityCsvLoader.TENOR_HEADER);
    if (containsDates) {
      csv.writeCell(SensitivityCsvLoader.DATE_HEADER);
    }
    csv.writeCell(SensitivityCsvLoader.CURRENCY_HEADER);
    csv.writeCell(SensitivityCsvLoader.VALUE_HEADER);
    csv.writeLine(additionalHeaders);

    // content, grouped by reference, then type
    MapStream.of(curveSens.getTypedSensitivities())
        .flatMapValues(sens -> sens.getSensitivities().stream())
        .mapKeys((type, sens) -> Pair.of(sens.getMarketDataName().getName(), type))
        .sortedKeys()
        .forEach((pair, paramSens) -> write(
            pair.getFirst(), pair.getSecond(), curveSens, paramSens, additionalHeaders, containsDates, csv));
  }

  // writes the rows for a single CurrencyParameterSensitivity
  private void write(
      String reference,
      CurveSensitivitiesType type,
      CurveSensitivities curveSens,
      CurrencyParameterSensitivity paramSens,
      List<String> additionalHeaders,
      boolean containsDates,
      CsvOutput csv) {

    List<String> additionalCells = supplier.values(additionalHeaders, curveSens, paramSens);
    for (int i = 0; i < paramSens.getParameterCount(); i++) {
      ParameterMetadata pmd = paramSens.getParameterMetadata(i);
      Tenor tenor = ((TenoredParameterMetadata) pmd).getTenor();
      double value = paramSens.getSensitivity().get(i);
      csv.writeCell(reference);
      csv.writeCell(type.getName());
      csv.writeCell(tenor.toString());
      if (containsDates) {
        csv.writeCell(pmd instanceof DatedParameterMetadata ? ((DatedParameterMetadata) pmd).getDate().toString() : "");
      }
      csv.writeCell(paramSens.getCurrency().getCode());
      csv.writeCell(BigDecimal.valueOf(value).toPlainString());
      csv.writeLine(additionalCells);
    }
  }

}
