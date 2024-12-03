/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
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
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.product.ResolvedTrade;
import com.opengamma.strata.product.TradeInfo;

/**
 * A trade in an FX collar, resolved for pricing.
 * <p>
 * This is the resolved form of {@link FxCollarTrade} and is the primary input to the pricers.
 * Applications will typically create a {@code ResolvedFxCollarTrade} from a {@code FxCollarTrade}
 * using {@link FxCollarTrade#resolve(ReferenceData)}.
 * <p>
 * A {@code ResolvedFxCollarTrade} is bound to data that changes over time, such as holiday calendars.
 * If the data changes, such as the addition of a new holiday, the resolved form will not be updated.
 * Care must be taken when placing the resolved form in a cache or persistence layer.
 */
@BeanDefinition
public class ResolvedFxCollarTrade  implements ResolvedTrade, ImmutableBean, Serializable {

  /**
   * The additional trade information, defaulted to an empty instance.
   * <p>
   * This allows additional information to be attached to the trade.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final TradeInfo info;

  /**
   * The resolved FX collar product.
   * <p>
   * The product captures the contracted financial details of the trade.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final ResolvedFxCollar product;

  /**
   * The premium of the FX collar.
   * <p>
   * The premium sign should be compatible with the product Long/Short flag.
   * This means that the premium is negative for long and positive for short.
   */
  @PropertyDefinition(validate = "notNull")
  private final Payment premium;

  /**
   * Obtains an instance of a resolved FX collar trade.
   *
   * @param info  the trade info
   * @param product  the product
   * @param premium  the premium
   * @return the resolved trade
   */
  public static ResolvedFxCollarTrade of(TradeInfo info, ResolvedFxCollar product, Payment premium) {
    return new ResolvedFxCollarTrade(info, product, premium);
  }
  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private ResolvedFxCollarTrade(
      TradeInfo info,
      ResolvedFxCollar product,
      Payment premium) {
    JodaBeanUtils.notNull(info, "info");
    JodaBeanUtils.notNull(product, "product");
    JodaBeanUtils.notNull(premium, "premium");
    this.info = info;
    this.product = product;
    this.premium = premium;
  }

  @Override
  public TradeInfo getInfo() {
    return info;
  }

  @Override
  public ResolvedFxCollar getProduct() {
    return product;
  }

  /**
   * Gets the premium of the FX collar.
   * <p>
   * The premium sign should be compatible with the product Long/Short flag.
   * This means that the premium is negative for long and positive for short.
   * @return the value of the property, not null
   */
  public Payment getPremium() {
    return premium;
  }

  @Override
  public MetaBean metaBean() {
    return null;
  }
}
