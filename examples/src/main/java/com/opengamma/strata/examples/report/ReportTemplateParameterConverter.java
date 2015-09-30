/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.examples.report;

import java.io.File;
import java.io.FileNotFoundException;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.ParameterException;
import com.google.common.io.CharSource;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.io.IniFile;
import com.opengamma.strata.collect.io.ResourceLocator;
import com.opengamma.strata.report.ReportTemplate;

/**
 * Parameter converter for {@link ReportTemplate}.
 */
public class ReportTemplateParameterConverter
    implements IStringConverter<ReportTemplate> {

  @Override
  public ReportTemplate convert(String fileName) {
    try {
      File file = new File(fileName);
      CharSource charSource = ResourceLocator.ofFile(file).getCharSource();
      IniFile ini = IniFile.of(charSource);

      return ReportTemplate.load(ini);

    } catch (RuntimeException ex) {
      if (ex.getCause() instanceof FileNotFoundException) {
        throw new ParameterException(Messages.format("File not found: {}", fileName));
      }
      throw new ParameterException(
          Messages.format("Invalid report template file: {}" +
              System.lineSeparator() + "Exception: {}", fileName, ex.getMessage()));
    }
  }

}
