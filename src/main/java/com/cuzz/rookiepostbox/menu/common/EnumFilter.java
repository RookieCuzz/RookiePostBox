package com.cuzz.rookiepostbox.menu.common;

public interface EnumFilter<T extends Enum<T> & EnumFilter<T>> {

    @SuppressWarnings("unchecked")
    default T currentEnumConstant() {
        return (T) this;
    }

    default T next() {
        return this.get(true);
    }

    default T previous() {
        return this.get(false);
    }

    @SuppressWarnings("rawtypes, unchecked")
    default T get(boolean next) {
        try {
            int ordinal = currentEnumConstant().ordinal() + ((next) ? 1 : -1);

            Enum[] enumConstants = currentEnumConstant().getClass().getEnumConstants();
            for (Enum constant : enumConstants) {
                if (constant.ordinal() == ordinal) {
                    return (T) constant;
                }
            }

            int index = (next) ? 0 : enumConstants.length - 1;
            return (T) enumConstants[index];
        } catch (Exception exception) {
            return currentEnumConstant();
        }
    }
}