package com.sch.ij.plugins.serializedname;

import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class Processor {
    public static final String SERIALIZED_NAME_FQCN = "com.google.gson.annotations.SerializedName";
    private static final String DATABASE_FIELD_FQCN = "com.j256.ormlite.field.DatabaseField";
    private static final String SUPPRESS_WARNINGS_FQCN = "java.lang.SuppressWarnings";

    private final Map<String, PsiClass> classCache = new HashMap<>();

    public void process(PsiJavaFile file) {
        System.out.println("Processing file: " + file.getName());

        final Collection<PsiClass> psiClasses = PsiTreeUtil.collectElementsOfType(file, PsiClass.class);
        for (PsiClass psiClass : psiClasses) {
            if (isApplicable(psiClass)) {
                process(file, psiClass);
            }
        }
    }

    public void process(PsiClass psiClass) {
        final PsiJavaFile javaFile = PsiTreeUtil.getParentOfType(psiClass, PsiJavaFile.class);
        if (javaFile != null) {
            process(javaFile, psiClass);
        }
    }

    private void process(PsiJavaFile javaFile, PsiClass psiClass) {
        System.out.println("Processing class " + psiClass.getQualifiedName());

        boolean hasImport = false;
        for (PsiField field : psiClass.getFields()) {
            if (Utils.hasAnnotation(field, SERIALIZED_NAME_FQCN)) {
                removeSuppressUnused(field);
            } else if (isApplicable(field)) {
                removeSuppressUnused(field);
                if (!hasImport) {
                    addImportStatement(javaFile, SERIALIZED_NAME_FQCN);
                    hasImport = true;
                }
                addSerializedName(field);
            }
        }
    }

    public void process(PsiField field) {
        final PsiJavaFile javaFile = PsiTreeUtil.getParentOfType(field, PsiJavaFile.class);
        if (javaFile != null) {
            if (Utils.hasAnnotation(field, SERIALIZED_NAME_FQCN)) {
                removeSuppressUnused(field);
            } else if (isApplicable(field)) {
                removeSuppressUnused(field);
                addImportStatement(javaFile, SERIALIZED_NAME_FQCN);
                addSerializedName(field);
            }
        }
    }

    public boolean isApplicable(PsiClass psiClass) {
        return !psiClass.isEnum() && !psiClass.isInterface() && !psiClass.isAnnotationType() &&
                !("Builder".equals(psiClass.getName()) && psiClass.getParent() instanceof PsiClass);
    }

    public boolean isApplicable(PsiField field) {
        final PsiModifierList modifierList = field.getModifierList();
        return modifierList != null &&
                !modifierList.hasModifierProperty(PsiModifier.STATIC) &&
                !modifierList.hasModifierProperty(PsiModifier.TRANSIENT) &&
                !isGeneratedIdField(field);
    }

    private void addAnnotationInOrder(PsiModifierList modifierList, PsiAnnotation annotationToAdd) {
        PsiAnnotation after = null;
        for (PsiAnnotation annotation : modifierList.getAnnotations()) {
            if (annotation.getText().compareTo(annotationToAdd.getText()) < 0) {
                after = annotation;
            } else {
                break;
            }
        }
        modifierList.addAfter(annotationToAdd, after);
    }

    private void addImportStatement(PsiJavaFile javaFile, String qualifiedName) {
        final PsiImportList importList = javaFile.getImportList();
        if (importList == null || importList.findSingleClassImportStatement(qualifiedName) != null) {
            return;
        }

        final PsiClass annotationClass = findClass(javaFile, qualifiedName);
        final JavaPsiFacade javaPsiFacade = JavaPsiFacade.getInstance(javaFile.getProject());
        final PsiImportStatement importStatement = javaPsiFacade.getElementFactory().createImportStatement(annotationClass);
        importList.add(importStatement);
    }

    private void addSerializedName(PsiField field) {
        final PsiModifierList modifierList = field.getModifierList();
        if (modifierList != null) {
            final PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(field.getProject());
            final PsiAnnotation annotation = elementFactory.createAnnotationFromText(String.format("@SerializedName(\"%s\")", field.getName()), modifierList);
            addAnnotationInOrder(modifierList, annotation);
        }
    }

    private void removeSuppressUnused(PsiField field) {
        for (PsiAnnotation annotation : Utils.getAnnotations(field)) {
            if (isSuppressUnused(annotation)) {
                annotation.delete();
            }
        }
    }

    private boolean isSuppressUnused(PsiAnnotation annotation) {
        if (SUPPRESS_WARNINGS_FQCN.equals(annotation.getQualifiedName())) {
            final PsiAnnotationMemberValue value = annotation.findAttributeValue("value");
            return value != null && "\"unused\"".equals(value.getText());
        }
        return false;
    }

    private boolean isGeneratedIdField(PsiField field) {
        for (PsiAnnotation annotation : Utils.getAnnotations(field)) {
            if (DATABASE_FIELD_FQCN.equals(annotation.getQualifiedName())) {
                final PsiAnnotationMemberValue value = annotation.findAttributeValue("generatedId");
                return value != null && "true".equals(value.getText());
            }
        }
        return false;
    }

    private PsiClass findClass(PsiJavaFile javaFile, String qualifiedName) {
        final JavaPsiFacade javaPsiFacade = JavaPsiFacade.getInstance(javaFile.getProject());
        PsiClass annotationClass = classCache.get(qualifiedName);
        if (annotationClass == null) {
            annotationClass = javaPsiFacade.findClass(qualifiedName, javaFile.getResolveScope());
            if (annotationClass == null) {
                throw new IllegalStateException("Class " + qualifiedName + " not found");
            }
            classCache.put(qualifiedName, annotationClass);
        }
        return annotationClass;
    }
}
