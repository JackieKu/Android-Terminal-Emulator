package jackpal.androidterm.autorun;

import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import jackpal.androidterm.R;
import jackpal.androidterm.util.Unchecked;

public class CommandDetailFragment extends Fragment {
    public static final String ARG_ITEM_ID = "item_id";

    private Script mScript;

    @BindView(R.id.command_detail) EditText mContentView;

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

        if (mScript != null) {
            String content;
            try {
                content = mScript.getContent();
            } catch (Throwable e) {
                content = "";
            }
            mContentView.setText(content);
        }

        return rootView;
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
                    mScript.setContent(mContentView.getText());
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
