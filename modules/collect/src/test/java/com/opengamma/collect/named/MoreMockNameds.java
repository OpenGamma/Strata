/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.collect.named;

/**
 * Mock named object.
 */
class MoreMockNameds implements MockNamed {

  public static final MoreMockNameds MORE = new MoreMockNameds();
  public static final String TEXT = "Not a constant";
  static final MoreMockNameds NOT_PUBLIC = null;
  public final MoreMockNameds NOT_STATIC = null;
  public static MoreMockNameds NOT_FINAL = null;

  @Override
  public String getName() {
    return "More";
  }

}
