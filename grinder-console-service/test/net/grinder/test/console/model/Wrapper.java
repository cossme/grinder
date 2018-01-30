package net.grinder.test.console.model;

/**
 * Created by solcyr on 30/01/2018.
 */
class Wrapper<T> {
    T value;

    public Wrapper() {
        value = null;
    }
    public Wrapper(T initialValue) {
        value = initialValue;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T newValue) {
        value = newValue;
    }
}
