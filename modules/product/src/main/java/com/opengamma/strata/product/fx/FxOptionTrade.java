/*
 * Copyright (C) 2021 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.fx;

/**
 * A foreign exchange option trade such as a FxVanillaOptionTrade.
 */
public interface FxOptionTrade extends FxTrade {

  @Override
  public abstract FxOptionProduct getProduct();

}
