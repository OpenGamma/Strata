/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.credit;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Stream;

import org.joda.beans.Bean;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.MetaProperty;
import org.joda.beans.gen.BeanDefinition;
import org.joda.beans.gen.ImmutableValidator;
import org.joda.beans.gen.PropertyDefinition;
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

/**
 * The immutable rates provider, used to calculate analytic measures.
 * <p>
 * The primary usage of this provider is to price credit default swaps on a legal entity.
 * This includes credit curves, discounting curves and recovery rate curves.
 */
@BeanDefinition
public final class ImmutableCreditRatesProvider
    implements CreditRatesProvider, ImmutableBean, Serializable {

  /**
   * The valuation date.
   * <p>
   * All curves and other data items in this provider are calibrated for this date.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final LocalDate valuationDate;
  /**
   * The credit curves.
   * <p>
   * The curve data, predicting the survival probability, associated with each legal entity and currency.
   */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableMap<Pair<StandardId, Currency>, LegalEntitySurvivalProbabilities> creditCurves;
  /**
   * The discounting curves.
   * <p>
   * The curve data, predicting the discount factor, associated with each currency.
   */
  @PropertyDefinition(validate = "notEmpty")
  private final ImmutableMap<Currency, CreditDiscountFactors> discountCurves;
  /**
   * The credit rate curves.
   * <p>
   * The curve date, predicting the recovery rate, associated with each legal entity.
   */
  @PropertyDefinition(validate = "notEmpty")
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
  @Override
  public LegalEntitySurvivalProbabilities survivalProbabilities(StandardId legalEntityId, Currency currency) {
    LegalEntitySurvivalProbabilities survivalProbabilities = creditCurves.get(Pair.of(legalEntityId, currency));
    if (survivalProbabilities == null) {
      throw new IllegalArgumentException("Unable to find credit curve: " + legalEntityId + ", " + currency);
    }
    return survivalProbabilities;
  }

  @Override
  public CreditDiscountFactors discountFactors(Currency currency) {
    CreditDiscountFactors discountFactors = discountCurves.get(currency);
    if (discountFactors == null) {
      throw new IllegalArgumentException("Unable to find discount curve: " + currency);
    }
    return discountFactors;
  }

  @Override
  public RecoveryRates recoveryRates(StandardId legalEntityId) {
    RecoveryRates recoveryRates = recoveryRateCurves.get(legalEntityId);
    if (recoveryRates == null) {
      throw new IllegalArgumentException("Unable to find recovery rate curve: " + legalEntityId);
    }
    return recoveryRates;
  }

  //-------------------------------------------------------------------------
  @Override
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

  @Override
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

  @Override
  public CurrencyParameterSensitivity singleDiscountCurveParameterSensitivity(
      PointSensitivities pointSensitivities,
      Currency currency) {

    CurrencyParameterSensitivities sens = CurrencyParameterSensitivities.empty();
    for (PointSensitivity point : pointSensitivities.getSensitivities()) {
      if (point instanceof ZeroRateSensitivity) {
        ZeroRateSensitivity pt = (ZeroRateSensitivity) point;
        if (pt.getCurveCurrency().equals(currency)) {
          CreditDiscountFactors factors = discountFactors(pt.getCurveCurrency());
          sens = sens.combinedWith(factors.parameterSensitivity(pt));
        }
      }
    }
    ArgChecker.isTrue(sens.size() == 1, "sensitivity must be unique");
    return sens.getSensitivities().get(0);
  }

  //-------------------------------------------------------------------------
  @Override
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

  //-------------------------------------------------------------------------
  @Override
  public ImmutableCreditRatesProvider toImmutableCreditRatesProvider() {
    return this;
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code ImmutableCreditRatesProvider}.
   * @return the meta-bean, not null
   */
  public static ImmutableCreditRatesProvider.Meta meta() {
    return ImmutableCreditRatesProvider.Meta.INSTANCE;
  }

  static {
    MetaBean.register(ImmutableCreditRatesProvider.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static ImmutableCreditRatesProvider.Builder builder() {
    return new ImmutableCreditRatesProvider.Builder();
  }

  private ImmutableCreditRatesProvider(
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
  public ImmutableCreditRatesProvider.Meta metaBean() {
    return ImmutableCreditRatesProvider.Meta.INSTANCE;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the valuation date.
   * <p>
   * All curves and other data items in this provider are calibrated for this date.
   * @return the value of the property, not null
   */
  @Override
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
  public ImmutableMap<Pair<StandardId, Currency>, LegalEntitySurvivalProbabilities> getCreditCurves() {
    return creditCurves;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the discounting curves.
   * <p>
   * The curve data, predicting the discount factor, associated with each currency.
   * @return the value of the property, not empty
   */
  public ImmutableMap<Currency, CreditDiscountFactors> getDiscountCurves() {
    return discountCurves;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the credit rate curves.
   * <p>
   * The curve date, predicting the recovery rate, associated with each legal entity.
   * @return the value of the property, not empty
   */
  public ImmutableMap<StandardId, RecoveryRates> getRecoveryRateCurves() {
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
      ImmutableCreditRatesProvider other = (ImmutableCreditRatesProvider) obj;
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
    buf.append("ImmutableCreditRatesProvider{");
    buf.append("valuationDate").append('=').append(JodaBeanUtils.toString(valuationDate)).append(',').append(' ');
    buf.append("creditCurves").append('=').append(JodaBeanUtils.toString(creditCurves)).append(',').append(' ');
    buf.append("discountCurves").append('=').append(JodaBeanUtils.toString(discountCurves)).append(',').append(' ');
    buf.append("recoveryRateCurves").append('=').append(JodaBeanUtils.toString(recoveryRateCurves));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code ImmutableCreditRatesProvider}.
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
        this, "valuationDate", ImmutableCreditRatesProvider.class, LocalDate.class);
    /**
     * The meta-property for the {@code creditCurves} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableMap<Pair<StandardId, Currency>, LegalEntitySurvivalProbabilities>> creditCurves = DirectMetaProperty.ofImmutable(
        this, "creditCurves", ImmutableCreditRatesProvider.class, (Class) ImmutableMap.class);
    /**
     * The meta-property for the {@code discountCurves} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableMap<Currency, CreditDiscountFactors>> discountCurves = DirectMetaProperty.ofImmutable(
        this, "discountCurves", ImmutableCreditRatesProvider.class, (Class) ImmutableMap.class);
    /**
     * The meta-property for the {@code recoveryRateCurves} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableMap<StandardId, RecoveryRates>> recoveryRateCurves = DirectMetaProperty.ofImmutable(
        this, "recoveryRateCurves", ImmutableCreditRatesProvider.class, (Class) ImmutableMap.class);
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
    public ImmutableCreditRatesProvider.Builder builder() {
      return new ImmutableCreditRatesProvider.Builder();
    }

    @Override
    public Class<? extends ImmutableCreditRatesProvider> beanType() {
      return ImmutableCreditRatesProvider.class;
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
          return ((ImmutableCreditRatesProvider) bean).getValuationDate();
        case -1612130883:  // creditCurves
          return ((ImmutableCreditRatesProvider) bean).getCreditCurves();
        case -624113147:  // discountCurves
          return ((ImmutableCreditRatesProvider) bean).getDiscountCurves();
        case 1744098265:  // recoveryRateCurves
          return ((ImmutableCreditRatesProvider) bean).getRecoveryRateCurves();
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
   * The bean-builder for {@code ImmutableCreditRatesProvider}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<ImmutableCreditRatesProvider> {

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
    private Builder(ImmutableCreditRatesProvider beanToCopy) {
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
    public ImmutableCreditRatesProvider build() {
      return new ImmutableCreditRatesProvider(
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
      buf.append("ImmutableCreditRatesProvider.Builder{");
      buf.append("valuationDate").append('=').append(JodaBeanUtils.toString(valuationDate)).append(',').append(' ');
      buf.append("creditCurves").append('=').append(JodaBeanUtils.toString(creditCurves)).append(',').append(' ');
      buf.append("discountCurves").append('=').append(JodaBeanUtils.toString(discountCurves)).append(',').append(' ');
      buf.append("recoveryRateCurves").append('=').append(JodaBeanUtils.toString(recoveryRateCurves));
      buf.append('}');
      return buf.toString();
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
