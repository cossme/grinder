// Copyright (C) 2005 - 2009 Philip Aston
// All rights reserved.
//
// This file is part of The Grinder software distribution. Refer to
// the file LICENSE which is part of The Grinder distribution for
// licensing details. The Grinder distribution is available on the
// Internet at http://grinder.sourceforge.net/
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
// FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
// COPYRIGHT HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
// INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
// SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
// HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
// STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
// OF THE POSSIBILITY OF SUCH DAMAGE.

package net.grinder.util;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import net.grinder.common.GrinderException;


/**
 * Introspects a boolean property of a Java Bean and provides setter
 * and getter methods.
 *
 * @author Philip Aston
 */
public final class BooleanProperty {
  private final Object m_bean;
  private final Class<?> m_beanClass;
  private final PropertyDescriptor m_propertyDescriptor;

  /**
   * Constructor.
   *
   * @param bean Bean to introspect.
   * @param propertyName The property.
   * @throws PropertyException If a boolean property of the given name
   * could not be found.
   */
  public BooleanProperty(Object bean, String propertyName)
    throws PropertyException {

    m_bean = bean;
    m_beanClass = bean.getClass();

    try {
      m_propertyDescriptor = new PropertyDescriptor(propertyName, m_beanClass);
    }
    catch (IntrospectionException e) {
      throw new PropertyException(
        "Could not find property '" + propertyName + "' in class '" +
        m_beanClass + "'", e);
    }

    final Class<?> propertyType = m_propertyDescriptor.getPropertyType();

    if (!propertyType.equals(Boolean.TYPE) &&
        !propertyType.equals(Boolean.class)) {
      throw new PropertyException(toString() + ": property is not boolean");
    }
  }

  /**
   * Getter method.
   *
   * @return The current value of the property.
   * @throws PropertyException If the value could not be read.
   */
  public boolean get() throws PropertyException {

    // Despite what the JavaDoc for PropertyDescriptor.getReadMethod()
    // says, this is guaranteed to be non-null if the property name is
    // non-null.
    final Method readMethod = m_propertyDescriptor.getReadMethod();

    try {
      final Boolean result = (Boolean)readMethod.invoke(m_bean, new Object[0]);
      return result.booleanValue();
    }
    catch (IllegalAccessException e) {
      throw new PropertyException(toString() + ": could not read", e);
    }
    catch (InvocationTargetException e) {
      throw new PropertyException(toString() + ": could not read",
                                  e.getTargetException());
    }
  }

  /**
   * Setter method.
   *
   * @param value The new value of the property.
   * @throws PropertyException If the value could not be written.
   */
  public void set(boolean value) throws PropertyException {
    // Despite what the JavaDoc for
    // PropertyDescriptor.getWriteMethod() says, this is guaranteed to
    // be non-null if the property name is non-null.
    final Method writeMethod = m_propertyDescriptor.getWriteMethod();

    try {
      writeMethod.invoke(
        m_bean, new Object[] { value ? Boolean.TRUE : Boolean.FALSE });
    }
    catch (IllegalAccessException e) {
      throw new PropertyException(toString() + ": could not write", e);
    }
    catch (InvocationTargetException e) {
      throw new PropertyException(toString() + ": could not write",
                                  e.getTargetException());
    }
  }

  /**
   * Describe the property.
   *
   * @return  A description of the property.
   */
  public String toString() {
    return m_beanClass.getName() + "." + m_propertyDescriptor.getName();
  }

  /**
   * Indicates a problem with accessing the property.
   */
  public static final class PropertyException extends GrinderException {

    private PropertyException(String message) {
      super(message);
    }

    private PropertyException(String message, Throwable t) {
      super(message, t);
    }
  }
}
