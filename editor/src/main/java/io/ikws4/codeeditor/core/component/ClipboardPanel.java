package io.ikws4.codeeditor.core.component;

import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;

import io.ikws4.codeeditor.R;
import io.ikws4.codeeditor.core.CodeEditor;

public class ClipboardPanel implements ActionMode.Callback {
    private final CodeEditor mEditor;
    private ActionMode mActionMode;

    public ClipboardPanel(CodeEditor editor) {
        mEditor = editor;
    }

    /**
     * Called when action mode is first created. The menu supplied will be used to
     * generate action buttons for the action mode.
     *
     * @param mode ActionMode being created
     * @param menu Menu used to populate action buttons
     * @return true if the action mode should be created, false if entering this
     * mode should be aborted.
     */
    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        mActionMode = mode;
        mode.getMenuInflater().inflate(R.menu.clipboard_panel_menu, menu);
        return true;
    }

    /**
     * Called to refresh an action mode's action menu whenever it is invalidated.
     *
     * @param mode ActionMode being prepared
     * @param menu Menu used to populate action buttons
     * @return true if the menu or action mode was updated, false otherwise.
     */
    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    /**
     * Called to report a user click on an action button.
     *
     * @param mode The current ActionMode
     * @param item The item that was clicked
     * @return true if this callback handled the event, false if the standard MenuItem
     * invocation should continue.
     */
    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.selectAll:
                break;
            default:
                hide();
        }
        return mEditor.onTextContextMenuItem(item.getItemId());
    }

    /**
     * Called when an action mode is about to be exited and destroyed.
     *
     * @param mode The current ActionMode being destroyed
     */
    @Override
    public void onDestroyActionMode(ActionMode mode) {
    }

    public boolean isShow() {
        return mActionMode != null;
    }

    public void hide() {
        if (mActionMode != null) {
            mActionMode.finish();
            mActionMode = null;
        }
    }
}