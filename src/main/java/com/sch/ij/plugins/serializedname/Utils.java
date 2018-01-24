package com.sch.ij.plugins.serializedname;

import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiModifierList;

public class Utils {
    public static PsiAnnotation[] getAnnotations(PsiField field) {
        final PsiModifierList modifierList = field.getModifierList();
        return modifierList != null ? modifierList.getAnnotations() : PsiAnnotation.EMPTY_ARRAY;
    }

    public static boolean hasAnnotation(PsiField field, String name) {
        for (PsiAnnotation annotation : getAnnotations(field)) {
            if (name.equals(annotation.getQualifiedName())) {
                return true;
            }
        }
        return false;
    }
}
