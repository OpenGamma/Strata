/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.array;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyStyle;
import org.joda.beans.impl.BasicImmutableBeanBuilder;
import org.joda.beans.impl.BasicMetaBean;
import org.joda.beans.impl.BasicMetaProperty;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.DoubleArrayMath;

/**
 * An immutable array of {@code double} values, boxed to {@code Double}.
 * <p>
 * This is used to wrap a {@code double[]} to meet the API of {@link Array}.
 * An instance is created using {@link DoubleArray#boxed()}.
 */
final class BoxedDoubleArray
    implements Array<Double>, ImmutableBean, Serializable {

  /**
   * Serialization version.
   */
  private static final long serialVersionUID = 1L;
  static {
    JodaBeanUtils.registerMetaBean(Meta.META);
  }

  /**
   * The underlying array of doubles.
   */
  private final double[] array;

  //-------------------------------------------------------------------------
  /**
   * Creates an instance from a {@code double[}.
   * 
   * @param array  the array, assigned not cloned
   */
  BoxedDoubleArray(double[] array) {
    this.array = array;
  }

  //-------------------------------------------------------------------------
  @Override
  public int size() {
    return array.length;
  }

  @Override
  public boolean isEmpty() {
    return array.length == 0;
  }

  @Override
  public Double get(int index) {
    return array[index];
  }

  @Override
  public Stream<Double> stream() {
    return DoubleStream.of(array).boxed();
  }

  //-------------------------------------------------------------------------
  @Override
  public MetaBean metaBean() {
    return Meta.META;
  }

  @Override
  public <R> Property<R> property(String propertyName) {
    return metaBean().<R>metaProperty(propertyName).createProperty(this);
  }

  @Override
  public Set<String> propertyNames() {
    return metaBean().metaPropertyMap().keySet();
  }

  //-------------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof BoxedDoubleArray) {
      BoxedDoubleArray other = (BoxedDoubleArray) obj;
      return Arrays.equals(array, other.array);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(array);
  }

  @Override
  public String toString() {
    return Arrays.toString(array);
  }

  //-------------------------------------------------------------------------
  /**
   * Meta bean.
   */
  static final class Meta extends BasicMetaBean {

    private static final MetaBean META = new Meta();
    private static final MetaProperty<double[]> ARRAY = new BasicMetaProperty<double[]>("array") {

      @Override
      public MetaBean metaBean() {
        return META;
      }

      @Override
      public Class<?> declaringType() {
        return BoxedDoubleArray.class;
      }

      @Override
      public Class<double[]> propertyType() {
        return double[].class;
      }

      @Override
      public Type propertyGenericType() {
        return double[].class;
      }

      @Override
      public PropertyStyle style() {
        return PropertyStyle.IMMUTABLE;
      }

      @Override
      public List<Annotation> annotations() {
        return ImmutableList.of();
      }

      @Override
      public double[] get(Bean bean) {
        return ((BoxedDoubleArray) bean).array.clone();
      }

      @Override
      public void set(Bean bean, Object value) {
        throw new UnsupportedOperationException("Property cannot be written: " + name());
      }
    };
    private static final ImmutableMap<String, MetaProperty<?>> MAP = ImmutableMap.of("array", ARRAY);

    private Meta() {
    }

    @Override
    public BeanBuilder<BoxedDoubleArray> builder() {
      return new BasicImmutableBeanBuilder<BoxedDoubleArray>(this) {
        private double[] array = DoubleArrayMath.EMPTY_DOUBLE_ARRAY;

        @Override
        public Object get(String propertyName) {
          if (propertyName.equals(ARRAY.name())) {
            return array.clone();
          } else {
            throw new NoSuchElementException("Unknown property: " + propertyName);
          }
        }

        @Override
        public BeanBuilder<BoxedDoubleArray> set(String propertyName, Object value) {
          if (propertyName.equals(ARRAY.name())) {
            this.array = ((double[]) ArgChecker.notNull(value, "value")).clone();
          } else {
            throw new NoSuchElementException("Unknown property: " + propertyName);
          }
          return this;
        }

        @Override
        public BoxedDoubleArray build() {
          return new BoxedDoubleArray(array);
        }
      };
    }

    @Override
    public Class<? extends Bean> beanType() {
      return BoxedDoubleArray.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return MAP;
    }
  }

}
