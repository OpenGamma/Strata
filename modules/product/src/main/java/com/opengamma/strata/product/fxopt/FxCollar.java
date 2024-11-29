package com.opengamma.strata.product.fxopt;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.Resolvable;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.product.fx.FxProduct;
import java.io.Serializable;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.gen.BeanDefinition;
import org.joda.beans.gen.PropertyDefinition;

@BeanDefinition(builderScope = "private")
public class FxCollar implements FxProduct, Resolvable<ResolvedFxCollar>, ImmutableBean,
    Serializable {

  @PropertyDefinition(validate = "notNull")
  private final FxVanillaOption option1;

  @PropertyDefinition(validate = "notNull")
  private final FxVanillaOption option2;

  public static FxCollar of(FxVanillaOption option1, FxVanillaOption option2) {
    return new FxCollar(option1, option2);
  }

  @Override
  public ResolvedFxCollar resolve(ReferenceData refData) {
    return ResolvedFxCollar.of(option1.resolve(refData) , option2.resolve(refData));
  }
  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private FxCollar(
      FxVanillaOption option1,
      FxVanillaOption option2) {
    JodaBeanUtils.notNull(option1, "option1");
    JodaBeanUtils.notNull(option2, "option2");
    this.option1 = option1;
    this.option2 = option2;
  }

  public FxVanillaOption getOption1() {
    return option1;
  }
  public FxVanillaOption getOption2() {
    return option2;
  }

  @Override
  public CurrencyPair getCurrencyPair() {
    return null;
  }

  @Override
  public MetaBean metaBean() {
    return null;
  }
}
