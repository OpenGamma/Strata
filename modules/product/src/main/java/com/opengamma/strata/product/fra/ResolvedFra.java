/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.fra;

import static com.opengamma.strata.collect.Guavate.toImmutableSet;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutableConstructor;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.product.ResolvedProduct;
import com.opengamma.strata.product.rate.RateComputation;

/**
 * A forward rate agreement (FRA), resolved for pricing.
 * <p>
 * This is the resolved form of {@link Fra} and is an input to the pricers.
 * Applications will typically create a {@code ResolvedFra} from a {@code Fra}
 * using {@link Fra#resolve(ReferenceData)}.
 * <p>
 * A {@code ResolvedFra} is bound to data that changes over time, such as holiday calendars.
 * If the data changes, such as the addition of a new holiday, the resolved form will not be updated.
 * Care must be taken when placing the resolved form in a cache or persistence layer.
 */
@BeanDefinition
public final class ResolvedFra
    implements ResolvedProduct, ImmutableBean, Serializable {

  /**
   * The primary currency.
   * <p>
   * This is the currency of the FRA and the currency that payment is made in.
   * The data model permits this currency to differ from that of the index,
   * however the two are typically the same.
   */
  @PropertyDefinition(validate = "notNull")
  private final Currency currency;
  /**
   * The notional amount.
   * <p>
   * The notional, which is a positive signed amount if the FRA is 'buy',
   * and a negative signed amount if the FRA is 'sell'.
   * <p>
   * The currency of the notional is specified by {@code currency}.
   */
  @PropertyDefinition
  private final double notional;
  /**
   * The date that payment occurs.
   * <p>
   * This is an adjusted date, which should be a valid business day
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate paymentDate;
  /**
   * The start date, which is the effective date of the FRA.
   * <p>
   * This is the first date that interest accrues.
   * <p>
   * This is an adjusted date, which should be a valid business day
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate startDate;
  /**
   * The end date, which is the termination date of the FRA.
   * <p>
   * This is the last day that interest accrues.
   * This date must be after the start date.
   * <p>
   * This is an adjusted date, which should be a valid business day
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate endDate;
  /**
   * The year fraction between the start and end date.
   * <p>
   * The value is usually calculated using a {@link DayCount}.
   * Typically the value will be close to 1 for one year and close to 0.5 for six months.
   * The fraction may be greater than 1, but not less than 0.
   */
  @PropertyDefinition(validate = "ArgChecker.notNegative")
  private final double yearFraction;
  /**
   * The fixed rate of interest.
   * A 5% rate will be expressed as 0.05.
   */
  @PropertyDefinition
  private final double fixedRate;
  /**
   * The floating rate of interest.
   * <p>
   * The floating rate to be paid is based on this index.
   * It will be a well known market index such as 'GBP-LIBOR-3M'.
   */
  @PropertyDefinition(validate = "notNull")
  private final RateComputation floatingRate;
  /**
   * The method to use for discounting.
   * <p>
   * There are different approaches to FRA pricing in the area of discounting.
   * This method specifies the approach for this FRA.
   */
  @PropertyDefinition(validate = "notNull")
  private final FraDiscountingMethod discounting;
  /**
   * The set of indices.
   */
  private final transient ImmutableSet<IborIndex> indices;  // not a property, derived and cached from input data

  //-------------------------------------------------------------------------
  @ImmutableConstructor
  private ResolvedFra(
      Currency currency,
      double notional,
      LocalDate paymentDate,
      LocalDate startDate,
      LocalDate endDate,
      double yearFraction,
      double fixedRate,
      RateComputation floatingRate,
      FraDiscountingMethod discounting) {

    this.currency = ArgChecker.notNull(currency, "currency");
    this.notional = notional;
    this.paymentDate = ArgChecker.notNull(paymentDate, "paymentDate");
    ArgChecker.inOrderNotEqual(startDate, endDate, "startDate", "endDate");
    this.startDate = startDate;
    this.endDate = endDate;
    this.yearFraction = ArgChecker.notNegative(yearFraction, "yearFraction");
    this.fixedRate = fixedRate;
    this.floatingRate = ArgChecker.notNull(floatingRate, "floatingRate");
    this.discounting = ArgChecker.notNull(discounting, "discounting");
    this.indices = buildIndices(floatingRate);
  }

  // collect the set of indices, validating they are IborIndex
  private static ImmutableSet<IborIndex> buildIndices(RateComputation floatingRate) {
    ImmutableSet.Builder<Index> builder = ImmutableSet.builder();
    floatingRate.collectIndices(builder);
    return builder.build().stream()
        .map(index -> IborIndex.class.cast(index))
        .collect(toImmutableSet());
  }

  // ensure standard constructor is invoked
  private Object readResolve() {
    return new ResolvedFra(
        currency, notional, paymentDate, startDate, endDate, yearFraction, fixedRate, floatingRate, discounting);
  }

  //-------------------------------------------------------------------------
  /**
   * Returns the set of indices referred to by the FRA.
   * <p>
   * A swap will typically refer to one index, such as 'GBP-LIBOR-3M'.
   * Occasionally, it will refer to two indices.
   * 
   * @return the set of indices referred to by this FRA
   */
  public Set<IborIndex> allIndices() {
    return indices;
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code ResolvedFra}.
   * @return the meta-bean, not null
   */
  public static ResolvedFra.Meta meta() {
    return ResolvedFra.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(ResolvedFra.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static ResolvedFra.Builder builder() {
    return new ResolvedFra.Builder();
  }

  @Override
  public ResolvedFra.Meta metaBean() {
    return ResolvedFra.Meta.INSTANCE;
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
   * Gets the primary currency.
   * <p>
   * This is the currency of the FRA and the currency that payment is made in.
   * The data model permits this currency to differ from that of the index,
   * however the two are typically the same.
   * @return the value of the property, not null
   */
  public Currency getCurrency() {
    return currency;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the notional amount.
   * <p>
   * The notional, which is a positive signed amount if the FRA is 'buy',
   * and a negative signed amount if the FRA is 'sell'.
   * <p>
   * The currency of the notional is specified by {@code currency}.
   * @return the value of the property
   */
  public double getNotional() {
    return notional;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the date that payment occurs.
   * <p>
   * This is an adjusted date, which should be a valid business day
   * @return the value of the property, not null
   */
  public LocalDate getPaymentDate() {
    return paymentDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the start date, which is the effective date of the FRA.
   * <p>
   * This is the first date that interest accrues.
   * <p>
   * This is an adjusted date, which should be a valid business day
   * @return the value of the property, not null
   */
  public LocalDate getStartDate() {
    return startDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the end date, which is the termination date of the FRA.
   * <p>
   * This is the last day that interest accrues.
   * This date must be after the start date.
   * <p>
   * This is an adjusted date, which should be a valid business day
   * @return the value of the property, not null
   */
  public LocalDate getEndDate() {
    return endDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the year fraction between the start and end date.
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
   * Gets the fixed rate of interest.
   * A 5% rate will be expressed as 0.05.
   * @return the value of the property
   */
  public double getFixedRate() {
    return fixedRate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the floating rate of interest.
   * <p>
   * The floating rate to be paid is based on this index.
   * It will be a well known market index such as 'GBP-LIBOR-3M'.
   * @return the value of the property, not null
   */
  public RateComputation getFloatingRate() {
    return floatingRate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the method to use for discounting.
   * <p>
   * There are different approaches to FRA pricing in the area of discounting.
   * This method specifies the approach for this FRA.
   * @return the value of the property, not null
   */
  public FraDiscountingMethod getDiscounting() {
    return discounting;
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
      ResolvedFra other = (ResolvedFra) obj;
      return JodaBeanUtils.equal(currency, other.currency) &&
          JodaBeanUtils.equal(notional, other.notional) &&
          JodaBeanUtils.equal(paymentDate, other.paymentDate) &&
          JodaBeanUtils.equal(startDate, other.startDate) &&
          JodaBeanUtils.equal(endDate, other.endDate) &&
          JodaBeanUtils.equal(yearFraction, other.yearFraction) &&
          JodaBeanUtils.equal(fixedRate, other.fixedRate) &&
          JodaBeanUtils.equal(floatingRate, other.floatingRate) &&
          JodaBeanUtils.equal(discounting, other.discounting);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(currency);
    hash = hash * 31 + JodaBeanUtils.hashCode(notional);
    hash = hash * 31 + JodaBeanUtils.hashCode(paymentDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(startDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(endDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(yearFraction);
    hash = hash * 31 + JodaBeanUtils.hashCode(fixedRate);
    hash = hash * 31 + JodaBeanUtils.hashCode(floatingRate);
    hash = hash * 31 + JodaBeanUtils.hashCode(discounting);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(320);
    buf.append("ResolvedFra{");
    buf.append("currency").append('=').append(currency).append(',').append(' ');
    buf.append("notional").append('=').append(notional).append(',').append(' ');
    buf.append("paymentDate").append('=').append(paymentDate).append(',').append(' ');
    buf.append("startDate").append('=').append(startDate).append(',').append(' ');
    buf.append("endDate").append('=').append(endDate).append(',').append(' ');
    buf.append("yearFraction").append('=').append(yearFraction).append(',').append(' ');
    buf.append("fixedRate").append('=').append(fixedRate).append(',').append(' ');
    buf.append("floatingRate").append('=').append(floatingRate).append(',').append(' ');
    buf.append("discounting").append('=').append(JodaBeanUtils.toString(discounting));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code ResolvedFra}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code currency} property.
     */
    private final MetaProperty<Currency> currency = DirectMetaProperty.ofImmutable(
        this, "currency", ResolvedFra.class, Currency.class);
    /**
     * The meta-property for the {@code notional} property.
     */
    private final MetaProperty<Double> notional = DirectMetaProperty.ofImmutable(
        this, "notional", ResolvedFra.class, Double.TYPE);
    /**
     * The meta-property for the {@code paymentDate} property.
     */
    private final MetaProperty<LocalDate> paymentDate = DirectMetaProperty.ofImmutable(
        this, "paymentDate", ResolvedFra.class, LocalDate.class);
    /**
     * The meta-property for the {@code startDate} property.
     */
    private final MetaProperty<LocalDate> startDate = DirectMetaProperty.ofImmutable(
        this, "startDate", ResolvedFra.class, LocalDate.class);
    /**
     * The meta-property for the {@code endDate} property.
     */
    private final MetaProperty<LocalDate> endDate = DirectMetaProperty.ofImmutable(
        this, "endDate", ResolvedFra.class, LocalDate.class);
    /**
     * The meta-property for the {@code yearFraction} property.
     */
    private final MetaProperty<Double> yearFraction = DirectMetaProperty.ofImmutable(
        this, "yearFraction", ResolvedFra.class, Double.TYPE);
    /**
     * The meta-property for the {@code fixedRate} property.
     */
    private final MetaProperty<Double> fixedRate = DirectMetaProperty.ofImmutable(
        this, "fixedRate", ResolvedFra.class, Double.TYPE);
    /**
     * The meta-property for the {@code floatingRate} property.
     */
    private final MetaProperty<RateComputation> floatingRate = DirectMetaProperty.ofImmutable(
        this, "floatingRate", ResolvedFra.class, RateComputation.class);
    /**
     * The meta-property for the {@code discounting} property.
     */
    private final MetaProperty<FraDiscountingMethod> discounting = DirectMetaProperty.ofImmutable(
        this, "discounting", ResolvedFra.class, FraDiscountingMethod.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "currency",
        "notional",
        "paymentDate",
        "startDate",
        "endDate",
        "yearFraction",
        "fixedRate",
        "floatingRate",
        "discounting");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 575402001:  // currency
          return currency;
        case 1585636160:  // notional
          return notional;
        case -1540873516:  // paymentDate
          return paymentDate;
        case -2129778896:  // startDate
          return startDate;
        case -1607727319:  // endDate
          return endDate;
        case -1731780257:  // yearFraction
          return yearFraction;
        case 747425396:  // fixedRate
          return fixedRate;
        case -2130225658:  // floatingRate
          return floatingRate;
        case -536441087:  // discounting
          return discounting;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public ResolvedFra.Builder builder() {
      return new ResolvedFra.Builder();
    }

    @Override
    public Class<? extends ResolvedFra> beanType() {
      return ResolvedFra.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
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
     * The meta-property for the {@code paymentDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> paymentDate() {
      return paymentDate;
    }

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
     * The meta-property for the {@code fixedRate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> fixedRate() {
      return fixedRate;
    }

    /**
     * The meta-property for the {@code floatingRate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<RateComputation> floatingRate() {
      return floatingRate;
    }

    /**
     * The meta-property for the {@code discounting} property.
     * @return the meta-property, not null
     */
    public MetaProperty<FraDiscountingMethod> discounting() {
      return discounting;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 575402001:  // currency
          return ((ResolvedFra) bean).getCurrency();
        case 1585636160:  // notional
          return ((ResolvedFra) bean).getNotional();
        case -1540873516:  // paymentDate
          return ((ResolvedFra) bean).getPaymentDate();
        case -2129778896:  // startDate
          return ((ResolvedFra) bean).getStartDate();
        case -1607727319:  // endDate
          return ((ResolvedFra) bean).getEndDate();
        case -1731780257:  // yearFraction
          return ((ResolvedFra) bean).getYearFraction();
        case 747425396:  // fixedRate
          return ((ResolvedFra) bean).getFixedRate();
        case -2130225658:  // floatingRate
          return ((ResolvedFra) bean).getFloatingRate();
        case -536441087:  // discounting
          return ((ResolvedFra) bean).getDiscounting();
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
   * The bean-builder for {@code ResolvedFra}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<ResolvedFra> {

    private Currency currency;
    private double notional;
    private LocalDate paymentDate;
    private LocalDate startDate;
    private LocalDate endDate;
    private double yearFraction;
    private double fixedRate;
    private RateComputation floatingRate;
    private FraDiscountingMethod discounting;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(ResolvedFra beanToCopy) {
      this.currency = beanToCopy.getCurrency();
      this.notional = beanToCopy.getNotional();
      this.paymentDate = beanToCopy.getPaymentDate();
      this.startDate = beanToCopy.getStartDate();
      this.endDate = beanToCopy.getEndDate();
      this.yearFraction = beanToCopy.getYearFraction();
      this.fixedRate = beanToCopy.getFixedRate();
      this.floatingRate = beanToCopy.getFloatingRate();
      this.discounting = beanToCopy.getDiscounting();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 575402001:  // currency
          return currency;
        case 1585636160:  // notional
          return notional;
        case -1540873516:  // paymentDate
          return paymentDate;
        case -2129778896:  // startDate
          return startDate;
        case -1607727319:  // endDate
          return endDate;
        case -1731780257:  // yearFraction
          return yearFraction;
        case 747425396:  // fixedRate
          return fixedRate;
        case -2130225658:  // floatingRate
          return floatingRate;
        case -536441087:  // discounting
          return discounting;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 575402001:  // currency
          this.currency = (Currency) newValue;
          break;
        case 1585636160:  // notional
          this.notional = (Double) newValue;
          break;
        case -1540873516:  // paymentDate
          this.paymentDate = (LocalDate) newValue;
          break;
        case -2129778896:  // startDate
          this.startDate = (LocalDate) newValue;
          break;
        case -1607727319:  // endDate
          this.endDate = (LocalDate) newValue;
          break;
        case -1731780257:  // yearFraction
          this.yearFraction = (Double) newValue;
          break;
        case 747425396:  // fixedRate
          this.fixedRate = (Double) newValue;
          break;
        case -2130225658:  // floatingRate
          this.floatingRate = (RateComputation) newValue;
          break;
        case -536441087:  // discounting
          this.discounting = (FraDiscountingMethod) newValue;
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
    public ResolvedFra build() {
      return new ResolvedFra(
          currency,
          notional,
          paymentDate,
          startDate,
          endDate,
          yearFraction,
          fixedRate,
          floatingRate,
          discounting);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the primary currency.
     * <p>
     * This is the currency of the FRA and the currency that payment is made in.
     * The data model permits this currency to differ from that of the index,
     * however the two are typically the same.
     * @param currency  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder currency(Currency currency) {
      JodaBeanUtils.notNull(currency, "currency");
      this.currency = currency;
      return this;
    }

    /**
     * Sets the notional amount.
     * <p>
     * The notional, which is a positive signed amount if the FRA is 'buy',
     * and a negative signed amount if the FRA is 'sell'.
     * <p>
     * The currency of the notional is specified by {@code currency}.
     * @param notional  the new value
     * @return this, for chaining, not null
     */
    public Builder notional(double notional) {
      this.notional = notional;
      return this;
    }

    /**
     * Sets the date that payment occurs.
     * <p>
     * This is an adjusted date, which should be a valid business day
     * @param paymentDate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder paymentDate(LocalDate paymentDate) {
      JodaBeanUtils.notNull(paymentDate, "paymentDate");
      this.paymentDate = paymentDate;
      return this;
    }

    /**
     * Sets the start date, which is the effective date of the FRA.
     * <p>
     * This is the first date that interest accrues.
     * <p>
     * This is an adjusted date, which should be a valid business day
     * @param startDate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder startDate(LocalDate startDate) {
      JodaBeanUtils.notNull(startDate, "startDate");
      this.startDate = startDate;
      return this;
    }

    /**
     * Sets the end date, which is the termination date of the FRA.
     * <p>
     * This is the last day that interest accrues.
     * This date must be after the start date.
     * <p>
     * This is an adjusted date, which should be a valid business day
     * @param endDate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder endDate(LocalDate endDate) {
      JodaBeanUtils.notNull(endDate, "endDate");
      this.endDate = endDate;
      return this;
    }

    /**
     * Sets the year fraction between the start and end date.
     * <p>
     * The value is usually calculated using a {@link DayCount}.
     * Typically the value will be close to 1 for one year and close to 0.5 for six months.
     * The fraction may be greater than 1, but not less than 0.
     * @param yearFraction  the new value
     * @return this, for chaining, not null
     */
    public Builder yearFraction(double yearFraction) {
      ArgChecker.notNegative(yearFraction, "yearFraction");
      this.yearFraction = yearFraction;
      return this;
    }

    /**
     * Sets the fixed rate of interest.
     * A 5% rate will be expressed as 0.05.
     * @param fixedRate  the new value
     * @return this, for chaining, not null
     */
    public Builder fixedRate(double fixedRate) {
      this.fixedRate = fixedRate;
      return this;
    }

    /**
     * Sets the floating rate of interest.
     * <p>
     * The floating rate to be paid is based on this index.
     * It will be a well known market index such as 'GBP-LIBOR-3M'.
     * @param floatingRate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder floatingRate(RateComputation floatingRate) {
      JodaBeanUtils.notNull(floatingRate, "floatingRate");
      this.floatingRate = floatingRate;
      return this;
    }

    /**
     * Sets the method to use for discounting.
     * <p>
     * There are different approaches to FRA pricing in the area of discounting.
     * This method specifies the approach for this FRA.
     * @param discounting  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder discounting(FraDiscountingMethod discounting) {
      JodaBeanUtils.notNull(discounting, "discounting");
      this.discounting = discounting;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(320);
      buf.append("ResolvedFra.Builder{");
      buf.append("currency").append('=').append(JodaBeanUtils.toString(currency)).append(',').append(' ');
      buf.append("notional").append('=').append(JodaBeanUtils.toString(notional)).append(',').append(' ');
      buf.append("paymentDate").append('=').append(JodaBeanUtils.toString(paymentDate)).append(',').append(' ');
      buf.append("startDate").append('=').append(JodaBeanUtils.toString(startDate)).append(',').append(' ');
      buf.append("endDate").append('=').append(JodaBeanUtils.toString(endDate)).append(',').append(' ');
      buf.append("yearFraction").append('=').append(JodaBeanUtils.toString(yearFraction)).append(',').append(' ');
      buf.append("fixedRate").append('=').append(JodaBeanUtils.toString(fixedRate)).append(',').append(' ');
      buf.append("floatingRate").append('=').append(JodaBeanUtils.toString(floatingRate)).append(',').append(' ');
      buf.append("discounting").append('=').append(JodaBeanUtils.toString(discounting));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
