/*
 * Copyright (C) 2023 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.credit;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.MetaProperty;
import org.joda.beans.gen.BeanDefinition;
import org.joda.beans.gen.ImmutableConstructor;
import org.joda.beans.gen.PropertyDefinition;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;
import org.joda.beans.impl.direct.DirectPrivateBeanBuilder;

import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.data.MarketDataName;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.CurveInfoType;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.curve.NodalCurve;
import com.opengamma.strata.market.param.ParameterMetadata;
import com.opengamma.strata.market.param.ParameterPerturbation;

/**
 * The recovery rates based on a nodal curve.
 * <p>
 * The underlying curve must contain {@linkplain ValueType#YEAR_FRACTION year fractions}
 * against {@linkplain ValueType#RECOVERY_RATE recovery rates}, and the day count must be present.
 */
@BeanDefinition(builderScope = "private")
public final class NodalRecoveryRates
    implements RecoveryRates, ImmutableBean, Serializable {

  /**
   * The legal entity identifier.
   * <p>
   * This identifier is used for the reference legal entity of a credit derivative.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final StandardId legalEntityId;
  /**
   * The valuation date.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final LocalDate valuationDate;
  /**
   * The underlying curve.
   * <p>
   * The metadata of the curve must define a day count.
   */
  @PropertyDefinition(validate = "notNull")
  private final NodalCurve curve;
  /**
   * The day count convention of the curve.
   */
  private final transient DayCount dayCount;  // cached, not a property

  //-------------------------------------------------------------------------

  /**
   * Obtains an instance.
   * <p>
   * The curve is specified by an instance of {@link NodalCurve}, such as {@link InterpolatedNodalCurve}.
   * The curve must contain {@linkplain ValueType#YEAR_FRACTION year fractions}
   * against {@linkplain ValueType#RECOVERY_RATE recovery rates}, and the day count must be present.
   *
   * @param legalEntityId  the legalEntity ID
   * @param valuationDate  the valuation date for which the curve is valid
   * @param curve  the underlying curve
   * @return the instance
   */
  public static NodalRecoveryRates of(StandardId legalEntityId, LocalDate valuationDate, NodalCurve curve) {
    return new NodalRecoveryRates(legalEntityId, valuationDate, curve);
  }

  @ImmutableConstructor
  private NodalRecoveryRates(StandardId legalEntityId, LocalDate valuationDate, NodalCurve curve) {

    ArgChecker.notNull(legalEntityId, "legalEntityId");
    ArgChecker.notNull(valuationDate, "valuationDate");
    ArgChecker.notNull(curve, "curve");
    curve.getMetadata().getXValueType().checkEquals(
        ValueType.YEAR_FRACTION, "Incorrect x-value type for recovery rate curve");
    curve.getMetadata().getYValueType().checkEquals(
        ValueType.RECOVERY_RATE, "Incorrect y-value type for recovery rate curve");
    DayCount dayCount = curve.getMetadata().findInfo(CurveInfoType.DAY_COUNT)
        .orElseThrow(() -> new IllegalArgumentException("Incorrect curve metadata, missing DayCount"));

    this.legalEntityId = legalEntityId;
    this.valuationDate = valuationDate;
    this.curve = curve;
    this.dayCount = dayCount;
  }

  //-------------------------------------------------------------------------

  @Override
  public double recoveryRate(LocalDate date) {
    double yearFraction = dayCount.relativeYearFraction(valuationDate, date);
    return curve.yValue(yearFraction);
  }
  @Override
  public <T> Optional<T> findData(MarketDataName<T> name) {
    if (curve.getName().equals(name)) {
      return Optional.of(name.getMarketDataType().cast(curve));
    }
    return Optional.empty();
  }

  @Override
  public int getParameterCount() {
    return curve.getParameterCount();
  }

  @Override
  public double getParameter(int parameterIndex) {
    return curve.getParameter(parameterIndex);
  }

  @Override
  public ParameterMetadata getParameterMetadata(int parameterIndex) {
    return curve.getParameterMetadata(parameterIndex);
  }

  @Override
  public NodalRecoveryRates withParameter(int parameterIndex, double newValue) {
    return withCurve(curve.withParameter(parameterIndex, newValue));
  }

  @Override
  public NodalRecoveryRates withPerturbation(ParameterPerturbation perturbation) {
    return withCurve(curve.withPerturbation(perturbation));
  }

  //-------------------------------------------------------------------------

  /**
   * Returns a new instance with a different curve.
   *
   * @param curve  the new curve
   * @return the new instance
   */
  public NodalRecoveryRates withCurve(NodalCurve curve) {
    return new NodalRecoveryRates(legalEntityId, valuationDate, curve);
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code NodalRecoveryRates}.
   * @return the meta-bean, not null
   */
  public static NodalRecoveryRates.Meta meta() {
    return NodalRecoveryRates.Meta.INSTANCE;
  }

  static {
    MetaBean.register(NodalRecoveryRates.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  @Override
  public NodalRecoveryRates.Meta metaBean() {
    return NodalRecoveryRates.Meta.INSTANCE;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the legal entity identifier.
   * <p>
   * This identifier is used for the reference legal entity of a credit derivative.
   * @return the value of the property, not null
   */
  @Override
  public StandardId getLegalEntityId() {
    return legalEntityId;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the valuation date.
   * @return the value of the property, not null
   */
  @Override
  public LocalDate getValuationDate() {
    return valuationDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the underlying curve.
   * <p>
   * The metadata of the curve must define a day count.
   * @return the value of the property, not null
   */
  public NodalCurve getCurve() {
    return curve;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      NodalRecoveryRates other = (NodalRecoveryRates) obj;
      return JodaBeanUtils.equal(legalEntityId, other.legalEntityId) &&
          JodaBeanUtils.equal(valuationDate, other.valuationDate) &&
          JodaBeanUtils.equal(curve, other.curve);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(legalEntityId);
    hash = hash * 31 + JodaBeanUtils.hashCode(valuationDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(curve);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(128);
    buf.append("NodalRecoveryRates{");
    buf.append("legalEntityId").append('=').append(JodaBeanUtils.toString(legalEntityId)).append(',').append(' ');
    buf.append("valuationDate").append('=').append(JodaBeanUtils.toString(valuationDate)).append(',').append(' ');
    buf.append("curve").append('=').append(JodaBeanUtils.toString(curve));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code NodalRecoveryRates}.
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
        this, "legalEntityId", NodalRecoveryRates.class, StandardId.class);
    /**
     * The meta-property for the {@code valuationDate} property.
     */
    private final MetaProperty<LocalDate> valuationDate = DirectMetaProperty.ofImmutable(
        this, "valuationDate", NodalRecoveryRates.class, LocalDate.class);
    /**
     * The meta-property for the {@code curve} property.
     */
    private final MetaProperty<NodalCurve> curve = DirectMetaProperty.ofImmutable(
        this, "curve", NodalRecoveryRates.class, NodalCurve.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "legalEntityId",
        "valuationDate",
        "curve");

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
        case 113107279:  // valuationDate
          return valuationDate;
        case 95027439:  // curve
          return curve;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends NodalRecoveryRates> builder() {
      return new NodalRecoveryRates.Builder();
    }

    @Override
    public Class<? extends NodalRecoveryRates> beanType() {
      return NodalRecoveryRates.class;
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
     * The meta-property for the {@code valuationDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> valuationDate() {
      return valuationDate;
    }

    /**
     * The meta-property for the {@code curve} property.
     * @return the meta-property, not null
     */
    public MetaProperty<NodalCurve> curve() {
      return curve;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 866287159:  // legalEntityId
          return ((NodalRecoveryRates) bean).getLegalEntityId();
        case 113107279:  // valuationDate
          return ((NodalRecoveryRates) bean).getValuationDate();
        case 95027439:  // curve
          return ((NodalRecoveryRates) bean).getCurve();
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
   * The bean-builder for {@code NodalRecoveryRates}.
   */
  private static final class Builder extends DirectPrivateBeanBuilder<NodalRecoveryRates> {

    private StandardId legalEntityId;
    private LocalDate valuationDate;
    private NodalCurve curve;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 866287159:  // legalEntityId
          return legalEntityId;
        case 113107279:  // valuationDate
          return valuationDate;
        case 95027439:  // curve
          return curve;
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
        case 113107279:  // valuationDate
          this.valuationDate = (LocalDate) newValue;
          break;
        case 95027439:  // curve
          this.curve = (NodalCurve) newValue;
          break;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
      return this;
    }

    @Override
    public NodalRecoveryRates build() {
      return new NodalRecoveryRates(
          legalEntityId,
          valuationDate,
          curve);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(128);
      buf.append("NodalRecoveryRates.Builder{");
      buf.append("legalEntityId").append('=').append(JodaBeanUtils.toString(legalEntityId)).append(',').append(' ');
      buf.append("valuationDate").append('=').append(JodaBeanUtils.toString(valuationDate)).append(',').append(' ');
      buf.append("curve").append('=').append(JodaBeanUtils.toString(curve));
      buf.append('}');
      return buf.toString();
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
