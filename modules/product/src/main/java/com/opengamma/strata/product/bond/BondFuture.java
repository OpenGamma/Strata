/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
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
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.Resolvable;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.value.Rounding;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.product.SecuritizedProduct;
import com.opengamma.strata.product.SecurityId;

/**
 * A futures contract, based on a basket of fixed coupon bonds.
 * <p>
 * A bond future is a financial instrument that is based on the future value of
 * a basket of fixed coupon bonds. The profit or loss of a bond future is settled daily.
 * This class represents the structure of a single futures contract.
 * 
 * <h4>Price</h4>
 * Strata uses <i>decimal prices</i> for bond futures in the trade model, pricers and market data.
 * This is coherent with the pricing of {@link FixedCouponBond}. The bond futures delivery is a bond
 * for an amount computed from the bond future price, a conversion factor and the accrued interest.
 */
@SuppressWarnings("unchecked")
@BeanDefinition(constructorScope = "package")
public final class BondFuture
    implements SecuritizedProduct, Resolvable<ResolvedBondFuture>, ImmutableBean, Serializable {

  /**
   * The security identifier.
   * <p>
   * This identifier uniquely identifies the security within the system.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final SecurityId securityId;
  /**
   * The basket of deliverable bonds.
   * <p>
   * The underling which will be delivered in the future time is chosen from
   * a basket of underling securities. This must not be empty.
   * <p>
   * All of the underlying bonds must have the same notional and currency.
   */
  @PropertyDefinition(validate = "notEmpty")
  private final ImmutableList<FixedCouponBond> deliveryBasket;
  /**
   * The conversion factor for each bond in the basket.
   * <p>
   * The price of each underlying security in the basket is rescaled by the conversion factor.
   * This must not be empty, and its size must be the same as the size of {@code deliveryBasket}.
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
    int size = deliveryBasket.size();
    ArgChecker.isTrue(size == conversionFactors.size(),
        "The delivery basket size should be the same as the conversion factor size");
    ArgChecker.inOrderOrEqual(firstNoticeDate, lastNoticeDate, "firstNoticeDate", "lastNoticeDate");
    if (firstDeliveryDate != null && lastDeliveryDate != null) {
      ArgChecker.inOrderOrEqual(firstDeliveryDate, lastDeliveryDate, "firstDeliveryDate", "lastDeliveryDate");
      ArgChecker.inOrderOrEqual(firstNoticeDate, firstDeliveryDate, "firstNoticeDate", "firstDeliveryDate");
      ArgChecker.inOrderOrEqual(lastNoticeDate, lastDeliveryDate, "lastNoticeDate", "lastDeliveryDate");
    }
    if (size > 1) {
      ImmutableList<FixedCouponBond> bondsList = getDeliveryBasket();
      double notional = getNotional();
      Currency currency = getCurrency();
      for (int i = 1; i < size; ++i) {
        ArgChecker.isTrue(bondsList.get(i).getNotional() == notional, "Notional must be same for all bonds");
        ArgChecker.isTrue(bondsList.get(i).getCurrency().equals(currency), "Currency must be same for all bonds");
      }
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains the notional of underlying fixed coupon bonds.
   * <p>
   * All of the bonds in the delivery basket have the same notional.
   * 
   * @return the notional
   */
  public double getNotional() {
    return deliveryBasket.get(0).getNotional();
  }

  /**
   * Obtains the currency of the underlying fixed coupon bonds.
   * <p>
   * All of the bonds in the delivery basket have the same currency.
   * 
   * @return the currency
   */
  @Override
  public Currency getCurrency() {
    return deliveryBasket.get(0).getCurrency();
  }

  //-------------------------------------------------------------------------
  @Override
  public ResolvedBondFuture resolve(ReferenceData refData) {
    List<ResolvedFixedCouponBond> basket = deliveryBasket.stream()
        .map(bond -> bond.resolve(refData))
        .collect(toImmutableList());
    DaysAdjustment settleOffset = deliveryBasket.get(0).getSettlementDateOffset();
    return ResolvedBondFuture.builder()
        .securityId(securityId)
        .deliveryBasket(basket)
        .conversionFactors(conversionFactors)
        .lastTradeDate(lastTradeDate)
        .firstNoticeDate(firstNoticeDate)
        .lastNoticeDate(lastNoticeDate)
        .firstDeliveryDate(firstDeliveryDate != null ? firstDeliveryDate : settleOffset.adjust(firstNoticeDate, refData))
        .lastDeliveryDate(lastDeliveryDate != null ? lastDeliveryDate : settleOffset.adjust(lastNoticeDate, refData))
        .rounding(rounding)
        .build();
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code BondFuture}.
   * @return the meta-bean, not null
   */
  public static BondFuture.Meta meta() {
    return BondFuture.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(BondFuture.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static BondFuture.Builder builder() {
    return new BondFuture.Builder();
  }

  /**
   * Creates an instance.
   * @param securityId  the value of the property, not null
   * @param deliveryBasket  the value of the property, not empty
   * @param conversionFactors  the value of the property, not empty
   * @param lastTradeDate  the value of the property, not null
   * @param firstNoticeDate  the value of the property, not null
   * @param lastNoticeDate  the value of the property, not null
   * @param firstDeliveryDate  the value of the property
   * @param lastDeliveryDate  the value of the property
   * @param rounding  the value of the property, not null
   */
  BondFuture(
      SecurityId securityId,
      List<FixedCouponBond> deliveryBasket,
      List<Double> conversionFactors,
      LocalDate lastTradeDate,
      LocalDate firstNoticeDate,
      LocalDate lastNoticeDate,
      LocalDate firstDeliveryDate,
      LocalDate lastDeliveryDate,
      Rounding rounding) {
    JodaBeanUtils.notNull(securityId, "securityId");
    JodaBeanUtils.notEmpty(deliveryBasket, "deliveryBasket");
    JodaBeanUtils.notEmpty(conversionFactors, "conversionFactors");
    JodaBeanUtils.notNull(lastTradeDate, "lastTradeDate");
    JodaBeanUtils.notNull(firstNoticeDate, "firstNoticeDate");
    JodaBeanUtils.notNull(lastNoticeDate, "lastNoticeDate");
    JodaBeanUtils.notNull(rounding, "rounding");
    this.securityId = securityId;
    this.deliveryBasket = ImmutableList.copyOf(deliveryBasket);
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
  public BondFuture.Meta metaBean() {
    return BondFuture.Meta.INSTANCE;
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
   * Gets the security identifier.
   * <p>
   * This identifier uniquely identifies the security within the system.
   * @return the value of the property, not null
   */
  @Override
  public SecurityId getSecurityId() {
    return securityId;
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
  public ImmutableList<FixedCouponBond> getDeliveryBasket() {
    return deliveryBasket;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the conversion factor for each bond in the basket.
   * <p>
   * The price of each underlying security in the basket is rescaled by the conversion factor.
   * This must not be empty, and its size must be the same as the size of {@code deliveryBasket}.
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
      BondFuture other = (BondFuture) obj;
      return JodaBeanUtils.equal(securityId, other.securityId) &&
          JodaBeanUtils.equal(deliveryBasket, other.deliveryBasket) &&
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
    hash = hash * 31 + JodaBeanUtils.hashCode(securityId);
    hash = hash * 31 + JodaBeanUtils.hashCode(deliveryBasket);
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
    StringBuilder buf = new StringBuilder(320);
    buf.append("BondFuture{");
    buf.append("securityId").append('=').append(securityId).append(',').append(' ');
    buf.append("deliveryBasket").append('=').append(deliveryBasket).append(',').append(' ');
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
   * The meta-bean for {@code BondFuture}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code securityId} property.
     */
    private final MetaProperty<SecurityId> securityId = DirectMetaProperty.ofImmutable(
        this, "securityId", BondFuture.class, SecurityId.class);
    /**
     * The meta-property for the {@code deliveryBasket} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableList<FixedCouponBond>> deliveryBasket = DirectMetaProperty.ofImmutable(
        this, "deliveryBasket", BondFuture.class, (Class) ImmutableList.class);
    /**
     * The meta-property for the {@code conversionFactors} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableList<Double>> conversionFactors = DirectMetaProperty.ofImmutable(
        this, "conversionFactors", BondFuture.class, (Class) ImmutableList.class);
    /**
     * The meta-property for the {@code lastTradeDate} property.
     */
    private final MetaProperty<LocalDate> lastTradeDate = DirectMetaProperty.ofImmutable(
        this, "lastTradeDate", BondFuture.class, LocalDate.class);
    /**
     * The meta-property for the {@code firstNoticeDate} property.
     */
    private final MetaProperty<LocalDate> firstNoticeDate = DirectMetaProperty.ofImmutable(
        this, "firstNoticeDate", BondFuture.class, LocalDate.class);
    /**
     * The meta-property for the {@code lastNoticeDate} property.
     */
    private final MetaProperty<LocalDate> lastNoticeDate = DirectMetaProperty.ofImmutable(
        this, "lastNoticeDate", BondFuture.class, LocalDate.class);
    /**
     * The meta-property for the {@code firstDeliveryDate} property.
     */
    private final MetaProperty<LocalDate> firstDeliveryDate = DirectMetaProperty.ofImmutable(
        this, "firstDeliveryDate", BondFuture.class, LocalDate.class);
    /**
     * The meta-property for the {@code lastDeliveryDate} property.
     */
    private final MetaProperty<LocalDate> lastDeliveryDate = DirectMetaProperty.ofImmutable(
        this, "lastDeliveryDate", BondFuture.class, LocalDate.class);
    /**
     * The meta-property for the {@code rounding} property.
     */
    private final MetaProperty<Rounding> rounding = DirectMetaProperty.ofImmutable(
        this, "rounding", BondFuture.class, Rounding.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "securityId",
        "deliveryBasket",
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
        case 1574023291:  // securityId
          return securityId;
        case 1999764186:  // deliveryBasket
          return deliveryBasket;
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
    public BondFuture.Builder builder() {
      return new BondFuture.Builder();
    }

    @Override
    public Class<? extends BondFuture> beanType() {
      return BondFuture.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code securityId} property.
     * @return the meta-property, not null
     */
    public MetaProperty<SecurityId> securityId() {
      return securityId;
    }

    /**
     * The meta-property for the {@code deliveryBasket} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableList<FixedCouponBond>> deliveryBasket() {
      return deliveryBasket;
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
        case 1574023291:  // securityId
          return ((BondFuture) bean).getSecurityId();
        case 1999764186:  // deliveryBasket
          return ((BondFuture) bean).getDeliveryBasket();
        case 1655488270:  // conversionFactors
          return ((BondFuture) bean).getConversionFactors();
        case -1041950404:  // lastTradeDate
          return ((BondFuture) bean).getLastTradeDate();
        case -1085415050:  // firstNoticeDate
          return ((BondFuture) bean).getFirstNoticeDate();
        case -1060668964:  // lastNoticeDate
          return ((BondFuture) bean).getLastNoticeDate();
        case 1755448466:  // firstDeliveryDate
          return ((BondFuture) bean).firstDeliveryDate;
        case -233366664:  // lastDeliveryDate
          return ((BondFuture) bean).lastDeliveryDate;
        case -142444:  // rounding
          return ((BondFuture) bean).getRounding();
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
   * The bean-builder for {@code BondFuture}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<BondFuture> {

    private SecurityId securityId;
    private List<FixedCouponBond> deliveryBasket = ImmutableList.of();
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
    private Builder(BondFuture beanToCopy) {
      this.securityId = beanToCopy.getSecurityId();
      this.deliveryBasket = beanToCopy.getDeliveryBasket();
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
        case 1574023291:  // securityId
          return securityId;
        case 1999764186:  // deliveryBasket
          return deliveryBasket;
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
        case 1574023291:  // securityId
          this.securityId = (SecurityId) newValue;
          break;
        case 1999764186:  // deliveryBasket
          this.deliveryBasket = (List<FixedCouponBond>) newValue;
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
    public BondFuture build() {
      return new BondFuture(
          securityId,
          deliveryBasket,
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
     * Sets the security identifier.
     * <p>
     * This identifier uniquely identifies the security within the system.
     * @param securityId  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder securityId(SecurityId securityId) {
      JodaBeanUtils.notNull(securityId, "securityId");
      this.securityId = securityId;
      return this;
    }

    /**
     * Sets the basket of deliverable bonds.
     * <p>
     * The underling which will be delivered in the future time is chosen from
     * a basket of underling securities. This must not be empty.
     * <p>
     * All of the underlying bonds must have the same notional and currency.
     * @param deliveryBasket  the new value, not empty
     * @return this, for chaining, not null
     */
    public Builder deliveryBasket(List<FixedCouponBond> deliveryBasket) {
      JodaBeanUtils.notEmpty(deliveryBasket, "deliveryBasket");
      this.deliveryBasket = deliveryBasket;
      return this;
    }

    /**
     * Sets the {@code deliveryBasket} property in the builder
     * from an array of objects.
     * @param deliveryBasket  the new value, not empty
     * @return this, for chaining, not null
     */
    public Builder deliveryBasket(FixedCouponBond... deliveryBasket) {
      return deliveryBasket(ImmutableList.copyOf(deliveryBasket));
    }

    /**
     * Sets the conversion factor for each bond in the basket.
     * <p>
     * The price of each underlying security in the basket is rescaled by the conversion factor.
     * This must not be empty, and its size must be the same as the size of {@code deliveryBasket}.
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
      StringBuilder buf = new StringBuilder(320);
      buf.append("BondFuture.Builder{");
      buf.append("securityId").append('=').append(JodaBeanUtils.toString(securityId)).append(',').append(' ');
      buf.append("deliveryBasket").append('=').append(JodaBeanUtils.toString(deliveryBasket)).append(',').append(' ');
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
