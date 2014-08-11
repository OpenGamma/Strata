/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.sesame.credit;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.joda.beans.Bean;
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

import com.opengamma.analytics.financial.credit.isdastandardmodel.ISDACompliantCreditCurve;
import com.opengamma.financial.analytics.isda.credit.CreditCurveData;

/**
 * Represents a credit curve for use in pricing on the ISDA model.
 * As well as holding the calibrated curve, the curve data used
 * as input to calibration is also captured. This is useful when
 * access to term structure, base market data and/or the underlying
 * yield curve is required.
 */
@BeanDefinition
public final class IsdaCreditCurve implements ImmutableBean {

  /**
   * The underlying yield curve used as an input in calibration.
   */
  @PropertyDefinition(validate = "notNull")
  private final IsdaYieldCurve _yieldCurve;
  
  /**
   * The data used to calibrated this curve, i.e. term structure,
   * market data, conventions, etc.
   */
  @PropertyDefinition(validate = "notNull")
  private final CreditCurveData _curveData;
  
  /**
   * The calibrated credit curve.
   */
  @PropertyDefinition(validate = "notNull")
  private final ISDACompliantCreditCurve _calibratedCurve;
  
  
  
  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code IsdaCreditCurve}.
   * @return the meta-bean, not null
   */
  public static IsdaCreditCurve.Meta meta() {
    return IsdaCreditCurve.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(IsdaCreditCurve.Meta.INSTANCE);
  }

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static IsdaCreditCurve.Builder builder() {
    return new IsdaCreditCurve.Builder();
  }

  private IsdaCreditCurve(
      IsdaYieldCurve yieldCurve,
      CreditCurveData curveData,
      ISDACompliantCreditCurve calibratedCurve) {
    JodaBeanUtils.notNull(yieldCurve, "yieldCurve");
    JodaBeanUtils.notNull(curveData, "curveData");
    JodaBeanUtils.notNull(calibratedCurve, "calibratedCurve");
    this._yieldCurve = yieldCurve;
    this._curveData = curveData;
    this._calibratedCurve = calibratedCurve;
  }

  @Override
  public IsdaCreditCurve.Meta metaBean() {
    return IsdaCreditCurve.Meta.INSTANCE;
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
   * Gets the underlying yield curve used as an input in calibration.
   * @return the value of the property, not null
   */
  public IsdaYieldCurve getYieldCurve() {
    return _yieldCurve;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the data used to calibrated this curve, i.e. term structure,
   * market data, conventions, etc.
   * @return the value of the property, not null
   */
  public CreditCurveData getCurveData() {
    return _curveData;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the calibrated credit curve.
   * @return the value of the property, not null
   */
  public ISDACompliantCreditCurve getCalibratedCurve() {
    return _calibratedCurve;
  }

  //-----------------------------------------------------------------------
  /**
   * Returns a builder that allows this bean to be mutated.
   * @return the mutable builder, not null
   */
  public Builder toBuilder() {
    return new Builder(this);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      IsdaCreditCurve other = (IsdaCreditCurve) obj;
      return JodaBeanUtils.equal(getYieldCurve(), other.getYieldCurve()) &&
          JodaBeanUtils.equal(getCurveData(), other.getCurveData()) &&
          JodaBeanUtils.equal(getCalibratedCurve(), other.getCalibratedCurve());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash += hash * 31 + JodaBeanUtils.hashCode(getYieldCurve());
    hash += hash * 31 + JodaBeanUtils.hashCode(getCurveData());
    hash += hash * 31 + JodaBeanUtils.hashCode(getCalibratedCurve());
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(128);
    buf.append("IsdaCreditCurve{");
    buf.append("yieldCurve").append('=').append(getYieldCurve()).append(',').append(' ');
    buf.append("curveData").append('=').append(getCurveData()).append(',').append(' ');
    buf.append("calibratedCurve").append('=').append(JodaBeanUtils.toString(getCalibratedCurve()));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code IsdaCreditCurve}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code yieldCurve} property.
     */
    private final MetaProperty<IsdaYieldCurve> _yieldCurve = DirectMetaProperty.ofImmutable(
        this, "yieldCurve", IsdaCreditCurve.class, IsdaYieldCurve.class);
    /**
     * The meta-property for the {@code curveData} property.
     */
    private final MetaProperty<CreditCurveData> _curveData = DirectMetaProperty.ofImmutable(
        this, "curveData", IsdaCreditCurve.class, CreditCurveData.class);
    /**
     * The meta-property for the {@code calibratedCurve} property.
     */
    private final MetaProperty<ISDACompliantCreditCurve> _calibratedCurve = DirectMetaProperty.ofImmutable(
        this, "calibratedCurve", IsdaCreditCurve.class, ISDACompliantCreditCurve.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> _metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "yieldCurve",
        "curveData",
        "calibratedCurve");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 1112236386:  // yieldCurve
          return _yieldCurve;
        case 770856249:  // curveData
          return _curveData;
        case -1314959246:  // calibratedCurve
          return _calibratedCurve;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public IsdaCreditCurve.Builder builder() {
      return new IsdaCreditCurve.Builder();
    }

    @Override
    public Class<? extends IsdaCreditCurve> beanType() {
      return IsdaCreditCurve.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return _metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code yieldCurve} property.
     * @return the meta-property, not null
     */
    public MetaProperty<IsdaYieldCurve> yieldCurve() {
      return _yieldCurve;
    }

    /**
     * The meta-property for the {@code curveData} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CreditCurveData> curveData() {
      return _curveData;
    }

    /**
     * The meta-property for the {@code calibratedCurve} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ISDACompliantCreditCurve> calibratedCurve() {
      return _calibratedCurve;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 1112236386:  // yieldCurve
          return ((IsdaCreditCurve) bean).getYieldCurve();
        case 770856249:  // curveData
          return ((IsdaCreditCurve) bean).getCurveData();
        case -1314959246:  // calibratedCurve
          return ((IsdaCreditCurve) bean).getCalibratedCurve();
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
   * The bean-builder for {@code IsdaCreditCurve}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<IsdaCreditCurve> {

    private IsdaYieldCurve _yieldCurve;
    private CreditCurveData _curveData;
    private ISDACompliantCreditCurve _calibratedCurve;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(IsdaCreditCurve beanToCopy) {
      this._yieldCurve = beanToCopy.getYieldCurve();
      this._curveData = beanToCopy.getCurveData();
      this._calibratedCurve = beanToCopy.getCalibratedCurve();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 1112236386:  // yieldCurve
          return _yieldCurve;
        case 770856249:  // curveData
          return _curveData;
        case -1314959246:  // calibratedCurve
          return _calibratedCurve;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 1112236386:  // yieldCurve
          this._yieldCurve = (IsdaYieldCurve) newValue;
          break;
        case 770856249:  // curveData
          this._curveData = (CreditCurveData) newValue;
          break;
        case -1314959246:  // calibratedCurve
          this._calibratedCurve = (ISDACompliantCreditCurve) newValue;
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
    public IsdaCreditCurve build() {
      return new IsdaCreditCurve(
          _yieldCurve,
          _curveData,
          _calibratedCurve);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the {@code yieldCurve} property in the builder.
     * @param yieldCurve  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder yieldCurve(IsdaYieldCurve yieldCurve) {
      JodaBeanUtils.notNull(yieldCurve, "yieldCurve");
      this._yieldCurve = yieldCurve;
      return this;
    }

    /**
     * Sets the {@code curveData} property in the builder.
     * @param curveData  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder curveData(CreditCurveData curveData) {
      JodaBeanUtils.notNull(curveData, "curveData");
      this._curveData = curveData;
      return this;
    }

    /**
     * Sets the {@code calibratedCurve} property in the builder.
     * @param calibratedCurve  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder calibratedCurve(ISDACompliantCreditCurve calibratedCurve) {
      JodaBeanUtils.notNull(calibratedCurve, "calibratedCurve");
      this._calibratedCurve = calibratedCurve;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(128);
      buf.append("IsdaCreditCurve.Builder{");
      buf.append("yieldCurve").append('=').append(JodaBeanUtils.toString(_yieldCurve)).append(',').append(' ');
      buf.append("curveData").append('=').append(JodaBeanUtils.toString(_curveData)).append(',').append(' ');
      buf.append("calibratedCurve").append('=').append(JodaBeanUtils.toString(_calibratedCurve));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
