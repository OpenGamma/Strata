package com.opengamma.strata.product.fxopt;

import com.opengamma.strata.product.ResolvedProduct;
import java.io.Serializable;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.gen.BeanDefinition;
import org.joda.beans.gen.PropertyDefinition;

@BeanDefinition(builderScope = "private")
public class ResolvedFxCollar  implements ResolvedProduct, ImmutableBean, Serializable {


  @PropertyDefinition(validate = "notNull")
  private final ResolvedFxVanillaOption option1;

  @PropertyDefinition(validate = "notNull")
  private final ResolvedFxVanillaOption option2;

  public static ResolvedFxCollar of(ResolvedFxVanillaOption option1, ResolvedFxVanillaOption option2) {
    return new ResolvedFxCollar(option1, option2);
  }

  private ResolvedFxCollar(
      ResolvedFxVanillaOption option1,
      ResolvedFxVanillaOption option2) {
    JodaBeanUtils.notNull(option1, "option1");
    JodaBeanUtils.notNull(option2, "option2");
    this.option1 = option1;
    this.option2 = option2;
  }
  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  public ResolvedFxVanillaOption getOption1() {
    return option1;
  }
  public ResolvedFxVanillaOption getOption2() { return option2; }

  @Override
  public MetaBean metaBean() {
    return null;
  }
}
