/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.finance.rate.deposit;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutableConstructor;
import org.joda.beans.ImmutableValidator;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.collect.ArgChecker;

/**
 * An expanded term deposit, with information calculated ready for pricing.
 * <p>
 * <p>
 * A term deposit is a financial instrument that provides a fixed rate of interest on
 * an amount for a specific term.
 * The principal is signed based on the payment at the end date, when the interest is generated.
 * For example, investing GBP 1,000 for 3 months at a 1% interest rate.
 * <p>
 * An {@code ExpandedTermDeposit} contains information based on holiday calendars.
 * If a holiday calendar changes, the adjusted dates may no longer be correct.
 * Care must be taken when placing the expanded form in a cache or persistence layer.
 */
@BeanDefinition
public final class ExpandedTermDeposit
    implements TermDepositProduct, ImmutableBean, Serializable {

  /**
   * The start date of the deposit.
   * <p>
   * Interest accrues from this date.
   * This date should be a valid business day.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate startDate;
  /**
   * The end date of the deposit.
   * <p>
   * Interest accrues until this date.
   * This date should be a valid business day.
   * This date must be after the start date.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate endDate;
  /**
   * The year fraction that the period represents.
   * <p>
   * The value is usually calculated using a {@link DayCount}.
   * Typically the value will be close to 1 for one year and close to 0.5 for six months.
   * The fraction may be greater than 1, but not less than 0.
   */
  @PropertyDefinition(validate = "ArgChecker.notNegative")
  private final double yearFraction;
  /**
   * The currency.
   * <p>
   * The currency of the deposit.
   */
  @PropertyDefinition(validate = "notNull")
  private final Currency currency;
  /**
   * The notional amount.
   * <p>
   * The amount that is deposited, is a positive signed amount if the term deposit is 'buy',
   * and a negative signed amount if the term deposit is 'sell'.
   */
  @PropertyDefinition
  private final double notional;
  /**
   * The fixed interest rate to be paid.
   * A 5% rate will be expressed as 0.05.
   */
  @PropertyDefinition
  private final double rate;
  /**
   * The accrued interest.
   * <p>
   * The interest is {@code rate * principal * yearFraction}, 
   * thus is a positive signed amount if the term deposit is 'buy',
   * and a negative signed amount if the term deposit is 'sell'.
   */
  private final double interest;  // not a property

  //-------------------------------------------------------------------------
  @ImmutableConstructor
  private ExpandedTermDeposit(ExpandedTermDeposit.Builder builder) {
    JodaBeanUtils.notNull(builder.startDate, "startDate");
    JodaBeanUtils.notNull(builder.endDate, "endDate");
    ArgChecker.inOrderNotEqual(builder.startDate, builder.endDate, "startDate", "endDate");
    ArgChecker.notNegative(builder.yearFraction, "yearFraction");
    JodaBeanUtils.notNull(builder.rate, "rate");
    this.startDate = builder.startDate;
    this.endDate = builder.endDate;
    this.yearFraction = builder.yearFraction;
    this.currency = builder.currency;
    this.notional = builder.notional;
    this.rate = builder.rate;
    interest = (rate * notional * yearFraction);
    validate();
  }

  @ImmutableValidator
  private void validate() {
    ArgChecker.inOrderNotEqual(startDate, endDate, "startDate", "endDate");
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the accrued interest.
   * <p>
   * The interest is {@code rate * principal * yearFraction}.
   * 
   * @return the accrued interest
   */
  public double getInterest() {
    return interest;
  }

  //-------------------------------------------------------------------------
  /**
   * Expands this term deposit, trivially returning {@code this}.
   * 
   * @return this
   */
  @Override
  public ExpandedTermDeposit expand() {
    return this;
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code ExpandedTermDeposit}.
   * @return the meta-bean, not null
   */
  public static ExpandedTermDeposit.Meta meta() {
    return ExpandedTermDeposit.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(ExpandedTermDeposit.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static ExpandedTermDeposit.Builder builder() {
    return new ExpandedTermDeposit.Builder();
  }

  @Override
  public ExpandedTermDeposit.Meta metaBean() {
    return ExpandedTermDeposit.Meta.INSTANCE;
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
   * Gets the start date of the deposit.
   * <p>
   * Interest accrues from this date.
   * This date should be a valid business day.
   * @return the value of the property, not null
   */
  public LocalDate getStartDate() {
    return startDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the end date of the deposit.
   * <p>
   * Interest accrues until this date.
   * This date should be a valid business day.
   * This date must be after the start date.
   * @return the value of the property, not null
   */
  public LocalDate getEndDate() {
    return endDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the year fraction that the period represents.
   * <p>
   * The value is usually calculated using a {@link DayCount}.
   * Typically the value will be close to 1 for one year and close to 0.5 for six months.
   * The fraction may be greater than 1, but not less than 0.
   * @return the value of the property
   */
  public double getYearFraction() {
    return yearFraction;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the currency.
   * <p>
   * The currency of the deposit.
   * @return the value of the property, not null
   */
  public Currency getCurrency() {
    return currency;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the notional amount.
   * <p>
   * The amount that is deposited, is a positive signed amount if the term deposit is 'buy',
   * and a negative signed amount if the term deposit is 'sell'.
   * @return the value of the property
   */
  public double getNotional() {
    return notional;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the fixed interest rate to be paid.
   * A 5% rate will be expressed as 0.05.
   * @return the value of the property
   */
  public double getRate() {
    return rate;
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
      ExpandedTermDeposit other = (ExpandedTermDeposit) obj;
      return JodaBeanUtils.equal(getStartDate(), other.getStartDate()) &&
          JodaBeanUtils.equal(getEndDate(), other.getEndDate()) &&
          JodaBeanUtils.equal(getYearFraction(), other.getYearFraction()) &&
          JodaBeanUtils.equal(getCurrency(), other.getCurrency()) &&
          JodaBeanUtils.equal(getNotional(), other.getNotional()) &&
          JodaBeanUtils.equal(getRate(), other.getRate());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(getStartDate());
    hash = hash * 31 + JodaBeanUtils.hashCode(getEndDate());
    hash = hash * 31 + JodaBeanUtils.hashCode(getYearFraction());
    hash = hash * 31 + JodaBeanUtils.hashCode(getCurrency());
    hash = hash * 31 + JodaBeanUtils.hashCode(getNotional());
    hash = hash * 31 + JodaBeanUtils.hashCode(getRate());
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(224);
    buf.append("ExpandedTermDeposit{");
    buf.append("startDate").append('=').append(getStartDate()).append(',').append(' ');
    buf.append("endDate").append('=').append(getEndDate()).append(',').append(' ');
    buf.append("yearFraction").append('=').append(getYearFraction()).append(',').append(' ');
    buf.append("currency").append('=').append(getCurrency()).append(',').append(' ');
    buf.append("notional").append('=').append(getNotional()).append(',').append(' ');
    buf.append("rate").append('=').append(JodaBeanUtils.toString(getRate()));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code ExpandedTermDeposit}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code startDate} property.
     */
    private final MetaProperty<LocalDate> startDate = DirectMetaProperty.ofImmutable(
        this, "startDate", ExpandedTermDeposit.class, LocalDate.class);
    /**
     * The meta-property for the {@code endDate} property.
     */
    private final MetaProperty<LocalDate> endDate = DirectMetaProperty.ofImmutable(
        this, "endDate", ExpandedTermDeposit.class, LocalDate.class);
    /**
     * The meta-property for the {@code yearFraction} property.
     */
    private final MetaProperty<Double> yearFraction = DirectMetaProperty.ofImmutable(
        this, "yearFraction", ExpandedTermDeposit.class, Double.TYPE);
    /**
     * The meta-property for the {@code currency} property.
     */
    private final MetaProperty<Currency> currency = DirectMetaProperty.ofImmutable(
        this, "currency", ExpandedTermDeposit.class, Currency.class);
    /**
     * The meta-property for the {@code notional} property.
     */
    private final MetaProperty<Double> notional = DirectMetaProperty.ofImmutable(
        this, "notional", ExpandedTermDeposit.class, Double.TYPE);
    /**
     * The meta-property for the {@code rate} property.
     */
    private final MetaProperty<Double> rate = DirectMetaProperty.ofImmutable(
        this, "rate", ExpandedTermDeposit.class, Double.TYPE);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "startDate",
        "endDate",
        "yearFraction",
        "currency",
        "notional",
        "rate");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case -2129778896:  // startDate
          return startDate;
        case -1607727319:  // endDate
          return endDate;
        case -1731780257:  // yearFraction
          return yearFraction;
        case 575402001:  // currency
          return currency;
        case 1585636160:  // notional
          return notional;
        case 3493088:  // rate
          return rate;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public ExpandedTermDeposit.Builder builder() {
      return new ExpandedTermDeposit.Builder();
    }

    @Override
    public Class<? extends ExpandedTermDeposit> beanType() {
      return ExpandedTermDeposit.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code startDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> startDate() {
      return startDate;
    }

    /**
     * The meta-property for the {@code endDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> endDate() {
      return endDate;
    }

    /**
     * The meta-property for the {@code yearFraction} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> yearFraction() {
      return yearFraction;
    }

    /**
     * The meta-property for the {@code currency} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Currency> currency() {
      return currency;
    }

    /**
     * The meta-property for the {@code notional} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> notional() {
      return notional;
    }

    /**
     * The meta-property for the {@code rate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> rate() {
      return rate;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -2129778896:  // startDate
          return ((ExpandedTermDeposit) bean).getStartDate();
        case -1607727319:  // endDate
          return ((ExpandedTermDeposit) bean).getEndDate();
        case -1731780257:  // yearFraction
          return ((ExpandedTermDeposit) bean).getYearFraction();
        case 575402001:  // currency
          return ((ExpandedTermDeposit) bean).getCurrency();
        case 1585636160:  // notional
          return ((ExpandedTermDeposit) bean).getNotional();
        case 3493088:  // rate
          return ((ExpandedTermDeposit) bean).getRate();
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
   * The bean-builder for {@code ExpandedTermDeposit}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<ExpandedTermDeposit> {

    private LocalDate startDate;
    private LocalDate endDate;
    private double yearFraction;
    private Currency currency;
    private double notional;
    private double rate;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(ExpandedTermDeposit beanToCopy) {
      this.startDate = beanToCopy.getStartDate();
      this.endDate = beanToCopy.getEndDate();
      this.yearFraction = beanToCopy.getYearFraction();
      this.currency = beanToCopy.getCurrency();
      this.notional = beanToCopy.getNotional();
      this.rate = beanToCopy.getRate();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case -2129778896:  // startDate
          return startDate;
        case -1607727319:  // endDate
          return endDate;
        case -1731780257:  // yearFraction
          return yearFraction;
        case 575402001:  // currency
          return currency;
        case 1585636160:  // notional
          return notional;
        case 3493088:  // rate
          return rate;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case -2129778896:  // startDate
          this.startDate = (LocalDate) newValue;
          break;
        case -1607727319:  // endDate
          this.endDate = (LocalDate) newValue;
          break;
        case -1731780257:  // yearFraction
          this.yearFraction = (Double) newValue;
          break;
        case 575402001:  // currency
          this.currency = (Currency) newValue;
          break;
        case 1585636160:  // notional
          this.notional = (Double) newValue;
          break;
        case 3493088:  // rate
          this.rate = (Double) newValue;
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
    public ExpandedTermDeposit build() {
      return new ExpandedTermDeposit(this);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the {@code startDate} property in the builder.
     * @param startDate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder startDate(LocalDate startDate) {
      JodaBeanUtils.notNull(startDate, "startDate");
      this.startDate = startDate;
      return this;
    }

    /**
     * Sets the {@code endDate} property in the builder.
     * @param endDate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder endDate(LocalDate endDate) {
      JodaBeanUtils.notNull(endDate, "endDate");
      this.endDate = endDate;
      return this;
    }

    /**
     * Sets the {@code yearFraction} property in the builder.
     * @param yearFraction  the new value
     * @return this, for chaining, not null
     */
    public Builder yearFraction(double yearFraction) {
      ArgChecker.notNegative(yearFraction, "yearFraction");
      this.yearFraction = yearFraction;
      return this;
    }

    /**
     * Sets the {@code currency} property in the builder.
     * @param currency  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder currency(Currency currency) {
      JodaBeanUtils.notNull(currency, "currency");
      this.currency = currency;
      return this;
    }

    /**
     * Sets the {@code notional} property in the builder.
     * @param notional  the new value
     * @return this, for chaining, not null
     */
    public Builder notional(double notional) {
      this.notional = notional;
      return this;
    }

    /**
     * Sets the {@code rate} property in the builder.
     * @param rate  the new value
     * @return this, for chaining, not null
     */
    public Builder rate(double rate) {
      this.rate = rate;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(224);
      buf.append("ExpandedTermDeposit.Builder{");
      buf.append("startDate").append('=').append(JodaBeanUtils.toString(startDate)).append(',').append(' ');
      buf.append("endDate").append('=').append(JodaBeanUtils.toString(endDate)).append(',').append(' ');
      buf.append("yearFraction").append('=').append(JodaBeanUtils.toString(yearFraction)).append(',').append(' ');
      buf.append("currency").append('=').append(JodaBeanUtils.toString(currency)).append(',').append(' ');
      buf.append("notional").append('=').append(JodaBeanUtils.toString(notional)).append(',').append(' ');
      buf.append("rate").append('=').append(JodaBeanUtils.toString(rate));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
