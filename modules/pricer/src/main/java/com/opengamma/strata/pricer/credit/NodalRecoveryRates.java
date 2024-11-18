/*
 * Copyright (C) 2023 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.credit;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.MetaProperty;
import org.joda.beans.TypedMetaBean;
import org.joda.beans.gen.BeanDefinition;
import org.joda.beans.gen.ImmutableConstructor;
import org.joda.beans.gen.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.MinimalMetaBean;

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
@BeanDefinition(style = "minimal")
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
   */
  private static final TypedMetaBean<NodalRecoveryRates> META_BEAN =
      MinimalMetaBean.of(
          NodalRecoveryRates.class,
          new String[] {
              "legalEntityId",
              "valuationDate",
              "curve"},
          () -> new NodalRecoveryRates.Builder(),
          b -> b.getLegalEntityId(),
          b -> b.getValuationDate(),
          b -> b.getCurve());

  /**
   * The meta-bean for {@code NodalRecoveryRates}.
   * @return the meta-bean, not null
   */
  public static TypedMetaBean<NodalRecoveryRates> meta() {
    return META_BEAN;
  }

  static {
    MetaBean.register(META_BEAN);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static NodalRecoveryRates.Builder builder() {
    return new NodalRecoveryRates.Builder();
  }

  @Override
  public TypedMetaBean<NodalRecoveryRates> metaBean() {
    return META_BEAN;
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
   * The bean-builder for {@code NodalRecoveryRates}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<NodalRecoveryRates> {

    private StandardId legalEntityId;
    private LocalDate valuationDate;
    private NodalCurve curve;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(NodalRecoveryRates beanToCopy) {
      this.legalEntityId = beanToCopy.getLegalEntityId();
      this.valuationDate = beanToCopy.getValuationDate();
      this.curve = beanToCopy.getCurve();
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
    public Builder set(MetaProperty<?> property, Object value) {
      super.set(property, value);
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
    /**
     * Sets the legal entity identifier.
     * <p>
     * This identifier is used for the reference legal entity of a credit derivative.
     * @param legalEntityId  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder legalEntityId(StandardId legalEntityId) {
      JodaBeanUtils.notNull(legalEntityId, "legalEntityId");
      this.legalEntityId = legalEntityId;
      return this;
    }

    /**
     * Sets the valuation date.
     * @param valuationDate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder valuationDate(LocalDate valuationDate) {
      JodaBeanUtils.notNull(valuationDate, "valuationDate");
      this.valuationDate = valuationDate;
      return this;
    }

    /**
     * Sets the underlying curve.
     * <p>
     * The metadata of the curve must define a day count.
     * @param curve  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder curve(NodalCurve curve) {
      JodaBeanUtils.notNull(curve, "curve");
      this.curve = curve;
      return this;
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
