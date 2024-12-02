/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.fxopt;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.product.ResolvedProduct;
import java.io.Serializable;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.gen.BeanDefinition;
import org.joda.beans.gen.PropertyDefinition;

/**
 * An FX Collar, resolved for pricing.
 * <p>
 * This is the resolved form of {@link FxCollar} and is an input to the pricers.
 * Applications will typically create a {@code ResolvedFxCollar} from a {@code FxCollar}
 * using {@link FxCollar#resolve(ReferenceData)}.
 * <p>
 * A {@code ResolvedFxCollar} is bound to data that changes over time, such as holiday calendars.
 * If the data changes, such as the addition of a new holiday, the resolved form will not be updated.
 * Care must be taken when placing the resolved form in a cache or persistence layer.
 */
@BeanDefinition(builderScope = "private")
public class ResolvedFxCollar  implements ResolvedProduct, ImmutableBean, Serializable {

  @PropertyDefinition(validate = "notNull")
  private final ResolvedFxVanillaOption option1;

  @PropertyDefinition(validate = "notNull")
  private final ResolvedFxVanillaOption option2;

  /**
   * Creates a {@code ResolvedFxCollar} from two legs.
   * <p>
   * The transactions must be passed in with payment dates in the correct order.
   * The currency pair of each leg must match and have amounts flowing in opposite directions.
   *
   * @param option1  the earlier leg
   * @param option2  the later leg
   * @return the resolved FX collar
   */
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

  /**
   * Gets the foreign option.
   * <p>
   * This provides details of a single foreign exchange.
   * @return the value of the property, not null
   */
  public ResolvedFxVanillaOption getOption1() {
    return option1;
  }

  /**
   * Gets the foreign option.
   * <p>
   * This provides details of a single foreign exchange.
   * @return the value of the property, not null
   */
  public ResolvedFxVanillaOption getOption2() {
    return option2;
  }

  @Override
  public MetaBean metaBean() {
    return null;
  }
}
