package com.sch.ij.plugins.serializedname;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtilCore;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class AddSerializedNameAction extends AnAction {
    private final Processor processor = new Processor();

    @Override
    public void actionPerformed(AnActionEvent e) {
        PsiElement element = CommonDataKeys.PSI_ELEMENT.getData(e.getDataContext());
        if (element == null) {
            return;
        }

        PsiDirectory dir;
        PsiFile file = null;
        if (element instanceof PsiDirectoryContainer) {
            dir = ((PsiDirectoryContainer) element).getDirectories()[0];
        } else if (element instanceof PsiDirectory) {
            dir = (PsiDirectory) element;
        } else {
            file = element.getContainingFile();
            if (file == null) {
                return;
            }

            dir = file.getContainingDirectory();
        }

        if (file instanceof PsiJavaFile) {
            processFile((PsiJavaFile) file);
        } else if (dir != null) {
            processDirectory(dir);
        }
    }

    @Override
    public void update(AnActionEvent e) {
        Project project = CommonDataKeys.PROJECT.getData(e.getDataContext());

        if (project == null) {
            e.getPresentation().setEnabled(false);
        }

        // TODO
    }

    private void processDirectory(PsiDirectory directory) {
        System.out.println("Processing directory: " + directory.getName());
        for (PsiDirectory subdirectory : directory.getSubdirectories()) {
            processDirectory(subdirectory);
        }
        for (PsiFile file : directory.getFiles()) {
            if (!(file instanceof PsiJavaFile)) {
                continue;
            }

            final Runnable task = () -> processFile((PsiJavaFile) file);
            ApplicationManager.getApplication().invokeAndWait(() -> {
                WriteCommandAction.runWriteCommandAction(directory.getProject(), "Add @SerializedName", null, task);
            });
        }
    }

    private void processFile(PsiJavaFile file) {
        final PsiDocumentManager documentManager = PsiDocumentManager.getInstance(file.getProject());
        final Document document = documentManager.getDocument(file);
        if (document != null) {
            documentManager.doPostponedOperationsAndUnblockDocument(document);
            documentManager.commitDocument(document);

            try {
                processor.process(file);
            } finally {
                documentManager.commitDocument(document);
            }
        }
    }
}
