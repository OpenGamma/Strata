/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap;

import java.io.Serializable;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutableValidator;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.index.FxIndex;
import com.opengamma.strata.basics.index.FxIndexObservation;
import com.opengamma.strata.collect.Messages;

/**
 * An FX rate conversion for the notional amount of a swap leg.
 * <p>
 * Interest rate swaps are based on a notional amount of money.
 * The notional can be specified in a currency other than that of the swap leg,
 * with an FX conversion applied at each payment period boundary.
 * <p>
 * The two currencies involved are the swap leg currency and the reference currency.
 * The swap leg currency is, in most cases, the currency that payment will occur in.
 * The reference currency is the currency in which the notional is actually defined.
 * ISDA refers to the payment currency as the <i>variable currency</i> and the reference
 * currency as the <i>constant currency</i>.
 * <p>
 * Defined by the 2006 ISDA definitions article 10.
 */
@BeanDefinition(builderScope = "private")
public final class FxReset
    implements ImmutableBean, Serializable {

  /**
   * The FX index observation.
   * <p>
   * This defines the observation of the index used to obtain the FX reset rate.
   * <p>
   * An FX index is a daily rate of exchange between two currencies.
   * Note that the order of the currencies in the index does not matter, as the
   * conversion direction is fully defined by the currency of the reference amount.
   */
  @PropertyDefinition(validate = "notNull")
  private final FxIndexObservation observation;
  /**
   * The currency of the notional amount defined in the contract.
   * <p>
   * This is the currency of notional amount as defined in the contract.
   * The amount will be converted from this reference currency to the swap leg currency
   * when calculating the value of the leg.
   * <p>
   * The reference currency must be one of the two currencies of the index.
   * <p>
   * The reference currency is also known as the <i>constant currency</i>.
   */
  @PropertyDefinition(validate = "notNull")
  private final Currency referenceCurrency;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the observation and reference currency.
   * 
   * @param observation  the FX index observation
   * @param referenceCurrency  the reference currency
   * @return the FX reset
   * @throws IllegalArgumentException if the currency is not one of those in the index
   */
  public static FxReset of(FxIndexObservation observation, Currency referenceCurrency) {
    return new FxReset(observation, referenceCurrency);
  }

  //-------------------------------------------------------------------------
  @ImmutableValidator
  private void validate() {
    FxIndex index = observation.getIndex();
    if (!index.getCurrencyPair().contains(referenceCurrency)) {
      throw new IllegalArgumentException(
          Messages.format("Reference currency {} must be one of those in the FxIndex {}", referenceCurrency, index));
    }
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the FX index.
   * 
   * @return the FX index
   */
  public FxIndex getIndex() {
    return observation.getIndex();
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code FxReset}.
   * @return the meta-bean, not null
   */
  public static FxReset.Meta meta() {
    return FxReset.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(FxReset.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private FxReset(
      FxIndexObservation observation,
      Currency referenceCurrency) {
    JodaBeanUtils.notNull(observation, "observation");
    JodaBeanUtils.notNull(referenceCurrency, "referenceCurrency");
    this.observation = observation;
    this.referenceCurrency = referenceCurrency;
    validate();
  }

  @Override
  public FxReset.Meta metaBean() {
    return FxReset.Meta.INSTANCE;
  }

  @Override
  public <R> Property<R> property(String propertyName) {
    return metaBean().<R>metaProperty(propertyName).createProperty(this);
  }

  @Override
  public Set<String> propertyNames() {
    return metaBean().metaPropertyMap().keySet();
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the FX index observation.
   * <p>
   * This defines the observation of the index used to obtain the FX reset rate.
   * <p>
   * An FX index is a daily rate of exchange between two currencies.
   * Note that the order of the currencies in the index does not matter, as the
   * conversion direction is fully defined by the currency of the reference amount.
   * @return the value of the property, not null
   */
  public FxIndexObservation getObservation() {
    return observation;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the currency of the notional amount defined in the contract.
   * <p>
   * This is the currency of notional amount as defined in the contract.
   * The amount will be converted from this reference currency to the swap leg currency
   * when calculating the value of the leg.
   * <p>
   * The reference currency must be one of the two currencies of the index.
   * <p>
   * The reference currency is also known as the <i>constant currency</i>.
   * @return the value of the property, not null
   */
  public Currency getReferenceCurrency() {
    return referenceCurrency;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      FxReset other = (FxReset) obj;
      return JodaBeanUtils.equal(observation, other.observation) &&
          JodaBeanUtils.equal(referenceCurrency, other.referenceCurrency);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(observation);
    hash = hash * 31 + JodaBeanUtils.hashCode(referenceCurrency);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(96);
    buf.append("FxReset{");
    buf.append("observation").append('=').append(observation).append(',').append(' ');
    buf.append("referenceCurrency").append('=').append(JodaBeanUtils.toString(referenceCurrency));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code FxReset}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code observation} property.
     */
    private final MetaProperty<FxIndexObservation> observation = DirectMetaProperty.ofImmutable(
        this, "observation", FxReset.class, FxIndexObservation.class);
    /**
     * The meta-property for the {@code referenceCurrency} property.
     */
    private final MetaProperty<Currency> referenceCurrency = DirectMetaProperty.ofImmutable(
        this, "referenceCurrency", FxReset.class, Currency.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "observation",
        "referenceCurrency");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 122345516:  // observation
          return observation;
        case 727652476:  // referenceCurrency
          return referenceCurrency;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends FxReset> builder() {
      return new FxReset.Builder();
    }

    @Override
    public Class<? extends FxReset> beanType() {
      return FxReset.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code observation} property.
     * @return the meta-property, not null
     */
    public MetaProperty<FxIndexObservation> observation() {
      return observation;
    }

    /**
     * The meta-property for the {@code referenceCurrency} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Currency> referenceCurrency() {
      return referenceCurrency;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 122345516:  // observation
          return ((FxReset) bean).getObservation();
        case 727652476:  // referenceCurrency
          return ((FxReset) bean).getReferenceCurrency();
      }
      return super.propertyGet(bean, propertyName, quiet);
    }

    @Override
    protected void propertySet(Bean bean, String propertyName, Object newValue, boolean quiet) {
      metaProperty(propertyName);
      if (quiet) {
        return;
      }
      throw new UnsupportedOperationException("Property cannot be written: " + propertyName);
    }

  }

  //-----------------------------------------------------------------------
  /**
   * The bean-builder for {@code FxReset}.
   */
  private static final class Builder extends DirectFieldsBeanBuilder<FxReset> {

    private FxIndexObservation observation;
    private Currency referenceCurrency;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 122345516:  // observation
          return observation;
        case 727652476:  // referenceCurrency
          return referenceCurrency;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 122345516:  // observation
          this.observation = (FxIndexObservation) newValue;
          break;
        case 727652476:  // referenceCurrency
          this.referenceCurrency = (Currency) newValue;
          break;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
      return this;
    }

    @Override
    public Builder set(MetaProperty<?> property, Object value) {
      super.set(property, value);
      return this;
    }

    @Override
    public Builder setString(String propertyName, String value) {
      setString(meta().metaProperty(propertyName), value);
      return this;
    }

    @Override
    public Builder setString(MetaProperty<?> property, String value) {
      super.setString(property, value);
      return this;
    }

    @Override
    public Builder setAll(Map<String, ? extends Object> propertyValueMap) {
      super.setAll(propertyValueMap);
      return this;
    }

    @Override
    public FxReset build() {
      return new FxReset(
          observation,
          referenceCurrency);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(96);
      buf.append("FxReset.Builder{");
      buf.append("observation").append('=').append(JodaBeanUtils.toString(observation)).append(',').append(' ');
      buf.append("referenceCurrency").append('=').append(JodaBeanUtils.toString(referenceCurrency));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
