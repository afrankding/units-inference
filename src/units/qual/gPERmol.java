package units.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Molar mass gram per mol.
 *
 * @checker_framework.manual #units-checker Units Checker
 */
@UnitsAlias(
        baseUnitComponents = {@BUC(unit = "g", exponent = 1), @BUC(unit = "mol", exponent = -1)})
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface gPERmol {}
