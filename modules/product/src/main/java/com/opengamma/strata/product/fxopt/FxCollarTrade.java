package com.opengamma.strata.product.fxopt;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.AdjustablePayment;
import com.opengamma.strata.product.PortfolioItemInfo;
import com.opengamma.strata.product.ResolvableTrade;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.fx.FxTrade;
import java.io.Serializable;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.gen.BeanDefinition;
import org.joda.beans.gen.PropertyDefinition;

@BeanDefinition
public class FxCollarTrade implements FxTrade, ResolvableTrade<ResolvedFxCollarTrade>,
    ImmutableBean, Serializable {

  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final TradeInfo info;

  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final FxCollar product;

  @PropertyDefinition(validate = "notNull")
  private final AdjustablePayment premium;


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

  @Override
  public TradeInfo getInfo() {
    return info;
  }

  @Override
  public FxCollar getProduct() {
    return product;
  }

  public AdjustablePayment getPremium() {
    return premium;
  }

  @Override
  public MetaBean metaBean() {
    return null;
  }
}
