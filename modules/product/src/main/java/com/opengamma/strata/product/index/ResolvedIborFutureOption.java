/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.index;

import java.io.Serializable;
import java.time.LocalDate;
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
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.value.Rounding;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.product.ResolvedProduct;
import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.common.PutCall;
import com.opengamma.strata.product.option.FutureOptionPremiumStyle;

/**
 * A futures option contract based on an Ibor index, resolved for pricing.
 * <p>
 * This is the resolved form of {@link IborFutureOption} and is an input to the pricers.
 * Applications will typically create a {@code ResolvedIborFutureOption} from a {@code IborFutureOption}
 * using {@link IborFutureOption#resolve(ReferenceData)}.
 * <p>
 * A {@code ResolvedIborFutureOption} is bound to data that changes over time, such as holiday calendars.
 * If the data changes, such as the addition of a new holiday, the resolved form will not be updated.
 * Care must be taken when placing the resolved form in a cache or persistence layer.
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
@BeanDefinition(constructorScope = "package")
public final class ResolvedIborFutureOption
    implements ResolvedProduct, ImmutableBean, Serializable {

  /**
   * The security identifier.
   * <p>
   * This identifier uniquely identifies the security within the system.
   */
  @PropertyDefinition(validate = "notNull")
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
   * The expiry of the option.
   * <p>
   * The date must not be after last trade date of the underlying future.
   */
  @PropertyDefinition(validate = "notNull")
  private final ZonedDateTime expiry;
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
   * The underlying future.
   */
  @PropertyDefinition(validate = "notNull")
  private final ResolvedIborFuture underlyingFuture;

  //-------------------------------------------------------------------------
  @ImmutableDefaults
  private static void applyDefaults(Builder builder) {
    builder.rounding(Rounding.none());
  }

  @ImmutableValidator
  private void validate() {
    LocalDate lastTradeDate = underlyingFuture.getLastTradeDate();
    ArgChecker.inOrderOrEqual(expiry.toLocalDate(), lastTradeDate, "expiry.date", "underlying.lastTradeDate");
    ArgChecker.isTrue(
        strikePrice < 2, "Strike price must be in decimal form, such as 0.993 for a 0.7% rate, but was: {}", strikePrice);
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the expiry date of the option.
   * 
   * @return the expiry date
   */
  public LocalDate getExpiryDate() {
    return expiry.toLocalDate();
  }

  /**
   * Gets the Ibor index that the option is based on.
   * 
   * @return the Ibor index
   */
  public IborIndex getIndex() {
    return underlyingFuture.getIndex();
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code ResolvedIborFutureOption}.
   * @return the meta-bean, not null
   */
  public static ResolvedIborFutureOption.Meta meta() {
    return ResolvedIborFutureOption.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(ResolvedIborFutureOption.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static ResolvedIborFutureOption.Builder builder() {
    return new ResolvedIborFutureOption.Builder();
  }

  /**
   * Creates an instance.
   * @param securityId  the value of the property, not null
   * @param putCall  the value of the property
   * @param strikePrice  the value of the property
   * @param expiry  the value of the property, not null
   * @param premiumStyle  the value of the property, not null
   * @param rounding  the value of the property, not null
   * @param underlyingFuture  the value of the property, not null
   */
  ResolvedIborFutureOption(
      SecurityId securityId,
      PutCall putCall,
      double strikePrice,
      ZonedDateTime expiry,
      FutureOptionPremiumStyle premiumStyle,
      Rounding rounding,
      ResolvedIborFuture underlyingFuture) {
    JodaBeanUtils.notNull(securityId, "securityId");
    JodaBeanUtils.notNull(expiry, "expiry");
    JodaBeanUtils.notNull(premiumStyle, "premiumStyle");
    JodaBeanUtils.notNull(rounding, "rounding");
    JodaBeanUtils.notNull(underlyingFuture, "underlyingFuture");
    this.securityId = securityId;
    this.putCall = putCall;
    this.strikePrice = strikePrice;
    this.expiry = expiry;
    this.premiumStyle = premiumStyle;
    this.rounding = rounding;
    this.underlyingFuture = underlyingFuture;
    validate();
  }

  @Override
  public ResolvedIborFutureOption.Meta metaBean() {
    return ResolvedIborFutureOption.Meta.INSTANCE;
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
   * Gets the expiry of the option.
   * <p>
   * The date must not be after last trade date of the underlying future.
   * @return the value of the property, not null
   */
  public ZonedDateTime getExpiry() {
    return expiry;
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
   * Gets the underlying future.
   * @return the value of the property, not null
   */
  public ResolvedIborFuture getUnderlyingFuture() {
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
      ResolvedIborFutureOption other = (ResolvedIborFutureOption) obj;
      return JodaBeanUtils.equal(securityId, other.securityId) &&
          JodaBeanUtils.equal(putCall, other.putCall) &&
          JodaBeanUtils.equal(strikePrice, other.strikePrice) &&
          JodaBeanUtils.equal(expiry, other.expiry) &&
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
    hash = hash * 31 + JodaBeanUtils.hashCode(expiry);
    hash = hash * 31 + JodaBeanUtils.hashCode(premiumStyle);
    hash = hash * 31 + JodaBeanUtils.hashCode(rounding);
    hash = hash * 31 + JodaBeanUtils.hashCode(underlyingFuture);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(256);
    buf.append("ResolvedIborFutureOption{");
    buf.append("securityId").append('=').append(securityId).append(',').append(' ');
    buf.append("putCall").append('=').append(putCall).append(',').append(' ');
    buf.append("strikePrice").append('=').append(strikePrice).append(',').append(' ');
    buf.append("expiry").append('=').append(expiry).append(',').append(' ');
    buf.append("premiumStyle").append('=').append(premiumStyle).append(',').append(' ');
    buf.append("rounding").append('=').append(rounding).append(',').append(' ');
    buf.append("underlyingFuture").append('=').append(JodaBeanUtils.toString(underlyingFuture));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code ResolvedIborFutureOption}.
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
        this, "securityId", ResolvedIborFutureOption.class, SecurityId.class);
    /**
     * The meta-property for the {@code putCall} property.
     */
    private final MetaProperty<PutCall> putCall = DirectMetaProperty.ofImmutable(
        this, "putCall", ResolvedIborFutureOption.class, PutCall.class);
    /**
     * The meta-property for the {@code strikePrice} property.
     */
    private final MetaProperty<Double> strikePrice = DirectMetaProperty.ofImmutable(
        this, "strikePrice", ResolvedIborFutureOption.class, Double.TYPE);
    /**
     * The meta-property for the {@code expiry} property.
     */
    private final MetaProperty<ZonedDateTime> expiry = DirectMetaProperty.ofImmutable(
        this, "expiry", ResolvedIborFutureOption.class, ZonedDateTime.class);
    /**
     * The meta-property for the {@code premiumStyle} property.
     */
    private final MetaProperty<FutureOptionPremiumStyle> premiumStyle = DirectMetaProperty.ofImmutable(
        this, "premiumStyle", ResolvedIborFutureOption.class, FutureOptionPremiumStyle.class);
    /**
     * The meta-property for the {@code rounding} property.
     */
    private final MetaProperty<Rounding> rounding = DirectMetaProperty.ofImmutable(
        this, "rounding", ResolvedIborFutureOption.class, Rounding.class);
    /**
     * The meta-property for the {@code underlyingFuture} property.
     */
    private final MetaProperty<ResolvedIborFuture> underlyingFuture = DirectMetaProperty.ofImmutable(
        this, "underlyingFuture", ResolvedIborFutureOption.class, ResolvedIborFuture.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "securityId",
        "putCall",
        "strikePrice",
        "expiry",
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
        case -1289159373:  // expiry
          return expiry;
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
    public ResolvedIborFutureOption.Builder builder() {
      return new ResolvedIborFutureOption.Builder();
    }

    @Override
    public Class<? extends ResolvedIborFutureOption> beanType() {
      return ResolvedIborFutureOption.class;
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
     * The meta-property for the {@code expiry} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ZonedDateTime> expiry() {
      return expiry;
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
    public MetaProperty<ResolvedIborFuture> underlyingFuture() {
      return underlyingFuture;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 1574023291:  // securityId
          return ((ResolvedIborFutureOption) bean).getSecurityId();
        case -219971059:  // putCall
          return ((ResolvedIborFutureOption) bean).getPutCall();
        case 50946231:  // strikePrice
          return ((ResolvedIborFutureOption) bean).getStrikePrice();
        case -1289159373:  // expiry
          return ((ResolvedIborFutureOption) bean).getExpiry();
        case -1257652838:  // premiumStyle
          return ((ResolvedIborFutureOption) bean).getPremiumStyle();
        case -142444:  // rounding
          return ((ResolvedIborFutureOption) bean).getRounding();
        case -165476480:  // underlyingFuture
          return ((ResolvedIborFutureOption) bean).getUnderlyingFuture();
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
   * The bean-builder for {@code ResolvedIborFutureOption}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<ResolvedIborFutureOption> {

    private SecurityId securityId;
    private PutCall putCall;
    private double strikePrice;
    private ZonedDateTime expiry;
    private FutureOptionPremiumStyle premiumStyle;
    private Rounding rounding;
    private ResolvedIborFuture underlyingFuture;

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
    private Builder(ResolvedIborFutureOption beanToCopy) {
      this.securityId = beanToCopy.getSecurityId();
      this.putCall = beanToCopy.getPutCall();
      this.strikePrice = beanToCopy.getStrikePrice();
      this.expiry = beanToCopy.getExpiry();
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
        case -1289159373:  // expiry
          return expiry;
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
        case -1289159373:  // expiry
          this.expiry = (ZonedDateTime) newValue;
          break;
        case -1257652838:  // premiumStyle
          this.premiumStyle = (FutureOptionPremiumStyle) newValue;
          break;
        case -142444:  // rounding
          this.rounding = (Rounding) newValue;
          break;
        case -165476480:  // underlyingFuture
          this.underlyingFuture = (ResolvedIborFuture) newValue;
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
    public ResolvedIborFutureOption build() {
      return new ResolvedIborFutureOption(
          securityId,
          putCall,
          strikePrice,
          expiry,
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
     * Sets the expiry of the option.
     * <p>
     * The date must not be after last trade date of the underlying future.
     * @param expiry  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder expiry(ZonedDateTime expiry) {
      JodaBeanUtils.notNull(expiry, "expiry");
      this.expiry = expiry;
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
     * Sets the underlying future.
     * @param underlyingFuture  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder underlyingFuture(ResolvedIborFuture underlyingFuture) {
      JodaBeanUtils.notNull(underlyingFuture, "underlyingFuture");
      this.underlyingFuture = underlyingFuture;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(256);
      buf.append("ResolvedIborFutureOption.Builder{");
      buf.append("securityId").append('=').append(JodaBeanUtils.toString(securityId)).append(',').append(' ');
      buf.append("putCall").append('=').append(JodaBeanUtils.toString(putCall)).append(',').append(' ');
      buf.append("strikePrice").append('=').append(JodaBeanUtils.toString(strikePrice)).append(',').append(' ');
      buf.append("expiry").append('=').append(JodaBeanUtils.toString(expiry)).append(',').append(' ');
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
