/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.sensitivity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.DoubleUnaryOperator;
import java.util.stream.Collectors;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
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

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.FxConvertible;
import com.opengamma.strata.basics.currency.FxRateProvider;
import com.opengamma.strata.collect.Guavate;

/**
 * A collection of point sensitivities.
 * <p>
 * Contains a list of {@linkplain PointSensitivity point sensitivity} objects,
 * each referring to a specific point on a curve that was queried.
 * The order of the list has no specific meaning, but does allow duplicates.
 * <p>
 * For example, the point sensitivity for present value on a FRA might contain
 * two entries, one for the Ibor forward curve and one for the discount curve.
 * Each entry identifies the date that the curve was queried and the resulting multiplier.
 * <p>
 * When creating an instance, consider using {@link MutablePointSensitivities}.
 * <p>
 * One way of viewing this class is as a {@code Map} from a specific sensitivity
 * key to a {@code double} sensitivity value. However, instead or being structured
 * as a {@code Map}, the data is structured as a {@code List}, with the key and
 * value in each entry.
 */
@BeanDefinition(builderScope = "private")
public final class PointSensitivities
    implements FxConvertible<PointSensitivities>, ImmutableBean, Serializable {

  /**
   * An empty instance.
   */
  private static final PointSensitivities EMPTY = new PointSensitivities(ImmutableList.of());

  /**
   * The point sensitivities.
   * <p>
   * Each entry includes details of the market data query it relates to.
   */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableList<PointSensitivity> sensitivities;

  //-------------------------------------------------------------------------
  /**
   * An empty sensitivity instance.
   * 
   * @return the empty instance
   */
  public static PointSensitivities empty() {
    return EMPTY;
  }

  /**
   * Obtains an instance from an array of sensitivity entries.
   * 
   * @param sensitivity  the sensitivity entry
   * @return the sensitivities instance
   */
  public static PointSensitivities of(PointSensitivity... sensitivity) {
    return PointSensitivities.of(ImmutableList.copyOf(sensitivity));
  }

  /**
   * Obtains an instance from a list of sensitivity entries.
   * 
   * @param sensitivities  the list of sensitivity entries
   * @return the sensitivities instance
   */
  @SuppressWarnings("unchecked")
  public static PointSensitivities of(List<? extends PointSensitivity> sensitivities) {
    return new PointSensitivities((List<PointSensitivity>) sensitivities);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the number of sensitivity entries.
   * 
   * @return the size of the internal list of point sensitivities
   */
  public int size() {
    return sensitivities.size();
  }

  //-------------------------------------------------------------------------
  /**
   * Combines this point sensitivities with another instance.
   * <p>
   * This returns a new sensitivity instance with a combined list of point sensitivities.
   * This instance is immutable and unaffected by this method.
   * The result may contain duplicate point sensitivities.
   * 
   * @param other  the other point sensitivities
   * @return a {@code PointSensitivities} based on this one, with the other instance added
   */
  public PointSensitivities combinedWith(PointSensitivities other) {
    return new PointSensitivities(ImmutableList.<PointSensitivity>builder()
        .addAll(sensitivities)
        .addAll(other.sensitivities)
        .build());
  }

  //-------------------------------------------------------------------------
  /**
   * Multiplies the sensitivities in this instance by the specified factor.
   * <p>
   * The result will consist of the same entries, but with each sensitivity value multiplied.
   * This instance is immutable and unaffected by this method.
   * 
   * @param factor  the multiplicative factor
   * @return a {@code PointSensitivities} based on this one, with each sensitivity multiplied by the factor
   */
  public PointSensitivities multipliedBy(double factor) {
    return mapSensitivities(s -> s * factor);
  }

  /**
   * Applies an operation to the sensitivities in this instance.
   * <p>
   * The result will consist of the same entries, but with the operator applied to each sensitivity value.
   * This instance is immutable and unaffected by this method.
   * <p>
   * This is used to apply a mathematical operation to the sensitivity values.
   * For example, the operator could multiply the sensitivities by a constant, or take the inverse.
   * <pre>
   *   inverse = base.mapSensitivities(value -> 1 / value);
   * </pre>
   *
   * @param operator  the operator to be applied to the sensitivities
   * @return a {@code PointSensitivities} based on this one, with the operator applied to the sensitivity values
   */
  public PointSensitivities mapSensitivities(DoubleUnaryOperator operator) {
    return sensitivities.stream()
        .map(s -> s.withSensitivity(operator.applyAsDouble(s.getSensitivity())))
        .collect(
            Collectors.collectingAndThen(
                Guavate.toImmutableList(),
                PointSensitivities::new));
  }

  /**
   * Normalizes the point sensitivities by sorting and merging.
   * <p>
   * The list of sensitivities is sorted and then merged.
   * Any two entries that represent the same curve query are merged.
   * For example, if there are two point sensitivities that were created based on the same curve,
   * currency and fixing date, then the entries are combined, summing the sensitivity value.
   * <p>
   * The intention is that normalization occurs after gathering all the point sensitivities.
   * <p>
   * This instance is immutable and unaffected by this method.
   * 
   * @return a {@code PointSensitivities} based on this one, with the sensitivities normalized
   */
  public PointSensitivities normalized() {
    if (sensitivities.isEmpty()) {
      return this;
    }
    List<PointSensitivity> mutable = new ArrayList<>();
    for (PointSensitivity sensi : sensitivities) {
      insert(mutable, sensi);
    }
    return new PointSensitivities(mutable);
  }

  //-----------------------------------------------------------------------
  /**
   * Returns a mutable version of this object.
   * <p>
   * The result is an instance of the mutable {@link MutablePointSensitivities}.
   * It will contain the same individual sensitivity entries.
   * 
   * @return the mutable sensitivity instance, not null
   */
  public MutablePointSensitivities toMutable() {
    return new MutablePointSensitivities(sensitivities);
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if this sensitivity equals another within the specified tolerance.
   * <p>
   * This returns true if the two instances have the list of {@code PointSensitivity},
   * where the sensitivity {@code double} values are compared within the specified tolerance.
   * It is expected that this comparator will be used on the normalized version of the sensitivity.
   * 
   * @param other  the other sensitivity
   * @param tolerance  the tolerance
   * @return true if equal up to the tolerance
   */
  public boolean equalWithTolerance(PointSensitivities other, double tolerance) {
    ImmutableList<PointSensitivity> list1 = this.getSensitivities();
    ImmutableList<PointSensitivity> list2 = other.getSensitivities();
    int nbList1 = list1.size();
    int nbList2 = list2.size();
    if (nbList1 != nbList2) {
      return false;
    }
    for (int i1 = 0; i1 < nbList1; i1++) {
      if (list1.get(i1).compareKey(list2.get(i1)) == 0) {
        if (Math.abs(list1.get(i1).getSensitivity() - list2.get(i1).getSensitivity()) > tolerance) {
          return false;
        }
      } else {
        return false;
      }
    }
    return true;
  }

  //-------------------------------------------------------------------------
  @Override
  public PointSensitivities convertedTo(Currency resultCurrency, FxRateProvider rateProvider) {
    List<PointSensitivity> mutable = new ArrayList<>();
    for (PointSensitivity sensi : sensitivities) {
      insert(mutable, sensi.convertedTo(resultCurrency, rateProvider));
    }
    return new PointSensitivities(mutable);
  }

  // inserts a sensitivity into the mutable list in the right location
  // merges the entry with an existing entry if the key matches
  private static void insert(List<PointSensitivity> mutable, PointSensitivity addition) {
    int index = Collections.binarySearch(mutable, addition, PointSensitivity::compareKey);
    if (index >= 0) {
      PointSensitivity base = mutable.get(index);
      double combined = base.getSensitivity() + addition.getSensitivity();
      mutable.set(index, base.withSensitivity(combined));
    } else {
      int insertionPoint = -(index + 1);
      mutable.add(insertionPoint, addition);
    }
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code PointSensitivities}.
   * @return the meta-bean, not null
   */
  public static PointSensitivities.Meta meta() {
    return PointSensitivities.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(PointSensitivities.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private PointSensitivities(
      List<PointSensitivity> sensitivities) {
    JodaBeanUtils.notNull(sensitivities, "sensitivities");
    this.sensitivities = ImmutableList.copyOf(sensitivities);
  }

  @Override
  public PointSensitivities.Meta metaBean() {
    return PointSensitivities.Meta.INSTANCE;
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
   * Gets the point sensitivities.
   * <p>
   * Each entry includes details of the market data query it relates to.
   * @return the value of the property, not null
   */
  public ImmutableList<PointSensitivity> getSensitivities() {
    return sensitivities;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      PointSensitivities other = (PointSensitivities) obj;
      return JodaBeanUtils.equal(sensitivities, other.sensitivities);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(sensitivities);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(64);
    buf.append("PointSensitivities{");
    buf.append("sensitivities").append('=').append(JodaBeanUtils.toString(sensitivities));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code PointSensitivities}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code sensitivities} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableList<PointSensitivity>> sensitivities = DirectMetaProperty.ofImmutable(
        this, "sensitivities", PointSensitivities.class, (Class) ImmutableList.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "sensitivities");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 1226228605:  // sensitivities
          return sensitivities;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends PointSensitivities> builder() {
      return new PointSensitivities.Builder();
    }

    @Override
    public Class<? extends PointSensitivities> beanType() {
      return PointSensitivities.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code sensitivities} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableList<PointSensitivity>> sensitivities() {
      return sensitivities;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 1226228605:  // sensitivities
          return ((PointSensitivities) bean).getSensitivities();
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
   * The bean-builder for {@code PointSensitivities}.
   */
  private static final class Builder extends DirectFieldsBeanBuilder<PointSensitivities> {

    private List<PointSensitivity> sensitivities = ImmutableList.of();

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 1226228605:  // sensitivities
          return sensitivities;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 1226228605:  // sensitivities
          this.sensitivities = (List<PointSensitivity>) newValue;
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
    public PointSensitivities build() {
      return new PointSensitivities(
          sensitivities);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(64);
      buf.append("PointSensitivities.Builder{");
      buf.append("sensitivities").append('=').append(JodaBeanUtils.toString(sensitivities));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
