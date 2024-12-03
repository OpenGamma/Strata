/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.fxopt;

import java.io.Serializable;

import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.gen.BeanDefinition;
import org.joda.beans.gen.PropertyDefinition;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.AdjustablePayment;
import com.opengamma.strata.product.PortfolioItemInfo;
import com.opengamma.strata.product.ResolvableTrade;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.fx.FxTrade;

/**
 * A trade in an FX collar.
 * <p>
 */
@BeanDefinition
public class FxCollarTrade implements FxTrade, ResolvableTrade<ResolvedFxCollarTrade>,
    ImmutableBean, Serializable {

  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final TradeInfo info;

  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final FxCollar product;

  @PropertyDefinition(validate = "notNull")
  private final AdjustablePayment premium;

  /**
   * Obtains an instance of an FX collar trade.
   *
   * @param info  the trade info
   * @param product  the product
   * @param premium  the premium
   * @return the trade
   */
  public static FxCollarTrade of(TradeInfo info, FxCollar product, AdjustablePayment premium) {
    return new FxCollarTrade(info, product, premium);
  }

  @Override
  public FxCollarTrade withInfo(PortfolioItemInfo info) {
    return new FxCollarTrade(TradeInfo.from(info), product, premium);
  }

  @Override
  public ResolvedFxCollarTrade resolve(ReferenceData refData) {
    return ResolvedFxCollarTrade.of(info, product.resolve(refData), premium.resolve(refData));
  }

  private FxCollarTrade(
      TradeInfo info,
      FxCollar product,
      AdjustablePayment premium) {
    JodaBeanUtils.notNull(info, "info");
    JodaBeanUtils.notNull(product, "product");
    JodaBeanUtils.notNull(premium, "premium");
    this.info = info;
    this.product = product;
    this.premium = premium;
  }

  /**
   * Gets the additional trade information, defaulted to an empty instance.
   * <p>
   * This allows additional information to be attached to the trade.
   * @return the value of the property, not null
   */
  @Override
  public TradeInfo getInfo() {
    return info;
  }

  /**
   * Gets the resolved FX collar product.
   * <p>
   * The product captures the contracted financial details of the trade.
   * @return the value of the property, not null
   */
  @Override
  public FxCollar getProduct() {
    return product;
  }

  /**
   * Gets the premium of the FX collar.
   * <p>
   * The premium sign should be compatible with the product Long/Short flag.
   * This means that the premium is negative for long and positive for short.
   * @return the value of the property, not null
   */
  public AdjustablePayment getPremium() {
    return premium;
  }

  @Override
  public MetaBean metaBean() {
    return null;
  }
}
