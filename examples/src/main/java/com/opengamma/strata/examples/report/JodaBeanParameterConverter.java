/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.examples.report;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.joda.beans.ser.JodaBeanSer;

import com.beust.jcommander.IStringConverter;

/**
 * Abstract parameter converter for bean types.
 */
public abstract class JodaBeanParameterConverter<T> implements IStringConverter<T> {

  abstract Class<T> getExpectedType();
  
  @Override
  public T convert(String fileName) {
    File f = new File(fileName);
    try (FileReader reader = new FileReader(f)) {
      return JodaBeanSer.PRETTY.xmlReader().read(reader, getExpectedType());
    } catch (IOException ex) {
      throw new IllegalArgumentException("Error reading from file " + fileName, ex);
    }
  }

}
