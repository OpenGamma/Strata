/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.fx.type;

import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.fx.FxSwap;
import com.opengamma.strata.product.fx.FxSwapTrade;

/**
 * A market convention for FX swap trades
 * <p>
 * This defines the market convention for a FX swap based on a particular currency pair.
 * <p>
 * The convention is defined by four dates.
 * <ul>
 * <li>Trade date, the date that the trade is agreed
 * <li>Spot date, the base for date calculations, typically 2 business days after the trade date
 * <li>Near date, the date on which the near leg of the swap is exchanged, typically equal to the spot date
 * <li>Far date, the date on which the far leg of the swap is exchanged, typically a number of months or years after the spot date
 * </ul>
 * The period between the spot date and the start/end date is specified by {@link FxSwapTemplate}, not by this convention.
 */
@BeanDefinition
public final class ImmutableFxSwapConvention
    implements FxSwapConvention, ImmutableBean, Serializable {

  /**
   * The currency pair associated with the convention.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final CurrencyPair currencyPair;
  /**
   * The convention name, such as 'EUR/USD', optional with defaulting getter.
   * <p>
   * This will default to the name of the currency pair if not specified.
   */
  @PropertyDefinition(get = "field")
  private final String name;
  /**
   * The offset of the spot value date from the trade date.
   * <p>
   * The offset is applied to the trade date and is typically plus 2 business days
   * in the joint calendar of the two currencies.
   * The start and end date of the FX swap are relative to the spot date.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final DaysAdjustment spotDateOffset;
  /**
   * The business day adjustment to apply to the start and end date, optional with defaulting getter.
   * <p>
   * The start and end date are typically defined as valid business days and thus
   * do not need to be adjusted. If this optional property is present, then the
   * start and end date will be adjusted as defined here.
   * <p>
   * This will default to 'ModifiedFollowing' using the spot date offset calendar if not specified.
   */
  @PropertyDefinition(get = "field")
  private final BusinessDayAdjustment businessDayAdjustment;

  //-------------------------------------------------------------------------
  /**
   * Obtains a convention based on the specified currency pair and spot date offset.
   * <p>
   * Use the {@linkplain #builder() builder} for unusual conventions.
   * 
   * @param currencyPair  the currency pair associated to the convention
   * @param spotDateOffset  the spot date offset
   * @return the convention
   */
  public static ImmutableFxSwapConvention of(CurrencyPair currencyPair, DaysAdjustment spotDateOffset) {
    return ImmutableFxSwapConvention.builder()
        .currencyPair(currencyPair)
        .spotDateOffset(spotDateOffset)
        .build();
  }

  /**
   * Obtains a convention based on the specified currency pair, spot date offset and adjustment.
   * <p>
   * Use the {@linkplain #builder() builder} for unusual conventions.
   * 
   * @param currencyPair  the currency pair associated to the convention
   * @param spotDateOffset  the spot date offset 
   * @param businessDayAdjustment  the business day adjustment to apply
   * @return the convention
   */
  public static ImmutableFxSwapConvention of(
      CurrencyPair currencyPair,
      DaysAdjustment spotDateOffset,
      BusinessDayAdjustment businessDayAdjustment) {

    ArgChecker.notNull(businessDayAdjustment, "businessDayAdjustment");
    return ImmutableFxSwapConvention.builder()
        .currencyPair(currencyPair)
        .spotDateOffset(spotDateOffset)
        .businessDayAdjustment(businessDayAdjustment)
        .build();
  }

  @Override
  public String getName() {
    return name != null ? name : currencyPair.toString();
  }

  /**
   * Gets the business day adjustment to apply to the start and end date,
   * providing a default result if no override specified.
   * <p>
   * The start and end date are typically defined as valid business days and thus
   * do not need to be adjusted. If this optional property is present, then the
   * start and end date will be adjusted as defined here.
   * <p>
   * This will default to 'ModifiedFollowing' using the spot date offset calendar if not specified.
   * 
   * @return the business day adjustment, not null
   */
  public BusinessDayAdjustment getBusinessDayAdjustment() {
    return businessDayAdjustment != null ?
        businessDayAdjustment :
        BusinessDayAdjustment.of(MODIFIED_FOLLOWING, spotDateOffset.getCalendar());
  }

  //-------------------------------------------------------------------------
  @Override
  public FxSwapTrade toTrade(
      TradeInfo tradeInfo,
      LocalDate startDate,
      LocalDate endDate,
      BuySell buySell,
      double notional,
      double nearFxRate,
      double farLegForwardPoints) {

    Optional<LocalDate> tradeDate = tradeInfo.getTradeDate();
    if (tradeDate.isPresent()) {
      ArgChecker.inOrderOrEqual(tradeDate.get(), startDate, "tradeDate", "startDate");
    }
    double amount1 = BuySell.BUY.normalize(notional);
    return FxSwapTrade.builder()
        .info(tradeInfo)
        .product(FxSwap.ofForwardPoints(
            CurrencyAmount.of(currencyPair.getBase(), amount1),
            FxRate.of(currencyPair, nearFxRate),
            farLegForwardPoints,
            startDate,
            endDate,
            getBusinessDayAdjustment()))
        .build();
  }

  @Override
  public String toString() {
    return getName();
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code ImmutableFxSwapConvention}.
   * @return the meta-bean, not null
   */
  public static ImmutableFxSwapConvention.Meta meta() {
    return ImmutableFxSwapConvention.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(ImmutableFxSwapConvention.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static ImmutableFxSwapConvention.Builder builder() {
    return new ImmutableFxSwapConvention.Builder();
  }

  private ImmutableFxSwapConvention(
      CurrencyPair currencyPair,
      String name,
      DaysAdjustment spotDateOffset,
      BusinessDayAdjustment businessDayAdjustment) {
    JodaBeanUtils.notNull(currencyPair, "currencyPair");
    JodaBeanUtils.notNull(spotDateOffset, "spotDateOffset");
    this.currencyPair = currencyPair;
    this.name = name;
    this.spotDateOffset = spotDateOffset;
    this.businessDayAdjustment = businessDayAdjustment;
  }

  @Override
  public ImmutableFxSwapConvention.Meta metaBean() {
    return ImmutableFxSwapConvention.Meta.INSTANCE;
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
   * Gets the currency pair associated with the convention.
   * @return the value of the property, not null
   */
  @Override
  public CurrencyPair getCurrencyPair() {
    return currencyPair;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the offset of the spot value date from the trade date.
   * <p>
   * The offset is applied to the trade date and is typically plus 2 business days
   * in the joint calendar of the two currencies.
   * The start and end date of the FX swap are relative to the spot date.
   * @return the value of the property, not null
   */
  @Override
  public DaysAdjustment getSpotDateOffset() {
    return spotDateOffset;
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
      ImmutableFxSwapConvention other = (ImmutableFxSwapConvention) obj;
      return JodaBeanUtils.equal(currencyPair, other.currencyPair) &&
          JodaBeanUtils.equal(name, other.name) &&
          JodaBeanUtils.equal(spotDateOffset, other.spotDateOffset) &&
          JodaBeanUtils.equal(businessDayAdjustment, other.businessDayAdjustment);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(currencyPair);
    hash = hash * 31 + JodaBeanUtils.hashCode(name);
    hash = hash * 31 + JodaBeanUtils.hashCode(spotDateOffset);
    hash = hash * 31 + JodaBeanUtils.hashCode(businessDayAdjustment);
    return hash;
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code ImmutableFxSwapConvention}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code currencyPair} property.
     */
    private final MetaProperty<CurrencyPair> currencyPair = DirectMetaProperty.ofImmutable(
        this, "currencyPair", ImmutableFxSwapConvention.class, CurrencyPair.class);
    /**
     * The meta-property for the {@code name} property.
     */
    private final MetaProperty<String> name = DirectMetaProperty.ofImmutable(
        this, "name", ImmutableFxSwapConvention.class, String.class);
    /**
     * The meta-property for the {@code spotDateOffset} property.
     */
    private final MetaProperty<DaysAdjustment> spotDateOffset = DirectMetaProperty.ofImmutable(
        this, "spotDateOffset", ImmutableFxSwapConvention.class, DaysAdjustment.class);
    /**
     * The meta-property for the {@code businessDayAdjustment} property.
     */
    private final MetaProperty<BusinessDayAdjustment> businessDayAdjustment = DirectMetaProperty.ofImmutable(
        this, "businessDayAdjustment", ImmutableFxSwapConvention.class, BusinessDayAdjustment.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "currencyPair",
        "name",
        "spotDateOffset",
        "businessDayAdjustment");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 1005147787:  // currencyPair
          return currencyPair;
        case 3373707:  // name
          return name;
        case 746995843:  // spotDateOffset
          return spotDateOffset;
        case -1065319863:  // businessDayAdjustment
          return businessDayAdjustment;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public ImmutableFxSwapConvention.Builder builder() {
      return new ImmutableFxSwapConvention.Builder();
    }

    @Override
    public Class<? extends ImmutableFxSwapConvention> beanType() {
      return ImmutableFxSwapConvention.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code currencyPair} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CurrencyPair> currencyPair() {
      return currencyPair;
    }

    /**
     * The meta-property for the {@code name} property.
     * @return the meta-property, not null
     */
    public MetaProperty<String> name() {
      return name;
    }

    /**
     * The meta-property for the {@code spotDateOffset} property.
     * @return the meta-property, not null
     */
    public MetaProperty<DaysAdjustment> spotDateOffset() {
      return spotDateOffset;
    }

    /**
     * The meta-property for the {@code businessDayAdjustment} property.
     * @return the meta-property, not null
     */
    public MetaProperty<BusinessDayAdjustment> businessDayAdjustment() {
      return businessDayAdjustment;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 1005147787:  // currencyPair
          return ((ImmutableFxSwapConvention) bean).getCurrencyPair();
        case 3373707:  // name
          return ((ImmutableFxSwapConvention) bean).name;
        case 746995843:  // spotDateOffset
          return ((ImmutableFxSwapConvention) bean).getSpotDateOffset();
        case -1065319863:  // businessDayAdjustment
          return ((ImmutableFxSwapConvention) bean).businessDayAdjustment;
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
   * The bean-builder for {@code ImmutableFxSwapConvention}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<ImmutableFxSwapConvention> {

    private CurrencyPair currencyPair;
    private String name;
    private DaysAdjustment spotDateOffset;
    private BusinessDayAdjustment businessDayAdjustment;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(ImmutableFxSwapConvention beanToCopy) {
      this.currencyPair = beanToCopy.getCurrencyPair();
      this.name = beanToCopy.name;
      this.spotDateOffset = beanToCopy.getSpotDateOffset();
      this.businessDayAdjustment = beanToCopy.businessDayAdjustment;
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 1005147787:  // currencyPair
          return currencyPair;
        case 3373707:  // name
          return name;
        case 746995843:  // spotDateOffset
          return spotDateOffset;
        case -1065319863:  // businessDayAdjustment
          return businessDayAdjustment;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 1005147787:  // currencyPair
          this.currencyPair = (CurrencyPair) newValue;
          break;
        case 3373707:  // name
          this.name = (String) newValue;
          break;
        case 746995843:  // spotDateOffset
          this.spotDateOffset = (DaysAdjustment) newValue;
          break;
        case -1065319863:  // businessDayAdjustment
          this.businessDayAdjustment = (BusinessDayAdjustment) newValue;
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
    public ImmutableFxSwapConvention build() {
      return new ImmutableFxSwapConvention(
          currencyPair,
          name,
          spotDateOffset,
          businessDayAdjustment);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the currency pair associated with the convention.
     * @param currencyPair  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder currencyPair(CurrencyPair currencyPair) {
      JodaBeanUtils.notNull(currencyPair, "currencyPair");
      this.currencyPair = currencyPair;
      return this;
    }

    /**
     * Sets the convention name, such as 'EUR/USD', optional with defaulting getter.
     * <p>
     * This will default to the name of the currency pair if not specified.
     * @param name  the new value
     * @return this, for chaining, not null
     */
    public Builder name(String name) {
      this.name = name;
      return this;
    }

    /**
     * Sets the offset of the spot value date from the trade date.
     * <p>
     * The offset is applied to the trade date and is typically plus 2 business days
     * in the joint calendar of the two currencies.
     * The start and end date of the FX swap are relative to the spot date.
     * @param spotDateOffset  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder spotDateOffset(DaysAdjustment spotDateOffset) {
      JodaBeanUtils.notNull(spotDateOffset, "spotDateOffset");
      this.spotDateOffset = spotDateOffset;
      return this;
    }

    /**
     * Sets the business day adjustment to apply to the start and end date, optional with defaulting getter.
     * <p>
     * The start and end date are typically defined as valid business days and thus
     * do not need to be adjusted. If this optional property is present, then the
     * start and end date will be adjusted as defined here.
     * <p>
     * This will default to 'ModifiedFollowing' using the spot date offset calendar if not specified.
     * @param businessDayAdjustment  the new value
     * @return this, for chaining, not null
     */
    public Builder businessDayAdjustment(BusinessDayAdjustment businessDayAdjustment) {
      this.businessDayAdjustment = businessDayAdjustment;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(160);
      buf.append("ImmutableFxSwapConvention.Builder{");
      buf.append("currencyPair").append('=').append(JodaBeanUtils.toString(currencyPair)).append(',').append(' ');
      buf.append("name").append('=').append(JodaBeanUtils.toString(name)).append(',').append(' ');
      buf.append("spotDateOffset").append('=').append(JodaBeanUtils.toString(spotDateOffset)).append(',').append(' ');
      buf.append("businessDayAdjustment").append('=').append(JodaBeanUtils.toString(businessDayAdjustment));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
