/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.bond;

import static com.opengamma.strata.collect.Guavate.toImmutableList;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutableDefaults;
import org.joda.beans.ImmutableValidator;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.value.Rounding;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.product.Security;
import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.SecurityInfo;
import com.opengamma.strata.product.TradeInfo;

/**
 * A security representing a futures contract, based on a basket of fixed coupon bonds.
 * <p>
 * A bond future is a financial instrument that is based on the future value of
 * a basket of fixed coupon bonds. The profit or loss of a bond future is settled daily.
 * 
 * <h4>Price</h4>
 * Strata uses <i>decimal prices</i> for bond futures in the trade model, pricers and market data.
 * This is coherent with the pricing of {@link FixedCouponBond}. The bond futures delivery is a bond
 * for an amount computed from the bond future price, a conversion factor and the accrued interest.
 */
@BeanDefinition
public final class BondFutureSecurity
    implements Security, ImmutableBean, Serializable {

  /**
   * The standard security information.
   * <p>
   * This includes the security identifier.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final SecurityInfo info;
  /**
   * The currency that the future is traded in.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final Currency currency;
  /**
   * The basket of deliverable bonds.
   * <p>
   * The underling which will be delivered in the future time is chosen from
   * a basket of underling securities. This must not be empty.
   * <p>
   * All of the underlying bonds must have the same notional and currency.
   */
  @PropertyDefinition(validate = "notEmpty")
  private final ImmutableList<SecurityId> deliveryBasketIds;
  /**
   * The conversion factor for each bond in the basket.
   * <p>
   * The price of each underlying security in the basket is rescaled by the conversion factor.
   * This must not be empty, and its size must be the same as the size of {@code deliveryBasketIds}.
   * <p>
   * All of the underlying bonds must have the same notional and currency.
   */
  @PropertyDefinition(validate = "notEmpty")
  private final ImmutableList<Double> conversionFactors;
  /**
   * The last trading date.
   * <p>
   * The future security is traded until this date.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate lastTradeDate;
  /**
   * The first notice date.
   * <p>
   * The first date on which the delivery of the underlying is authorized.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate firstNoticeDate;
  /**
   * The last notice date.
   * <p>
   * The last date on which the delivery of the underlying is authorized.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate lastNoticeDate;
  /**
   * The first delivery date.
   * <p>
   * The first date on which the underlying is delivered.
   * <p>
   * If not specified, the date will be computed from {@code firstNoticeDate} by using
   * {@code settlementDateOffset} in the first element of the delivery basket.
   */
  @PropertyDefinition(get = "optional")
  private final LocalDate firstDeliveryDate;
  /**
   * The last notice date.
   * <p>
   * The last date on which the underlying is delivered.
   * <p>
   * If not specified, the date will be computed from {@code lastNoticeDate} by using
   * {@code settlementDateOffset} in the first element of the delivery basket.
   */
  @PropertyDefinition(get = "optional")
  private final LocalDate lastDeliveryDate;
  /**
   * The definition of how to round the futures price, defaulted to no rounding.
   * <p>
   * The price is represented in decimal form, not percentage form.
   * As such, the decimal places expressed by the rounding refers to this decimal form.
   * For example, the common market price of 99.7125 for a 0.2875% rate is
   * represented as 0.997125 which has 6 decimal places.
   */
  @PropertyDefinition(validate = "notNull")
  private final Rounding rounding;

  //-------------------------------------------------------------------------
  @ImmutableDefaults
  private static void applyDefaults(Builder builder) {
    builder.rounding(Rounding.none());
  }

  @ImmutableValidator
  private void validate() {
    int size = deliveryBasketIds.size();
    ArgChecker.isTrue(size == conversionFactors.size(),
        "The delivery basket size should be the same as the conversion factor size");
    ArgChecker.inOrderOrEqual(firstNoticeDate, lastNoticeDate, "firstNoticeDate", "lastNoticeDate");
    if (firstDeliveryDate != null && lastDeliveryDate != null) {
      ArgChecker.inOrderOrEqual(firstDeliveryDate, lastDeliveryDate, "firstDeliveryDate", "lastDeliveryDate");
      ArgChecker.inOrderOrEqual(firstNoticeDate, firstDeliveryDate, "firstNoticeDate", "firstDeliveryDate");
      ArgChecker.inOrderOrEqual(lastNoticeDate, lastDeliveryDate, "lastNoticeDate", "lastDeliveryDate");
    }
  }

  //-------------------------------------------------------------------------
  @Override
  public ImmutableSet<SecurityId> getUnderlyingIds() {
    return ImmutableSet.copyOf(deliveryBasketIds);
  }

  //-------------------------------------------------------------------------
  @Override
  public BondFuture createProduct(ReferenceData refData) {
    List<FixedCouponBond> bonds = deliveryBasketIds.stream()
        .map(id -> resolveBond(id, refData))
        .collect(toImmutableList());
    return new BondFuture(
        getSecurityId(),
        bonds,
        conversionFactors,
        lastTradeDate,
        firstNoticeDate,
        lastNoticeDate,
        firstDeliveryDate,
        lastDeliveryDate,
        rounding);
  }

  // resolve an underlying bond
  private FixedCouponBond resolveBond(SecurityId id, ReferenceData refData) {
    Security security = refData.getValue(id);
    if (!(security instanceof FixedCouponBondSecurity)) {
      throw new ClassCastException(Messages.format(
          "{} underlying bond '{}' resolved to '{}' when '{}' was expected",
          BondFutureSecurity.class.getSimpleName(),
          id,
          security.getClass().getSimpleName(),
          FixedCouponBondSecurity.class.getSimpleName()));
    }
    FixedCouponBondSecurity bondSec = (FixedCouponBondSecurity) security;
    return bondSec.createProduct(refData);
  }

  @Override
  public BondFutureTrade createTrade(TradeInfo info, double quantity, double tradePrice, ReferenceData refData) {
    BondFuture product = createProduct(refData);
    return new BondFutureTrade(info, product, quantity, tradePrice);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code BondFutureSecurity}.
   * @return the meta-bean, not null
   */
  public static BondFutureSecurity.Meta meta() {
    return BondFutureSecurity.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(BondFutureSecurity.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static BondFutureSecurity.Builder builder() {
    return new BondFutureSecurity.Builder();
  }

  private BondFutureSecurity(
      SecurityInfo info,
      Currency currency,
      List<SecurityId> deliveryBasketIds,
      List<Double> conversionFactors,
      LocalDate lastTradeDate,
      LocalDate firstNoticeDate,
      LocalDate lastNoticeDate,
      LocalDate firstDeliveryDate,
      LocalDate lastDeliveryDate,
      Rounding rounding) {
    JodaBeanUtils.notNull(info, "info");
    JodaBeanUtils.notNull(currency, "currency");
    JodaBeanUtils.notEmpty(deliveryBasketIds, "deliveryBasketIds");
    JodaBeanUtils.notEmpty(conversionFactors, "conversionFactors");
    JodaBeanUtils.notNull(lastTradeDate, "lastTradeDate");
    JodaBeanUtils.notNull(firstNoticeDate, "firstNoticeDate");
    JodaBeanUtils.notNull(lastNoticeDate, "lastNoticeDate");
    JodaBeanUtils.notNull(rounding, "rounding");
    this.info = info;
    this.currency = currency;
    this.deliveryBasketIds = ImmutableList.copyOf(deliveryBasketIds);
    this.conversionFactors = ImmutableList.copyOf(conversionFactors);
    this.lastTradeDate = lastTradeDate;
    this.firstNoticeDate = firstNoticeDate;
    this.lastNoticeDate = lastNoticeDate;
    this.firstDeliveryDate = firstDeliveryDate;
    this.lastDeliveryDate = lastDeliveryDate;
    this.rounding = rounding;
    validate();
  }

  @Override
  public BondFutureSecurity.Meta metaBean() {
    return BondFutureSecurity.Meta.INSTANCE;
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
   * Gets the standard security information.
   * <p>
   * This includes the security identifier.
   * @return the value of the property, not null
   */
  @Override
  public SecurityInfo getInfo() {
    return info;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the currency that the future is traded in.
   * @return the value of the property, not null
   */
  @Override
  public Currency getCurrency() {
    return currency;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the basket of deliverable bonds.
   * <p>
   * The underling which will be delivered in the future time is chosen from
   * a basket of underling securities. This must not be empty.
   * <p>
   * All of the underlying bonds must have the same notional and currency.
   * @return the value of the property, not empty
   */
  public ImmutableList<SecurityId> getDeliveryBasketIds() {
    return deliveryBasketIds;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the conversion factor for each bond in the basket.
   * <p>
   * The price of each underlying security in the basket is rescaled by the conversion factor.
   * This must not be empty, and its size must be the same as the size of {@code deliveryBasketIds}.
   * <p>
   * All of the underlying bonds must have the same notional and currency.
   * @return the value of the property, not empty
   */
  public ImmutableList<Double> getConversionFactors() {
    return conversionFactors;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the last trading date.
   * <p>
   * The future security is traded until this date.
   * @return the value of the property, not null
   */
  public LocalDate getLastTradeDate() {
    return lastTradeDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the first notice date.
   * <p>
   * The first date on which the delivery of the underlying is authorized.
   * @return the value of the property, not null
   */
  public LocalDate getFirstNoticeDate() {
    return firstNoticeDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the last notice date.
   * <p>
   * The last date on which the delivery of the underlying is authorized.
   * @return the value of the property, not null
   */
  public LocalDate getLastNoticeDate() {
    return lastNoticeDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the first delivery date.
   * <p>
   * The first date on which the underlying is delivered.
   * <p>
   * If not specified, the date will be computed from {@code firstNoticeDate} by using
   * {@code settlementDateOffset} in the first element of the delivery basket.
   * @return the optional value of the property, not null
   */
  public Optional<LocalDate> getFirstDeliveryDate() {
    return Optional.ofNullable(firstDeliveryDate);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the last notice date.
   * <p>
   * The last date on which the underlying is delivered.
   * <p>
   * If not specified, the date will be computed from {@code lastNoticeDate} by using
   * {@code settlementDateOffset} in the first element of the delivery basket.
   * @return the optional value of the property, not null
   */
  public Optional<LocalDate> getLastDeliveryDate() {
    return Optional.ofNullable(lastDeliveryDate);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the definition of how to round the futures price, defaulted to no rounding.
   * <p>
   * The price is represented in decimal form, not percentage form.
   * As such, the decimal places expressed by the rounding refers to this decimal form.
   * For example, the common market price of 99.7125 for a 0.2875% rate is
   * represented as 0.997125 which has 6 decimal places.
   * @return the value of the property, not null
   */
  public Rounding getRounding() {
    return rounding;
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
      BondFutureSecurity other = (BondFutureSecurity) obj;
      return JodaBeanUtils.equal(info, other.info) &&
          JodaBeanUtils.equal(currency, other.currency) &&
          JodaBeanUtils.equal(deliveryBasketIds, other.deliveryBasketIds) &&
          JodaBeanUtils.equal(conversionFactors, other.conversionFactors) &&
          JodaBeanUtils.equal(lastTradeDate, other.lastTradeDate) &&
          JodaBeanUtils.equal(firstNoticeDate, other.firstNoticeDate) &&
          JodaBeanUtils.equal(lastNoticeDate, other.lastNoticeDate) &&
          JodaBeanUtils.equal(firstDeliveryDate, other.firstDeliveryDate) &&
          JodaBeanUtils.equal(lastDeliveryDate, other.lastDeliveryDate) &&
          JodaBeanUtils.equal(rounding, other.rounding);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(info);
    hash = hash * 31 + JodaBeanUtils.hashCode(currency);
    hash = hash * 31 + JodaBeanUtils.hashCode(deliveryBasketIds);
    hash = hash * 31 + JodaBeanUtils.hashCode(conversionFactors);
    hash = hash * 31 + JodaBeanUtils.hashCode(lastTradeDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(firstNoticeDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(lastNoticeDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(firstDeliveryDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(lastDeliveryDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(rounding);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(352);
    buf.append("BondFutureSecurity{");
    buf.append("info").append('=').append(info).append(',').append(' ');
    buf.append("currency").append('=').append(currency).append(',').append(' ');
    buf.append("deliveryBasketIds").append('=').append(deliveryBasketIds).append(',').append(' ');
    buf.append("conversionFactors").append('=').append(conversionFactors).append(',').append(' ');
    buf.append("lastTradeDate").append('=').append(lastTradeDate).append(',').append(' ');
    buf.append("firstNoticeDate").append('=').append(firstNoticeDate).append(',').append(' ');
    buf.append("lastNoticeDate").append('=').append(lastNoticeDate).append(',').append(' ');
    buf.append("firstDeliveryDate").append('=').append(firstDeliveryDate).append(',').append(' ');
    buf.append("lastDeliveryDate").append('=').append(lastDeliveryDate).append(',').append(' ');
    buf.append("rounding").append('=').append(JodaBeanUtils.toString(rounding));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code BondFutureSecurity}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code info} property.
     */
    private final MetaProperty<SecurityInfo> info = DirectMetaProperty.ofImmutable(
        this, "info", BondFutureSecurity.class, SecurityInfo.class);
    /**
     * The meta-property for the {@code currency} property.
     */
    private final MetaProperty<Currency> currency = DirectMetaProperty.ofImmutable(
        this, "currency", BondFutureSecurity.class, Currency.class);
    /**
     * The meta-property for the {@code deliveryBasketIds} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableList<SecurityId>> deliveryBasketIds = DirectMetaProperty.ofImmutable(
        this, "deliveryBasketIds", BondFutureSecurity.class, (Class) ImmutableList.class);
    /**
     * The meta-property for the {@code conversionFactors} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableList<Double>> conversionFactors = DirectMetaProperty.ofImmutable(
        this, "conversionFactors", BondFutureSecurity.class, (Class) ImmutableList.class);
    /**
     * The meta-property for the {@code lastTradeDate} property.
     */
    private final MetaProperty<LocalDate> lastTradeDate = DirectMetaProperty.ofImmutable(
        this, "lastTradeDate", BondFutureSecurity.class, LocalDate.class);
    /**
     * The meta-property for the {@code firstNoticeDate} property.
     */
    private final MetaProperty<LocalDate> firstNoticeDate = DirectMetaProperty.ofImmutable(
        this, "firstNoticeDate", BondFutureSecurity.class, LocalDate.class);
    /**
     * The meta-property for the {@code lastNoticeDate} property.
     */
    private final MetaProperty<LocalDate> lastNoticeDate = DirectMetaProperty.ofImmutable(
        this, "lastNoticeDate", BondFutureSecurity.class, LocalDate.class);
    /**
     * The meta-property for the {@code firstDeliveryDate} property.
     */
    private final MetaProperty<LocalDate> firstDeliveryDate = DirectMetaProperty.ofImmutable(
        this, "firstDeliveryDate", BondFutureSecurity.class, LocalDate.class);
    /**
     * The meta-property for the {@code lastDeliveryDate} property.
     */
    private final MetaProperty<LocalDate> lastDeliveryDate = DirectMetaProperty.ofImmutable(
        this, "lastDeliveryDate", BondFutureSecurity.class, LocalDate.class);
    /**
     * The meta-property for the {@code rounding} property.
     */
    private final MetaProperty<Rounding> rounding = DirectMetaProperty.ofImmutable(
        this, "rounding", BondFutureSecurity.class, Rounding.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "info",
        "currency",
        "deliveryBasketIds",
        "conversionFactors",
        "lastTradeDate",
        "firstNoticeDate",
        "lastNoticeDate",
        "firstDeliveryDate",
        "lastDeliveryDate",
        "rounding");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 3237038:  // info
          return info;
        case 575402001:  // currency
          return currency;
        case -516424322:  // deliveryBasketIds
          return deliveryBasketIds;
        case 1655488270:  // conversionFactors
          return conversionFactors;
        case -1041950404:  // lastTradeDate
          return lastTradeDate;
        case -1085415050:  // firstNoticeDate
          return firstNoticeDate;
        case -1060668964:  // lastNoticeDate
          return lastNoticeDate;
        case 1755448466:  // firstDeliveryDate
          return firstDeliveryDate;
        case -233366664:  // lastDeliveryDate
          return lastDeliveryDate;
        case -142444:  // rounding
          return rounding;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BondFutureSecurity.Builder builder() {
      return new BondFutureSecurity.Builder();
    }

    @Override
    public Class<? extends BondFutureSecurity> beanType() {
      return BondFutureSecurity.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code info} property.
     * @return the meta-property, not null
     */
    public MetaProperty<SecurityInfo> info() {
      return info;
    }

    /**
     * The meta-property for the {@code currency} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Currency> currency() {
      return currency;
    }

    /**
     * The meta-property for the {@code deliveryBasketIds} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableList<SecurityId>> deliveryBasketIds() {
      return deliveryBasketIds;
    }

    /**
     * The meta-property for the {@code conversionFactors} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableList<Double>> conversionFactors() {
      return conversionFactors;
    }

    /**
     * The meta-property for the {@code lastTradeDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> lastTradeDate() {
      return lastTradeDate;
    }

    /**
     * The meta-property for the {@code firstNoticeDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> firstNoticeDate() {
      return firstNoticeDate;
    }

    /**
     * The meta-property for the {@code lastNoticeDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> lastNoticeDate() {
      return lastNoticeDate;
    }

    /**
     * The meta-property for the {@code firstDeliveryDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> firstDeliveryDate() {
      return firstDeliveryDate;
    }

    /**
     * The meta-property for the {@code lastDeliveryDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> lastDeliveryDate() {
      return lastDeliveryDate;
    }

    /**
     * The meta-property for the {@code rounding} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Rounding> rounding() {
      return rounding;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 3237038:  // info
          return ((BondFutureSecurity) bean).getInfo();
        case 575402001:  // currency
          return ((BondFutureSecurity) bean).getCurrency();
        case -516424322:  // deliveryBasketIds
          return ((BondFutureSecurity) bean).getDeliveryBasketIds();
        case 1655488270:  // conversionFactors
          return ((BondFutureSecurity) bean).getConversionFactors();
        case -1041950404:  // lastTradeDate
          return ((BondFutureSecurity) bean).getLastTradeDate();
        case -1085415050:  // firstNoticeDate
          return ((BondFutureSecurity) bean).getFirstNoticeDate();
        case -1060668964:  // lastNoticeDate
          return ((BondFutureSecurity) bean).getLastNoticeDate();
        case 1755448466:  // firstDeliveryDate
          return ((BondFutureSecurity) bean).firstDeliveryDate;
        case -233366664:  // lastDeliveryDate
          return ((BondFutureSecurity) bean).lastDeliveryDate;
        case -142444:  // rounding
          return ((BondFutureSecurity) bean).getRounding();
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
   * The bean-builder for {@code BondFutureSecurity}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<BondFutureSecurity> {

    private SecurityInfo info;
    private Currency currency;
    private List<SecurityId> deliveryBasketIds = ImmutableList.of();
    private List<Double> conversionFactors = ImmutableList.of();
    private LocalDate lastTradeDate;
    private LocalDate firstNoticeDate;
    private LocalDate lastNoticeDate;
    private LocalDate firstDeliveryDate;
    private LocalDate lastDeliveryDate;
    private Rounding rounding;

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
    private Builder(BondFutureSecurity beanToCopy) {
      this.info = beanToCopy.getInfo();
      this.currency = beanToCopy.getCurrency();
      this.deliveryBasketIds = beanToCopy.getDeliveryBasketIds();
      this.conversionFactors = beanToCopy.getConversionFactors();
      this.lastTradeDate = beanToCopy.getLastTradeDate();
      this.firstNoticeDate = beanToCopy.getFirstNoticeDate();
      this.lastNoticeDate = beanToCopy.getLastNoticeDate();
      this.firstDeliveryDate = beanToCopy.firstDeliveryDate;
      this.lastDeliveryDate = beanToCopy.lastDeliveryDate;
      this.rounding = beanToCopy.getRounding();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 3237038:  // info
          return info;
        case 575402001:  // currency
          return currency;
        case -516424322:  // deliveryBasketIds
          return deliveryBasketIds;
        case 1655488270:  // conversionFactors
          return conversionFactors;
        case -1041950404:  // lastTradeDate
          return lastTradeDate;
        case -1085415050:  // firstNoticeDate
          return firstNoticeDate;
        case -1060668964:  // lastNoticeDate
          return lastNoticeDate;
        case 1755448466:  // firstDeliveryDate
          return firstDeliveryDate;
        case -233366664:  // lastDeliveryDate
          return lastDeliveryDate;
        case -142444:  // rounding
          return rounding;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 3237038:  // info
          this.info = (SecurityInfo) newValue;
          break;
        case 575402001:  // currency
          this.currency = (Currency) newValue;
          break;
        case -516424322:  // deliveryBasketIds
          this.deliveryBasketIds = (List<SecurityId>) newValue;
          break;
        case 1655488270:  // conversionFactors
          this.conversionFactors = (List<Double>) newValue;
          break;
        case -1041950404:  // lastTradeDate
          this.lastTradeDate = (LocalDate) newValue;
          break;
        case -1085415050:  // firstNoticeDate
          this.firstNoticeDate = (LocalDate) newValue;
          break;
        case -1060668964:  // lastNoticeDate
          this.lastNoticeDate = (LocalDate) newValue;
          break;
        case 1755448466:  // firstDeliveryDate
          this.firstDeliveryDate = (LocalDate) newValue;
          break;
        case -233366664:  // lastDeliveryDate
          this.lastDeliveryDate = (LocalDate) newValue;
          break;
        case -142444:  // rounding
          this.rounding = (Rounding) newValue;
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
    public BondFutureSecurity build() {
      return new BondFutureSecurity(
          info,
          currency,
          deliveryBasketIds,
          conversionFactors,
          lastTradeDate,
          firstNoticeDate,
          lastNoticeDate,
          firstDeliveryDate,
          lastDeliveryDate,
          rounding);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the standard security information.
     * <p>
     * This includes the security identifier.
     * @param info  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder info(SecurityInfo info) {
      JodaBeanUtils.notNull(info, "info");
      this.info = info;
      return this;
    }

    /**
     * Sets the currency that the future is traded in.
     * @param currency  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder currency(Currency currency) {
      JodaBeanUtils.notNull(currency, "currency");
      this.currency = currency;
      return this;
    }

    /**
     * Sets the basket of deliverable bonds.
     * <p>
     * The underling which will be delivered in the future time is chosen from
     * a basket of underling securities. This must not be empty.
     * <p>
     * All of the underlying bonds must have the same notional and currency.
     * @param deliveryBasketIds  the new value, not empty
     * @return this, for chaining, not null
     */
    public Builder deliveryBasketIds(List<SecurityId> deliveryBasketIds) {
      JodaBeanUtils.notEmpty(deliveryBasketIds, "deliveryBasketIds");
      this.deliveryBasketIds = deliveryBasketIds;
      return this;
    }

    /**
     * Sets the {@code deliveryBasketIds} property in the builder
     * from an array of objects.
     * @param deliveryBasketIds  the new value, not empty
     * @return this, for chaining, not null
     */
    public Builder deliveryBasketIds(SecurityId... deliveryBasketIds) {
      return deliveryBasketIds(ImmutableList.copyOf(deliveryBasketIds));
    }

    /**
     * Sets the conversion factor for each bond in the basket.
     * <p>
     * The price of each underlying security in the basket is rescaled by the conversion factor.
     * This must not be empty, and its size must be the same as the size of {@code deliveryBasketIds}.
     * <p>
     * All of the underlying bonds must have the same notional and currency.
     * @param conversionFactors  the new value, not empty
     * @return this, for chaining, not null
     */
    public Builder conversionFactors(List<Double> conversionFactors) {
      JodaBeanUtils.notEmpty(conversionFactors, "conversionFactors");
      this.conversionFactors = conversionFactors;
      return this;
    }

    /**
     * Sets the {@code conversionFactors} property in the builder
     * from an array of objects.
     * @param conversionFactors  the new value, not empty
     * @return this, for chaining, not null
     */
    public Builder conversionFactors(Double... conversionFactors) {
      return conversionFactors(ImmutableList.copyOf(conversionFactors));
    }

    /**
     * Sets the last trading date.
     * <p>
     * The future security is traded until this date.
     * @param lastTradeDate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder lastTradeDate(LocalDate lastTradeDate) {
      JodaBeanUtils.notNull(lastTradeDate, "lastTradeDate");
      this.lastTradeDate = lastTradeDate;
      return this;
    }

    /**
     * Sets the first notice date.
     * <p>
     * The first date on which the delivery of the underlying is authorized.
     * @param firstNoticeDate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder firstNoticeDate(LocalDate firstNoticeDate) {
      JodaBeanUtils.notNull(firstNoticeDate, "firstNoticeDate");
      this.firstNoticeDate = firstNoticeDate;
      return this;
    }

    /**
     * Sets the last notice date.
     * <p>
     * The last date on which the delivery of the underlying is authorized.
     * @param lastNoticeDate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder lastNoticeDate(LocalDate lastNoticeDate) {
      JodaBeanUtils.notNull(lastNoticeDate, "lastNoticeDate");
      this.lastNoticeDate = lastNoticeDate;
      return this;
    }

    /**
     * Sets the first delivery date.
     * <p>
     * The first date on which the underlying is delivered.
     * <p>
     * If not specified, the date will be computed from {@code firstNoticeDate} by using
     * {@code settlementDateOffset} in the first element of the delivery basket.
     * @param firstDeliveryDate  the new value
     * @return this, for chaining, not null
     */
    public Builder firstDeliveryDate(LocalDate firstDeliveryDate) {
      this.firstDeliveryDate = firstDeliveryDate;
      return this;
    }

    /**
     * Sets the last notice date.
     * <p>
     * The last date on which the underlying is delivered.
     * <p>
     * If not specified, the date will be computed from {@code lastNoticeDate} by using
     * {@code settlementDateOffset} in the first element of the delivery basket.
     * @param lastDeliveryDate  the new value
     * @return this, for chaining, not null
     */
    public Builder lastDeliveryDate(LocalDate lastDeliveryDate) {
      this.lastDeliveryDate = lastDeliveryDate;
      return this;
    }

    /**
     * Sets the definition of how to round the futures price, defaulted to no rounding.
     * <p>
     * The price is represented in decimal form, not percentage form.
     * As such, the decimal places expressed by the rounding refers to this decimal form.
     * For example, the common market price of 99.7125 for a 0.2875% rate is
     * represented as 0.997125 which has 6 decimal places.
     * @param rounding  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder rounding(Rounding rounding) {
      JodaBeanUtils.notNull(rounding, "rounding");
      this.rounding = rounding;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(352);
      buf.append("BondFutureSecurity.Builder{");
      buf.append("info").append('=').append(JodaBeanUtils.toString(info)).append(',').append(' ');
      buf.append("currency").append('=').append(JodaBeanUtils.toString(currency)).append(',').append(' ');
      buf.append("deliveryBasketIds").append('=').append(JodaBeanUtils.toString(deliveryBasketIds)).append(',').append(' ');
      buf.append("conversionFactors").append('=').append(JodaBeanUtils.toString(conversionFactors)).append(',').append(' ');
      buf.append("lastTradeDate").append('=').append(JodaBeanUtils.toString(lastTradeDate)).append(',').append(' ');
      buf.append("firstNoticeDate").append('=').append(JodaBeanUtils.toString(firstNoticeDate)).append(',').append(' ');
      buf.append("lastNoticeDate").append('=').append(JodaBeanUtils.toString(lastNoticeDate)).append(',').append(' ');
      buf.append("firstDeliveryDate").append('=').append(JodaBeanUtils.toString(firstDeliveryDate)).append(',').append(' ');
      buf.append("lastDeliveryDate").append('=').append(JodaBeanUtils.toString(lastDeliveryDate)).append(',').append(' ');
      buf.append("rounding").append('=').append(JodaBeanUtils.toString(rounding));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
