/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.bond;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.Resolvable;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.value.Rounding;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.product.SecuritizedProduct;
import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.common.PutCall;
import com.opengamma.strata.product.option.FutureOptionPremiumStyle;

/**
 * A futures option contract, based on bonds.
 * <p>
 * A bond future option is a financial instrument that provides an option based on the future value of
 * fixed coupon bonds. The option is American, exercised at any point up to the exercise time.
 * It handles options with either daily margining or upfront premium.
 * <p>
 * This class represents the structure of a single option contract.
 * 
 * <h4>Price</h4>
 * Strata uses <i>decimal prices</i> for bond futures options in the trade model, pricers and market data.
 * This is coherent with the pricing of {@link BondFuture}.
 */
@BeanDefinition(constructorScope = "package")
public final class BondFutureOption
    implements SecuritizedProduct, Resolvable<ResolvedBondFutureOption>, ImmutableBean, Serializable {

  /**
   * The security identifier.
   * <p>
   * This identifier uniquely identifies the security within the system.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final SecurityId securityId;
  /**
   * Whether the option is put or call.
   * <p>
   * A call gives the owner the right, but not obligation, to buy the underlying at
   * an agreed price in the future. A put gives a similar option to sell.
   */
  @PropertyDefinition
  private final PutCall putCall;
  /**
   * The strike price, represented in decimal form.
   * <p>
   * This is the price at which the option applies and refers to the price of the underlying future.
   * This must be represented in decimal form, {@code (1.0 - decimalRate)}. 
   * As such, the common market price of 99.3 for a 0.7% rate must be input as 0.993.
   * The rate implied by the strike can take negative values.
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
   * For example, the common market price of 99.7125 is represented as 0.997125 which
   * has 6 decimal places.
   */
  @PropertyDefinition(validate = "notNull")
  private final Rounding rounding;
  /**
   * The underlying future.
   */
  @PropertyDefinition(validate = "notNull")
  private final BondFuture underlyingFuture;

  //-------------------------------------------------------------------------
  @ImmutableDefaults
  private static void applyDefaults(Builder builder) {
    builder.rounding(Rounding.none());
  }

  @ImmutableValidator
  private void validate() {
    ArgChecker.inOrderOrEqual(
        expiryDate, underlyingFuture.getLastTradeDate(), "expiryDate", "underlyingFuture.lastTradeDate");
  }

  //-------------------------------------------------------------------------
  @Override
  public Currency getCurrency() {
    return underlyingFuture.getCurrency();
  }

  /**
   * Gets the expiry date-time.
   * <p>
   * The option expires at this date and time.
   * <p>
   * The result is returned by combining the expiry date, time and time-zone.
   * 
   * @return the expiry date and time
   */
  public ZonedDateTime getExpiry() {
    return expiryDate.atTime(expiryTime).atZone(expiryZone);
  }

  //-------------------------------------------------------------------------
  @Override
  public ResolvedBondFutureOption resolve(ReferenceData refData) {
    ResolvedBondFuture resolved = underlyingFuture.resolve(refData);
    return new ResolvedBondFutureOption(securityId, putCall, strikePrice, getExpiry(), premiumStyle, rounding, resolved);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code BondFutureOption}.
   * @return the meta-bean, not null
   */
  public static BondFutureOption.Meta meta() {
    return BondFutureOption.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(BondFutureOption.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static BondFutureOption.Builder builder() {
    return new BondFutureOption.Builder();
  }

  /**
   * Creates an instance.
   * @param securityId  the value of the property, not null
   * @param putCall  the value of the property
   * @param strikePrice  the value of the property
   * @param expiryDate  the value of the property, not null
   * @param expiryTime  the value of the property, not null
   * @param expiryZone  the value of the property, not null
   * @param premiumStyle  the value of the property, not null
   * @param rounding  the value of the property, not null
   * @param underlyingFuture  the value of the property, not null
   */
  BondFutureOption(
      SecurityId securityId,
      PutCall putCall,
      double strikePrice,
      LocalDate expiryDate,
      LocalTime expiryTime,
      ZoneId expiryZone,
      FutureOptionPremiumStyle premiumStyle,
      Rounding rounding,
      BondFuture underlyingFuture) {
    JodaBeanUtils.notNull(securityId, "securityId");
    JodaBeanUtils.notNull(expiryDate, "expiryDate");
    JodaBeanUtils.notNull(expiryTime, "expiryTime");
    JodaBeanUtils.notNull(expiryZone, "expiryZone");
    JodaBeanUtils.notNull(premiumStyle, "premiumStyle");
    JodaBeanUtils.notNull(rounding, "rounding");
    JodaBeanUtils.notNull(underlyingFuture, "underlyingFuture");
    this.securityId = securityId;
    this.putCall = putCall;
    this.strikePrice = strikePrice;
    this.expiryDate = expiryDate;
    this.expiryTime = expiryTime;
    this.expiryZone = expiryZone;
    this.premiumStyle = premiumStyle;
    this.rounding = rounding;
    this.underlyingFuture = underlyingFuture;
    validate();
  }

  @Override
  public BondFutureOption.Meta metaBean() {
    return BondFutureOption.Meta.INSTANCE;
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
   * Gets the strike price, represented in decimal form.
   * <p>
   * This is the price at which the option applies and refers to the price of the underlying future.
   * This must be represented in decimal form, {@code (1.0 - decimalRate)}.
   * As such, the common market price of 99.3 for a 0.7% rate must be input as 0.993.
   * The rate implied by the strike can take negative values.
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
   * For example, the common market price of 99.7125 is represented as 0.997125 which
   * has 6 decimal places.
   * @return the value of the property, not null
   */
  public Rounding getRounding() {
    return rounding;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the underlying future.
   * @return the value of the property, not null
   */
  public BondFuture getUnderlyingFuture() {
    return underlyingFuture;
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
      BondFutureOption other = (BondFutureOption) obj;
      return JodaBeanUtils.equal(securityId, other.securityId) &&
          JodaBeanUtils.equal(putCall, other.putCall) &&
          JodaBeanUtils.equal(strikePrice, other.strikePrice) &&
          JodaBeanUtils.equal(expiryDate, other.expiryDate) &&
          JodaBeanUtils.equal(expiryTime, other.expiryTime) &&
          JodaBeanUtils.equal(expiryZone, other.expiryZone) &&
          JodaBeanUtils.equal(premiumStyle, other.premiumStyle) &&
          JodaBeanUtils.equal(rounding, other.rounding) &&
          JodaBeanUtils.equal(underlyingFuture, other.underlyingFuture);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(securityId);
    hash = hash * 31 + JodaBeanUtils.hashCode(putCall);
    hash = hash * 31 + JodaBeanUtils.hashCode(strikePrice);
    hash = hash * 31 + JodaBeanUtils.hashCode(expiryDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(expiryTime);
    hash = hash * 31 + JodaBeanUtils.hashCode(expiryZone);
    hash = hash * 31 + JodaBeanUtils.hashCode(premiumStyle);
    hash = hash * 31 + JodaBeanUtils.hashCode(rounding);
    hash = hash * 31 + JodaBeanUtils.hashCode(underlyingFuture);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(320);
    buf.append("BondFutureOption{");
    buf.append("securityId").append('=').append(securityId).append(',').append(' ');
    buf.append("putCall").append('=').append(putCall).append(',').append(' ');
    buf.append("strikePrice").append('=').append(strikePrice).append(',').append(' ');
    buf.append("expiryDate").append('=').append(expiryDate).append(',').append(' ');
    buf.append("expiryTime").append('=').append(expiryTime).append(',').append(' ');
    buf.append("expiryZone").append('=').append(expiryZone).append(',').append(' ');
    buf.append("premiumStyle").append('=').append(premiumStyle).append(',').append(' ');
    buf.append("rounding").append('=').append(rounding).append(',').append(' ');
    buf.append("underlyingFuture").append('=').append(JodaBeanUtils.toString(underlyingFuture));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code BondFutureOption}.
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
        this, "securityId", BondFutureOption.class, SecurityId.class);
    /**
     * The meta-property for the {@code putCall} property.
     */
    private final MetaProperty<PutCall> putCall = DirectMetaProperty.ofImmutable(
        this, "putCall", BondFutureOption.class, PutCall.class);
    /**
     * The meta-property for the {@code strikePrice} property.
     */
    private final MetaProperty<Double> strikePrice = DirectMetaProperty.ofImmutable(
        this, "strikePrice", BondFutureOption.class, Double.TYPE);
    /**
     * The meta-property for the {@code expiryDate} property.
     */
    private final MetaProperty<LocalDate> expiryDate = DirectMetaProperty.ofImmutable(
        this, "expiryDate", BondFutureOption.class, LocalDate.class);
    /**
     * The meta-property for the {@code expiryTime} property.
     */
    private final MetaProperty<LocalTime> expiryTime = DirectMetaProperty.ofImmutable(
        this, "expiryTime", BondFutureOption.class, LocalTime.class);
    /**
     * The meta-property for the {@code expiryZone} property.
     */
    private final MetaProperty<ZoneId> expiryZone = DirectMetaProperty.ofImmutable(
        this, "expiryZone", BondFutureOption.class, ZoneId.class);
    /**
     * The meta-property for the {@code premiumStyle} property.
     */
    private final MetaProperty<FutureOptionPremiumStyle> premiumStyle = DirectMetaProperty.ofImmutable(
        this, "premiumStyle", BondFutureOption.class, FutureOptionPremiumStyle.class);
    /**
     * The meta-property for the {@code rounding} property.
     */
    private final MetaProperty<Rounding> rounding = DirectMetaProperty.ofImmutable(
        this, "rounding", BondFutureOption.class, Rounding.class);
    /**
     * The meta-property for the {@code underlyingFuture} property.
     */
    private final MetaProperty<BondFuture> underlyingFuture = DirectMetaProperty.ofImmutable(
        this, "underlyingFuture", BondFutureOption.class, BondFuture.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "securityId",
        "putCall",
        "strikePrice",
        "expiryDate",
        "expiryTime",
        "expiryZone",
        "premiumStyle",
        "rounding",
        "underlyingFuture");

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
        case -165476480:  // underlyingFuture
          return underlyingFuture;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BondFutureOption.Builder builder() {
      return new BondFutureOption.Builder();
    }

    @Override
    public Class<? extends BondFutureOption> beanType() {
      return BondFutureOption.class;
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
     * The meta-property for the {@code underlyingFuture} property.
     * @return the meta-property, not null
     */
    public MetaProperty<BondFuture> underlyingFuture() {
      return underlyingFuture;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 1574023291:  // securityId
          return ((BondFutureOption) bean).getSecurityId();
        case -219971059:  // putCall
          return ((BondFutureOption) bean).getPutCall();
        case 50946231:  // strikePrice
          return ((BondFutureOption) bean).getStrikePrice();
        case -816738431:  // expiryDate
          return ((BondFutureOption) bean).getExpiryDate();
        case -816254304:  // expiryTime
          return ((BondFutureOption) bean).getExpiryTime();
        case -816069761:  // expiryZone
          return ((BondFutureOption) bean).getExpiryZone();
        case -1257652838:  // premiumStyle
          return ((BondFutureOption) bean).getPremiumStyle();
        case -142444:  // rounding
          return ((BondFutureOption) bean).getRounding();
        case -165476480:  // underlyingFuture
          return ((BondFutureOption) bean).getUnderlyingFuture();
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
   * The bean-builder for {@code BondFutureOption}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<BondFutureOption> {

    private SecurityId securityId;
    private PutCall putCall;
    private double strikePrice;
    private LocalDate expiryDate;
    private LocalTime expiryTime;
    private ZoneId expiryZone;
    private FutureOptionPremiumStyle premiumStyle;
    private Rounding rounding;
    private BondFuture underlyingFuture;

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
    private Builder(BondFutureOption beanToCopy) {
      this.securityId = beanToCopy.getSecurityId();
      this.putCall = beanToCopy.getPutCall();
      this.strikePrice = beanToCopy.getStrikePrice();
      this.expiryDate = beanToCopy.getExpiryDate();
      this.expiryTime = beanToCopy.getExpiryTime();
      this.expiryZone = beanToCopy.getExpiryZone();
      this.premiumStyle = beanToCopy.getPremiumStyle();
      this.rounding = beanToCopy.getRounding();
      this.underlyingFuture = beanToCopy.getUnderlyingFuture();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 1574023291:  // securityId
          return securityId;
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
        case -165476480:  // underlyingFuture
          return underlyingFuture;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 1574023291:  // securityId
          this.securityId = (SecurityId) newValue;
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
        case -165476480:  // underlyingFuture
          this.underlyingFuture = (BondFuture) newValue;
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
    public BondFutureOption build() {
      return new BondFutureOption(
          securityId,
          putCall,
          strikePrice,
          expiryDate,
          expiryTime,
          expiryZone,
          premiumStyle,
          rounding,
          underlyingFuture);
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
     * Sets the strike price, represented in decimal form.
     * <p>
     * This is the price at which the option applies and refers to the price of the underlying future.
     * This must be represented in decimal form, {@code (1.0 - decimalRate)}.
     * As such, the common market price of 99.3 for a 0.7% rate must be input as 0.993.
     * The rate implied by the strike can take negative values.
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
     * For example, the common market price of 99.7125 is represented as 0.997125 which
     * has 6 decimal places.
     * @param rounding  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder rounding(Rounding rounding) {
      JodaBeanUtils.notNull(rounding, "rounding");
      this.rounding = rounding;
      return this;
    }

    /**
     * Sets the underlying future.
     * @param underlyingFuture  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder underlyingFuture(BondFuture underlyingFuture) {
      JodaBeanUtils.notNull(underlyingFuture, "underlyingFuture");
      this.underlyingFuture = underlyingFuture;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(320);
      buf.append("BondFutureOption.Builder{");
      buf.append("securityId").append('=').append(JodaBeanUtils.toString(securityId)).append(',').append(' ');
      buf.append("putCall").append('=').append(JodaBeanUtils.toString(putCall)).append(',').append(' ');
      buf.append("strikePrice").append('=').append(JodaBeanUtils.toString(strikePrice)).append(',').append(' ');
      buf.append("expiryDate").append('=').append(JodaBeanUtils.toString(expiryDate)).append(',').append(' ');
      buf.append("expiryTime").append('=').append(JodaBeanUtils.toString(expiryTime)).append(',').append(' ');
      buf.append("expiryZone").append('=').append(JodaBeanUtils.toString(expiryZone)).append(',').append(' ');
      buf.append("premiumStyle").append('=').append(JodaBeanUtils.toString(premiumStyle)).append(',').append(' ');
      buf.append("rounding").append('=').append(JodaBeanUtils.toString(rounding)).append(',').append(' ');
      buf.append("underlyingFuture").append('=').append(JodaBeanUtils.toString(underlyingFuture));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
