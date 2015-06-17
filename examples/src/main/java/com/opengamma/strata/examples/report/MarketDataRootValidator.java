/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.examples.report;

import java.io.File;

import com.beust.jcommander.IValueValidator;
import com.beust.jcommander.ParameterException;
import com.opengamma.strata.collect.Messages;

/**
 * Value validator for the market data root directory.
 */
public class MarketDataRootValidator implements IValueValidator<File> {

  @Override
  public void validate(String name, File value) throws ParameterException {
    if (!value.exists()) {
      throw new ParameterException(
          Messages.format("Invalid market data root directory: {}", value.getAbsolutePath()));
    }
    if (!value.isDirectory()) {
      throw new ParameterException(
          Messages.format("Market data root must be a directory: {}", value.getAbsolutePath()));
    }
  }

}
