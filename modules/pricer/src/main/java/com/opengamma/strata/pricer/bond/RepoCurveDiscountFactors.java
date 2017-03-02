/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.bond;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

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

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.pricer.DiscountFactors;
import com.opengamma.strata.pricer.ZeroRateSensitivity;

/**
 * Provides access to discount factors for a repo curve.
 * <p>
 * The discount factor represents the time value of money for the specified security, issuer and currency
 * when comparing the valuation date to the specified date.
 */
@BeanDefinition(builderScope = "private")
public final class RepoCurveDiscountFactors
    implements ImmutableBean, Serializable {

  /**
   * The underlying discount factors for a single currency.
   * <p>
   * This contains curve, curve currency, valuation date and day count convention.
   * The discount factor, its point sensitivity and curve sensitivity are computed by this {@code DiscountFactors}.
   */
  @PropertyDefinition(validate = "notNull")
  private final DiscountFactors discountFactors;
  /**
   * The repo group.
   * <p>
   * This defines the group that the discount factors are for.
   */
  @PropertyDefinition(validate = "notNull")
  private final RepoGroup repoGroup;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance based on discount factors and group.
   * 
   * @param discountFactors  the discount factors
   * @param group  the group
   * @return the repo curve discount factors
   */
  public static RepoCurveDiscountFactors of(DiscountFactors discountFactors, RepoGroup group) {
    return new RepoCurveDiscountFactors(discountFactors, group);
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the currency.
   * <p>
   * The currency that discount factors are provided for.
   * 
   * @return the currency
   */
  public Currency getCurrency() {
    return discountFactors.getCurrency();
  }

  /**
   * Gets the valuation date.
   * <p>
   * The raw data in this provider is calibrated for this date.
   * 
   * @return the valuation date
   */
  public LocalDate getValuationDate() {
    return discountFactors.getValuationDate();
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the discount factor.
   * <p>
   * The discount factor represents the time value of money for the specified currency and bond
   * when comparing the valuation date to the specified date.
   * <p>
   * If the valuation date is on or after the specified date, the discount factor is 1.
   * 
   * @param date  the date to discount to
   * @return the discount factor
   */
  public double discountFactor(LocalDate date) {
    return discountFactors.discountFactor(date);
  }

  /**
   * Calculates the zero rate point sensitivity at the specified date.
   * <p>
   * This returns a sensitivity instance referring to the zero rate sensitivity of the curve
   * used to determine the discount factor.
   * The sensitivity typically has the value {@code (-discountFactor * relativeYearFraction)}.
   * The sensitivity refers to the result of {@link #discountFactor(LocalDate)}.
   * 
   * @param date  the date to discount to
   * @return the point sensitivity of the zero rate
   * @throws RuntimeException if the result cannot be calculated
   */
  public RepoCurveZeroRateSensitivity zeroRatePointSensitivity(LocalDate date) {
    return zeroRatePointSensitivity(date, getCurrency());
  }

  /**
   * Calculates the zero rate point sensitivity at the specified date specifying the currency of the sensitivity.
   * <p>
   * This returns a sensitivity instance referring to the zero rate sensitivity of the curve
   * used to determine the discount factor.
   * The sensitivity typically has the value {@code (-discountFactor * relativeYearFraction)}.
   * The sensitivity refers to the result of {@link #discountFactor(LocalDate)}.
   * <p>
   * This method allows the currency of the sensitivity to differ from the currency of the curve.
   * 
   * @param date  the date to discount to
   * @param sensitivityCurrency  the currency of the sensitivity
   * @return the point sensitivity of the zero rate
   * @throws RuntimeException if the result cannot be calculated
   */
  public RepoCurveZeroRateSensitivity zeroRatePointSensitivity(LocalDate date, Currency sensitivityCurrency) {
    ZeroRateSensitivity zeroRateSensitivity = discountFactors.zeroRatePointSensitivity(date, sensitivityCurrency);
    return RepoCurveZeroRateSensitivity.of(zeroRateSensitivity, repoGroup);
  }

  /**
   * Calculates the curve parameter sensitivity from the point sensitivity.
   * <p>
   * This is used to convert a single point sensitivity to curve parameter sensitivity.
   * The calculation typically involves multiplying the point and unit sensitivities.
   * 
   * @param pointSensitivity  the point sensitivity to convert
   * @return the parameter sensitivity
   * @throws RuntimeException if the result cannot be calculated
   */
  public CurrencyParameterSensitivities parameterSensitivity(RepoCurveZeroRateSensitivity pointSensitivity) {
    return discountFactors.parameterSensitivity(pointSensitivity.createZeroRateSensitivity());
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code RepoCurveDiscountFactors}.
   * @return the meta-bean, not null
   */
  public static RepoCurveDiscountFactors.Meta meta() {
    return RepoCurveDiscountFactors.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(RepoCurveDiscountFactors.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private RepoCurveDiscountFactors(
      DiscountFactors discountFactors,
      RepoGroup repoGroup) {
    JodaBeanUtils.notNull(discountFactors, "discountFactors");
    JodaBeanUtils.notNull(repoGroup, "repoGroup");
    this.discountFactors = discountFactors;
    this.repoGroup = repoGroup;
  }

  @Override
  public RepoCurveDiscountFactors.Meta metaBean() {
    return RepoCurveDiscountFactors.Meta.INSTANCE;
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
   * Gets the underlying discount factors for a single currency.
   * <p>
   * This contains curve, curve currency, valuation date and day count convention.
   * The discount factor, its point sensitivity and curve sensitivity are computed by this {@code DiscountFactors}.
   * @return the value of the property, not null
   */
  public DiscountFactors getDiscountFactors() {
    return discountFactors;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the repo group.
   * <p>
   * This defines the group that the discount factors are for.
   * @return the value of the property, not null
   */
  public RepoGroup getRepoGroup() {
    return repoGroup;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      RepoCurveDiscountFactors other = (RepoCurveDiscountFactors) obj;
      return JodaBeanUtils.equal(discountFactors, other.discountFactors) &&
          JodaBeanUtils.equal(repoGroup, other.repoGroup);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(discountFactors);
    hash = hash * 31 + JodaBeanUtils.hashCode(repoGroup);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(96);
    buf.append("RepoCurveDiscountFactors{");
    buf.append("discountFactors").append('=').append(discountFactors).append(',').append(' ');
    buf.append("repoGroup").append('=').append(JodaBeanUtils.toString(repoGroup));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code RepoCurveDiscountFactors}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code discountFactors} property.
     */
    private final MetaProperty<DiscountFactors> discountFactors = DirectMetaProperty.ofImmutable(
        this, "discountFactors", RepoCurveDiscountFactors.class, DiscountFactors.class);
    /**
     * The meta-property for the {@code repoGroup} property.
     */
    private final MetaProperty<RepoGroup> repoGroup = DirectMetaProperty.ofImmutable(
        this, "repoGroup", RepoCurveDiscountFactors.class, RepoGroup.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "discountFactors",
        "repoGroup");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case -91613053:  // discountFactors
          return discountFactors;
        case -393084371:  // repoGroup
          return repoGroup;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends RepoCurveDiscountFactors> builder() {
      return new RepoCurveDiscountFactors.Builder();
    }

    @Override
    public Class<? extends RepoCurveDiscountFactors> beanType() {
      return RepoCurveDiscountFactors.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code discountFactors} property.
     * @return the meta-property, not null
     */
    public MetaProperty<DiscountFactors> discountFactors() {
      return discountFactors;
    }

    /**
     * The meta-property for the {@code repoGroup} property.
     * @return the meta-property, not null
     */
    public MetaProperty<RepoGroup> repoGroup() {
      return repoGroup;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -91613053:  // discountFactors
          return ((RepoCurveDiscountFactors) bean).getDiscountFactors();
        case -393084371:  // repoGroup
          return ((RepoCurveDiscountFactors) bean).getRepoGroup();
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
   * The bean-builder for {@code RepoCurveDiscountFactors}.
   */
  private static final class Builder extends DirectPrivateBeanBuilder<RepoCurveDiscountFactors> {

    private DiscountFactors discountFactors;
    private RepoGroup repoGroup;

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
        case -91613053:  // discountFactors
          return discountFactors;
        case -393084371:  // repoGroup
          return repoGroup;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case -91613053:  // discountFactors
          this.discountFactors = (DiscountFactors) newValue;
          break;
        case -393084371:  // repoGroup
          this.repoGroup = (RepoGroup) newValue;
          break;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
      return this;
    }

    @Override
    public RepoCurveDiscountFactors build() {
      return new RepoCurveDiscountFactors(
          discountFactors,
          repoGroup);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(96);
      buf.append("RepoCurveDiscountFactors.Builder{");
      buf.append("discountFactors").append('=').append(JodaBeanUtils.toString(discountFactors)).append(',').append(' ');
      buf.append("repoGroup").append('=').append(JodaBeanUtils.toString(repoGroup));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
