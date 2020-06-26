import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.ReadonlyStatusHandler;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Consumer;
import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public class LocalizeAction extends AnAction {
    VirtualFile lastFile;

    @Override
    public void update(AnActionEvent e) {
        final Editor editor = e.getData(CommonDataKeys.EDITOR);

        e.getPresentation().setEnabledAndVisible(e.getProject() != null && editor != null && editor.getSelectionModel().hasSelection());
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        final Project project = e.getProject();
        final Editor editor = e.getData(CommonDataKeys.EDITOR);

        if (lastFile == null) {
            Util.promptForFile(project, new Consumer<VirtualFile>() {
                @Override
                public void consume(VirtualFile virtualFile) {
                    if (virtualFile != null && !virtualFile.isDirectory()) {
                        lastFile = virtualFile;

                        translate(project, editor, virtualFile);
                    }
                }
            });
        } else {
            translate(project, editor, lastFile);
        }
    }

    static void translate(Project project, Editor editor, VirtualFile virtualFile) {
        final String text = Util.getSelectedText(editor);
        String translationId = Util.getTranslationId(text);

        try {
            translationId = addTranslationEntry(project, virtualFile, translationId, text);

            final String newText = "{{ '" + translationId + "' | translate }}";
            Util.replaceText(editor, project, newText);
        } catch (Exception exception) {
            exception.printStackTrace();

            Util.showNotification(project, exception.getMessage());
        }
    }

    static String addTranslationEntry(Project project, VirtualFile file, String translationId, String text) throws IOException, ParseException, Exception {
        List<VirtualFile> list = new ArrayList<>();
        list.add(file);

        final ReadonlyStatusHandler.OperationStatus status = ReadonlyStatusHandler.getInstance(project).ensureFilesWritable(list);

        if (status.hasReadonlyFiles()) {
            throw new IOException("File is read only.");
        }

        JSONObject json = (JSONObject)new JSONParser().parse(new InputStreamReader(file.getInputStream()));

        final Object value = json.get(translationId);

        if (value instanceof String) {
            if (value.equals(text)) {
                return translationId;
            }

            // TODO: increment until unique
            translationId += "_2";

            if (json.get(translationId) != null) {
                throw new Exception("Translation ID conflict.");
            }
        }

        json.put(translationId, text);

        // TODO: fix
        // json = sortJSONObject(json);

        Util.writeJSONToVirtualFile(project, json, file, true);

        return translationId;
    }
}