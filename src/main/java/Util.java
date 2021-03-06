import com.intellij.codeInsight.actions.ReformatCodeProcessor;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.Consumer;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;

import static com.intellij.openapi.updateSettings.impl.pluginsAdvertisement.PluginsAdvertiser.NOTIFICATION_GROUP;

public class Util {
    static void showNotification(Project project, String text) {
        final Notification notification = NOTIFICATION_GROUP.createNotification(text, NotificationType.ERROR);
        notification.notify(project);
    }

    static void writeJSONToVirtualFile(Project project, JSONObject json, VirtualFile file, boolean reformat) throws Exception {
        WriteCommandAction.runWriteCommandAction(project, new Runnable() {
            @Override
            public void run() {
                try {
                    VfsUtil.saveText(file, json.toJSONString());
                } catch (IOException e) {
                    // TODO: throw higher

                    e.printStackTrace();

                    showNotification(project, e.getMessage());
                }

                if (reformat) {
                    try {
                        formatFile(project, file);
                    } catch (Exception e) {
                        // TODO: throw higher

                        e.printStackTrace();

                        showNotification(project, e.getMessage());
                    }
                }
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
        return text.toUpperCase().replaceAll("[^A-Za-z]", "_").replaceAll("__", "_").replaceAll("^_+", "").replaceAll("_+$", "");
    }

    static void formatFile(Project project, VirtualFile file) throws Exception {
        final PsiFile psiFile = PsiManager.getInstance(project).findFile(file);

        if (psiFile == null) {
            throw new Exception("Unable to create PsiFile.");
        }

        final ReformatCodeProcessor processor = new ReformatCodeProcessor(project, psiFile, null, false);
        processor.run();
    }

    static JSONObject sortJSONObject(JSONObject obj) {
        final ArrayList<String> list = new ArrayList<>();
        final JSONObject result = new JSONObject();

        for(Object key : obj.keySet()) {
            list.add((String)key);
        }

        list.sort(new Comparator<String>() {
            @Override
            public int compare(String a, String b) {
                return a.compareTo(b);
            }
        });

        for (String key : list) {
            result.put(key, obj.get(key));
        }

        return result;
    }
}