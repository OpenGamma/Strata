/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap.type;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.joda.beans.Bean;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.MetaProperty;
import org.joda.beans.gen.BeanDefinition;
import org.joda.beans.gen.ImmutableValidator;
import org.joda.beans.gen.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.common.PayReceive;
import com.opengamma.strata.product.swap.Swap;
import com.opengamma.strata.product.swap.SwapLeg;
import com.opengamma.strata.product.swap.SwapTrade;

/**
 * A market convention for Fixed-Ibor swap trades.
 * <p>
 * This defines the market convention for a Fixed-Ibor single currency swap.
 * This is often known as a <i>vanilla swap</i>.
 * The convention is formed by combining two swap leg conventions in the same currency.
 * <p>
 * The convention is defined by four key dates.
 * <ul>
 * <li>Trade date, the date that the trade is agreed
 * <li>Spot date, the base for date calculations, typically 2 business days after the trade date
 * <li>Start date, the date on which the interest calculation starts, often the same as the spot date
 * <li>End date, the date on which the interest calculation ends, typically a number of years after the start date
 * </ul>
 */
@BeanDefinition
public final class ImmutableFixedIborSwapConvention
    implements FixedIborSwapConvention, ImmutableBean, Serializable {

  /**
   * The convention name, such as 'USD-FIXED-6M-LIBOR-3M'.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final String name;
  /**
   * The market convention of the fixed leg.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final FixedRateSwapLegConvention fixedLeg;
  /**
   * The market convention of the floating leg.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final IborRateSwapLegConvention floatingLeg;
  /**
   * The offset of the spot value date from the trade date.
   * <p>
   * The offset is applied to the trade date to find the start date.
   * A typical value is "plus 2 business days".
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final DaysAdjustment spotDateOffset;

  //-------------------------------------------------------------------------
  /**
   * Obtains a convention based on the specified name and leg conventions.
   * <p>
   * The two leg conventions must be in the same currency.
   * The spot date offset is set to be the effective date offset of the index.
   * 
   * @param name  the unique name of the convention 
   * @param fixedLeg  the market convention for the fixed leg
   * @param floatingLeg  the market convention for the floating leg
   * @return the convention
   */
  public static ImmutableFixedIborSwapConvention of(
      String name,
      FixedRateSwapLegConvention fixedLeg,
      IborRateSwapLegConvention floatingLeg) {

    return of(name, fixedLeg, floatingLeg, floatingLeg.getIndex().getEffectiveDateOffset());
  }

  /**
   * Obtains a convention based on the specified name and leg conventions.
   * <p>
   * The two leg conventions must be in the same currency.
   * 
   * @param name  the unique name of the convention 
   * @param fixedLeg  the market convention for the fixed leg
   * @param floatingLeg  the market convention for the floating leg
   * @param spotDateOffset  the offset of the spot value date from the trade date
   * @return the convention
   */
  public static ImmutableFixedIborSwapConvention of(
      String name,
      FixedRateSwapLegConvention fixedLeg,
      IborRateSwapLegConvention floatingLeg,
      DaysAdjustment spotDateOffset) {

    return new ImmutableFixedIborSwapConvention(name, fixedLeg, floatingLeg, spotDateOffset);
  }

  //-------------------------------------------------------------------------
  @ImmutableValidator
  private void validate() {
    ArgChecker.isTrue(fixedLeg.getCurrency().equals(floatingLeg.getCurrency()), "Conventions must have same currency");
  }

  //-------------------------------------------------------------------------
  @Override
  public SwapTrade toTrade(
      TradeInfo tradeInfo,
      LocalDate startDate,
      LocalDate endDate,
      BuySell buySell,
      double notional,
      double fixedRate) {

    Optional<LocalDate> tradeDate = tradeInfo.getTradeDate();
    if (tradeDate.isPresent()) {
      ArgChecker.inOrderOrEqual(tradeDate.get(), startDate, "tradeDate", "startDate");
    }
    SwapLeg leg1 = fixedLeg.toLeg(startDate, endDate, PayReceive.ofPay(buySell.isBuy()), notional, fixedRate);
    SwapLeg leg2 = floatingLeg.toLeg(startDate, endDate, PayReceive.ofPay(buySell.isSell()), notional);
    return SwapTrade.builder()
        .info(tradeInfo)
        .product(Swap.of(leg1, leg2))
        .build();
  }

  //-------------------------------------------------------------------------
  @Override
  public String toString() {
    return getName();
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code ImmutableFixedIborSwapConvention}.
   * @return the meta-bean, not null
   */
  public static ImmutableFixedIborSwapConvention.Meta meta() {
    return ImmutableFixedIborSwapConvention.Meta.INSTANCE;
  }

  static {
    MetaBean.register(ImmutableFixedIborSwapConvention.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static ImmutableFixedIborSwapConvention.Builder builder() {
    return new ImmutableFixedIborSwapConvention.Builder();
  }

  private ImmutableFixedIborSwapConvention(
      String name,
      FixedRateSwapLegConvention fixedLeg,
      IborRateSwapLegConvention floatingLeg,
      DaysAdjustment spotDateOffset) {
    JodaBeanUtils.notNull(name, "name");
    JodaBeanUtils.notNull(fixedLeg, "fixedLeg");
    JodaBeanUtils.notNull(floatingLeg, "floatingLeg");
    JodaBeanUtils.notNull(spotDateOffset, "spotDateOffset");
    this.name = name;
    this.fixedLeg = fixedLeg;
    this.floatingLeg = floatingLeg;
    this.spotDateOffset = spotDateOffset;
    validate();
  }

  @Override
  public ImmutableFixedIborSwapConvention.Meta metaBean() {
    return ImmutableFixedIborSwapConvention.Meta.INSTANCE;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the convention name, such as 'USD-FIXED-6M-LIBOR-3M'.
   * @return the value of the property, not null
   */
  @Override
  public String getName() {
    return name;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the market convention of the fixed leg.
   * @return the value of the property, not null
   */
  @Override
  public FixedRateSwapLegConvention getFixedLeg() {
    return fixedLeg;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the market convention of the floating leg.
   * @return the value of the property, not null
   */
  @Override
  public IborRateSwapLegConvention getFloatingLeg() {
    return floatingLeg;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the offset of the spot value date from the trade date.
   * <p>
   * The offset is applied to the trade date to find the start date.
   * A typical value is "plus 2 business days".
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
      ImmutableFixedIborSwapConvention other = (ImmutableFixedIborSwapConvention) obj;
      return JodaBeanUtils.equal(name, other.name) &&
          JodaBeanUtils.equal(fixedLeg, other.fixedLeg) &&
          JodaBeanUtils.equal(floatingLeg, other.floatingLeg) &&
          JodaBeanUtils.equal(spotDateOffset, other.spotDateOffset);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(name);
    hash = hash * 31 + JodaBeanUtils.hashCode(fixedLeg);
    hash = hash * 31 + JodaBeanUtils.hashCode(floatingLeg);
    hash = hash * 31 + JodaBeanUtils.hashCode(spotDateOffset);
    return hash;
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code ImmutableFixedIborSwapConvention}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code name} property.
     */
    private final MetaProperty<String> name = DirectMetaProperty.ofImmutable(
        this, "name", ImmutableFixedIborSwapConvention.class, String.class);
    /**
     * The meta-property for the {@code fixedLeg} property.
     */
    private final MetaProperty<FixedRateSwapLegConvention> fixedLeg = DirectMetaProperty.ofImmutable(
        this, "fixedLeg", ImmutableFixedIborSwapConvention.class, FixedRateSwapLegConvention.class);
    /**
     * The meta-property for the {@code floatingLeg} property.
     */
    private final MetaProperty<IborRateSwapLegConvention> floatingLeg = DirectMetaProperty.ofImmutable(
        this, "floatingLeg", ImmutableFixedIborSwapConvention.class, IborRateSwapLegConvention.class);
    /**
     * The meta-property for the {@code spotDateOffset} property.
     */
    private final MetaProperty<DaysAdjustment> spotDateOffset = DirectMetaProperty.ofImmutable(
        this, "spotDateOffset", ImmutableFixedIborSwapConvention.class, DaysAdjustment.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "name",
        "fixedLeg",
        "floatingLeg",
        "spotDateOffset");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 3373707:  // name
          return name;
        case -391537158:  // fixedLeg
          return fixedLeg;
        case -1177101272:  // floatingLeg
          return floatingLeg;
        case 746995843:  // spotDateOffset
          return spotDateOffset;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public ImmutableFixedIborSwapConvention.Builder builder() {
      return new ImmutableFixedIborSwapConvention.Builder();
    }

    @Override
    public Class<? extends ImmutableFixedIborSwapConvention> beanType() {
      return ImmutableFixedIborSwapConvention.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code name} property.
     * @return the meta-property, not null
     */
    public MetaProperty<String> name() {
      return name;
    }

    /**
     * The meta-property for the {@code fixedLeg} property.
     * @return the meta-property, not null
     */
    public MetaProperty<FixedRateSwapLegConvention> fixedLeg() {
      return fixedLeg;
    }

    /**
     * The meta-property for the {@code floatingLeg} property.
     * @return the meta-property, not null
     */
    public MetaProperty<IborRateSwapLegConvention> floatingLeg() {
      return floatingLeg;
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
        case 3373707:  // name
          return ((ImmutableFixedIborSwapConvention) bean).getName();
        case -391537158:  // fixedLeg
          return ((ImmutableFixedIborSwapConvention) bean).getFixedLeg();
        case -1177101272:  // floatingLeg
          return ((ImmutableFixedIborSwapConvention) bean).getFloatingLeg();
        case 746995843:  // spotDateOffset
          return ((ImmutableFixedIborSwapConvention) bean).getSpotDateOffset();
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
   * The bean-builder for {@code ImmutableFixedIborSwapConvention}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<ImmutableFixedIborSwapConvention> {

    private String name;
    private FixedRateSwapLegConvention fixedLeg;
    private IborRateSwapLegConvention floatingLeg;
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
    private Builder(ImmutableFixedIborSwapConvention beanToCopy) {
      this.name = beanToCopy.getName();
      this.fixedLeg = beanToCopy.getFixedLeg();
      this.floatingLeg = beanToCopy.getFloatingLeg();
      this.spotDateOffset = beanToCopy.getSpotDateOffset();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 3373707:  // name
          return name;
        case -391537158:  // fixedLeg
          return fixedLeg;
        case -1177101272:  // floatingLeg
          return floatingLeg;
        case 746995843:  // spotDateOffset
          return spotDateOffset;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 3373707:  // name
          this.name = (String) newValue;
          break;
        case -391537158:  // fixedLeg
          this.fixedLeg = (FixedRateSwapLegConvention) newValue;
          break;
        case -1177101272:  // floatingLeg
          this.floatingLeg = (IborRateSwapLegConvention) newValue;
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
    public ImmutableFixedIborSwapConvention build() {
      return new ImmutableFixedIborSwapConvention(
          name,
          fixedLeg,
          floatingLeg,
          spotDateOffset);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the convention name, such as 'USD-FIXED-6M-LIBOR-3M'.
     * @param name  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder name(String name) {
      JodaBeanUtils.notNull(name, "name");
      this.name = name;
      return this;
    }

    /**
     * Sets the market convention of the fixed leg.
     * @param fixedLeg  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder fixedLeg(FixedRateSwapLegConvention fixedLeg) {
      JodaBeanUtils.notNull(fixedLeg, "fixedLeg");
      this.fixedLeg = fixedLeg;
      return this;
    }

    /**
     * Sets the market convention of the floating leg.
     * @param floatingLeg  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder floatingLeg(IborRateSwapLegConvention floatingLeg) {
      JodaBeanUtils.notNull(floatingLeg, "floatingLeg");
      this.floatingLeg = floatingLeg;
      return this;
    }

    /**
     * Sets the offset of the spot value date from the trade date.
     * <p>
     * The offset is applied to the trade date to find the start date.
     * A typical value is "plus 2 business days".
     * @param spotDateOffset  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder spotDateOffset(DaysAdjustment spotDateOffset) {
      JodaBeanUtils.notNull(spotDateOffset, "spotDateOffset");
      this.spotDateOffset = spotDateOffset;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(160);
      buf.append("ImmutableFixedIborSwapConvention.Builder{");
      buf.append("name").append('=').append(JodaBeanUtils.toString(name)).append(',').append(' ');
      buf.append("fixedLeg").append('=').append(JodaBeanUtils.toString(fixedLeg)).append(',').append(' ');
      buf.append("floatingLeg").append('=').append(JodaBeanUtils.toString(floatingLeg)).append(',').append(' ');
      buf.append("spotDateOffset").append('=').append(JodaBeanUtils.toString(spotDateOffset));
      buf.append('}');
      return buf.toString();
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
