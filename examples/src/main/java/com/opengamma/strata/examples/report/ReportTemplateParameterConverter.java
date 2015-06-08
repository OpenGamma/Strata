/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.examples.report;

import java.io.File;

import com.beust.jcommander.IStringConverter;
import com.google.common.io.CharSource;
import com.opengamma.strata.collect.io.IniFile;
import com.opengamma.strata.collect.io.ResourceLocator;
import com.opengamma.strata.report.ReportTemplate;
import com.opengamma.strata.report.trade.TradeReportTemplateIniLoader;

/**
 * Parameter converter for {@link ReportTemplate}.
 */
public class ReportTemplateParameterConverter implements IStringConverter<ReportTemplate> {

  @Override
  public ReportTemplate convert(String fileName) {
    File file = new File(fileName);
    CharSource charSource = ResourceLocator.ofFile(file).getCharSource();
    IniFile ini = IniFile.of(charSource);

    // TODO - support for other report template types
    TradeReportTemplateIniLoader loader = new TradeReportTemplateIniLoader();
    return loader.load(ini);
  }

}
