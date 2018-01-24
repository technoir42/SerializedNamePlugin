package com.sch.ij.plugins.serializedname;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

public class AddSerializedNameIntentionAction extends PsiElementBaseIntentionAction implements IntentionAction {
    private final Processor processor = new Processor();

    @NotNull
    @Override
    public String getText() {
        return "Add @SerializedName";
    }

    @Nls
    @NotNull
    @Override
    public String getFamilyName() {
        return getText();
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement psiElement) throws IncorrectOperationException {
        final PsiField psiField = PsiTreeUtil.getParentOfType(psiElement, PsiField.class);
        if (psiField != null) {
            processor.process(psiField);
        } else {
            final PsiClass psiClass = PsiTreeUtil.getParentOfType(psiElement, PsiClass.class);
            if (psiClass != null) {
                processor.process(psiClass);
            }
        }
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement psiElement) {
        final PsiField field = PsiTreeUtil.getParentOfType(psiElement, PsiField.class);
        if (field != null) {
            return processor.isApplicable(field) && !Utils.hasAnnotation(field, Processor.SERIALIZED_NAME_FQCN);
        } else {
            final PsiClass psiClass = PsiTreeUtil.getParentOfType(psiElement, PsiClass.class);
            if (psiClass == null) {
                return false;
            }
            final PsiElement leftBrace = psiClass.getLBrace();
            return processor.isApplicable(psiClass) && leftBrace != null && psiElement.getTextOffset() < leftBrace.getTextOffset();
        }
    }
}
