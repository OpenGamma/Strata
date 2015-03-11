/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.sensitivity;

import java.io.Serializable;
import java.util.ArrayList;
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
import com.opengamma.collect.Guavate;

/**
 * Sensitivity to a group of curves.
 * <p>
 * Contains a list of {@linkplain CurveSensitivity point sensitivity} objects, each
 * referring to a specific point on a curve that was queried.
 * The order of the list has no specific meaning, but does allow duplicates.
 * <p>
 * When creating an instance, consider using {@link MutableCurveGroupSensitivity}.
 */
@BeanDefinition(builderScope = "private")
public final class CurveGroupSensitivity
    implements ImmutableBean, Serializable {

  /**
   * An empty group sensitivities instance.
   */
  public static final CurveGroupSensitivity EMPTY = new CurveGroupSensitivity(ImmutableList.of());

  /**
   * The point sensitivities.
   * <p>
   * Each entry includes details of the curve it relates to.
   */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableList<CurveSensitivity> sensitivities;

  //-------------------------------------------------------------------------
  /**
   * Obtains a {@code CurveGroupSensitivity} from a list of point sensitivities.
   * 
   * @param sensitivities  the list of sensitivities
   * @return the sensitivities instance
   */
  @SuppressWarnings("unchecked")
  public static CurveGroupSensitivity of(List<? extends CurveSensitivity> sensitivities) {
    return new CurveGroupSensitivity((List<CurveSensitivity>) sensitivities);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the number of point sensitivities.
   * 
   * @return the size of the internal list of point sensitivities
   */
  public int size() {
    return sensitivities.size();
  }

  //-------------------------------------------------------------------------
  /**
   * Merges this group sensitivity with another instance.
   * <p>
   * This returns a new group sensitivity with a combined list of point sensitivities.
   * This instance is immutable and unaffected by this method.
   * The result may contain duplicate point sensitivities.
   * 
   * @param other  the other group sensitivity
   * @return a {@code CurveGroupSensitivity} based on this one, with the other instance added
   */
  public CurveGroupSensitivity combinedWith(CurveGroupSensitivity other) {
    return new CurveGroupSensitivity(ImmutableList.<CurveSensitivity>builder()
        .addAll(sensitivities)
        .addAll(other.sensitivities)
        .build());
  }

  /**
   * Multiplies the point sensitivities by the specified factor.
   * <p>
   * The result will consist of the same points, but with each sensitivity multiplied.
   * This instance is immutable and unaffected by this method. 
   * 
   * @param factor  the multiplicative factor
   * @return a {@code CurveGroupSensitivity} based on this one, with each sensitivity multiplied by the factor
   */
  public CurveGroupSensitivity multipliedBy(double factor) {
    return mapSensitivities(s -> s * factor);
  }

  /**
   * Applies an operation to the point sensitivities.
   * <p>
   * The result will consist of the same points, but with the operator applied to each sensitivity.
   * This instance is immutable and unaffected by this method. 
   * <p>
   * This is used to apply a mathematical operation to the sensitivities.
   * For example, the operator could multiply the sensitivities by a constant, or take the inverse.
   * <pre>
   *   multiplied = base.mapSensitivities(value -> 1 / value);
   * </pre>
   *
   * @param operator  the operator to be applied to the sensitivities
   * @return a {@code CurveGroupSensitivity} based on this one, with the operator applied to the point sensitivities
   */
  public CurveGroupSensitivity mapSensitivities(DoubleUnaryOperator operator) {
    return sensitivities.stream()
        .map(cs -> cs.withSensitivity(operator.applyAsDouble(cs.getSensitivity())))
        .collect(
            Collectors.collectingAndThen(
                Guavate.toImmutableList(),
                CurveGroupSensitivity::new));
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
   * @return a {@code CurveGroupSensitivity} based on this one, with the the sensitivities normalized
   */
  public CurveGroupSensitivity normalized() {
    if (sensitivities.isEmpty()) {
      return this;
    }
    List<CurveSensitivity> mutable = new ArrayList<>(sensitivities);
    mutable.sort(CurveSensitivity::compareExcludingSensitivity);
    CurveSensitivity last = mutable.get(0);
    for (int i = 1; i < mutable.size(); i++) {
      CurveSensitivity current = mutable.get(i);
      if (current.compareExcludingSensitivity(last) == 0) {
        mutable.set(i - 1, last.withSensitivity(last.getSensitivity() + current.getSensitivity()));
        mutable.remove(i);
        i--;
      }
      last = current;
    }
    return new CurveGroupSensitivity(mutable);
  }

  //-----------------------------------------------------------------------
  /**
   * Returns a mutable version of this object.
   * <p>
   * The result is the mutable {@link MutableCurveGroupSensitivity} class.
   * It will contain the same individual point sensitivities.
   * 
   * @return the mutable sensitivity instance, not null
   */
  public MutableCurveGroupSensitivity toMutable() {
    return new MutableCurveGroupSensitivity(sensitivities);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code CurveGroupSensitivity}.
   * @return the meta-bean, not null
   */
  public static CurveGroupSensitivity.Meta meta() {
    return CurveGroupSensitivity.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(CurveGroupSensitivity.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private CurveGroupSensitivity(
      List<CurveSensitivity> sensitivities) {
    JodaBeanUtils.notNull(sensitivities, "sensitivities");
    this.sensitivities = ImmutableList.copyOf(sensitivities);
  }

  @Override
  public CurveGroupSensitivity.Meta metaBean() {
    return CurveGroupSensitivity.Meta.INSTANCE;
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
   * Each entry includes details of the curve it relates to.
   * @return the value of the property, not null
   */
  public ImmutableList<CurveSensitivity> getSensitivities() {
    return sensitivities;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      CurveGroupSensitivity other = (CurveGroupSensitivity) obj;
      return JodaBeanUtils.equal(getSensitivities(), other.getSensitivities());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(getSensitivities());
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(64);
    buf.append("CurveGroupSensitivity{");
    buf.append("sensitivities").append('=').append(JodaBeanUtils.toString(getSensitivities()));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code CurveGroupSensitivity}.
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
    private final MetaProperty<ImmutableList<CurveSensitivity>> sensitivities = DirectMetaProperty.ofImmutable(
        this, "sensitivities", CurveGroupSensitivity.class, (Class) ImmutableList.class);
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
    public BeanBuilder<? extends CurveGroupSensitivity> builder() {
      return new CurveGroupSensitivity.Builder();
    }

    @Override
    public Class<? extends CurveGroupSensitivity> beanType() {
      return CurveGroupSensitivity.class;
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
    public MetaProperty<ImmutableList<CurveSensitivity>> sensitivities() {
      return sensitivities;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 1226228605:  // sensitivities
          return ((CurveGroupSensitivity) bean).getSensitivities();
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
   * The bean-builder for {@code CurveGroupSensitivity}.
   */
  private static final class Builder extends DirectFieldsBeanBuilder<CurveGroupSensitivity> {

    private List<CurveSensitivity> sensitivities = ImmutableList.of();

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
          this.sensitivities = (List<CurveSensitivity>) newValue;
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
    public CurveGroupSensitivity build() {
      return new CurveGroupSensitivity(
          sensitivities);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(64);
      buf.append("CurveGroupSensitivity.Builder{");
      buf.append("sensitivities").append('=').append(JodaBeanUtils.toString(sensitivities));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
