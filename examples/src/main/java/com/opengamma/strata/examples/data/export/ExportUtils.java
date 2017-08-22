/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.examples.data.export;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.Unchecked;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.param.CurrencyParameterSensitivity;
import com.opengamma.strata.market.param.ParameterMetadata;
import com.opengamma.strata.pricer.swaption.SwaptionSurfaceExpiryTenorParameterMetadata;

/**
 * Utilities to export objects (in csv files or in the console). Typically used in the tutorials.
 */
public class ExportUtils {

  /**
   * Exports a {@link MultiCurrencyAmount} to a csv file.
   * 
   * @param multiCurrencyAmount  the amount
   * @param fileName  the file name
   */
  public static void export(MultiCurrencyAmount multiCurrencyAmount, String fileName) {
    StringBuilder builder = new StringBuilder();
    for (CurrencyAmount ca : multiCurrencyAmount.getAmounts()) {
      builder.append(ca.getCurrency().toString()).append(',').append(ca.getAmount()).append(',');
    }
    export(builder.toString(), fileName);
  }

  /**
   * Exports into a csv file a {@link CurrencyParameterSensitivity}, which is the sensitivity with respect to
   * a unique curve or surface. 
   * <p>
   * In the export the figures are often scaled to match market conventions, for examples a one basis point 
   * scaling for interest rate curves. The factor can be provided and will apply to all points of the sensitivity.
   * 
   * @param sensitivity  the sensitivity object
   * @param scale  the scaling factor
   * @param fileName  the file name for the export
   */
  public static void export(
      CurrencyParameterSensitivity sensitivity,
      double scale,
      String fileName) {

    ArgChecker.isTrue(sensitivity.getParameterMetadata().size() > 0, "Parameter metadata must be present");
    DoubleArray s = sensitivity.getSensitivity();
    List<ParameterMetadata> pmdl = sensitivity.getParameterMetadata();
    int nbPts = sensitivity.getSensitivity().size();
    String output = "Expiry, Tenor, Label, Value\n";
    for (int looppts = 0; looppts < nbPts; looppts++) {
      ArgChecker.isTrue(pmdl.get(looppts) instanceof SwaptionSurfaceExpiryTenorParameterMetadata, "tenor expiry");
      SwaptionSurfaceExpiryTenorParameterMetadata pmd = (SwaptionSurfaceExpiryTenorParameterMetadata) pmdl.get(looppts);
      double sens = s.get(looppts) * scale;
      output = output + pmd.getYearFraction() + ", " + pmd.getTenor() + ", " + pmd.getLabel() + ", " + sens + "\n";
    }
    export(output, fileName);
  }

  /**
   * Exports into a csv file a {@link CurrencyParameterSensitivities}, which is the sensitivity with respect to
   * multiple curves or surfaces. 
   * <p>
   * In the export the figures are often scaled to match market conventions, for examples a one basis point 
   * scaling for interest rate curves. The factor can be provided and will apply to all points of the sensitivity.
   * 
   * @param sensitivity  the sensitivity object
   * @param scale  the scaling factor
   * @param fileName  the file name for the export
   */
  public static void export(
      CurrencyParameterSensitivities sensitivity,
      double scale,
      String fileName) {

    ImmutableList<CurrencyParameterSensitivity> sl = sensitivity.getSensitivities();
    String output = "Label, Value\n";
    for (CurrencyParameterSensitivity s : sl) {
      output = output + s.getMarketDataName().toString() + ", " + s.getCurrency().toString() + "\n";
      ArgChecker.isTrue(s.getParameterMetadata().size() > 0, "Parameters metadata required");
      DoubleArray sa = s.getSensitivity();
      List<ParameterMetadata> pmd = s.getParameterMetadata();
      for (int loopnode = 0; loopnode < sa.size(); loopnode++) {
        output = output + pmd.get(loopnode).getLabel() + ", " + (sa.get(loopnode) * scale) + "\n";
      }
    }
    export(output, fileName);
  }

  /**
   * Exports a string to a file. Useful in particular for XML and beans.
   * 
   * @param string  the string to export
   * @param fileName  the name of the file
   */
  public static void export(String string, String fileName) {
    File file = new File(fileName);
    Unchecked.wrap(() -> Files.createParentDirs(file));
    Unchecked.wrap(() -> Files.asCharSink(file, StandardCharsets.UTF_8).write(string));
  }

}
