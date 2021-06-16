/*
 * Copyright (C) 2021 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.fx;

import java.time.ZonedDateTime;

/**
 * A foreign exchange product that is an option.
 */
public interface FxOptionProduct extends FxProduct {

  public ZonedDateTime getExpiry();

}
