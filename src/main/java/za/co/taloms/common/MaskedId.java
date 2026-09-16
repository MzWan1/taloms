package za.co.taloms.common;

import java.lang.annotation.*;

/**
 * Marks a field to be masked in JSON responses.
 * Only the annotated fields will be masked; other Long values (counts, timestamps, etc.)
 * remain unaffected.
 * <p>
 * Masking format: first 6 characters + "***" + last 3 characters.
 * Example: 1234567890123 → "123456***012"
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MaskedId {
    /**
     * Whether to mask this field as an ID number (e.g. SA ID number) instead of a numeric ID.
     * ID numbers get special formatting.
     */
    boolean idNumber() default false;

    /**
     * Custom mask pattern - if empty, uses default (first 6 + *** + last 3).
     * Use {prefix} and {suffix} placeholders for variable-length masks.
     */
    String pattern() default "";
}


