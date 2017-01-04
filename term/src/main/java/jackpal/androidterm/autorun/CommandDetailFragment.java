package jackpal.androidterm.autorun;

import android.os.Bundle;
import android.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import jackpal.androidterm.R;
import jackpal.androidterm.util.ShowSoftKeyboard;
import jackpal.androidterm.util.Unchecked;

public class CommandDetailFragment extends Fragment {
    public static final String ARG_ITEM_ID = "item_id";

    private Script mScript;

    @BindView(R.id.script_name) EditText mNameView;
    @BindView(R.id.script_editor) EditText mEditorView;

    private String mScriptName = "";
    private String mContent = "";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            //noinspection ConstantConditions
            mScript = new Script(new File(getArguments().getString(ARG_ITEM_ID)));
        }

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.command_detail, container, false);
        ButterKnife.bind(this, rootView);

        readScript();

        ShowSoftKeyboard.onFocus(mNameView, mEditorView);

        return rootView;
    }

    private void readScript() {
        if (mScript != null) {
            mScriptName = mScript.getBaseName();

            try {
                mContent = mScript.getContent();
            } catch (Throwable e) {
                mContent = "";
            }

            mNameView.setText(mScriptName);
            mEditorView.setText(mContent);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.script_editor, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_save:
                try {
                    String newName = mNameView.getText().toString();
                    if (!Script.isValidName(newName)) {
                        mNameView.setError(getString(R.string.invalid_script_name));
                        if (TextUtils.isEmpty(newName))
                            mNameView.setText(mScriptName);
                        return true;
                    }
                    if (!mScriptName.equals(newName)) {
                        File oldPath = mScript.getPath();
                        File newPath = new File(oldPath.getParent(), newName + Script.EXTENSION);
                        if (oldPath.renameTo(newPath))
                            mScriptName = newName;
                        else {
                            Toast.makeText(getActivity(), R.string.rename_failed, Toast.LENGTH_SHORT).show();
                            return true;
                        }
                    }

                    String newContent = mEditorView.getText().toString();
                    if (!mContent.equals(newContent)) {
                        mScript.setContent(newContent);
                        mContent = newContent;
                    }
                } catch (IOException e) {
                    throw Unchecked.of(e);
                }
                getActivity().onBackPressed();
                return true;
            case R.id.menu_cancel:
                getActivity().onBackPressed();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
