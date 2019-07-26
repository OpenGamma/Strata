/*
 * Copyright (C) 2019 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
import org.joda.beans.ImmutableBean;
import org.joda.beans.MetaBean;
import org.joda.beans.MetaProperty;
import org.joda.beans.PropertyStyle;
import org.joda.beans.impl.BasicImmutableBeanBuilder;
import org.joda.beans.impl.BasicMetaBean;
import org.joda.beans.impl.BasicMetaProperty;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.collect.ArgChecker;

/**
 * A byte source implementation that obtains data from a file.
 * <p>
 * This implementation differs from Guava in that it is is a Joda-Bean.
 * In addition, {@link #read()} throws {@link UncheckedIOException} instead of {@link IOException}.
 */
public final class FileByteSource extends BeanByteSource implements ImmutableBean, Serializable {

  /**
   * Serialization version.
   */
  private static final long serialVersionUID = 1L;
  static {
    MetaBean.register(Meta.META);
  }

  /**
   * The underlying file.
   */
  private final File file;

  //-------------------------------------------------------------------------
  /**
   * Creates an instance based on the underlying file.
   * 
   * @param file  the file
   * @return the byte source
   */
  public static FileByteSource of(File file) {
    return new FileByteSource(file);
  }

  /**
   * Creates an instance based on a file path.
   * 
   * @param path  the path to a file
   * @return the byte source
   */
  public static FileByteSource of(Path path) {
    return new FileByteSource(path.toFile());
  }

  //-------------------------------------------------------------------------
  /**
   * Creates an instance.
   * 
   * @param array  the file
   */
  private FileByteSource(File file) {
    this.file = ArgChecker.notNull(file, "file");
  }

  //-------------------------------------------------------------------------
  @Override
  public MetaBean metaBean() {
    return Meta.META;
  }

  /**
   * Gets the File.
   * 
   * @return the File
   */
  public File getFile() {
    return file;
  }

  //-------------------------------------------------------------------------
  @Override
  public InputStream openStream() throws IOException {
    return new FileInputStream(file);
  }

  @Override
  public Optional<Long> sizeIfKnown() {
    if (file.isFile()) {
      return Optional.of(file.length());
    } else {
      return Optional.absent();
    }
  }

  @Override
  public long size() {
    if (!file.isFile()) {
      throw new UncheckedIOException(new FileNotFoundException(file.toString()));
    }
    return file.length();
  }

  //-------------------------------------------------------------------------
  @Override
  public boolean equals(Object other) {
    if (other instanceof FileByteSource) {
      return file.equals(((FileByteSource) other).file);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return file.hashCode();
  }

  @Override
  public String toString() {
    return "FileByteSource[" + file + "]";
  }

  //-------------------------------------------------------------------------
  /**
   * Meta bean.
   */
  static final class Meta extends BasicMetaBean {

    private static final MetaBean META = new Meta();
    private static final MetaProperty<File> PROP_FILE = new BasicMetaProperty<File>("file") {

      @Override
      public MetaBean metaBean() {
        return META;
      }

      @Override
      public Class<?> declaringType() {
        return FileByteSource.class;
      }

      @Override
      public Class<File> propertyType() {
        return File.class;
      }

      @Override
      public Type propertyGenericType() {
        return File.class;
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
      public File get(Bean bean) {
        return ((FileByteSource) bean).file;
      }

      @Override
      public void set(Bean bean, Object value) {
        throw new UnsupportedOperationException("Property cannot be written: " + name());
      }
    };
    private static final ImmutableMap<String, MetaProperty<?>> MAP = ImmutableMap.of("file", PROP_FILE);

    private Meta() {
    }

    @Override
    public boolean isBuildable() {
      return true;
    }

    @Override
    public BeanBuilder<FileByteSource> builder() {
      return new BasicImmutableBeanBuilder<FileByteSource>(this) {
        private File file;

        @Override
        public Object get(String propertyName) {
          if (propertyName.equals(PROP_FILE.name())) {
            return file;
          } else {
            throw new NoSuchElementException("Unknown property: " + propertyName);
          }
        }

        @Override
        public BeanBuilder<FileByteSource> set(String propertyName, Object value) {
          if (propertyName.equals(PROP_FILE.name())) {
            this.file = ((File) ArgChecker.notNull(value, "value"));
          } else {
            throw new NoSuchElementException("Unknown property: " + propertyName);
          }
          return this;
        }

        @Override
        public FileByteSource build() {
          return new FileByteSource(file);
        }
      };
    }

    @Override
    public Class<? extends Bean> beanType() {
      return FileByteSource.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return MAP;
    }
  }

}
