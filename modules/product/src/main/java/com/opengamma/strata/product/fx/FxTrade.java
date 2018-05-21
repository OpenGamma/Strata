/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.fx;

import com.opengamma.strata.product.ProductTrade;
import com.opengamma.strata.product.TradeInfo;

/**
 * A foreign exchange trade, such as an FX forward, FX spot or FX option.
 * <p>
 * FX trades operate on two different currencies.
 * For example, it might represent the payment of USD 1,000 and the receipt of EUR 932.
 */
public interface FxTrade extends ProductTrade {

  @Override
  public abstract FxTrade withInfo(TradeInfo info);

  @Override
  public abstract FxProduct getProduct();

}
