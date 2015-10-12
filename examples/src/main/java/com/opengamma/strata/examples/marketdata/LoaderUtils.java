/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.examples.marketdata;

import java.io.FileInputStream;

import org.joda.beans.Bean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.ser.JodaBeanSer;

import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.index.OvernightIndex;
import com.opengamma.strata.basics.index.RateIndex;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.Unchecked;
import com.opengamma.strata.math.impl.interpolation.FlatExtrapolator1D;
import com.opengamma.strata.math.impl.interpolation.LinearInterpolator1D;

/**
 * Contains utilities for loading market data from input files.
 */
public final class LoaderUtils {
  
  static {
    JodaBeanUtils.registerMetaBean(LinearInterpolator1D.meta());
    JodaBeanUtils.registerMetaBean(FlatExtrapolator1D.meta());
  }

  /**
   * Restricted constructor
   */
  private LoaderUtils() {
  }

  //-------------------------------------------------------------------------
  /**
   * Attempts to locate a rate index by reference name.
   * <p>
   * This utility searches both {@link IborIndex} and {@link OvernightIndex}.
   * 
   * @param reference  the reference name
   * @return the resolved rate index
   */
  public static RateIndex findIndex(String reference) {
    if (IborIndex.extendedEnum().lookupAll().containsKey(reference)) {
      return IborIndex.of(reference);
    }
    if (OvernightIndex.extendedEnum().lookupAll().containsKey(reference)) {
      return OvernightIndex.of(reference);
    }
    throw new IllegalArgumentException(Messages.format("No index found for reference: {}", reference));
  }

  /**
   * Load a bean in XML format from a file.
   * 
   * @param fileName  the file name
   * @param type  the bean type
   * @return the bean
   */
  public static <T extends Bean> T loadXmlBean(String fileName, Class<T> type) {
    return Unchecked.wrap(() -> JodaBeanSer.PRETTY.xmlReader().read(new FileInputStream(fileName), type));
  }

}
