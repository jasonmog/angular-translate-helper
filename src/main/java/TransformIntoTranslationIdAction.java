import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class TransformIntoTranslationIdAction extends AnAction {
    @Override
    public void update(AnActionEvent e) {
        final Editor editor = e.getData(CommonDataKeys.EDITOR);

        e.getPresentation().setEnabledAndVisible(e.getProject() != null && editor != null && editor.getSelectionModel().hasSelection());
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        final Project project = e.getProject();
        final Editor editor = e.getData(CommonDataKeys.EDITOR);

        final String text = Util.getSelectedText(editor);
        String translationId = Util.getTranslationId(text);

        Util.replaceText(editor, project, translationId);
    }
}