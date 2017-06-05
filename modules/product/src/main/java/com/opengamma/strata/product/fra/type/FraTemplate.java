/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.fra.type;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.Period;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutablePreBuild;
import org.joda.beans.ImmutableValidator;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.ReferenceDataNotFoundException;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.product.TradeTemplate;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.fra.Fra;
import com.opengamma.strata.product.fra.FraTrade;

/**
 * A template for creating a forward rate agreement (FRA) trade.
 * <p>
 * This defines almost all the data necessary to create a {@link FraTrade}.
 * The trade date, notional and fixed rate are required to complete the template and create the trade.
 * As such, it is often possible to get a market price for a trade based on the template.
 * The market price is typically quoted as a bid/ask on the fixed rate.
 * <p>
 * The template is defined by six dates.
 * <ul>
 * <li>Trade date, the date that the trade is agreed
 * <li>Spot date, the base for date calculations, typically 2 business days after the trade date
 * <li>Start date, the date on which the implied deposit starts, typically a number of months after the spot value date
 * <li>End date, the date on which the implied deposit ends, typically a number of months after the spot value date
 * <li>Fixing date, the date on which the index is to be observed, typically 2 business days before the start date
 * <li>Payment date, the date on which payment is made, typically the same as the start date
 * </ul>
 * Some of these dates are specified by the convention embedded within this template.
 */
@BeanDefinition
public final class FraTemplate
    implements TradeTemplate, ImmutableBean, Serializable {

  /**
   * The period between the spot value date and the start date.
   * <p>
   * In a FRA described as '2 x 5', the period to the start date is 2 months.
   */
  @PropertyDefinition(validate = "notNull")
  private final Period periodToStart;
  /**
   * The period between the spot value date and the end date.
   * <p>
   * In a FRA described as '2 x 5', the period to the end date is 5 months.
   * The difference between the start date and the end date typically matches the tenor of the index,
   * however this is not validated.
   * <p>
   * When building, this will default to the period to start plus the tenor of the index if not specified.
   */
  @PropertyDefinition(validate = "notNull")
  private final Period periodToEnd;
  /**
   * The underlying FRA convention.
   * <p>
   * This specifies the market convention of the FRA to be created.
   */
  @PropertyDefinition(validate = "notNull")
  private final FraConvention convention;

  //-------------------------------------------------------------------------
  @ImmutablePreBuild
  private static void preBuild(Builder builder) {
    if (builder.periodToEnd == null && builder.convention != null && builder.periodToStart != null) {
      builder.periodToEnd = builder.periodToStart.plus(builder.convention.getIndex().getTenor().getPeriod());
    }
  }

  @ImmutableValidator
  private void validate() {
    ArgChecker.isFalse(periodToStart.isNegative(), "Period to start must not be negative");
    ArgChecker.isFalse(periodToEnd.isNegative(), "Period to end must not be negative");
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains a template based on the specified period and index.
   * <p>
   * The period from the spot date to the start date is specified.
   * The period from the spot date to the end date will be the period to start
   * plus the tenor of the index.
   * <p>
   * For example, a '2 x 5' FRA has a period to the start date of 2 months.
   * The index will be a 3 month index, such as 'USD-LIBOR-3M'.
   * The period to the end date will be the period to the start date plus the index tenor.
   * 
   * @param periodToStart  the period between the spot date and the start date
   * @param index  the index that defines the market convention
   * @return the template
   */
  public static FraTemplate of(Period periodToStart, IborIndex index) {
    return of(periodToStart, periodToStart.plus(index.getTenor().getPeriod()), FraConvention.of(index));
  }

  /**
   * Obtains a template based on the specified periods and convention.
   * <p>
   * The periods from the spot date to the start date and to the end date are specified.
   * <p>
   * For example, a '2 x 5' FRA has a period to the start date of 2 months and
   * a period to the end date of 5 months.
   * 
   * @param periodToStart  the period between the spot date and the start date
   * @param periodToEnd  the period between the spot date and the end date
   * @param convention  the market convention
   * @return the template
   */
  public static FraTemplate of(Period periodToStart, Period periodToEnd, FraConvention convention) {
    ArgChecker.notNull(periodToStart, "periodToStart");
    ArgChecker.notNull(periodToEnd, "periodToEnd");
    ArgChecker.notNull(convention, "convention");
    return FraTemplate.builder()
        .periodToStart(periodToStart)
        .periodToEnd(periodToEnd)
        .convention(convention)
        .build();
  }

  //-------------------------------------------------------------------------
  /**
   * Creates a trade based on this template.
   * <p>
   * This returns a trade based on the specified date.
   * The notional is unsigned, with buy/sell determining the direction of the trade.
   * If buying the FRA, the floating rate is received from the counterparty, with the fixed rate being paid.
   * If selling the FRA, the floating rate is paid to the counterparty, with the fixed rate being received.
   * 
   * @param tradeDate  the date of the trade
   * @param buySell  the buy/sell flag, see {@link Fra#getBuySell()}
   * @param notional  the notional amount, in the payment currency of the template
   * @param fixedRate  the fixed rate, typically derived from the market
   * @param refData  the reference data, used to resolve the trade dates
   * @return the trade
   * @throws ReferenceDataNotFoundException if an identifier cannot be resolved in the reference data
   */
  public FraTrade createTrade(
      LocalDate tradeDate,
      BuySell buySell,
      double notional,
      double fixedRate,
      ReferenceData refData) {

    return convention.createTrade(tradeDate, periodToStart, periodToEnd, buySell, notional, fixedRate, refData);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code FraTemplate}.
   * @return the meta-bean, not null
   */
  public static FraTemplate.Meta meta() {
    return FraTemplate.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(FraTemplate.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static FraTemplate.Builder builder() {
    return new FraTemplate.Builder();
  }

  private FraTemplate(
      Period periodToStart,
      Period periodToEnd,
      FraConvention convention) {
    JodaBeanUtils.notNull(periodToStart, "periodToStart");
    JodaBeanUtils.notNull(periodToEnd, "periodToEnd");
    JodaBeanUtils.notNull(convention, "convention");
    this.periodToStart = periodToStart;
    this.periodToEnd = periodToEnd;
    this.convention = convention;
    validate();
  }

  @Override
  public FraTemplate.Meta metaBean() {
    return FraTemplate.Meta.INSTANCE;
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
   * Gets the period between the spot value date and the start date.
   * <p>
   * In a FRA described as '2 x 5', the period to the start date is 2 months.
   * @return the value of the property, not null
   */
  public Period getPeriodToStart() {
    return periodToStart;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the period between the spot value date and the end date.
   * <p>
   * In a FRA described as '2 x 5', the period to the end date is 5 months.
   * The difference between the start date and the end date typically matches the tenor of the index,
   * however this is not validated.
   * <p>
   * When building, this will default to the period to start plus the tenor of the index if not specified.
   * @return the value of the property, not null
   */
  public Period getPeriodToEnd() {
    return periodToEnd;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the underlying FRA convention.
   * <p>
   * This specifies the market convention of the FRA to be created.
   * @return the value of the property, not null
   */
  public FraConvention getConvention() {
    return convention;
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
      FraTemplate other = (FraTemplate) obj;
      return JodaBeanUtils.equal(periodToStart, other.periodToStart) &&
          JodaBeanUtils.equal(periodToEnd, other.periodToEnd) &&
          JodaBeanUtils.equal(convention, other.convention);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(periodToStart);
    hash = hash * 31 + JodaBeanUtils.hashCode(periodToEnd);
    hash = hash * 31 + JodaBeanUtils.hashCode(convention);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(128);
    buf.append("FraTemplate{");
    buf.append("periodToStart").append('=').append(periodToStart).append(',').append(' ');
    buf.append("periodToEnd").append('=').append(periodToEnd).append(',').append(' ');
    buf.append("convention").append('=').append(JodaBeanUtils.toString(convention));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code FraTemplate}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code periodToStart} property.
     */
    private final MetaProperty<Period> periodToStart = DirectMetaProperty.ofImmutable(
        this, "periodToStart", FraTemplate.class, Period.class);
    /**
     * The meta-property for the {@code periodToEnd} property.
     */
    private final MetaProperty<Period> periodToEnd = DirectMetaProperty.ofImmutable(
        this, "periodToEnd", FraTemplate.class, Period.class);
    /**
     * The meta-property for the {@code convention} property.
     */
    private final MetaProperty<FraConvention> convention = DirectMetaProperty.ofImmutable(
        this, "convention", FraTemplate.class, FraConvention.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "periodToStart",
        "periodToEnd",
        "convention");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case -574688858:  // periodToStart
          return periodToStart;
        case -970442977:  // periodToEnd
          return periodToEnd;
        case 2039569265:  // convention
          return convention;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public FraTemplate.Builder builder() {
      return new FraTemplate.Builder();
    }

    @Override
    public Class<? extends FraTemplate> beanType() {
      return FraTemplate.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code periodToStart} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Period> periodToStart() {
      return periodToStart;
    }

    /**
     * The meta-property for the {@code periodToEnd} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Period> periodToEnd() {
      return periodToEnd;
    }

    /**
     * The meta-property for the {@code convention} property.
     * @return the meta-property, not null
     */
    public MetaProperty<FraConvention> convention() {
      return convention;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -574688858:  // periodToStart
          return ((FraTemplate) bean).getPeriodToStart();
        case -970442977:  // periodToEnd
          return ((FraTemplate) bean).getPeriodToEnd();
        case 2039569265:  // convention
          return ((FraTemplate) bean).getConvention();
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
   * The bean-builder for {@code FraTemplate}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<FraTemplate> {

    private Period periodToStart;
    private Period periodToEnd;
    private FraConvention convention;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(FraTemplate beanToCopy) {
      this.periodToStart = beanToCopy.getPeriodToStart();
      this.periodToEnd = beanToCopy.getPeriodToEnd();
      this.convention = beanToCopy.getConvention();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case -574688858:  // periodToStart
          return periodToStart;
        case -970442977:  // periodToEnd
          return periodToEnd;
        case 2039569265:  // convention
          return convention;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case -574688858:  // periodToStart
          this.periodToStart = (Period) newValue;
          break;
        case -970442977:  // periodToEnd
          this.periodToEnd = (Period) newValue;
          break;
        case 2039569265:  // convention
          this.convention = (FraConvention) newValue;
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

    /**
     * @deprecated Use Joda-Convert in application code
     */
    @Override
    @Deprecated
    public Builder setString(String propertyName, String value) {
      setString(meta().metaProperty(propertyName), value);
      return this;
    }

    /**
     * @deprecated Use Joda-Convert in application code
     */
    @Override
    @Deprecated
    public Builder setString(MetaProperty<?> property, String value) {
      super.setString(property, value);
      return this;
    }

    /**
     * @deprecated Loop in application code
     */
    @Override
    @Deprecated
    public Builder setAll(Map<String, ? extends Object> propertyValueMap) {
      super.setAll(propertyValueMap);
      return this;
    }

    @Override
    public FraTemplate build() {
      preBuild(this);
      return new FraTemplate(
          periodToStart,
          periodToEnd,
          convention);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the period between the spot value date and the start date.
     * <p>
     * In a FRA described as '2 x 5', the period to the start date is 2 months.
     * @param periodToStart  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder periodToStart(Period periodToStart) {
      JodaBeanUtils.notNull(periodToStart, "periodToStart");
      this.periodToStart = periodToStart;
      return this;
    }

    /**
     * Sets the period between the spot value date and the end date.
     * <p>
     * In a FRA described as '2 x 5', the period to the end date is 5 months.
     * The difference between the start date and the end date typically matches the tenor of the index,
     * however this is not validated.
     * <p>
     * When building, this will default to the period to start plus the tenor of the index if not specified.
     * @param periodToEnd  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder periodToEnd(Period periodToEnd) {
      JodaBeanUtils.notNull(periodToEnd, "periodToEnd");
      this.periodToEnd = periodToEnd;
      return this;
    }

    /**
     * Sets the underlying FRA convention.
     * <p>
     * This specifies the market convention of the FRA to be created.
     * @param convention  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder convention(FraConvention convention) {
      JodaBeanUtils.notNull(convention, "convention");
      this.convention = convention;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(128);
      buf.append("FraTemplate.Builder{");
      buf.append("periodToStart").append('=').append(JodaBeanUtils.toString(periodToStart)).append(',').append(' ');
      buf.append("periodToEnd").append('=').append(JodaBeanUtils.toString(periodToEnd)).append(',').append(' ');
      buf.append("convention").append('=').append(JodaBeanUtils.toString(convention));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
