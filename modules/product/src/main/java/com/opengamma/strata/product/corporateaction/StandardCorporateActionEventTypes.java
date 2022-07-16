/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.corporateaction;

enum StandardCorporateActionEventTypes implements CorporateActionEventType {

  CASH_DIVIDEND("Cash Dividend")
  ;

  // name
  private final String name;

  // create
  StandardCorporateActionEventTypes(String name) {
    this.name = name;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    return name;
  }

}
