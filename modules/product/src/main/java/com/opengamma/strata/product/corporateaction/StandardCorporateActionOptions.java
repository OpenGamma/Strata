/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.corporateaction;

import org.joda.convert.ToString;

enum StandardCorporateActionOptions implements CorporateActionOption {

  CASH("Cash")

  ;

  private final String name;

  StandardCorporateActionOptions(String name) {
    this.name = name;
  }

  @ToString
  @Override
  public String getName() {
    return name;
  }

}
