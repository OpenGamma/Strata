/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.credit;

import java.io.Serializable;
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
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;
import org.joda.beans.impl.direct.DirectPrivateBeanBuilder;

import com.google.common.collect.ComparisonChain;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.FxRateProvider;
import com.opengamma.strata.market.sensitivity.MutablePointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.ZeroRateSensitivity;

/**
 * Point sensitivity to the zero hazard rate curve.
 * <p>
 * Holds the sensitivity to the zero hazard rate curve at a specific date.
 */
@BeanDefinition(builderScope = "private")
public final class CreditCurveZeroRateSensitivity
    implements PointSensitivity, PointSensitivityBuilder, ImmutableBean, Serializable {

  /**
   * The legal entity identifier.
   * <p>
   * This identifier is used for the reference legal entity of a credit derivative.
   */
  @PropertyDefinition(validate = "notNull")
  private final StandardId legalEntityId;
  /**
   * The zero rate sensitivity.
   * <p>
   * This stores curve currency, year fraction, sensitivity currency and sensitivity value.
   */
  @PropertyDefinition(validate = "notNull")
  private final ZeroRateSensitivity zeroRateSensitivity;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance.
   * 
   * @param legalEntityId  the legal entity identifier
   * @param currency  the currency of the curve and sensitivity
   * @param yearFraction  the year fraction that was looked up on the curve
   * @param sensitivity  the value of the sensitivity
   * @return the point sensitivity object
   */
  public static CreditCurveZeroRateSensitivity of(
      StandardId legalEntityId,
      Currency currency,
      double yearFraction,
      double sensitivity) {

    ZeroRateSensitivity zeroRateSensitivity = ZeroRateSensitivity.of(currency, yearFraction, sensitivity);
    return new CreditCurveZeroRateSensitivity(legalEntityId, zeroRateSensitivity);
  }

  /**
   * Obtains an instance with sensitivity currency specified.
   * 
   * @param legalEntityId  the legal entity identifier
   * @param curveCurrency  the currency of the curve
   * @param yearFraction  the year fraction that was looked up on the curve
   * @param sensitivityCurrency  the currency of the sensitivity
   * @param sensitivity  the value of the sensitivity
   * @return the point sensitivity object
   */
  public static CreditCurveZeroRateSensitivity of(
      StandardId legalEntityId,
      Currency curveCurrency,
      double yearFraction,
      Currency sensitivityCurrency,
      double sensitivity) {

    ZeroRateSensitivity zeroRateSensitivity =
        ZeroRateSensitivity.of(curveCurrency, yearFraction, sensitivityCurrency, sensitivity);
    return new CreditCurveZeroRateSensitivity(legalEntityId, zeroRateSensitivity);
  }

  /**
   * Obtains an instance from {@code ZeroRateSensitivity} and {@code StandardId}.
   * 
   * @param legalEntityId  the legal entity identifier
   * @param zeroRateSensitivity  the zero rate sensitivity
   * @return the point sensitivity object
   */
  public static CreditCurveZeroRateSensitivity of(
      StandardId legalEntityId,
      ZeroRateSensitivity zeroRateSensitivity) {

    return new CreditCurveZeroRateSensitivity(legalEntityId, zeroRateSensitivity);
  }

  //-------------------------------------------------------------------------
  @Override
  public Currency getCurrency() {
    return zeroRateSensitivity.getCurrency();
  }

  @Override
  public double getSensitivity() {
    return zeroRateSensitivity.getSensitivity();
  }

  /**
   * Gets the currency of the curve for which the sensitivity is computed.
   * 
   * @return the curve currency
   */
  public Currency getCurveCurrency() {
    return zeroRateSensitivity.getCurveCurrency();
  }

  /**
   * Gets the time that was queried, expressed as a year fraction.
   * 
   * @return the year fraction
   */
  public double getYearFraction() {
    return zeroRateSensitivity.getYearFraction();
  }

  //-------------------------------------------------------------------------
  @Override
  public CreditCurveZeroRateSensitivity withCurrency(Currency currency) {
    if (this.zeroRateSensitivity.getCurrency().equals(currency)) {
      return this;
    }
    return new CreditCurveZeroRateSensitivity(legalEntityId, zeroRateSensitivity.withCurrency(currency));
  }

  @Override
  public CreditCurveZeroRateSensitivity withSensitivity(double sensitivity) {
    return new CreditCurveZeroRateSensitivity(legalEntityId, zeroRateSensitivity.withSensitivity(sensitivity));
  }

  @Override
  public int compareKey(PointSensitivity other) {
    if (other instanceof CreditCurveZeroRateSensitivity) {
      CreditCurveZeroRateSensitivity otherZero = (CreditCurveZeroRateSensitivity) other;
      return ComparisonChain.start()
          .compare(zeroRateSensitivity.getYearFraction(), otherZero.zeroRateSensitivity.getYearFraction())
          .compare(zeroRateSensitivity.getCurrency(), otherZero.zeroRateSensitivity.getCurrency())
          .compare(zeroRateSensitivity.getCurveCurrency(), otherZero.zeroRateSensitivity.getCurveCurrency())
          .compare(legalEntityId, otherZero.legalEntityId)
          .result();
    }
    return getClass().getSimpleName().compareTo(other.getClass().getSimpleName());
  }

  @Override
  public CreditCurveZeroRateSensitivity convertedTo(Currency resultCurrency, FxRateProvider rateProvider) {
    return new CreditCurveZeroRateSensitivity(legalEntityId, zeroRateSensitivity.convertedTo(resultCurrency, rateProvider));
  }

  //-------------------------------------------------------------------------
  @Override
  public CreditCurveZeroRateSensitivity multipliedBy(double factor) {
    return new CreditCurveZeroRateSensitivity(legalEntityId, zeroRateSensitivity.multipliedBy(factor));
  }

  @Override
  public CreditCurveZeroRateSensitivity mapSensitivity(DoubleUnaryOperator operator) {
    return new CreditCurveZeroRateSensitivity(legalEntityId, zeroRateSensitivity.mapSensitivity(operator));
  }

  @Override
  public CreditCurveZeroRateSensitivity normalize() {
    return this;
  }

  @Override
  public MutablePointSensitivities buildInto(MutablePointSensitivities combination) {
    return combination.add(this);
  }

  @Override
  public CreditCurveZeroRateSensitivity cloned() {
    return this;
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains the underlying {@code ZeroRateSensitivity}. 
   * <p>
   * This creates the zero rate sensitivity object by omitting the legal entity identifier.
   * 
   * @return the point sensitivity object
   */
  public ZeroRateSensitivity toZeroRateSensitivity() {
    return zeroRateSensitivity;
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code CreditCurveZeroRateSensitivity}.
   * @return the meta-bean, not null
   */
  public static CreditCurveZeroRateSensitivity.Meta meta() {
    return CreditCurveZeroRateSensitivity.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(CreditCurveZeroRateSensitivity.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private CreditCurveZeroRateSensitivity(
      StandardId legalEntityId,
      ZeroRateSensitivity zeroRateSensitivity) {
    JodaBeanUtils.notNull(legalEntityId, "legalEntityId");
    JodaBeanUtils.notNull(zeroRateSensitivity, "zeroRateSensitivity");
    this.legalEntityId = legalEntityId;
    this.zeroRateSensitivity = zeroRateSensitivity;
  }

  @Override
  public CreditCurveZeroRateSensitivity.Meta metaBean() {
    return CreditCurveZeroRateSensitivity.Meta.INSTANCE;
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
   * Gets the legal entity identifier.
   * <p>
   * This identifier is used for the reference legal entity of a credit derivative.
   * @return the value of the property, not null
   */
  public StandardId getLegalEntityId() {
    return legalEntityId;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the zero rate sensitivity.
   * <p>
   * This stores curve currency, year fraction, sensitivity currency and sensitivity value.
   * @return the value of the property, not null
   */
  public ZeroRateSensitivity getZeroRateSensitivity() {
    return zeroRateSensitivity;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      CreditCurveZeroRateSensitivity other = (CreditCurveZeroRateSensitivity) obj;
      return JodaBeanUtils.equal(legalEntityId, other.legalEntityId) &&
          JodaBeanUtils.equal(zeroRateSensitivity, other.zeroRateSensitivity);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(legalEntityId);
    hash = hash * 31 + JodaBeanUtils.hashCode(zeroRateSensitivity);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(96);
    buf.append("CreditCurveZeroRateSensitivity{");
    buf.append("legalEntityId").append('=').append(legalEntityId).append(',').append(' ');
    buf.append("zeroRateSensitivity").append('=').append(JodaBeanUtils.toString(zeroRateSensitivity));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code CreditCurveZeroRateSensitivity}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code legalEntityId} property.
     */
    private final MetaProperty<StandardId> legalEntityId = DirectMetaProperty.ofImmutable(
        this, "legalEntityId", CreditCurveZeroRateSensitivity.class, StandardId.class);
    /**
     * The meta-property for the {@code zeroRateSensitivity} property.
     */
    private final MetaProperty<ZeroRateSensitivity> zeroRateSensitivity = DirectMetaProperty.ofImmutable(
        this, "zeroRateSensitivity", CreditCurveZeroRateSensitivity.class, ZeroRateSensitivity.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "legalEntityId",
        "zeroRateSensitivity");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 866287159:  // legalEntityId
          return legalEntityId;
        case 1232683479:  // zeroRateSensitivity
          return zeroRateSensitivity;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends CreditCurveZeroRateSensitivity> builder() {
      return new CreditCurveZeroRateSensitivity.Builder();
    }

    @Override
    public Class<? extends CreditCurveZeroRateSensitivity> beanType() {
      return CreditCurveZeroRateSensitivity.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code legalEntityId} property.
     * @return the meta-property, not null
     */
    public MetaProperty<StandardId> legalEntityId() {
      return legalEntityId;
    }

    /**
     * The meta-property for the {@code zeroRateSensitivity} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ZeroRateSensitivity> zeroRateSensitivity() {
      return zeroRateSensitivity;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 866287159:  // legalEntityId
          return ((CreditCurveZeroRateSensitivity) bean).getLegalEntityId();
        case 1232683479:  // zeroRateSensitivity
          return ((CreditCurveZeroRateSensitivity) bean).getZeroRateSensitivity();
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
   * The bean-builder for {@code CreditCurveZeroRateSensitivity}.
   */
  private static final class Builder extends DirectPrivateBeanBuilder<CreditCurveZeroRateSensitivity> {

    private StandardId legalEntityId;
    private ZeroRateSensitivity zeroRateSensitivity;

    /**
     * Restricted constructor.
     */
    private Builder() {
      super(meta());
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 866287159:  // legalEntityId
          return legalEntityId;
        case 1232683479:  // zeroRateSensitivity
          return zeroRateSensitivity;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 866287159:  // legalEntityId
          this.legalEntityId = (StandardId) newValue;
          break;
        case 1232683479:  // zeroRateSensitivity
          this.zeroRateSensitivity = (ZeroRateSensitivity) newValue;
          break;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
      return this;
    }

    @Override
    public CreditCurveZeroRateSensitivity build() {
      return new CreditCurveZeroRateSensitivity(
          legalEntityId,
          zeroRateSensitivity);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(96);
      buf.append("CreditCurveZeroRateSensitivity.Builder{");
      buf.append("legalEntityId").append('=').append(JodaBeanUtils.toString(legalEntityId)).append(',').append(' ');
      buf.append("zeroRateSensitivity").append('=').append(JodaBeanUtils.toString(zeroRateSensitivity));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
