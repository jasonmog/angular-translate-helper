import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.ReadonlyStatusHandler;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Consumer;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class LocalizeAction extends AnAction {
    @Override
    public void update(AnActionEvent e) {
        final Editor editor = e.getData(CommonDataKeys.EDITOR);

        e.getPresentation().setEnabledAndVisible(e.getProject() != null && editor != null && editor.getSelectionModel().hasSelection());
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        final Project project = e.getProject();
        final Editor editor = e.getData(CommonDataKeys.EDITOR);

        promptForFile(project, new Consumer<VirtualFile>() {
            @Override
            public void consume(VirtualFile virtualFile) {
                final String text = getSelectedText(editor);
                final String translationId = getTranslationId(text);
                final String newText = "{{ '" + translationId + "'  | translate }}";

                replaceText(editor, project, newText);
                addTranslationEntry(project, virtualFile, translationId, text);
            }
        });
    }

    static void addTranslationEntry(Project project, VirtualFile file, String translationId, String text) {
        final Document document = FileDocumentManager.getInstance().getDocument(file);
        final int closingBraceIndex = document.getText().lastIndexOf("\n};");
        final String entry = ",\n    " + translationId + ": '" + text + "'";

        ReadonlyStatusHandler.OperationStatus status = ReadonlyStatusHandler.getInstance(project).ensureFilesWritable(List.of(file));

        if (status.hasReadonlyFiles()) {
            return;
        }

        WriteCommandAction.runWriteCommandAction(project, new Runnable() {
            @Override
            public void run() {
                document.insertString(closingBraceIndex, entry);
            }
        });
    }

    static void promptForFile(Project project, Consumer<VirtualFile> consumer) {
        final FileChooserDescriptor descriptor = new FileChooserDescriptor(true, false, false, false, false, true);

        FileChooser.chooseFile(descriptor, project, null, consumer);
    }

    static String getSelectedText(Editor editor) {
        return editor.getSelectionModel().getSelectedText();
    }

    static void replaceText(Editor editor, Project project, String newText) {
        final Caret primaryCaret = editor.getCaretModel().getPrimaryCaret();
        final int start = primaryCaret.getSelectionStart();
        final int end = primaryCaret.getSelectionEnd();

        // Replace the selection with a fixed string.
        // Must do this document change in a write action context.
        WriteCommandAction.runWriteCommandAction(project, () ->
            editor.getDocument().replaceString(start, end, newText)
        );

        // De-select the text range that was just replaced
        primaryCaret.removeSelection();
    }

    static String getTranslationId(String text) {
        return text.toUpperCase().replaceAll(" ", "_");
    }
}