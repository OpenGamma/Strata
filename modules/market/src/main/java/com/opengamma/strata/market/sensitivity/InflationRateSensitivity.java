/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.sensitivity;

import java.io.Serializable;
import java.time.YearMonth;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.DoubleUnaryOperator;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.google.common.collect.ComparisonChain;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.FxRateProvider;
import com.opengamma.strata.basics.index.PriceIndex;

/**
 * Point sensitivity to a rate from a price index curve.
 * <p>
 * Holds the sensitivity to the {@link PriceIndex} curve at a reference month.
 */
@BeanDefinition(builderScope = "private")
public final class InflationRateSensitivity
    implements PointSensitivity, PointSensitivityBuilder, ImmutableBean, Serializable {

  /**
   * The index of the curve for which the sensitivity is computed.
   */
  @PropertyDefinition(validate = "notNull")
  private final PriceIndex index;
  /**
   * The reference month for the index.
   */
  @PropertyDefinition(validate = "notNull")
  private final YearMonth referenceMonth;
  /**
   * The currency of the sensitivity.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final Currency currency;
  /**
   * The value of the sensitivity.
   */
  @PropertyDefinition(overrideGet = true)
  private final double sensitivity;

  //-------------------------------------------------------------------------
  /**
   * Obtains an {@code InflationRateSensitivity} from the index, reference month and value.
   * <p>
   * The currency is defaulted from the index.
   * 
   * @param index  the index of prices
   * @param referenceMonth  the reference month for the index
   * @param sensitivity  the value of the sensitivity
   * @return the point sensitivity object
   */
  public static InflationRateSensitivity of(PriceIndex index, YearMonth referenceMonth, double sensitivity) {
    return new InflationRateSensitivity(index, referenceMonth, index.getCurrency(), sensitivity);
  }

  /**
   * Obtains an {@code InflationRateSensitivity} from the index, reference month, sensitivity currency and value.
   * 
   * @param index  the index of prices
   * @param referenceMonth  the reference month for the index
   * @param sensitivityCurrency  the currency of the sensitivity
   * @param sensitivity  the value of the sensitivity
   * @return the point sensitivity object
   */
  public static InflationRateSensitivity of(
      PriceIndex index,
      YearMonth referenceMonth,
      Currency sensitivityCurrency,
      double sensitivity) {

    return new InflationRateSensitivity(index, referenceMonth, sensitivityCurrency, sensitivity);
  }

  //-------------------------------------------------------------------------
  @Override
  public InflationRateSensitivity withCurrency(Currency currency) {
    if (this.currency.equals(currency)) {
      return this;
    }
    return new InflationRateSensitivity(index, referenceMonth, currency, sensitivity);
  }

  @Override
  public InflationRateSensitivity withSensitivity(double sensitivity) {
    return new InflationRateSensitivity(index, referenceMonth, currency, sensitivity);
  }

  @Override
  public int compareKey(PointSensitivity other) {
    if (other instanceof InflationRateSensitivity) {
      InflationRateSensitivity otherInflation = (InflationRateSensitivity) other;
      return ComparisonChain.start()
          .compare(index.toString(), otherInflation.index.toString())
          .compare(currency, otherInflation.currency)
          .compare(referenceMonth, otherInflation.referenceMonth)
          .result();
    }
    return getClass().getSimpleName().compareTo(other.getClass().getSimpleName());
  }

  @Override
  public InflationRateSensitivity convertedTo(Currency resultCurrency, FxRateProvider rateProvider) {
    return (InflationRateSensitivity) PointSensitivity.super.convertedTo(resultCurrency, rateProvider);
  }

  //-------------------------------------------------------------------------
  @Override
  public InflationRateSensitivity multipliedBy(double factor) {
    return new InflationRateSensitivity(index, referenceMonth, currency, sensitivity * factor);
  }

  @Override
  public InflationRateSensitivity mapSensitivity(DoubleUnaryOperator operator) {
    return new InflationRateSensitivity(index, referenceMonth, currency, operator.applyAsDouble(sensitivity));
  }

  @Override
  public InflationRateSensitivity normalize() {
    return this;
  }

  @Override
  public MutablePointSensitivities buildInto(MutablePointSensitivities combination) {
    return combination.add(this);
  }

  @Override
  public InflationRateSensitivity cloned() {
    return this;
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code InflationRateSensitivity}.
   * @return the meta-bean, not null
   */
  public static InflationRateSensitivity.Meta meta() {
    return InflationRateSensitivity.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(InflationRateSensitivity.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private InflationRateSensitivity(
      PriceIndex index,
      YearMonth referenceMonth,
      Currency currency,
      double sensitivity) {
    JodaBeanUtils.notNull(index, "index");
    JodaBeanUtils.notNull(referenceMonth, "referenceMonth");
    JodaBeanUtils.notNull(currency, "currency");
    this.index = index;
    this.referenceMonth = referenceMonth;
    this.currency = currency;
    this.sensitivity = sensitivity;
  }

  @Override
  public InflationRateSensitivity.Meta metaBean() {
    return InflationRateSensitivity.Meta.INSTANCE;
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
   * Gets the index of the curve for which the sensitivity is computed.
   * @return the value of the property, not null
   */
  public PriceIndex getIndex() {
    return index;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the reference month for the index.
   * @return the value of the property, not null
   */
  public YearMonth getReferenceMonth() {
    return referenceMonth;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the currency of the sensitivity.
   * @return the value of the property, not null
   */
  @Override
  public Currency getCurrency() {
    return currency;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the value of the sensitivity.
   * @return the value of the property
   */
  @Override
  public double getSensitivity() {
    return sensitivity;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      InflationRateSensitivity other = (InflationRateSensitivity) obj;
      return JodaBeanUtils.equal(index, other.index) &&
          JodaBeanUtils.equal(referenceMonth, other.referenceMonth) &&
          JodaBeanUtils.equal(currency, other.currency) &&
          JodaBeanUtils.equal(sensitivity, other.sensitivity);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(index);
    hash = hash * 31 + JodaBeanUtils.hashCode(referenceMonth);
    hash = hash * 31 + JodaBeanUtils.hashCode(currency);
    hash = hash * 31 + JodaBeanUtils.hashCode(sensitivity);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(160);
    buf.append("InflationRateSensitivity{");
    buf.append("index").append('=').append(index).append(',').append(' ');
    buf.append("referenceMonth").append('=').append(referenceMonth).append(',').append(' ');
    buf.append("currency").append('=').append(currency).append(',').append(' ');
    buf.append("sensitivity").append('=').append(JodaBeanUtils.toString(sensitivity));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code InflationRateSensitivity}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code index} property.
     */
    private final MetaProperty<PriceIndex> index = DirectMetaProperty.ofImmutable(
        this, "index", InflationRateSensitivity.class, PriceIndex.class);
    /**
     * The meta-property for the {@code referenceMonth} property.
     */
    private final MetaProperty<YearMonth> referenceMonth = DirectMetaProperty.ofImmutable(
        this, "referenceMonth", InflationRateSensitivity.class, YearMonth.class);
    /**
     * The meta-property for the {@code currency} property.
     */
    private final MetaProperty<Currency> currency = DirectMetaProperty.ofImmutable(
        this, "currency", InflationRateSensitivity.class, Currency.class);
    /**
     * The meta-property for the {@code sensitivity} property.
     */
    private final MetaProperty<Double> sensitivity = DirectMetaProperty.ofImmutable(
        this, "sensitivity", InflationRateSensitivity.class, Double.TYPE);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "index",
        "referenceMonth",
        "currency",
        "sensitivity");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 100346066:  // index
          return index;
        case -1916745227:  // referenceMonth
          return referenceMonth;
        case 575402001:  // currency
          return currency;
        case 564403871:  // sensitivity
          return sensitivity;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends InflationRateSensitivity> builder() {
      return new InflationRateSensitivity.Builder();
    }

    @Override
    public Class<? extends InflationRateSensitivity> beanType() {
      return InflationRateSensitivity.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code index} property.
     * @return the meta-property, not null
     */
    public MetaProperty<PriceIndex> index() {
      return index;
    }

    /**
     * The meta-property for the {@code referenceMonth} property.
     * @return the meta-property, not null
     */
    public MetaProperty<YearMonth> referenceMonth() {
      return referenceMonth;
    }

    /**
     * The meta-property for the {@code currency} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Currency> currency() {
      return currency;
    }

    /**
     * The meta-property for the {@code sensitivity} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> sensitivity() {
      return sensitivity;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 100346066:  // index
          return ((InflationRateSensitivity) bean).getIndex();
        case -1916745227:  // referenceMonth
          return ((InflationRateSensitivity) bean).getReferenceMonth();
        case 575402001:  // currency
          return ((InflationRateSensitivity) bean).getCurrency();
        case 564403871:  // sensitivity
          return ((InflationRateSensitivity) bean).getSensitivity();
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
   * The bean-builder for {@code InflationRateSensitivity}.
   */
  private static final class Builder extends DirectFieldsBeanBuilder<InflationRateSensitivity> {

    private PriceIndex index;
    private YearMonth referenceMonth;
    private Currency currency;
    private double sensitivity;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 100346066:  // index
          return index;
        case -1916745227:  // referenceMonth
          return referenceMonth;
        case 575402001:  // currency
          return currency;
        case 564403871:  // sensitivity
          return sensitivity;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 100346066:  // index
          this.index = (PriceIndex) newValue;
          break;
        case -1916745227:  // referenceMonth
          this.referenceMonth = (YearMonth) newValue;
          break;
        case 575402001:  // currency
          this.currency = (Currency) newValue;
          break;
        case 564403871:  // sensitivity
          this.sensitivity = (Double) newValue;
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
    public InflationRateSensitivity build() {
      return new InflationRateSensitivity(
          index,
          referenceMonth,
          currency,
          sensitivity);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(160);
      buf.append("InflationRateSensitivity.Builder{");
      buf.append("index").append('=').append(JodaBeanUtils.toString(index)).append(',').append(' ');
      buf.append("referenceMonth").append('=').append(JodaBeanUtils.toString(referenceMonth)).append(',').append(' ');
      buf.append("currency").append('=').append(JodaBeanUtils.toString(currency)).append(',').append(' ');
      buf.append("sensitivity").append('=').append(JodaBeanUtils.toString(sensitivity));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
