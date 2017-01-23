/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.credit;

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

import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.pricer.ZeroRateSensitivity;

/**
 * The legal entity survival probabilities. 
 * <p>
 * This represents the survival probabilities of a legal entity for a single currency.
 */
@BeanDefinition(builderScope = "private")
public final class LegalEntitySurvivalProbabilities
    implements ImmutableBean, Serializable {

  /**
   * The legal entity identifier.
   * <p>
   * This identifier is used for the reference legal entity of a credit derivative.
   */
  @PropertyDefinition(validate = "notNull")
  private final StandardId legalEntityId;
  /**
   * The underlying curve.
   * <p>
   * The metadata of the curve must define a day count.
   */
  @PropertyDefinition(validate = "notNull")
  private final CreditDiscountFactors survivalProbabilities;

  //-------------------------------------------------------------------------
  /**
   * Creates an instance.
   * 
   * @param legalEntityId  the legal entity ID
   * @param survivalProbabilities  the survival probabilities
   * @return the instance
   */
  public static LegalEntitySurvivalProbabilities of(StandardId legalEntityId, CreditDiscountFactors survivalProbabilities) {
    return new LegalEntitySurvivalProbabilities(legalEntityId, survivalProbabilities);
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the currency.
   * <p>
   * The currency that survival probabilities are provided for.
   * 
   * @return the currency
   */
  public Currency getCurrency() {
    return survivalProbabilities.getCurrency();
  }

  /**
   * Gets the valuation date.
   * <p>
   * The raw data in this provider is calibrated for this date.
   * 
   * @return the valuation date
   */
  public LocalDate getValuationDate() {
    return survivalProbabilities.getValuationDate();
  }

  /**
   * Obtains the parameter keys of the underlying curve.
   * 
   * @return the parameter keys
   */
  public DoubleArray getParameterKeys() {
    return survivalProbabilities.getParameterKeys();
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the survival probability for the specified date.
   * <p>
   * If the valuation date is on the specified date, the survival probability is 1.
   * 
   * @param date  the date
   * @return the survival probability
   * @throws RuntimeException if the value cannot be obtained
   */
  public double survivalProbability(LocalDate date) {
    return survivalProbabilities.discountFactor(date);
  }

  /**
   * Gets the continuously compounded zero hazard rate for specified year fraction.
   * 
   * @param yearFraction  the year fraction 
   * @return the zero hazard rate
   * @throws RuntimeException if the value cannot be obtained
   */
  public double zeroRate(double yearFraction) {
    return survivalProbabilities.zeroRate(yearFraction);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the zero rate point sensitivity at the specified date.
   * <p>
   * This returns a sensitivity instance referring to the zero hazard rate sensitivity of the
   * points that were queried in the market data.
   * The sensitivity typically has the value {@code (-survivalProbability * yearFraction)}.
   * The sensitivity refers to the result of {@link #survivalProbability(LocalDate)}.
   * 
   * @param date  the date
   * @return the point sensitivity of the zero rate
   * @throws RuntimeException if the result cannot be calculated
   */
  public CreditCurveZeroRateSensitivity zeroRatePointSensitivity(LocalDate date) {
    return zeroRatePointSensitivity(date, getCurrency());
  }

  /**
   * Calculates the zero rate point sensitivity at the specified year fraction.
   * <p>
   * This returns a sensitivity instance referring to the zero hazard rate sensitivity of the
   * points that were queried in the market data.
   * The sensitivity typically has the value {@code (-survivalProbability * yearFraction)}.
   * The sensitivity refers to the result of {@link #survivalProbability(LocalDate)}.
   * 
   * @param yearFraction  the year fraction
   * @return the point sensitivity of the zero rate
   * @throws RuntimeException if the result cannot be calculated
   */
  public CreditCurveZeroRateSensitivity zeroRatePointSensitivity(double yearFraction) {
    return zeroRatePointSensitivity(yearFraction, getCurrency());
  }

  /**
   * Calculates the zero rate point sensitivity at the specified date specifying the currency of the sensitivity.
   * <p>
   * This returns a sensitivity instance referring to the zero hazard rate sensitivity of the
   * points that were queried in the market data.
   * The sensitivity typically has the value {@code (-survivalProbability * yearFraction)}.
   * The sensitivity refers to the result of {@link #survivalProbability(LocalDate)}.
   * <p>
   * This method allows the currency of the sensitivity to differ from the currency of the market data.
   * 
   * @param date  the date
   * @param sensitivityCurrency  the currency of the sensitivity
   * @return the point sensitivity of the zero rate
   * @throws RuntimeException if the result cannot be calculated
   */
  public CreditCurveZeroRateSensitivity zeroRatePointSensitivity(LocalDate date, Currency sensitivityCurrency) {
    ZeroRateSensitivity zeroRateSensitivity = survivalProbabilities.zeroRatePointSensitivity(date, sensitivityCurrency);
    return CreditCurveZeroRateSensitivity.of(legalEntityId, zeroRateSensitivity);
  }

  /**
   * Calculates the zero rate point sensitivity at the specified year fraction specifying the currency of the sensitivity.
   * <p>
   * This returns a sensitivity instance referring to the zero hazard rate sensitivity of the
   * points that were queried in the market data.
   * The sensitivity typically has the value {@code (-survivalProbability * yearFraction)}.
   * The sensitivity refers to the result of {@link #survivalProbability(LocalDate)}.
   * <p>
   * This method allows the currency of the sensitivity to differ from the currency of the market data.
   * 
   * @param yearFraction  the year fraction
   * @param sensitivityCurrency  the currency of the sensitivity
   * @return the point sensitivity of the zero rate
   * @throws RuntimeException if the result cannot be calculated
   */
  public CreditCurveZeroRateSensitivity zeroRatePointSensitivity(double yearFraction, Currency sensitivityCurrency) {
    ZeroRateSensitivity zeroRateSensitivity = survivalProbabilities.zeroRatePointSensitivity(yearFraction, sensitivityCurrency);
    return CreditCurveZeroRateSensitivity.of(legalEntityId, zeroRateSensitivity);
  }

  /**
   * Calculates the parameter sensitivity from the point sensitivity.
   * <p>
   * This is used to convert a single point sensitivity to parameter sensitivity.
   * The calculation typically involves multiplying the point and unit sensitivities.
   * 
   * @param pointSensitivity  the point sensitivity to convert
   * @return the parameter sensitivity
   * @throws RuntimeException if the result cannot be calculated
   */
  public CurrencyParameterSensitivities parameterSensitivity(CreditCurveZeroRateSensitivity pointSensitivity) {
    return survivalProbabilities.parameterSensitivity(pointSensitivity.toZeroRateSensitivity());
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code LegalEntitySurvivalProbabilities}.
   * @return the meta-bean, not null
   */
  public static LegalEntitySurvivalProbabilities.Meta meta() {
    return LegalEntitySurvivalProbabilities.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(LegalEntitySurvivalProbabilities.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private LegalEntitySurvivalProbabilities(
      StandardId legalEntityId,
      CreditDiscountFactors survivalProbabilities) {
    JodaBeanUtils.notNull(legalEntityId, "legalEntityId");
    JodaBeanUtils.notNull(survivalProbabilities, "survivalProbabilities");
    this.legalEntityId = legalEntityId;
    this.survivalProbabilities = survivalProbabilities;
  }

  @Override
  public LegalEntitySurvivalProbabilities.Meta metaBean() {
    return LegalEntitySurvivalProbabilities.Meta.INSTANCE;
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
   * Gets the underlying curve.
   * <p>
   * The metadata of the curve must define a day count.
   * @return the value of the property, not null
   */
  public CreditDiscountFactors getSurvivalProbabilities() {
    return survivalProbabilities;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      LegalEntitySurvivalProbabilities other = (LegalEntitySurvivalProbabilities) obj;
      return JodaBeanUtils.equal(legalEntityId, other.legalEntityId) &&
          JodaBeanUtils.equal(survivalProbabilities, other.survivalProbabilities);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(legalEntityId);
    hash = hash * 31 + JodaBeanUtils.hashCode(survivalProbabilities);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(96);
    buf.append("LegalEntitySurvivalProbabilities{");
    buf.append("legalEntityId").append('=').append(legalEntityId).append(',').append(' ');
    buf.append("survivalProbabilities").append('=').append(JodaBeanUtils.toString(survivalProbabilities));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code LegalEntitySurvivalProbabilities}.
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
        this, "legalEntityId", LegalEntitySurvivalProbabilities.class, StandardId.class);
    /**
     * The meta-property for the {@code survivalProbabilities} property.
     */
    private final MetaProperty<CreditDiscountFactors> survivalProbabilities = DirectMetaProperty.ofImmutable(
        this, "survivalProbabilities", LegalEntitySurvivalProbabilities.class, CreditDiscountFactors.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "legalEntityId",
        "survivalProbabilities");

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
        case -2020275979:  // survivalProbabilities
          return survivalProbabilities;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends LegalEntitySurvivalProbabilities> builder() {
      return new LegalEntitySurvivalProbabilities.Builder();
    }

    @Override
    public Class<? extends LegalEntitySurvivalProbabilities> beanType() {
      return LegalEntitySurvivalProbabilities.class;
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
     * The meta-property for the {@code survivalProbabilities} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CreditDiscountFactors> survivalProbabilities() {
      return survivalProbabilities;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 866287159:  // legalEntityId
          return ((LegalEntitySurvivalProbabilities) bean).getLegalEntityId();
        case -2020275979:  // survivalProbabilities
          return ((LegalEntitySurvivalProbabilities) bean).getSurvivalProbabilities();
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
   * The bean-builder for {@code LegalEntitySurvivalProbabilities}.
   */
  private static final class Builder extends DirectPrivateBeanBuilder<LegalEntitySurvivalProbabilities> {

    private StandardId legalEntityId;
    private CreditDiscountFactors survivalProbabilities;

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
        case -2020275979:  // survivalProbabilities
          return survivalProbabilities;
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
        case -2020275979:  // survivalProbabilities
          this.survivalProbabilities = (CreditDiscountFactors) newValue;
          break;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
      return this;
    }

    @Override
    public LegalEntitySurvivalProbabilities build() {
      return new LegalEntitySurvivalProbabilities(
          legalEntityId,
          survivalProbabilities);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(96);
      buf.append("LegalEntitySurvivalProbabilities.Builder{");
      buf.append("legalEntityId").append('=').append(JodaBeanUtils.toString(legalEntityId)).append(',').append(' ');
      buf.append("survivalProbabilities").append('=').append(JodaBeanUtils.toString(survivalProbabilities));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
