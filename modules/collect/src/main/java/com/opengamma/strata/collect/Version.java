/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect;

import com.opengamma.strata.collect.io.PropertiesFile;
import com.opengamma.strata.collect.io.ResourceLocator;

/**
 * Provides access to the version of Strata.
 */
public final class Version {

  /**
   * The version, which will be populated by the Maven build.
   */
  private static final String VERSION = PropertiesFile.of(
      ResourceLocator.ofClasspath(Version.class, "version.properties").getCharSource()).getProperties().value("version");

  /**
   * Restricted constructor.
   */
  private Version() {
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the version of Strata.
   * 
   * @return the version
   */
  public static String getVersionString() {
    return VERSION;
  }

}
