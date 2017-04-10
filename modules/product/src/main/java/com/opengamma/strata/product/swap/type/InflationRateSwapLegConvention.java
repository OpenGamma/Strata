/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap.type;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.Period;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutableDefaults;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.index.PriceIndex;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.product.common.PayReceive;
import com.opengamma.strata.product.swap.InflationRateCalculation;
import com.opengamma.strata.product.swap.NotionalSchedule;
import com.opengamma.strata.product.swap.PaymentSchedule;
import com.opengamma.strata.product.swap.PriceIndexCalculationMethod;
import com.opengamma.strata.product.swap.RateCalculationSwapLeg;

/**
 * A market convention for the floating leg of rate swap trades based on a price index.
 * <p>
 * This defines the market convention for a floating leg based on the observed value
 * of a Price index such as 'GB-HICP' or 'US-CPI-U'.
 */
@BeanDefinition
public final class InflationRateSwapLegConvention
    implements SwapLegConvention, ImmutableBean, Serializable {

  /**
   * The Price index.
   * <p>
   * The floating rate to be paid is based on this price index
   * It will be a well known price index such as 'GB-HICP'.
   */
  @PropertyDefinition(validate = "notNull")
  private final PriceIndex index;
  /**
   * The positive period between the price index and the accrual date,
   * typically a number of months.
   * <p>
   * A price index is typically published monthly and has a delay before publication.
   * The lag is subtracted from the accrual start and end date to locate the
   * month of the data to be observed.
   * <p>
   * For example, the September data may be published in October or November.
   * A 3 month lag will cause an accrual date in December to be based on the
   * observed data for September, which should be available by then.
   */
  @PropertyDefinition(validate = "notNull")
  private final Period lag;
  /**
   * Reference price index calculation method.
   * <p>
   * This specifies how the reference index calculation occurs.
   * <p>
   * This will default to 'Monthly' if not specified.
   */
  @PropertyDefinition(validate = "notNull")
  private final PriceIndexCalculationMethod indexCalculationMethod;
  /**
   * The flag indicating whether to exchange the notional.
   * <p>
   * If 'true', the notional there is both an initial exchange and a final exchange of notional.
   * <p>
   * This will default to 'false' if not specified.
   */
  @PropertyDefinition
  private final boolean notionalExchange;
  /**
   * The offset of payment from the base date, optional with defaulting getter.
   * <p>
   * The offset is applied to the unadjusted date specified by {@code paymentRelativeTo}.
   * Offset can be based on calendar days or business days.
   * <p>
   * This will default to 'None' if not specified.
   */
  @PropertyDefinition(get = "field")
  private final DaysAdjustment paymentDateOffset;
  /**
   * The business day adjustment to apply to accrual schedule dates.
   * <p>
   * Each date in the calculated schedule is determined without taking into account weekends and holidays.
   * The adjustment specified here is used to convert those dates to valid business days.
   * <p>
   * The start date and end date may have their own business day adjustment rules.
   * If those are not present, then this adjustment is used instead.
   * <p>
   * This will default to 'ModifiedFollowing' using the index fixing calendar if not specified.
   */
  @PropertyDefinition(get = "field")
  private final BusinessDayAdjustment accrualBusinessDayAdjustment;

  //-------------------------------------------------------------------------
  /**
   * Obtains a convention based on the specified index.
   * <p>
   * The standard market convention for an Inflation rate leg is based on the index.
   * Use the {@linkplain #builder() builder} for unusual conventions.
   * 
   * @param index  the index, the market convention values are extracted from the index
   * @param lag  the lag between the price index and the accrual date, typically a number of months
   * @param businessDayAdjustment the business day 
   * @return the convention
   */
  public static InflationRateSwapLegConvention of(
      PriceIndex index,
      Period lag,
      BusinessDayAdjustment businessDayAdjustment) {

    return new InflationRateSwapLegConvention(index, lag, PriceIndexCalculationMethod.MONTHLY, false,
        DaysAdjustment.NONE, businessDayAdjustment);
  }

  //-------------------------------------------------------------------------
  @ImmutableDefaults
  private static void applyDefaults(Builder builder) {
    builder.indexCalculationMethod = PriceIndexCalculationMethod.MONTHLY;
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the currency of the leg from the index.
   * 
   * @return the currency
   */
  public Currency getCurrency() {
    return index.getCurrency();
  }

  //-------------------------------------------------------------------------
  /**
   * Creates a leg based on this convention.
   * <p>
   * This returns a leg based on the specified date.
   * The notional is unsigned, with pay/receive determining the direction of the leg.
   * If the leg is 'Pay', the fixed rate is paid to the counterparty.
   * If the leg is 'Receive', the fixed rate is received from the counterparty.
   *
   * @param startDate  the start date
   * @param endDate  the end date
   * @param payReceive  determines if the leg is to be paid or received
   * @param notional  the business day adjustment to apply to accrual schedule dates
   * @return the leg
   */
  public RateCalculationSwapLeg toLeg(
      LocalDate startDate,
      LocalDate endDate,
      PayReceive payReceive,
      double notional) {

    return RateCalculationSwapLeg
        .builder()
        .payReceive(payReceive)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(startDate)
            .endDate(endDate)
            .frequency(Frequency.TERM)
            .businessDayAdjustment(accrualBusinessDayAdjustment)
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(Frequency.TERM)
            .paymentDateOffset(paymentDateOffset)
            .build())
        .calculation(InflationRateCalculation.builder()
            .index(index)
            .indexCalculationMethod(indexCalculationMethod)
            .lag(lag)
            .build())
        .notionalSchedule(NotionalSchedule.of(getCurrency(), notional))
        .build();
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code InflationRateSwapLegConvention}.
   * @return the meta-bean, not null
   */
  public static InflationRateSwapLegConvention.Meta meta() {
    return InflationRateSwapLegConvention.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(InflationRateSwapLegConvention.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static InflationRateSwapLegConvention.Builder builder() {
    return new InflationRateSwapLegConvention.Builder();
  }

  private InflationRateSwapLegConvention(
      PriceIndex index,
      Period lag,
      PriceIndexCalculationMethod indexCalculationMethod,
      boolean notionalExchange,
      DaysAdjustment paymentDateOffset,
      BusinessDayAdjustment accrualBusinessDayAdjustment) {
    JodaBeanUtils.notNull(index, "index");
    JodaBeanUtils.notNull(lag, "lag");
    JodaBeanUtils.notNull(indexCalculationMethod, "indexCalculationMethod");
    this.index = index;
    this.lag = lag;
    this.indexCalculationMethod = indexCalculationMethod;
    this.notionalExchange = notionalExchange;
    this.paymentDateOffset = paymentDateOffset;
    this.accrualBusinessDayAdjustment = accrualBusinessDayAdjustment;
  }

  @Override
  public InflationRateSwapLegConvention.Meta metaBean() {
    return InflationRateSwapLegConvention.Meta.INSTANCE;
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
   * Gets the Price index.
   * <p>
   * The floating rate to be paid is based on this price index
   * It will be a well known price index such as 'GB-HICP'.
   * @return the value of the property, not null
   */
  public PriceIndex getIndex() {
    return index;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the positive period between the price index and the accrual date,
   * typically a number of months.
   * <p>
   * A price index is typically published monthly and has a delay before publication.
   * The lag is subtracted from the accrual start and end date to locate the
   * month of the data to be observed.
   * <p>
   * For example, the September data may be published in October or November.
   * A 3 month lag will cause an accrual date in December to be based on the
   * observed data for September, which should be available by then.
   * @return the value of the property, not null
   */
  public Period getLag() {
    return lag;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets reference price index calculation method.
   * <p>
   * This specifies how the reference index calculation occurs.
   * <p>
   * This will default to 'Monthly' if not specified.
   * @return the value of the property, not null
   */
  public PriceIndexCalculationMethod getIndexCalculationMethod() {
    return indexCalculationMethod;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the flag indicating whether to exchange the notional.
   * <p>
   * If 'true', the notional there is both an initial exchange and a final exchange of notional.
   * <p>
   * This will default to 'false' if not specified.
   * @return the value of the property
   */
  public boolean isNotionalExchange() {
    return notionalExchange;
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
      InflationRateSwapLegConvention other = (InflationRateSwapLegConvention) obj;
      return JodaBeanUtils.equal(index, other.index) &&
          JodaBeanUtils.equal(lag, other.lag) &&
          JodaBeanUtils.equal(indexCalculationMethod, other.indexCalculationMethod) &&
          (notionalExchange == other.notionalExchange) &&
          JodaBeanUtils.equal(paymentDateOffset, other.paymentDateOffset) &&
          JodaBeanUtils.equal(accrualBusinessDayAdjustment, other.accrualBusinessDayAdjustment);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(index);
    hash = hash * 31 + JodaBeanUtils.hashCode(lag);
    hash = hash * 31 + JodaBeanUtils.hashCode(indexCalculationMethod);
    hash = hash * 31 + JodaBeanUtils.hashCode(notionalExchange);
    hash = hash * 31 + JodaBeanUtils.hashCode(paymentDateOffset);
    hash = hash * 31 + JodaBeanUtils.hashCode(accrualBusinessDayAdjustment);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(224);
    buf.append("InflationRateSwapLegConvention{");
    buf.append("index").append('=').append(index).append(',').append(' ');
    buf.append("lag").append('=').append(lag).append(',').append(' ');
    buf.append("indexCalculationMethod").append('=').append(indexCalculationMethod).append(',').append(' ');
    buf.append("notionalExchange").append('=').append(notionalExchange).append(',').append(' ');
    buf.append("paymentDateOffset").append('=').append(paymentDateOffset).append(',').append(' ');
    buf.append("accrualBusinessDayAdjustment").append('=').append(JodaBeanUtils.toString(accrualBusinessDayAdjustment));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code InflationRateSwapLegConvention}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code index} property.
     */
    private final MetaProperty<PriceIndex> index = DirectMetaProperty.ofImmutable(
        this, "index", InflationRateSwapLegConvention.class, PriceIndex.class);
    /**
     * The meta-property for the {@code lag} property.
     */
    private final MetaProperty<Period> lag = DirectMetaProperty.ofImmutable(
        this, "lag", InflationRateSwapLegConvention.class, Period.class);
    /**
     * The meta-property for the {@code indexCalculationMethod} property.
     */
    private final MetaProperty<PriceIndexCalculationMethod> indexCalculationMethod = DirectMetaProperty.ofImmutable(
        this, "indexCalculationMethod", InflationRateSwapLegConvention.class, PriceIndexCalculationMethod.class);
    /**
     * The meta-property for the {@code notionalExchange} property.
     */
    private final MetaProperty<Boolean> notionalExchange = DirectMetaProperty.ofImmutable(
        this, "notionalExchange", InflationRateSwapLegConvention.class, Boolean.TYPE);
    /**
     * The meta-property for the {@code paymentDateOffset} property.
     */
    private final MetaProperty<DaysAdjustment> paymentDateOffset = DirectMetaProperty.ofImmutable(
        this, "paymentDateOffset", InflationRateSwapLegConvention.class, DaysAdjustment.class);
    /**
     * The meta-property for the {@code accrualBusinessDayAdjustment} property.
     */
    private final MetaProperty<BusinessDayAdjustment> accrualBusinessDayAdjustment = DirectMetaProperty.ofImmutable(
        this, "accrualBusinessDayAdjustment", InflationRateSwapLegConvention.class, BusinessDayAdjustment.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "index",
        "lag",
        "indexCalculationMethod",
        "notionalExchange",
        "paymentDateOffset",
        "accrualBusinessDayAdjustment");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 100346066:  // index
          return index;
        case 106898:  // lag
          return lag;
        case -1409010088:  // indexCalculationMethod
          return indexCalculationMethod;
        case -159410813:  // notionalExchange
          return notionalExchange;
        case -716438393:  // paymentDateOffset
          return paymentDateOffset;
        case 896049114:  // accrualBusinessDayAdjustment
          return accrualBusinessDayAdjustment;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public InflationRateSwapLegConvention.Builder builder() {
      return new InflationRateSwapLegConvention.Builder();
    }

    @Override
    public Class<? extends InflationRateSwapLegConvention> beanType() {
      return InflationRateSwapLegConvention.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code index} property.
     * @return the meta-property, not null
     */
    public MetaProperty<PriceIndex> index() {
      return index;
    }

    /**
     * The meta-property for the {@code lag} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Period> lag() {
      return lag;
    }

    /**
     * The meta-property for the {@code indexCalculationMethod} property.
     * @return the meta-property, not null
     */
    public MetaProperty<PriceIndexCalculationMethod> indexCalculationMethod() {
      return indexCalculationMethod;
    }

    /**
     * The meta-property for the {@code notionalExchange} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Boolean> notionalExchange() {
      return notionalExchange;
    }

    /**
     * The meta-property for the {@code paymentDateOffset} property.
     * @return the meta-property, not null
     */
    public MetaProperty<DaysAdjustment> paymentDateOffset() {
      return paymentDateOffset;
    }

    /**
     * The meta-property for the {@code accrualBusinessDayAdjustment} property.
     * @return the meta-property, not null
     */
    public MetaProperty<BusinessDayAdjustment> accrualBusinessDayAdjustment() {
      return accrualBusinessDayAdjustment;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 100346066:  // index
          return ((InflationRateSwapLegConvention) bean).getIndex();
        case 106898:  // lag
          return ((InflationRateSwapLegConvention) bean).getLag();
        case -1409010088:  // indexCalculationMethod
          return ((InflationRateSwapLegConvention) bean).getIndexCalculationMethod();
        case -159410813:  // notionalExchange
          return ((InflationRateSwapLegConvention) bean).isNotionalExchange();
        case -716438393:  // paymentDateOffset
          return ((InflationRateSwapLegConvention) bean).paymentDateOffset;
        case 896049114:  // accrualBusinessDayAdjustment
          return ((InflationRateSwapLegConvention) bean).accrualBusinessDayAdjustment;
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
   * The bean-builder for {@code InflationRateSwapLegConvention}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<InflationRateSwapLegConvention> {

    private PriceIndex index;
    private Period lag;
    private PriceIndexCalculationMethod indexCalculationMethod;
    private boolean notionalExchange;
    private DaysAdjustment paymentDateOffset;
    private BusinessDayAdjustment accrualBusinessDayAdjustment;

    /**
     * Restricted constructor.
     */
    private Builder() {
      applyDefaults(this);
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(InflationRateSwapLegConvention beanToCopy) {
      this.index = beanToCopy.getIndex();
      this.lag = beanToCopy.getLag();
      this.indexCalculationMethod = beanToCopy.getIndexCalculationMethod();
      this.notionalExchange = beanToCopy.isNotionalExchange();
      this.paymentDateOffset = beanToCopy.paymentDateOffset;
      this.accrualBusinessDayAdjustment = beanToCopy.accrualBusinessDayAdjustment;
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 100346066:  // index
          return index;
        case 106898:  // lag
          return lag;
        case -1409010088:  // indexCalculationMethod
          return indexCalculationMethod;
        case -159410813:  // notionalExchange
          return notionalExchange;
        case -716438393:  // paymentDateOffset
          return paymentDateOffset;
        case 896049114:  // accrualBusinessDayAdjustment
          return accrualBusinessDayAdjustment;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 100346066:  // index
          this.index = (PriceIndex) newValue;
          break;
        case 106898:  // lag
          this.lag = (Period) newValue;
          break;
        case -1409010088:  // indexCalculationMethod
          this.indexCalculationMethod = (PriceIndexCalculationMethod) newValue;
          break;
        case -159410813:  // notionalExchange
          this.notionalExchange = (Boolean) newValue;
          break;
        case -716438393:  // paymentDateOffset
          this.paymentDateOffset = (DaysAdjustment) newValue;
          break;
        case 896049114:  // accrualBusinessDayAdjustment
          this.accrualBusinessDayAdjustment = (BusinessDayAdjustment) newValue;
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
    public InflationRateSwapLegConvention build() {
      return new InflationRateSwapLegConvention(
          index,
          lag,
          indexCalculationMethod,
          notionalExchange,
          paymentDateOffset,
          accrualBusinessDayAdjustment);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the Price index.
     * <p>
     * The floating rate to be paid is based on this price index
     * It will be a well known price index such as 'GB-HICP'.
     * @param index  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder index(PriceIndex index) {
      JodaBeanUtils.notNull(index, "index");
      this.index = index;
      return this;
    }

    /**
     * Sets the positive period between the price index and the accrual date,
     * typically a number of months.
     * <p>
     * A price index is typically published monthly and has a delay before publication.
     * The lag is subtracted from the accrual start and end date to locate the
     * month of the data to be observed.
     * <p>
     * For example, the September data may be published in October or November.
     * A 3 month lag will cause an accrual date in December to be based on the
     * observed data for September, which should be available by then.
     * @param lag  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder lag(Period lag) {
      JodaBeanUtils.notNull(lag, "lag");
      this.lag = lag;
      return this;
    }

    /**
     * Sets reference price index calculation method.
     * <p>
     * This specifies how the reference index calculation occurs.
     * <p>
     * This will default to 'Monthly' if not specified.
     * @param indexCalculationMethod  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder indexCalculationMethod(PriceIndexCalculationMethod indexCalculationMethod) {
      JodaBeanUtils.notNull(indexCalculationMethod, "indexCalculationMethod");
      this.indexCalculationMethod = indexCalculationMethod;
      return this;
    }

    /**
     * Sets the flag indicating whether to exchange the notional.
     * <p>
     * If 'true', the notional there is both an initial exchange and a final exchange of notional.
     * <p>
     * This will default to 'false' if not specified.
     * @param notionalExchange  the new value
     * @return this, for chaining, not null
     */
    public Builder notionalExchange(boolean notionalExchange) {
      this.notionalExchange = notionalExchange;
      return this;
    }

    /**
     * Sets the offset of payment from the base date, optional with defaulting getter.
     * <p>
     * The offset is applied to the unadjusted date specified by {@code paymentRelativeTo}.
     * Offset can be based on calendar days or business days.
     * <p>
     * This will default to 'None' if not specified.
     * @param paymentDateOffset  the new value
     * @return this, for chaining, not null
     */
    public Builder paymentDateOffset(DaysAdjustment paymentDateOffset) {
      this.paymentDateOffset = paymentDateOffset;
      return this;
    }

    /**
     * Sets the business day adjustment to apply to accrual schedule dates.
     * <p>
     * Each date in the calculated schedule is determined without taking into account weekends and holidays.
     * The adjustment specified here is used to convert those dates to valid business days.
     * <p>
     * The start date and end date may have their own business day adjustment rules.
     * If those are not present, then this adjustment is used instead.
     * <p>
     * This will default to 'ModifiedFollowing' using the index fixing calendar if not specified.
     * @param accrualBusinessDayAdjustment  the new value
     * @return this, for chaining, not null
     */
    public Builder accrualBusinessDayAdjustment(BusinessDayAdjustment accrualBusinessDayAdjustment) {
      this.accrualBusinessDayAdjustment = accrualBusinessDayAdjustment;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(224);
      buf.append("InflationRateSwapLegConvention.Builder{");
      buf.append("index").append('=').append(JodaBeanUtils.toString(index)).append(',').append(' ');
      buf.append("lag").append('=').append(JodaBeanUtils.toString(lag)).append(',').append(' ');
      buf.append("indexCalculationMethod").append('=').append(JodaBeanUtils.toString(indexCalculationMethod)).append(',').append(' ');
      buf.append("notionalExchange").append('=').append(JodaBeanUtils.toString(notionalExchange)).append(',').append(' ');
      buf.append("paymentDateOffset").append('=').append(JodaBeanUtils.toString(paymentDateOffset)).append(',').append(' ');
      buf.append("accrualBusinessDayAdjustment").append('=').append(JodaBeanUtils.toString(accrualBusinessDayAdjustment));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
