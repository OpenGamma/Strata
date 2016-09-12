/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.index;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.NoSuchElementException;
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
import com.opengamma.strata.product.common.PutCall;
import com.opengamma.strata.product.option.FutureOptionPremiumStyle;

/**
 * A security representing a futures option contract, based on an Ibor index.
 * <p>
 * An Ibor future option is a financial instrument that provides an option based on the future value of
 * an Ibor index interest rate. The option is American, exercised at any point up to the exercise time.
 * It handles options with either daily margining or upfront premium.
 * <p>
 * An Ibor future option is also known as a <i>STIR future option</i> (Short Term Interest Rate).
 * 
 * <h4>Price</h4>
 * The price of an Ibor future option is based on the price of the underlying future, the volatility
 * and the time to expiry. The price of the at-the-money option tends to zero as expiry approaches.
 * <p>
 * Strata uses <i>decimal prices</i> for Ibor future options in the trade model, pricers and market data.
 * The decimal price is based on the decimal rate equivalent to the percentage.
 * For example, an option price of 0.2 is related to a futures price of 99.32 that implies an
 * interest rate of 0.68%. Strata represents the price of the future as 0.9932 and thus
 * represents the price of the option as 0.002.
 */
@BeanDefinition
public final class IborFutureOptionSecurity
    implements Security, ImmutableBean, Serializable {

  /**
   * The standard security information.
   * <p>
   * This includes the security identifier.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final SecurityInfo info;
  /**
   * The currency that the option is traded in.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final Currency currency;
  /**
   * Whether the option is put or call.
   * <p>
   * A call gives the owner the right, but not obligation, to buy the underlying at
   * an agreed price in the future. A put gives a similar option to sell.
   */
  @PropertyDefinition
  private final PutCall putCall;
  /**
   * The strike price, in decimal form.
   * <p>
   * This is the price at which the option applies and refers to the price of the underlying future.
   * The rate implied by the strike can take negative values.
   * <p>
   * Strata uses <i>decimal prices</i> for Ibor futures in the trade model, pricers and market data.
   * The decimal price is based on the decimal rate equivalent to the percentage.
   * For example, a price of 99.32 implies an interest rate of 0.68% which is represented in Strata by 0.9932.
   */
  @PropertyDefinition
  private final double strikePrice;
  /**
   * The expiry date of the option.
   * <p>
   * The expiry date is related to the expiry time and time-zone.
   * The date must not be after last trade date of the underlying future.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate expiryDate;
  /**
   * The expiry time of the option.
   * <p>
   * The expiry time is related to the expiry date and time-zone.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalTime expiryTime;
  /**
   * The time-zone of the expiry time.
   * <p>
   * The expiry time-zone is related to the expiry date and time.
   */
  @PropertyDefinition(validate = "notNull")
  private final ZoneId expiryZone;
  /**
   * The style of the option premium.
   * <p>
   * The two options are daily margining and upfront premium.
   */
  @PropertyDefinition(validate = "notNull")
  private final FutureOptionPremiumStyle premiumStyle;
  /**
   * The definition of how to round the option price, defaulted to no rounding.
   * <p>
   * The price is represented in decimal form, not percentage form.
   * As such, the decimal places expressed by the rounding refers to this decimal form.
   */
  @PropertyDefinition(validate = "notNull")
  private final Rounding rounding;
  /**
   * The identifier of the underlying future.
   */
  @PropertyDefinition(validate = "notNull")
  private final SecurityId underlyingFutureId;

  //-------------------------------------------------------------------------
  @ImmutableDefaults
  private static void applyDefaults(Builder builder) {
    builder.rounding(Rounding.none());
  }

  @ImmutableValidator
  private void validate() {
    ArgChecker.isTrue(
        strikePrice < 2, "Strike price must be in decimal form, such as 0.993 for a 0.7% rate, but was: {}", strikePrice);
  }

  //-------------------------------------------------------------------------
  @Override
  public ImmutableSet<SecurityId> getUnderlyingIds() {
    return ImmutableSet.of(underlyingFutureId);
  }

  //-------------------------------------------------------------------------
  @Override
  public IborFutureOption createProduct(ReferenceData refData) {
    Security security = refData.getValue(underlyingFutureId);
    if (!(security instanceof IborFutureSecurity)) {
      throw new ClassCastException(Messages.format(
          "{} underlying future '{}' resolved to '{}' when '{}' was expected",
          IborFutureOptionSecurity.class.getSimpleName(),
          underlyingFutureId,
          security.getClass().getSimpleName(),
          IborFutureSecurity.class.getSimpleName()));
    }
    IborFutureSecurity futureSec = (IborFutureSecurity) security;
    IborFuture underlying = futureSec.createProduct(refData);
    return new IborFutureOption(
        getSecurityId(), putCall, strikePrice, expiryDate, expiryTime, expiryZone, premiumStyle, rounding, underlying);
  }

  @Override
  public IborFutureOptionTrade createTrade(
      TradeInfo info,
      double quantity,
      double tradePrice,
      ReferenceData refData) {

    return new IborFutureOptionTrade(info, createProduct(refData), quantity, tradePrice);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code IborFutureOptionSecurity}.
   * @return the meta-bean, not null
   */
  public static IborFutureOptionSecurity.Meta meta() {
    return IborFutureOptionSecurity.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(IborFutureOptionSecurity.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static IborFutureOptionSecurity.Builder builder() {
    return new IborFutureOptionSecurity.Builder();
  }

  private IborFutureOptionSecurity(
      SecurityInfo info,
      Currency currency,
      PutCall putCall,
      double strikePrice,
      LocalDate expiryDate,
      LocalTime expiryTime,
      ZoneId expiryZone,
      FutureOptionPremiumStyle premiumStyle,
      Rounding rounding,
      SecurityId underlyingFutureId) {
    JodaBeanUtils.notNull(info, "info");
    JodaBeanUtils.notNull(currency, "currency");
    JodaBeanUtils.notNull(expiryDate, "expiryDate");
    JodaBeanUtils.notNull(expiryTime, "expiryTime");
    JodaBeanUtils.notNull(expiryZone, "expiryZone");
    JodaBeanUtils.notNull(premiumStyle, "premiumStyle");
    JodaBeanUtils.notNull(rounding, "rounding");
    JodaBeanUtils.notNull(underlyingFutureId, "underlyingFutureId");
    this.info = info;
    this.currency = currency;
    this.putCall = putCall;
    this.strikePrice = strikePrice;
    this.expiryDate = expiryDate;
    this.expiryTime = expiryTime;
    this.expiryZone = expiryZone;
    this.premiumStyle = premiumStyle;
    this.rounding = rounding;
    this.underlyingFutureId = underlyingFutureId;
    validate();
  }

  @Override
  public IborFutureOptionSecurity.Meta metaBean() {
    return IborFutureOptionSecurity.Meta.INSTANCE;
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
   * Gets the currency that the option is traded in.
   * @return the value of the property, not null
   */
  @Override
  public Currency getCurrency() {
    return currency;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets whether the option is put or call.
   * <p>
   * A call gives the owner the right, but not obligation, to buy the underlying at
   * an agreed price in the future. A put gives a similar option to sell.
   * @return the value of the property
   */
  public PutCall getPutCall() {
    return putCall;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the strike price, in decimal form.
   * <p>
   * This is the price at which the option applies and refers to the price of the underlying future.
   * The rate implied by the strike can take negative values.
   * <p>
   * Strata uses <i>decimal prices</i> for Ibor futures in the trade model, pricers and market data.
   * The decimal price is based on the decimal rate equivalent to the percentage.
   * For example, a price of 99.32 implies an interest rate of 0.68% which is represented in Strata by 0.9932.
   * @return the value of the property
   */
  public double getStrikePrice() {
    return strikePrice;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the expiry date of the option.
   * <p>
   * The expiry date is related to the expiry time and time-zone.
   * The date must not be after last trade date of the underlying future.
   * @return the value of the property, not null
   */
  public LocalDate getExpiryDate() {
    return expiryDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the expiry time of the option.
   * <p>
   * The expiry time is related to the expiry date and time-zone.
   * @return the value of the property, not null
   */
  public LocalTime getExpiryTime() {
    return expiryTime;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the time-zone of the expiry time.
   * <p>
   * The expiry time-zone is related to the expiry date and time.
   * @return the value of the property, not null
   */
  public ZoneId getExpiryZone() {
    return expiryZone;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the style of the option premium.
   * <p>
   * The two options are daily margining and upfront premium.
   * @return the value of the property, not null
   */
  public FutureOptionPremiumStyle getPremiumStyle() {
    return premiumStyle;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the definition of how to round the option price, defaulted to no rounding.
   * <p>
   * The price is represented in decimal form, not percentage form.
   * As such, the decimal places expressed by the rounding refers to this decimal form.
   * @return the value of the property, not null
   */
  public Rounding getRounding() {
    return rounding;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the identifier of the underlying future.
   * @return the value of the property, not null
   */
  public SecurityId getUnderlyingFutureId() {
    return underlyingFutureId;
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
      IborFutureOptionSecurity other = (IborFutureOptionSecurity) obj;
      return JodaBeanUtils.equal(info, other.info) &&
          JodaBeanUtils.equal(currency, other.currency) &&
          JodaBeanUtils.equal(putCall, other.putCall) &&
          JodaBeanUtils.equal(strikePrice, other.strikePrice) &&
          JodaBeanUtils.equal(expiryDate, other.expiryDate) &&
          JodaBeanUtils.equal(expiryTime, other.expiryTime) &&
          JodaBeanUtils.equal(expiryZone, other.expiryZone) &&
          JodaBeanUtils.equal(premiumStyle, other.premiumStyle) &&
          JodaBeanUtils.equal(rounding, other.rounding) &&
          JodaBeanUtils.equal(underlyingFutureId, other.underlyingFutureId);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(info);
    hash = hash * 31 + JodaBeanUtils.hashCode(currency);
    hash = hash * 31 + JodaBeanUtils.hashCode(putCall);
    hash = hash * 31 + JodaBeanUtils.hashCode(strikePrice);
    hash = hash * 31 + JodaBeanUtils.hashCode(expiryDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(expiryTime);
    hash = hash * 31 + JodaBeanUtils.hashCode(expiryZone);
    hash = hash * 31 + JodaBeanUtils.hashCode(premiumStyle);
    hash = hash * 31 + JodaBeanUtils.hashCode(rounding);
    hash = hash * 31 + JodaBeanUtils.hashCode(underlyingFutureId);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(352);
    buf.append("IborFutureOptionSecurity{");
    buf.append("info").append('=').append(info).append(',').append(' ');
    buf.append("currency").append('=').append(currency).append(',').append(' ');
    buf.append("putCall").append('=').append(putCall).append(',').append(' ');
    buf.append("strikePrice").append('=').append(strikePrice).append(',').append(' ');
    buf.append("expiryDate").append('=').append(expiryDate).append(',').append(' ');
    buf.append("expiryTime").append('=').append(expiryTime).append(',').append(' ');
    buf.append("expiryZone").append('=').append(expiryZone).append(',').append(' ');
    buf.append("premiumStyle").append('=').append(premiumStyle).append(',').append(' ');
    buf.append("rounding").append('=').append(rounding).append(',').append(' ');
    buf.append("underlyingFutureId").append('=').append(JodaBeanUtils.toString(underlyingFutureId));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code IborFutureOptionSecurity}.
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
        this, "info", IborFutureOptionSecurity.class, SecurityInfo.class);
    /**
     * The meta-property for the {@code currency} property.
     */
    private final MetaProperty<Currency> currency = DirectMetaProperty.ofImmutable(
        this, "currency", IborFutureOptionSecurity.class, Currency.class);
    /**
     * The meta-property for the {@code putCall} property.
     */
    private final MetaProperty<PutCall> putCall = DirectMetaProperty.ofImmutable(
        this, "putCall", IborFutureOptionSecurity.class, PutCall.class);
    /**
     * The meta-property for the {@code strikePrice} property.
     */
    private final MetaProperty<Double> strikePrice = DirectMetaProperty.ofImmutable(
        this, "strikePrice", IborFutureOptionSecurity.class, Double.TYPE);
    /**
     * The meta-property for the {@code expiryDate} property.
     */
    private final MetaProperty<LocalDate> expiryDate = DirectMetaProperty.ofImmutable(
        this, "expiryDate", IborFutureOptionSecurity.class, LocalDate.class);
    /**
     * The meta-property for the {@code expiryTime} property.
     */
    private final MetaProperty<LocalTime> expiryTime = DirectMetaProperty.ofImmutable(
        this, "expiryTime", IborFutureOptionSecurity.class, LocalTime.class);
    /**
     * The meta-property for the {@code expiryZone} property.
     */
    private final MetaProperty<ZoneId> expiryZone = DirectMetaProperty.ofImmutable(
        this, "expiryZone", IborFutureOptionSecurity.class, ZoneId.class);
    /**
     * The meta-property for the {@code premiumStyle} property.
     */
    private final MetaProperty<FutureOptionPremiumStyle> premiumStyle = DirectMetaProperty.ofImmutable(
        this, "premiumStyle", IborFutureOptionSecurity.class, FutureOptionPremiumStyle.class);
    /**
     * The meta-property for the {@code rounding} property.
     */
    private final MetaProperty<Rounding> rounding = DirectMetaProperty.ofImmutable(
        this, "rounding", IborFutureOptionSecurity.class, Rounding.class);
    /**
     * The meta-property for the {@code underlyingFutureId} property.
     */
    private final MetaProperty<SecurityId> underlyingFutureId = DirectMetaProperty.ofImmutable(
        this, "underlyingFutureId", IborFutureOptionSecurity.class, SecurityId.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "info",
        "currency",
        "putCall",
        "strikePrice",
        "expiryDate",
        "expiryTime",
        "expiryZone",
        "premiumStyle",
        "rounding",
        "underlyingFutureId");

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
        case -219971059:  // putCall
          return putCall;
        case 50946231:  // strikePrice
          return strikePrice;
        case -816738431:  // expiryDate
          return expiryDate;
        case -816254304:  // expiryTime
          return expiryTime;
        case -816069761:  // expiryZone
          return expiryZone;
        case -1257652838:  // premiumStyle
          return premiumStyle;
        case -142444:  // rounding
          return rounding;
        case -109104965:  // underlyingFutureId
          return underlyingFutureId;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public IborFutureOptionSecurity.Builder builder() {
      return new IborFutureOptionSecurity.Builder();
    }

    @Override
    public Class<? extends IborFutureOptionSecurity> beanType() {
      return IborFutureOptionSecurity.class;
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
     * The meta-property for the {@code putCall} property.
     * @return the meta-property, not null
     */
    public MetaProperty<PutCall> putCall() {
      return putCall;
    }

    /**
     * The meta-property for the {@code strikePrice} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> strikePrice() {
      return strikePrice;
    }

    /**
     * The meta-property for the {@code expiryDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> expiryDate() {
      return expiryDate;
    }

    /**
     * The meta-property for the {@code expiryTime} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalTime> expiryTime() {
      return expiryTime;
    }

    /**
     * The meta-property for the {@code expiryZone} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ZoneId> expiryZone() {
      return expiryZone;
    }

    /**
     * The meta-property for the {@code premiumStyle} property.
     * @return the meta-property, not null
     */
    public MetaProperty<FutureOptionPremiumStyle> premiumStyle() {
      return premiumStyle;
    }

    /**
     * The meta-property for the {@code rounding} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Rounding> rounding() {
      return rounding;
    }

    /**
     * The meta-property for the {@code underlyingFutureId} property.
     * @return the meta-property, not null
     */
    public MetaProperty<SecurityId> underlyingFutureId() {
      return underlyingFutureId;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 3237038:  // info
          return ((IborFutureOptionSecurity) bean).getInfo();
        case 575402001:  // currency
          return ((IborFutureOptionSecurity) bean).getCurrency();
        case -219971059:  // putCall
          return ((IborFutureOptionSecurity) bean).getPutCall();
        case 50946231:  // strikePrice
          return ((IborFutureOptionSecurity) bean).getStrikePrice();
        case -816738431:  // expiryDate
          return ((IborFutureOptionSecurity) bean).getExpiryDate();
        case -816254304:  // expiryTime
          return ((IborFutureOptionSecurity) bean).getExpiryTime();
        case -816069761:  // expiryZone
          return ((IborFutureOptionSecurity) bean).getExpiryZone();
        case -1257652838:  // premiumStyle
          return ((IborFutureOptionSecurity) bean).getPremiumStyle();
        case -142444:  // rounding
          return ((IborFutureOptionSecurity) bean).getRounding();
        case -109104965:  // underlyingFutureId
          return ((IborFutureOptionSecurity) bean).getUnderlyingFutureId();
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
   * The bean-builder for {@code IborFutureOptionSecurity}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<IborFutureOptionSecurity> {

    private SecurityInfo info;
    private Currency currency;
    private PutCall putCall;
    private double strikePrice;
    private LocalDate expiryDate;
    private LocalTime expiryTime;
    private ZoneId expiryZone;
    private FutureOptionPremiumStyle premiumStyle;
    private Rounding rounding;
    private SecurityId underlyingFutureId;

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
    private Builder(IborFutureOptionSecurity beanToCopy) {
      this.info = beanToCopy.getInfo();
      this.currency = beanToCopy.getCurrency();
      this.putCall = beanToCopy.getPutCall();
      this.strikePrice = beanToCopy.getStrikePrice();
      this.expiryDate = beanToCopy.getExpiryDate();
      this.expiryTime = beanToCopy.getExpiryTime();
      this.expiryZone = beanToCopy.getExpiryZone();
      this.premiumStyle = beanToCopy.getPremiumStyle();
      this.rounding = beanToCopy.getRounding();
      this.underlyingFutureId = beanToCopy.getUnderlyingFutureId();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 3237038:  // info
          return info;
        case 575402001:  // currency
          return currency;
        case -219971059:  // putCall
          return putCall;
        case 50946231:  // strikePrice
          return strikePrice;
        case -816738431:  // expiryDate
          return expiryDate;
        case -816254304:  // expiryTime
          return expiryTime;
        case -816069761:  // expiryZone
          return expiryZone;
        case -1257652838:  // premiumStyle
          return premiumStyle;
        case -142444:  // rounding
          return rounding;
        case -109104965:  // underlyingFutureId
          return underlyingFutureId;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 3237038:  // info
          this.info = (SecurityInfo) newValue;
          break;
        case 575402001:  // currency
          this.currency = (Currency) newValue;
          break;
        case -219971059:  // putCall
          this.putCall = (PutCall) newValue;
          break;
        case 50946231:  // strikePrice
          this.strikePrice = (Double) newValue;
          break;
        case -816738431:  // expiryDate
          this.expiryDate = (LocalDate) newValue;
          break;
        case -816254304:  // expiryTime
          this.expiryTime = (LocalTime) newValue;
          break;
        case -816069761:  // expiryZone
          this.expiryZone = (ZoneId) newValue;
          break;
        case -1257652838:  // premiumStyle
          this.premiumStyle = (FutureOptionPremiumStyle) newValue;
          break;
        case -142444:  // rounding
          this.rounding = (Rounding) newValue;
          break;
        case -109104965:  // underlyingFutureId
          this.underlyingFutureId = (SecurityId) newValue;
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
    public IborFutureOptionSecurity build() {
      return new IborFutureOptionSecurity(
          info,
          currency,
          putCall,
          strikePrice,
          expiryDate,
          expiryTime,
          expiryZone,
          premiumStyle,
          rounding,
          underlyingFutureId);
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
     * Sets the currency that the option is traded in.
     * @param currency  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder currency(Currency currency) {
      JodaBeanUtils.notNull(currency, "currency");
      this.currency = currency;
      return this;
    }

    /**
     * Sets whether the option is put or call.
     * <p>
     * A call gives the owner the right, but not obligation, to buy the underlying at
     * an agreed price in the future. A put gives a similar option to sell.
     * @param putCall  the new value
     * @return this, for chaining, not null
     */
    public Builder putCall(PutCall putCall) {
      this.putCall = putCall;
      return this;
    }

    /**
     * Sets the strike price, in decimal form.
     * <p>
     * This is the price at which the option applies and refers to the price of the underlying future.
     * The rate implied by the strike can take negative values.
     * <p>
     * Strata uses <i>decimal prices</i> for Ibor futures in the trade model, pricers and market data.
     * The decimal price is based on the decimal rate equivalent to the percentage.
     * For example, a price of 99.32 implies an interest rate of 0.68% which is represented in Strata by 0.9932.
     * @param strikePrice  the new value
     * @return this, for chaining, not null
     */
    public Builder strikePrice(double strikePrice) {
      this.strikePrice = strikePrice;
      return this;
    }

    /**
     * Sets the expiry date of the option.
     * <p>
     * The expiry date is related to the expiry time and time-zone.
     * The date must not be after last trade date of the underlying future.
     * @param expiryDate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder expiryDate(LocalDate expiryDate) {
      JodaBeanUtils.notNull(expiryDate, "expiryDate");
      this.expiryDate = expiryDate;
      return this;
    }

    /**
     * Sets the expiry time of the option.
     * <p>
     * The expiry time is related to the expiry date and time-zone.
     * @param expiryTime  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder expiryTime(LocalTime expiryTime) {
      JodaBeanUtils.notNull(expiryTime, "expiryTime");
      this.expiryTime = expiryTime;
      return this;
    }

    /**
     * Sets the time-zone of the expiry time.
     * <p>
     * The expiry time-zone is related to the expiry date and time.
     * @param expiryZone  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder expiryZone(ZoneId expiryZone) {
      JodaBeanUtils.notNull(expiryZone, "expiryZone");
      this.expiryZone = expiryZone;
      return this;
    }

    /**
     * Sets the style of the option premium.
     * <p>
     * The two options are daily margining and upfront premium.
     * @param premiumStyle  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder premiumStyle(FutureOptionPremiumStyle premiumStyle) {
      JodaBeanUtils.notNull(premiumStyle, "premiumStyle");
      this.premiumStyle = premiumStyle;
      return this;
    }

    /**
     * Sets the definition of how to round the option price, defaulted to no rounding.
     * <p>
     * The price is represented in decimal form, not percentage form.
     * As such, the decimal places expressed by the rounding refers to this decimal form.
     * @param rounding  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder rounding(Rounding rounding) {
      JodaBeanUtils.notNull(rounding, "rounding");
      this.rounding = rounding;
      return this;
    }

    /**
     * Sets the identifier of the underlying future.
     * @param underlyingFutureId  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder underlyingFutureId(SecurityId underlyingFutureId) {
      JodaBeanUtils.notNull(underlyingFutureId, "underlyingFutureId");
      this.underlyingFutureId = underlyingFutureId;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(352);
      buf.append("IborFutureOptionSecurity.Builder{");
      buf.append("info").append('=').append(JodaBeanUtils.toString(info)).append(',').append(' ');
      buf.append("currency").append('=').append(JodaBeanUtils.toString(currency)).append(',').append(' ');
      buf.append("putCall").append('=').append(JodaBeanUtils.toString(putCall)).append(',').append(' ');
      buf.append("strikePrice").append('=').append(JodaBeanUtils.toString(strikePrice)).append(',').append(' ');
      buf.append("expiryDate").append('=').append(JodaBeanUtils.toString(expiryDate)).append(',').append(' ');
      buf.append("expiryTime").append('=').append(JodaBeanUtils.toString(expiryTime)).append(',').append(' ');
      buf.append("expiryZone").append('=').append(JodaBeanUtils.toString(expiryZone)).append(',').append(' ');
      buf.append("premiumStyle").append('=').append(JodaBeanUtils.toString(premiumStyle)).append(',').append(' ');
      buf.append("rounding").append('=').append(JodaBeanUtils.toString(rounding)).append(',').append(' ');
      buf.append("underlyingFutureId").append('=').append(JodaBeanUtils.toString(underlyingFutureId));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
