// Copyright (C) 2005 - 2010 Philip Aston
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

package net.grinder.console.model;

import net.grinder.console.common.ConsoleException;
import net.grinder.util.Directory;
import org.springframework.beans.PropertyAccessor;
import org.springframework.beans.PropertyAccessorFactory;

import java.awt.*;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by solcyr on 28/01/2018.
 */
public class Properties {

    /**
     * Convert a property value to an appropriate Clojure data type.
     * @param property an Object to coerce
     * @return the coerced form of the property passed as reference
     */
    static String coerceValue(Object property) {
        if (property instanceof Directory) {
            return coerceValue(((Directory)property).getFile());
        }
        else if (property instanceof File) {
            return ((File)property).getAbsolutePath();
        }
        else if (property instanceof Rectangle) {
            Rectangle r = (Rectangle)property;
            return "[(" +r.getX() + ") (" +r.getY() + ") (" +r.getWidth() + ") (" +r.getHeight() + ")])";
        }
        else {
            return property.toString();
        }
    }

    static Object coerceValue (String property, Class clazz) {
        if (clazz == Directory.class) {
            try {
                return new Directory(new File(property));
            } catch (Directory.DirectoryException e) {
                throw new RuntimeException(e);
            }
        } else if (clazz == File.class) {
            return new File(property);
        } else if (clazz == Rectangle.class) {
            return new Rectangle();
        } else {
            return property;
        }
    }

    /**
     * Convert a property value to an appropriate Clojure data type.
     * @param property an Object to coerce
     * @return the coerced form of the property passed as reference
     */

    /**
     * Return a map representing a ConsoleProperties.
     */
    public static Map<String, String> getProperties (ConsoleProperties properties) throws RuntimeException {
        Map<String, String> result = new HashMap<>();
        try {
            for (PropertyDescriptor propertyDescriptor :
                    Introspector.getBeanInfo(ConsoleProperties.class).getPropertyDescriptors()) {
                Method method = propertyDescriptor.getReadMethod();
                if (method != null) {
                    Object value = method.invoke(properties);
                    if (value != null) {
                        result.put(propertyDescriptor.getName(), coerceValue(value));
                    }
                }
            }
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    public static Map<String, String> setProperties (
            ConsoleProperties properties,
            Map<String, String> newProperties) throws RuntimeException {

        PropertyAccessor myAccessor = PropertyAccessorFactory.forBeanPropertyAccess(properties);
        for (String key : newProperties.keySet()) {
            Class clazz = myAccessor.getPropertyType(key);
            try {
                myAccessor.setPropertyValue(key, coerceValue(newProperties.get(key), clazz));
            }
            catch (Exception e) {
                throw new IllegalArgumentException("No write method for property '" + clazz + "'");
            }
        }
        return newProperties;
    }

    public static String save (ConsoleProperties properties) {
        try {
            properties.save();
            return "success";
        }
        catch (ConsoleException e) {
            throw new RuntimeException(e);
        }
    }
}