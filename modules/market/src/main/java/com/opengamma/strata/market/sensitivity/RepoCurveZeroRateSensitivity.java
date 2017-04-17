/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.sensitivity;

import java.io.Serializable;
import java.time.LocalDate;
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
import com.opengamma.strata.market.value.BondGroup;

/**
 * Point sensitivity to the issuer curve.
 * <p>
 * Holds the sensitivity to the repo curve at a specific date.
 */
@BeanDefinition(builderScope = "private")
public final class RepoCurveZeroRateSensitivity
    implements PointSensitivity, PointSensitivityBuilder, ImmutableBean, Serializable {

  /**
   * The currency of the curve for which the sensitivity is computed.
   */
  @PropertyDefinition(validate = "notNull")
  private final Currency curveCurrency;
  /**
   * The date that was looked up on the curve.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate date;
  /**
   * The currency of the sensitivity.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final Currency currency;
  /**
   * The bond group. 
   * <p>
   * This defines the bond group that the discount factors are for.
   * The bond group typically represents legal entity and bond security. 
   */
  @PropertyDefinition(validate = "notNull")
  private final BondGroup bondGroup;
  /** 
   * The value of the sensitivity. 
   */
  @PropertyDefinition(overrideGet = true)
  private final double sensitivity;

  //-------------------------------------------------------------------------
  /**
   * Obtains a {@code RepoCurveZeroRateSensitivity} from the curve currency, date, bond group and value.
   * <p>
   * The currency representing the curve is used also for the sensitivity currency.
   * 
   * @param currency  the currency of the curve and sensitivity
   * @param date  the date that was looked up on the curve
   * @param bondGroup  the bond group
   * @param sensitivity  the value of the sensitivity
   * @return the point sensitivity object
   */
  public static RepoCurveZeroRateSensitivity of(
      Currency currency,
      LocalDate date,
      BondGroup bondGroup,
      double sensitivity) {
    return of(currency, date, currency, bondGroup, sensitivity);
  }

  /**
   * Obtains a {@code RepoCurveZeroRateSensitivity} from zero rate sensitivity and bond group. 
   * 
   * @param zeroRateSensitivity  the zero rate sensitivity
   * @param bondGroup  the bond group
   * @return the point sensitivity object
   */
  public static RepoCurveZeroRateSensitivity of(ZeroRateSensitivity zeroRateSensitivity, BondGroup bondGroup) {
    return of(
        zeroRateSensitivity.getCurveCurrency(),
        zeroRateSensitivity.getDate(),
        zeroRateSensitivity.getCurrency(),
        bondGroup,
        zeroRateSensitivity.getSensitivity());
  }

  /**
   * Obtains a {@code RepoCurveZeroRateSensitivity} from the curve currency, date, sensitivity currency, bond group and 
   * value.
   * 
   * @param curveCurrency  the currency of the curve
   * @param date  the date that was looked up on the curve
   * @param sensitivityCurrency  the currency of the sensitivity
   * @param bondGroup  the bond group
   * @param sensitivity  the value of the sensitivity
   * @return the point sensitivity object
   */
  public static RepoCurveZeroRateSensitivity of(
      Currency curveCurrency,
      LocalDate date,
      Currency sensitivityCurrency,
      BondGroup bondGroup,
      double sensitivity) {
    return new RepoCurveZeroRateSensitivity(curveCurrency, date, sensitivityCurrency, bondGroup, sensitivity);
  }

  //-------------------------------------------------------------------------
  @Override
  public RepoCurveZeroRateSensitivity withCurrency(Currency currency) {
    if (this.currency.equals(currency)) {
      return this;
    }
    return new RepoCurveZeroRateSensitivity(curveCurrency, date, currency, bondGroup, sensitivity);
  }

  @Override
  public RepoCurveZeroRateSensitivity withSensitivity(double sensitivity) {
    return new RepoCurveZeroRateSensitivity(curveCurrency, date, currency, bondGroup, sensitivity);
  }

  @Override
  public int compareKey(PointSensitivity other) {
    if (other instanceof RepoCurveZeroRateSensitivity) {
      RepoCurveZeroRateSensitivity otherZero = (RepoCurveZeroRateSensitivity) other;
      return ComparisonChain.start()
          .compare(curveCurrency, otherZero.curveCurrency)
          .compare(currency, otherZero.currency)
          .compare(date, otherZero.date)
          .compare(bondGroup, otherZero.bondGroup)
          .result();
    }
    return getClass().getSimpleName().compareTo(other.getClass().getSimpleName());
  }

  @Override
  public RepoCurveZeroRateSensitivity convertedTo(Currency resultCurrency, FxRateProvider rateProvider) {
    return (RepoCurveZeroRateSensitivity) PointSensitivity.super.convertedTo(resultCurrency, rateProvider);
  }

  //-------------------------------------------------------------------------
  @Override
  public RepoCurveZeroRateSensitivity multipliedBy(double factor) {
    return new RepoCurveZeroRateSensitivity(curveCurrency, date, currency, bondGroup, sensitivity * factor);
  }

  @Override
  public RepoCurveZeroRateSensitivity mapSensitivity(DoubleUnaryOperator operator) {
    return new RepoCurveZeroRateSensitivity(
        curveCurrency, date, currency, bondGroup, operator.applyAsDouble(sensitivity));
  }

  @Override
  public RepoCurveZeroRateSensitivity normalize() {
    return this;
  }

  @Override
  public MutablePointSensitivities buildInto(MutablePointSensitivities combination) {
    return combination.add(this);
  }

  @Override
  public RepoCurveZeroRateSensitivity cloned() {
    return this;
  }

  /**
   * Obtains the underlying {@code ZeroRateSensitivity}. 
   * <p>
   * This creates the zero rate sensitivity object by omitting the bond group. 
   * 
   * @return the point sensitivity object
   */
  public ZeroRateSensitivity createZeroRateSensitivity() {
    return ZeroRateSensitivity.of(curveCurrency, date, currency, sensitivity);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code RepoCurveZeroRateSensitivity}.
   * @return the meta-bean, not null
   */
  public static RepoCurveZeroRateSensitivity.Meta meta() {
    return RepoCurveZeroRateSensitivity.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(RepoCurveZeroRateSensitivity.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private RepoCurveZeroRateSensitivity(
      Currency curveCurrency,
      LocalDate date,
      Currency currency,
      BondGroup bondGroup,
      double sensitivity) {
    JodaBeanUtils.notNull(curveCurrency, "curveCurrency");
    JodaBeanUtils.notNull(date, "date");
    JodaBeanUtils.notNull(currency, "currency");
    JodaBeanUtils.notNull(bondGroup, "bondGroup");
    this.curveCurrency = curveCurrency;
    this.date = date;
    this.currency = currency;
    this.bondGroup = bondGroup;
    this.sensitivity = sensitivity;
  }

  @Override
  public RepoCurveZeroRateSensitivity.Meta metaBean() {
    return RepoCurveZeroRateSensitivity.Meta.INSTANCE;
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
   * Gets the currency of the curve for which the sensitivity is computed.
   * @return the value of the property, not null
   */
  public Currency getCurveCurrency() {
    return curveCurrency;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the date that was looked up on the curve.
   * @return the value of the property, not null
   */
  public LocalDate getDate() {
    return date;
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
   * Gets the bond group.
   * <p>
   * This defines the bond group that the discount factors are for.
   * The bond group typically represents legal entity and bond security.
   * @return the value of the property, not null
   */
  public BondGroup getBondGroup() {
    return bondGroup;
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
      RepoCurveZeroRateSensitivity other = (RepoCurveZeroRateSensitivity) obj;
      return JodaBeanUtils.equal(getCurveCurrency(), other.getCurveCurrency()) &&
          JodaBeanUtils.equal(getDate(), other.getDate()) &&
          JodaBeanUtils.equal(getCurrency(), other.getCurrency()) &&
          JodaBeanUtils.equal(getBondGroup(), other.getBondGroup()) &&
          JodaBeanUtils.equal(getSensitivity(), other.getSensitivity());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(getCurveCurrency());
    hash = hash * 31 + JodaBeanUtils.hashCode(getDate());
    hash = hash * 31 + JodaBeanUtils.hashCode(getCurrency());
    hash = hash * 31 + JodaBeanUtils.hashCode(getBondGroup());
    hash = hash * 31 + JodaBeanUtils.hashCode(getSensitivity());
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(192);
    buf.append("RepoCurveZeroRateSensitivity{");
    buf.append("curveCurrency").append('=').append(getCurveCurrency()).append(',').append(' ');
    buf.append("date").append('=').append(getDate()).append(',').append(' ');
    buf.append("currency").append('=').append(getCurrency()).append(',').append(' ');
    buf.append("bondGroup").append('=').append(getBondGroup()).append(',').append(' ');
    buf.append("sensitivity").append('=').append(JodaBeanUtils.toString(getSensitivity()));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code RepoCurveZeroRateSensitivity}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code curveCurrency} property.
     */
    private final MetaProperty<Currency> curveCurrency = DirectMetaProperty.ofImmutable(
        this, "curveCurrency", RepoCurveZeroRateSensitivity.class, Currency.class);
    /**
     * The meta-property for the {@code date} property.
     */
    private final MetaProperty<LocalDate> date = DirectMetaProperty.ofImmutable(
        this, "date", RepoCurveZeroRateSensitivity.class, LocalDate.class);
    /**
     * The meta-property for the {@code currency} property.
     */
    private final MetaProperty<Currency> currency = DirectMetaProperty.ofImmutable(
        this, "currency", RepoCurveZeroRateSensitivity.class, Currency.class);
    /**
     * The meta-property for the {@code bondGroup} property.
     */
    private final MetaProperty<BondGroup> bondGroup = DirectMetaProperty.ofImmutable(
        this, "bondGroup", RepoCurveZeroRateSensitivity.class, BondGroup.class);
    /**
     * The meta-property for the {@code sensitivity} property.
     */
    private final MetaProperty<Double> sensitivity = DirectMetaProperty.ofImmutable(
        this, "sensitivity", RepoCurveZeroRateSensitivity.class, Double.TYPE);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "curveCurrency",
        "date",
        "currency",
        "bondGroup",
        "sensitivity");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 1303639584:  // curveCurrency
          return curveCurrency;
        case 3076014:  // date
          return date;
        case 575402001:  // currency
          return currency;
        case 914689404:  // bondGroup
          return bondGroup;
        case 564403871:  // sensitivity
          return sensitivity;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends RepoCurveZeroRateSensitivity> builder() {
      return new RepoCurveZeroRateSensitivity.Builder();
    }

    @Override
    public Class<? extends RepoCurveZeroRateSensitivity> beanType() {
      return RepoCurveZeroRateSensitivity.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code curveCurrency} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Currency> curveCurrency() {
      return curveCurrency;
    }

    /**
     * The meta-property for the {@code date} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> date() {
      return date;
    }

    /**
     * The meta-property for the {@code currency} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Currency> currency() {
      return currency;
    }

    /**
     * The meta-property for the {@code bondGroup} property.
     * @return the meta-property, not null
     */
    public MetaProperty<BondGroup> bondGroup() {
      return bondGroup;
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
        case 1303639584:  // curveCurrency
          return ((RepoCurveZeroRateSensitivity) bean).getCurveCurrency();
        case 3076014:  // date
          return ((RepoCurveZeroRateSensitivity) bean).getDate();
        case 575402001:  // currency
          return ((RepoCurveZeroRateSensitivity) bean).getCurrency();
        case 914689404:  // bondGroup
          return ((RepoCurveZeroRateSensitivity) bean).getBondGroup();
        case 564403871:  // sensitivity
          return ((RepoCurveZeroRateSensitivity) bean).getSensitivity();
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
   * The bean-builder for {@code RepoCurveZeroRateSensitivity}.
   */
  private static final class Builder extends DirectFieldsBeanBuilder<RepoCurveZeroRateSensitivity> {

    private Currency curveCurrency;
    private LocalDate date;
    private Currency currency;
    private BondGroup bondGroup;
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
        case 1303639584:  // curveCurrency
          return curveCurrency;
        case 3076014:  // date
          return date;
        case 575402001:  // currency
          return currency;
        case 914689404:  // bondGroup
          return bondGroup;
        case 564403871:  // sensitivity
          return sensitivity;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 1303639584:  // curveCurrency
          this.curveCurrency = (Currency) newValue;
          break;
        case 3076014:  // date
          this.date = (LocalDate) newValue;
          break;
        case 575402001:  // currency
          this.currency = (Currency) newValue;
          break;
        case 914689404:  // bondGroup
          this.bondGroup = (BondGroup) newValue;
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
    public RepoCurveZeroRateSensitivity build() {
      return new RepoCurveZeroRateSensitivity(
          curveCurrency,
          date,
          currency,
          bondGroup,
          sensitivity);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(192);
      buf.append("RepoCurveZeroRateSensitivity.Builder{");
      buf.append("curveCurrency").append('=').append(JodaBeanUtils.toString(curveCurrency)).append(',').append(' ');
      buf.append("date").append('=').append(JodaBeanUtils.toString(date)).append(',').append(' ');
      buf.append("currency").append('=').append(JodaBeanUtils.toString(currency)).append(',').append(' ');
      buf.append("bondGroup").append('=').append(JodaBeanUtils.toString(bondGroup)).append(',').append(' ');
      buf.append("sensitivity").append('=').append(JodaBeanUtils.toString(sensitivity));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
