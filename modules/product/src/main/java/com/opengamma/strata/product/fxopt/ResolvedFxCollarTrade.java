package com.opengamma.strata.product.fxopt;

import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.product.ResolvedTrade;
import com.opengamma.strata.product.TradeInfo;
import java.io.Serializable;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.gen.BeanDefinition;
import org.joda.beans.gen.PropertyDefinition;

@BeanDefinition
public class ResolvedFxCollarTrade  implements ResolvedTrade, ImmutableBean, Serializable {

  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final TradeInfo info;

  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final ResolvedFxCollar product;

  @PropertyDefinition(validate = "notNull")
  private final Payment premium;

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

  public Payment getPremium() {
    return premium;
  }

  @Override
  public MetaBean metaBean() {
    return null;
  }
}
