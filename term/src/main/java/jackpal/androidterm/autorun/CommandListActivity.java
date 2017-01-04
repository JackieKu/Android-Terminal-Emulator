package jackpal.androidterm.autorun;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.app.Activity;
import android.os.Handler;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import jackpal.androidterm.R;

import jackpal.androidterm.util.ShowSoftKeyboard;
import rx.android.schedulers.AndroidSchedulers;
import rx.subjects.BehaviorSubject;

import static android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE;

public class CommandListActivity extends Activity {
    private static final String TAG = "CommandListActivity";

    private boolean mTwoPane;

    private final Handler mUiHandler = new Handler();

    @BindView(R.id.command_list) RecyclerView mRecyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_command_list);
        ButterKnife.bind(this);

        mRecyclerView.setAdapter(new ScriptRecyclerViewAdapter());

        // The detail container view will be present only in the
        // large-screen layouts (res/values-w900dp).
        // If this view is present, then the
        // activity should be in two-pane mode.
        mTwoPane = findViewById(R.id.command_detail_container) != null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.script_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_new_script:
                onNewScript();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void onNewScript() {
        openNewScriptDialog(false);
    }

    private void openNewScriptDialog(boolean hasError) {
        EditText editView = (EditText)getLayoutInflater().inflate(R.layout.script_name_editor, null, false);
        if (hasError)
            editView.setError(getString(R.string.invalid_script_name));
        ShowSoftKeyboard.onClick(editView);

        new AlertDialog.Builder(this)
            .setTitle(R.string.script_name)
            .setView(editView)
            .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                String name = editView.getText().toString();
                if (Script.isValidName(name)) {
                    try {
                        if (!new File(Scripts.AUTORUN_DIR, name + Script.EXTENSION).createNewFile())
                            return; // Already exist
                    } catch (IOException e) {
                        Log.e(TAG, "", e);
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                    getAdapter().refresh();
                } else if (!name.isEmpty()) {
                    mUiHandler.post(() -> openNewScriptDialog(true));
                }
            })
            .setNegativeButton(android.R.string.cancel, null)
            .show().getWindow().setSoftInputMode(SOFT_INPUT_STATE_ALWAYS_VISIBLE);
    }

    private ScriptRecyclerViewAdapter getAdapter() {
        return (ScriptRecyclerViewAdapter) mRecyclerView.getAdapter();
    }

    final class ScriptRecyclerViewAdapter extends RecyclerView.Adapter<ScriptRecyclerViewAdapter.ViewHolder> {
        private final BehaviorSubject<Scripts> mScriptsSubject = BehaviorSubject.create(Scripts.EMPTY);

        ScriptRecyclerViewAdapter() {
            mScriptsSubject
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(scripts -> notifyDataSetChanged())
                ;

            refresh();
        }

        @Override
        public int getItemCount() {
            return scripts().list().size();
        }

        private void refresh() {
            Scripts.forAutoRun().subscribe(mScriptsSubject::onNext);
        }

        private Scripts scripts() {
            return mScriptsSubject.getValue();
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.command_list_content, parent, false));
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.bind(position);
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            public final View mView;

            @BindView(R.id.id) TextView mIdView;
            @BindView(R.id.enabled) ImageView mEnabledView;

            public Script mScript;

            public ViewHolder(View view) {
                super(view);
                mView = view;

                ButterKnife.bind(this, view);
            }

            void bind(int position) {
                mScript = scripts().list().get(position);

                mIdView.setText(mScript.getName());
                mView.setOnClickListener(onItemDetail(mScript.getPath().getAbsolutePath()));

                setIconTint(mEnabledView, mScript.isEnabled());
                mEnabledView.setOnClickListener(v -> {
                    mScript.toggleEnabled();
                    notifyItemChanged(position);
                });
            }

            @Override
            public String toString() {
                return mScript.toString();
            }
        }

        private View.OnClickListener onItemDetail(String id) {
            return mTwoPane ?
                    v -> {
                        Bundle arguments = new Bundle();
                        arguments.putString(CommandDetailFragment.ARG_ITEM_ID, id);
                        CommandDetailFragment fragment = new CommandDetailFragment();
                        fragment.setArguments(arguments);
                        getFragmentManager().beginTransaction()
                                .replace(R.id.command_detail_container, fragment)
                                .commit();
                    } :
                    v -> {
                        Context context = v.getContext();
                        Intent intent = new Intent(context, CommandDetailActivity.class);
                        intent.putExtra(CommandDetailFragment.ARG_ITEM_ID, id);

                        context.startActivity(intent);
                    };
        }
    }

    private static void setIconTint(ImageView icon, boolean isActivated) {
        if (isActivated)
            icon.setColorFilter(null);
        else
            icon.setColorFilter(Color.GRAY);
    }
}
