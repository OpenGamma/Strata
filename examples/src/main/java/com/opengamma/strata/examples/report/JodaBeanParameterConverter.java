/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.examples.report;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

import org.joda.beans.ser.JodaBeanSer;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.ParameterException;
import com.opengamma.strata.collect.Messages;

/**
 * Abstract parameter converter for bean types.
 * 
 * @param <T>  the type of the converter
 */
public abstract class JodaBeanParameterConverter<T>
    implements IStringConverter<T> {

  /**
   * The Joda-Bean type to parse.
   * 
   * @return the type
   */
  protected abstract Class<T> getExpectedType();

  @Override
  public T convert(String fileName) {
    try {
      File f = new File(fileName);
      try (Reader reader = new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8)) {
        return JodaBeanSer.PRETTY.xmlReader().read(reader, getExpectedType());
      }
    } catch (RuntimeException | IOException ex) {
      if (ex instanceof FileNotFoundException || ex.getCause() instanceof FileNotFoundException) {
        throw new ParameterException(Messages.format("File not found: {}", fileName));
      }
      throw new ParameterException(
          Messages.format("Invalid file: {}" +
              System.lineSeparator() + "Exception: {}", fileName, ex.getMessage()));
    }
  }

}
