/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.credit.cds;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.joda.beans.Bean;
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

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.data.MarketDataName;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.param.CurrencyParameterSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivity;
import com.opengamma.strata.pricer.ZeroRateSensitivity;
import com.opengamma.strata.pricer.rate.RatesProvider;

/**
 * The rates provider, used to calculate analytic measures.
 * <p>
 * The primary usage of this provider is to price credit default swaps on a legal entity.
 * This includes credit curves, discounting curves and recovery rate curves.
 */
@BeanDefinition
public final class CreditRatesProvider
    implements ImmutableBean, Serializable {

  /**
   * The valuation date.
   * <p>
   * All curves and other data items in this provider are calibrated for this date.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate valuationDate;
  /**
   * The credit curves.
   * <p>
   * The curve data, predicting the survival probability, associated with each legal entity and currency.
   */
  @PropertyDefinition(validate = "notNull", get = "private")
  private final ImmutableMap<Pair<StandardId, Currency>, LegalEntitySurvivalProbabilities> creditCurves;
  /**
   * The discounting curves.
   * <p>
   * The curve data, predicting the discount factor, associated with each currency.
   */
  @PropertyDefinition(validate = "notEmpty", get = "private")
  private final ImmutableMap<Currency, CreditDiscountFactors> discountCurves;
  /**
   * The credit rate curves.
   * <p>
   * The curve date, predicting the recovery rate, associated with each legal entity.
   */
  @PropertyDefinition(validate = "notEmpty", get = "private")
  private final ImmutableMap<StandardId, RecoveryRates> recoveryRateCurves;

  //-------------------------------------------------------------------------
  @ImmutableValidator
  private void validate() {
    for (Entry<Pair<StandardId, Currency>, LegalEntitySurvivalProbabilities> entry : creditCurves.entrySet()) {
      if (!entry.getValue().getValuationDate().isEqual(valuationDate)) {
        throw new IllegalArgumentException("Invalid valuation date for the credit curve: " + entry.getValue());
      }
    }
    for (Entry<Currency, CreditDiscountFactors> entry : discountCurves.entrySet()) {
      if (!entry.getValue().getValuationDate().isEqual(valuationDate)) {
        throw new IllegalArgumentException("Invalid valuation date for the discount curve: " + entry.getValue());
      }
    }
    for (Entry<StandardId, RecoveryRates> entry : recoveryRateCurves.entrySet()) {
      if (!entry.getValue().getValuationDate().isEqual(valuationDate)) {
        throw new IllegalArgumentException("Invalid valuation date for the recovery rate curve: " + entry.getValue());
      }
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the survival probabilities for a standard ID and a currency.
   * <p>
   * If both the standard ID and currency are matched, the relevant {@code LegalEntitySurvivalProbabilities} is returned. 
   * <p>
   * If the valuation date is on the specified date, the survival probability is 1.
   * 
   * @param legalEntityId  the standard ID of legal entity to get the discount factors for
   * @param currency  the currency to get the discount factors for
   * @return the survival probabilities 
   * @throws IllegalArgumentException if the survival probabilities are not available
   */
  public LegalEntitySurvivalProbabilities survivalProbabilities(StandardId legalEntityId, Currency currency) {
    LegalEntitySurvivalProbabilities survivalProbabilities = creditCurves.get(Pair.of(legalEntityId, currency));
    if (survivalProbabilities == null) {
      throw new IllegalArgumentException("Unable to find credit curve: " + legalEntityId + ", " + currency);
    }
    return survivalProbabilities;
  }

  /**
   * Gets the discount factors for a currency. 
   * <p>
   * The discount factor represents the time value of money for the specified currency 
   * when comparing the valuation date to the specified date. 
   * <p>
   * If the valuation date is on the specified date, the discount factor is 1.
   * 
   * @param currency  the currency to get the discount factors for
   * @return the discount factors for the specified currency
   */
  public CreditDiscountFactors discountFactors(Currency currency) {
    CreditDiscountFactors discountFactors = discountCurves.get(currency);
    if (discountFactors == null) {
      throw new IllegalArgumentException("Unable to find discount curve: " + currency);
    }
    return discountFactors;
  }

  /**
   * Gets the recovery rates for a standard ID.
   * <p>
   * If both the standard ID and currency are matched, the relevant {@code RecoveryRates} is returned. 
   * 
   * @param legalEntityId  the standard ID of legal entity to get the discount factors for
   * @return the recovery rates
   * @throws IllegalArgumentException if the recovery rates are not available
   */
  public RecoveryRates recoveryRates(StandardId legalEntityId) {
    RecoveryRates recoveryRates = recoveryRateCurves.get(legalEntityId);
    if (recoveryRates == null) {
      throw new IllegalArgumentException("Unable to find recovery rate curve: " + legalEntityId);
    }
    return recoveryRates;
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the parameter sensitivity.
   * <p>
   * This computes the {@link CurrencyParameterSensitivities} associated with the {@link PointSensitivities}.
   * This corresponds to the projection of the point sensitivity to the curve internal parameters representation.
   * <p>
   * The sensitivities handled here are {@link CreditCurveZeroRateSensitivity}, {@link ZeroRateSensitivity}. 
   * For the other sensitivity objects, use {@link RatesProvider} instead.
   * 
   * @param pointSensitivities  the point sensitivity
   * @return the sensitivity to the curve parameters
   */
  public CurrencyParameterSensitivities parameterSensitivity(PointSensitivities pointSensitivities) {
    CurrencyParameterSensitivities sens = CurrencyParameterSensitivities.empty();
    for (PointSensitivity point : pointSensitivities.getSensitivities()) {
      if (point instanceof CreditCurveZeroRateSensitivity) {
        CreditCurveZeroRateSensitivity pt = (CreditCurveZeroRateSensitivity) point;
        LegalEntitySurvivalProbabilities factors = survivalProbabilities(pt.getLegalEntityId(), pt.getCurveCurrency());
        sens = sens.combinedWith(factors.parameterSensitivity(pt));
      } else if (point instanceof ZeroRateSensitivity) {
        ZeroRateSensitivity pt = (ZeroRateSensitivity) point;
        CreditDiscountFactors factors = discountFactors(pt.getCurveCurrency());
        sens = sens.combinedWith(factors.parameterSensitivity(pt));
      }
    }
    return sens;
  }

  /**
   * Computes the parameter sensitivity for a specific credit curve.
   * <p>
   * The credit curve is specified by {@code legalEntityId} and {@code currency}.
   * 
   * @param pointSensitivities  the point sensitivity
   * @param legalEntityId  the legal entity
   * @param currency  the currency
   * @return the sensitivity to the curve parameters
   */
  public CurrencyParameterSensitivity singleCreditCurveParameterSensitivity(
      PointSensitivities pointSensitivities,
      StandardId legalEntityId,
      Currency currency) {
    CurrencyParameterSensitivities sens = CurrencyParameterSensitivities.empty();
    for (PointSensitivity point : pointSensitivities.getSensitivities()) {
      if (point instanceof CreditCurveZeroRateSensitivity) {
        CreditCurveZeroRateSensitivity pt = (CreditCurveZeroRateSensitivity) point;
        if (pt.getLegalEntityId().equals(legalEntityId) && pt.getCurrency().equals(currency)) {
          LegalEntitySurvivalProbabilities factors = survivalProbabilities(pt.getLegalEntityId(), pt.getCurveCurrency());
          sens = sens.combinedWith(factors.parameterSensitivity(pt));
        }
      }
    }
    ArgChecker.isTrue(sens.size() == 1, "sensitivity must be unique");
    return sens.getSensitivities().get(0);
  }

  //-------------------------------------------------------------------------
  public <T> Optional<T> findData(MarketDataName<T> name) {
    if (name instanceof CurveName) {
      return Stream
          .concat(discountCurves.values().stream(), creditCurves.values().stream().map(cc -> cc.getSurvivalProbabilities()))
          .map(df -> df.findData(name))
          .filter(op -> op.isPresent())
          .map(op -> op.get())
          .findFirst();
    }
    return Optional.empty();
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code CreditRatesProvider}.
   * @return the meta-bean, not null
   */
  public static CreditRatesProvider.Meta meta() {
    return CreditRatesProvider.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(CreditRatesProvider.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static CreditRatesProvider.Builder builder() {
    return new CreditRatesProvider.Builder();
  }

  private CreditRatesProvider(
      LocalDate valuationDate,
      Map<Pair<StandardId, Currency>, LegalEntitySurvivalProbabilities> creditCurves,
      Map<Currency, CreditDiscountFactors> discountCurves,
      Map<StandardId, RecoveryRates> recoveryRateCurves) {
    JodaBeanUtils.notNull(valuationDate, "valuationDate");
    JodaBeanUtils.notNull(creditCurves, "creditCurves");
    JodaBeanUtils.notEmpty(discountCurves, "discountCurves");
    JodaBeanUtils.notEmpty(recoveryRateCurves, "recoveryRateCurves");
    this.valuationDate = valuationDate;
    this.creditCurves = ImmutableMap.copyOf(creditCurves);
    this.discountCurves = ImmutableMap.copyOf(discountCurves);
    this.recoveryRateCurves = ImmutableMap.copyOf(recoveryRateCurves);
    validate();
  }

  @Override
  public CreditRatesProvider.Meta metaBean() {
    return CreditRatesProvider.Meta.INSTANCE;
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
   * Gets the valuation date.
   * <p>
   * All curves and other data items in this provider are calibrated for this date.
   * @return the value of the property, not null
   */
  public LocalDate getValuationDate() {
    return valuationDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the credit curves.
   * <p>
   * The curve data, predicting the survival probability, associated with each legal entity and currency.
   * @return the value of the property, not null
   */
  private ImmutableMap<Pair<StandardId, Currency>, LegalEntitySurvivalProbabilities> getCreditCurves() {
    return creditCurves;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the discounting curves.
   * <p>
   * The curve data, predicting the discount factor, associated with each currency.
   * @return the value of the property, not empty
   */
  private ImmutableMap<Currency, CreditDiscountFactors> getDiscountCurves() {
    return discountCurves;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the credit rate curves.
   * <p>
   * The curve date, predicting the recovery rate, associated with each legal entity.
   * @return the value of the property, not empty
   */
  private ImmutableMap<StandardId, RecoveryRates> getRecoveryRateCurves() {
    return recoveryRateCurves;
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
      CreditRatesProvider other = (CreditRatesProvider) obj;
      return JodaBeanUtils.equal(valuationDate, other.valuationDate) &&
          JodaBeanUtils.equal(creditCurves, other.creditCurves) &&
          JodaBeanUtils.equal(discountCurves, other.discountCurves) &&
          JodaBeanUtils.equal(recoveryRateCurves, other.recoveryRateCurves);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(valuationDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(creditCurves);
    hash = hash * 31 + JodaBeanUtils.hashCode(discountCurves);
    hash = hash * 31 + JodaBeanUtils.hashCode(recoveryRateCurves);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(160);
    buf.append("CreditRatesProvider{");
    buf.append("valuationDate").append('=').append(valuationDate).append(',').append(' ');
    buf.append("creditCurves").append('=').append(creditCurves).append(',').append(' ');
    buf.append("discountCurves").append('=').append(discountCurves).append(',').append(' ');
    buf.append("recoveryRateCurves").append('=').append(JodaBeanUtils.toString(recoveryRateCurves));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code CreditRatesProvider}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code valuationDate} property.
     */
    private final MetaProperty<LocalDate> valuationDate = DirectMetaProperty.ofImmutable(
        this, "valuationDate", CreditRatesProvider.class, LocalDate.class);
    /**
     * The meta-property for the {@code creditCurves} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableMap<Pair<StandardId, Currency>, LegalEntitySurvivalProbabilities>> creditCurves = DirectMetaProperty.ofImmutable(
        this, "creditCurves", CreditRatesProvider.class, (Class) ImmutableMap.class);
    /**
     * The meta-property for the {@code discountCurves} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableMap<Currency, CreditDiscountFactors>> discountCurves = DirectMetaProperty.ofImmutable(
        this, "discountCurves", CreditRatesProvider.class, (Class) ImmutableMap.class);
    /**
     * The meta-property for the {@code recoveryRateCurves} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableMap<StandardId, RecoveryRates>> recoveryRateCurves = DirectMetaProperty.ofImmutable(
        this, "recoveryRateCurves", CreditRatesProvider.class, (Class) ImmutableMap.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "valuationDate",
        "creditCurves",
        "discountCurves",
        "recoveryRateCurves");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 113107279:  // valuationDate
          return valuationDate;
        case -1612130883:  // creditCurves
          return creditCurves;
        case -624113147:  // discountCurves
          return discountCurves;
        case 1744098265:  // recoveryRateCurves
          return recoveryRateCurves;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public CreditRatesProvider.Builder builder() {
      return new CreditRatesProvider.Builder();
    }

    @Override
    public Class<? extends CreditRatesProvider> beanType() {
      return CreditRatesProvider.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code valuationDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> valuationDate() {
      return valuationDate;
    }

    /**
     * The meta-property for the {@code creditCurves} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableMap<Pair<StandardId, Currency>, LegalEntitySurvivalProbabilities>> creditCurves() {
      return creditCurves;
    }

    /**
     * The meta-property for the {@code discountCurves} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableMap<Currency, CreditDiscountFactors>> discountCurves() {
      return discountCurves;
    }

    /**
     * The meta-property for the {@code recoveryRateCurves} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableMap<StandardId, RecoveryRates>> recoveryRateCurves() {
      return recoveryRateCurves;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 113107279:  // valuationDate
          return ((CreditRatesProvider) bean).getValuationDate();
        case -1612130883:  // creditCurves
          return ((CreditRatesProvider) bean).getCreditCurves();
        case -624113147:  // discountCurves
          return ((CreditRatesProvider) bean).getDiscountCurves();
        case 1744098265:  // recoveryRateCurves
          return ((CreditRatesProvider) bean).getRecoveryRateCurves();
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
   * The bean-builder for {@code CreditRatesProvider}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<CreditRatesProvider> {

    private LocalDate valuationDate;
    private Map<Pair<StandardId, Currency>, LegalEntitySurvivalProbabilities> creditCurves = ImmutableMap.of();
    private Map<Currency, CreditDiscountFactors> discountCurves = ImmutableMap.of();
    private Map<StandardId, RecoveryRates> recoveryRateCurves = ImmutableMap.of();

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(CreditRatesProvider beanToCopy) {
      this.valuationDate = beanToCopy.getValuationDate();
      this.creditCurves = beanToCopy.getCreditCurves();
      this.discountCurves = beanToCopy.getDiscountCurves();
      this.recoveryRateCurves = beanToCopy.getRecoveryRateCurves();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 113107279:  // valuationDate
          return valuationDate;
        case -1612130883:  // creditCurves
          return creditCurves;
        case -624113147:  // discountCurves
          return discountCurves;
        case 1744098265:  // recoveryRateCurves
          return recoveryRateCurves;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 113107279:  // valuationDate
          this.valuationDate = (LocalDate) newValue;
          break;
        case -1612130883:  // creditCurves
          this.creditCurves = (Map<Pair<StandardId, Currency>, LegalEntitySurvivalProbabilities>) newValue;
          break;
        case -624113147:  // discountCurves
          this.discountCurves = (Map<Currency, CreditDiscountFactors>) newValue;
          break;
        case 1744098265:  // recoveryRateCurves
          this.recoveryRateCurves = (Map<StandardId, RecoveryRates>) newValue;
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
    public CreditRatesProvider build() {
      return new CreditRatesProvider(
          valuationDate,
          creditCurves,
          discountCurves,
          recoveryRateCurves);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the valuation date.
     * <p>
     * All curves and other data items in this provider are calibrated for this date.
     * @param valuationDate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder valuationDate(LocalDate valuationDate) {
      JodaBeanUtils.notNull(valuationDate, "valuationDate");
      this.valuationDate = valuationDate;
      return this;
    }

    /**
     * Sets the credit curves.
     * <p>
     * The curve data, predicting the survival probability, associated with each legal entity and currency.
     * @param creditCurves  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder creditCurves(Map<Pair<StandardId, Currency>, LegalEntitySurvivalProbabilities> creditCurves) {
      JodaBeanUtils.notNull(creditCurves, "creditCurves");
      this.creditCurves = creditCurves;
      return this;
    }

    /**
     * Sets the discounting curves.
     * <p>
     * The curve data, predicting the discount factor, associated with each currency.
     * @param discountCurves  the new value, not empty
     * @return this, for chaining, not null
     */
    public Builder discountCurves(Map<Currency, CreditDiscountFactors> discountCurves) {
      JodaBeanUtils.notEmpty(discountCurves, "discountCurves");
      this.discountCurves = discountCurves;
      return this;
    }

    /**
     * Sets the credit rate curves.
     * <p>
     * The curve date, predicting the recovery rate, associated with each legal entity.
     * @param recoveryRateCurves  the new value, not empty
     * @return this, for chaining, not null
     */
    public Builder recoveryRateCurves(Map<StandardId, RecoveryRates> recoveryRateCurves) {
      JodaBeanUtils.notEmpty(recoveryRateCurves, "recoveryRateCurves");
      this.recoveryRateCurves = recoveryRateCurves;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(160);
      buf.append("CreditRatesProvider.Builder{");
      buf.append("valuationDate").append('=').append(JodaBeanUtils.toString(valuationDate)).append(',').append(' ');
      buf.append("creditCurves").append('=').append(JodaBeanUtils.toString(creditCurves)).append(',').append(' ');
      buf.append("discountCurves").append('=').append(JodaBeanUtils.toString(discountCurves)).append(',').append(' ');
      buf.append("recoveryRateCurves").append('=').append(JodaBeanUtils.toString(recoveryRateCurves));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
