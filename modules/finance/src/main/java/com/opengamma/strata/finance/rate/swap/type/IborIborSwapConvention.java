/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.finance.rate.swap.type;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.Period;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutableValidator;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.opengamma.strata.basics.BuySell;
import com.opengamma.strata.basics.PayReceive;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.finance.Convention;
import com.opengamma.strata.finance.TradeInfo;
import com.opengamma.strata.finance.rate.swap.Swap;
import com.opengamma.strata.finance.rate.swap.SwapLeg;
import com.opengamma.strata.finance.rate.swap.SwapTrade;

/**
 * A market convention for Ibor-Ibor swap trades.
 * <p>
 * This defines the market convention for a Ibor-Ibor single currency swap.
 * The convention is formed by combining two swap leg conventions in the same currency.
 * <p>
 * The market price is for the difference (spread) between the values of the two legs.
 * This convention has two legs, the "spread leg" and the "flat leg". The spread will be
 * added to the "spread leg", which is typically the leg with the shorter underlying tenor.
 * The payment frequency is typically determined by the longer underlying tenor, with
 * compounding applied.
 * <p>
 * For example, a 'USD 3s1s' basis swap has 'USD-LIBOR-1M' as the spread leg and 'USD-LIBOR-3M'
 * as the flat leg. Payment is every 3 months, with the one month leg compounded.
 * <p>
 * See also {@link IborIborSwapTemplate}.
 */
@BeanDefinition
public final class IborIborSwapConvention
    implements Convention, ImmutableBean, Serializable {

  /**
   * The market convention of the floating leg that has the spread applied.
   * <p>
   * The spread is the market price of the instrument.
   * It is added to the observed interest rate.
   */
  @PropertyDefinition(validate = "notNull")
  private final IborRateSwapLegConvention spreadLeg;
  /**
   * The market convention of the floating leg that does not have the spread applied.
   */
  @PropertyDefinition(validate = "notNull")
  private final IborRateSwapLegConvention flatLeg;
  /**
   * The offset of the spot value date from the trade date, optional with defaulting getter.
   * <p>
   * The offset is applied to the trade date and is typically plus 2 business days.
   * The start date of the swap is relative to the spot date.
   */
  @PropertyDefinition(get = "field")
  private final DaysAdjustment spotDateOffset;

  //-------------------------------------------------------------------------
  /**
   * Obtains a convention based on the specified conventions.
   * <p>
   * The spot date offset is set to be the effective date offset of the index
   * of the leg with the spread.
   * 
   * @param spreadLeg  the market convention for the leg that the spread is added to
   * @param flatLeg  the market convention for the other, flat, leg
   * @return the convention
   */
  public static IborIborSwapConvention of(IborRateSwapLegConvention spreadLeg, IborRateSwapLegConvention flatLeg) {
    return IborIborSwapConvention.builder()
        .spreadLeg(spreadLeg)
        .flatLeg(flatLeg)
        .build();
  }

  //-------------------------------------------------------------------------
  @ImmutableValidator
  private void validate() {
    ArgChecker.isTrue(spreadLeg.getCurrency().equals(flatLeg.getCurrency()), "Conventions must have same currency");
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the offset of the spot value date from the trade date,
   * providing a default result if no override specified.
   * <p>
   * The offset is applied to the trade date and is typically plus 2 business days.
   * The start date of the swap is relative to the spot date.
   * <p>
   * This will default to the effective date offset of the index of the leg with the spread if not specified.
   * 
   * @return the spot date offset, not null
   */
  public DaysAdjustment getSpotDateOffset() {
    return spotDateOffset != null ? spotDateOffset : spreadLeg.getIndex().getEffectiveDateOffset();
  }

  //-------------------------------------------------------------------------
  /**
   * Expands this convention, returning an instance where all the optional fields are present.
   * <p>
   * This returns an equivalent instance where any empty optional have been filled in.
   * 
   * @return the expanded convention
   */
  public IborIborSwapConvention expand() {
    return IborIborSwapConvention.builder()
        .spreadLeg(spreadLeg.expand())
        .flatLeg(flatLeg.expand())
        .spotDateOffset(getSpotDateOffset())
        .build();
  }

  //-------------------------------------------------------------------------
  /**
   * Creates a spot-starting trade based on this convention.
   * <p>
   * This returns a trade based on the specified tenor. For example, a tenor
   * of 5 years creates a swap starting on the spot date and maturing 5 years later.
   * <p>
   * The notional is unsigned, with buy/sell determining the direction of the trade.
   * If buying the swap, the rate of the flat leg is received from the counterparty,
   * with the rate of the spread leg being paid. If selling the swap, the opposite occurs.
   * 
   * @param tradeDate  the date of the trade
   * @param tenor  the tenor of the swap
   * @param buySell  the buy/sell flag
   * @param notional  the notional amount
   * @param spread  the spread, typically derived from the market
   * @return the trade
   */
  public SwapTrade toTrade(
      LocalDate tradeDate,
      Tenor tenor,
      BuySell buySell,
      double notional,
      double spread) {

    return toTrade(tradeDate, Period.ZERO, tenor, buySell, notional, spread);
  }

  /**
   * Creates a forward-starting trade based on this convention.
   * <p>
   * This returns a trade based on the specified period and tenor. For example, a period of
   * 3 months and a tenor of 5 years creates a swap starting three months after the spot date
   * and maturing 5 years later.
   * <p>
   * The notional is unsigned, with buy/sell determining the direction of the trade.
   * If buying the swap, the rate of the flat leg is received from the counterparty,
   * with the rate of the spread leg being paid. If selling the swap, the opposite occurs.
   * 
   * @param tradeDate  the date of the trade
   * @param periodToStart  the period between the spot date and the start date
   * @param tenor  the tenor of the swap
   * @param buySell  the buy/sell flag
   * @param notional  the notional amount
   * @param spread  the spread, typically derived from the market
   * @return the trade
   */
  public SwapTrade toTrade(
      LocalDate tradeDate,
      Period periodToStart,
      Tenor tenor,
      BuySell buySell,
      double notional,
      double spread) {

    LocalDate spotValue = getSpotDateOffset().adjust(tradeDate);
    LocalDate startDate = spotValue.plus(periodToStart);
    LocalDate endDate = startDate.plus(tenor.getPeriod());
    return toTrade(tradeDate, startDate, endDate, buySell, notional, spread);
  }

  /**
   * Creates a trade based on this convention.
   * <p>
   * This returns a trade based on the specified dates.
   * <p>
   * The notional is unsigned, with buy/sell determining the direction of the trade.
   * If buying the swap, the rate of the flat leg is received from the counterparty,
   * with the rate of the spread leg being paid. If selling the swap, the opposite occurs.
   * 
   * @param tradeDate  the date of the trade
   * @param startDate  the start date
   * @param endDate  the end date
   * @param buySell  the buy/sell flag
   * @param notional  the notional amount
   * @param spread  the spread, typically derived from the market
   * @return the trade
   */
  public SwapTrade toTrade(
      LocalDate tradeDate,
      LocalDate startDate,
      LocalDate endDate,
      BuySell buySell,
      double notional,
      double spread) {

    ArgChecker.inOrderOrEqual(tradeDate, startDate, "tradeDate", "startDate");
    SwapLeg leg1 = spreadLeg.toLeg(startDate, endDate, PayReceive.ofPay(buySell.isBuy()), notional, spread);
    SwapLeg leg2 = flatLeg.toLeg(startDate, endDate, PayReceive.ofPay(buySell.isSell()), notional);
    return SwapTrade.builder()
        .tradeInfo(TradeInfo.builder()
            .tradeDate(tradeDate)
            .build())
        .product(Swap.of(leg1, leg2))
        .build();
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code IborIborSwapConvention}.
   * @return the meta-bean, not null
   */
  public static IborIborSwapConvention.Meta meta() {
    return IborIborSwapConvention.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(IborIborSwapConvention.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static IborIborSwapConvention.Builder builder() {
    return new IborIborSwapConvention.Builder();
  }

  private IborIborSwapConvention(
      IborRateSwapLegConvention spreadLeg,
      IborRateSwapLegConvention flatLeg,
      DaysAdjustment spotDateOffset) {
    JodaBeanUtils.notNull(spreadLeg, "spreadLeg");
    JodaBeanUtils.notNull(flatLeg, "flatLeg");
    this.spreadLeg = spreadLeg;
    this.flatLeg = flatLeg;
    this.spotDateOffset = spotDateOffset;
    validate();
  }

  @Override
  public IborIborSwapConvention.Meta metaBean() {
    return IborIborSwapConvention.Meta.INSTANCE;
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
   * Gets the market convention of the floating leg that has the spread applied.
   * <p>
   * The spread is the market price of the instrument.
   * It is added to the observed interest rate.
   * @return the value of the property, not null
   */
  public IborRateSwapLegConvention getSpreadLeg() {
    return spreadLeg;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the market convention of the floating leg that does not have the spread applied.
   * @return the value of the property, not null
   */
  public IborRateSwapLegConvention getFlatLeg() {
    return flatLeg;
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
      IborIborSwapConvention other = (IborIborSwapConvention) obj;
      return JodaBeanUtils.equal(getSpreadLeg(), other.getSpreadLeg()) &&
          JodaBeanUtils.equal(getFlatLeg(), other.getFlatLeg()) &&
          JodaBeanUtils.equal(spotDateOffset, other.spotDateOffset);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(getSpreadLeg());
    hash = hash * 31 + JodaBeanUtils.hashCode(getFlatLeg());
    hash = hash * 31 + JodaBeanUtils.hashCode(spotDateOffset);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(128);
    buf.append("IborIborSwapConvention{");
    buf.append("spreadLeg").append('=').append(getSpreadLeg()).append(',').append(' ');
    buf.append("flatLeg").append('=').append(getFlatLeg()).append(',').append(' ');
    buf.append("spotDateOffset").append('=').append(JodaBeanUtils.toString(spotDateOffset));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code IborIborSwapConvention}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code spreadLeg} property.
     */
    private final MetaProperty<IborRateSwapLegConvention> spreadLeg = DirectMetaProperty.ofImmutable(
        this, "spreadLeg", IborIborSwapConvention.class, IborRateSwapLegConvention.class);
    /**
     * The meta-property for the {@code flatLeg} property.
     */
    private final MetaProperty<IborRateSwapLegConvention> flatLeg = DirectMetaProperty.ofImmutable(
        this, "flatLeg", IborIborSwapConvention.class, IborRateSwapLegConvention.class);
    /**
     * The meta-property for the {@code spotDateOffset} property.
     */
    private final MetaProperty<DaysAdjustment> spotDateOffset = DirectMetaProperty.ofImmutable(
        this, "spotDateOffset", IborIborSwapConvention.class, DaysAdjustment.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "spreadLeg",
        "flatLeg",
        "spotDateOffset");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 1302781851:  // spreadLeg
          return spreadLeg;
        case -778843179:  // flatLeg
          return flatLeg;
        case 746995843:  // spotDateOffset
          return spotDateOffset;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public IborIborSwapConvention.Builder builder() {
      return new IborIborSwapConvention.Builder();
    }

    @Override
    public Class<? extends IborIborSwapConvention> beanType() {
      return IborIborSwapConvention.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code spreadLeg} property.
     * @return the meta-property, not null
     */
    public MetaProperty<IborRateSwapLegConvention> spreadLeg() {
      return spreadLeg;
    }

    /**
     * The meta-property for the {@code flatLeg} property.
     * @return the meta-property, not null
     */
    public MetaProperty<IborRateSwapLegConvention> flatLeg() {
      return flatLeg;
    }

    /**
     * The meta-property for the {@code spotDateOffset} property.
     * @return the meta-property, not null
     */
    public MetaProperty<DaysAdjustment> spotDateOffset() {
      return spotDateOffset;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 1302781851:  // spreadLeg
          return ((IborIborSwapConvention) bean).getSpreadLeg();
        case -778843179:  // flatLeg
          return ((IborIborSwapConvention) bean).getFlatLeg();
        case 746995843:  // spotDateOffset
          return ((IborIborSwapConvention) bean).spotDateOffset;
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
   * The bean-builder for {@code IborIborSwapConvention}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<IborIborSwapConvention> {

    private IborRateSwapLegConvention spreadLeg;
    private IborRateSwapLegConvention flatLeg;
    private DaysAdjustment spotDateOffset;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(IborIborSwapConvention beanToCopy) {
      this.spreadLeg = beanToCopy.getSpreadLeg();
      this.flatLeg = beanToCopy.getFlatLeg();
      this.spotDateOffset = beanToCopy.spotDateOffset;
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 1302781851:  // spreadLeg
          return spreadLeg;
        case -778843179:  // flatLeg
          return flatLeg;
        case 746995843:  // spotDateOffset
          return spotDateOffset;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 1302781851:  // spreadLeg
          this.spreadLeg = (IborRateSwapLegConvention) newValue;
          break;
        case -778843179:  // flatLeg
          this.flatLeg = (IborRateSwapLegConvention) newValue;
          break;
        case 746995843:  // spotDateOffset
          this.spotDateOffset = (DaysAdjustment) newValue;
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
    public IborIborSwapConvention build() {
      return new IborIborSwapConvention(
          spreadLeg,
          flatLeg,
          spotDateOffset);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the market convention of the floating leg that has the spread applied.
     * <p>
     * The spread is the market price of the instrument.
     * It is added to the observed interest rate.
     * @param spreadLeg  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder spreadLeg(IborRateSwapLegConvention spreadLeg) {
      JodaBeanUtils.notNull(spreadLeg, "spreadLeg");
      this.spreadLeg = spreadLeg;
      return this;
    }

    /**
     * Sets the market convention of the floating leg that does not have the spread applied.
     * @param flatLeg  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder flatLeg(IborRateSwapLegConvention flatLeg) {
      JodaBeanUtils.notNull(flatLeg, "flatLeg");
      this.flatLeg = flatLeg;
      return this;
    }

    /**
     * Sets the offset of the spot value date from the trade date, optional with defaulting getter.
     * <p>
     * The offset is applied to the trade date and is typically plus 2 business days.
     * The start date of the swap is relative to the spot date.
     * @param spotDateOffset  the new value
     * @return this, for chaining, not null
     */
    public Builder spotDateOffset(DaysAdjustment spotDateOffset) {
      this.spotDateOffset = spotDateOffset;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(128);
      buf.append("IborIborSwapConvention.Builder{");
      buf.append("spreadLeg").append('=').append(JodaBeanUtils.toString(spreadLeg)).append(',').append(' ');
      buf.append("flatLeg").append('=').append(JodaBeanUtils.toString(flatLeg)).append(',').append(' ');
      buf.append("spotDateOffset").append('=').append(JodaBeanUtils.toString(spotDateOffset));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
