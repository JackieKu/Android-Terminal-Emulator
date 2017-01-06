package jackpal.androidterm.autorun;

import android.app.AlertDialog;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.app.Activity;
import android.os.FileObserver;
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
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import jackpal.androidterm.R;

import jackpal.androidterm.util.ShowSoftKeyboard;
import rx.Completable;
import rx.Single;
import rx.android.schedulers.AndroidSchedulers;
import rx.subjects.BehaviorSubject;
import rx.subjects.PublishSubject;

import static android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE;

public class CommandListActivity extends Activity {
    private static final String TAG = "CommandListActivity";

    private PaneMode mPaneMode;

    private final Handler mUiHandler = new Handler();
    private boolean mStopped;
    private boolean mRefreshOnStart;

    @BindView(R.id.command_list) RecyclerView mRecyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_command_list);
        ButterKnife.bind(this);

        mRecyclerView.setAdapter(new ScriptRecyclerViewAdapter());

        mPaneMode = findViewById(R.id.command_detail_container) != null ? new TwoPane() : new OnePane();
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

    @Override
    protected void onStart() {
        super.onStart();

        mStopped = false;
        if (mRefreshOnStart)
            getAdapter().refresh();
    }

    @Override
    protected void onStop() {
        mStopped = true;

        super.onStop();
    }

    @Override
    protected void onDestroy() {
        getAdapter().mFileObserver.stopWatching();
        super.onDestroy();
    }

    private void onNewScript() {
        openNewScriptDialog(false);
    }

    private void openNewScriptDialog(boolean hasError) {
        EditText editView = (EditText)getLayoutInflater().inflate(R.layout.script_name_editor, null, false);
        if (hasError)
            editView.setError(getString(R.string.invalid_script_name));
        ShowSoftKeyboard.onFocusOrClick(editView);

        new AlertDialog.Builder(this)
            .setTitle(R.string.script_name)
            .setView(editView)
            .setPositiveButton(android.R.string.ok, (d, which) -> {
                String name = editView.getText().toString();
                if (Script.isValidName(name)) {
                    try {
                        if (!new File(Scripts.AUTORUN_DIR, name + Script.EXTENSION).createNewFile())
                            return; // Already exist
                    } catch (IOException e) {
                        Log.e(TAG, "", e);
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                } else if (!name.isEmpty()) {
                    mUiHandler.post(() -> openNewScriptDialog(true));
                }
            })
            .setNegativeButton(android.R.string.cancel, null)
            .show();
    }

    private ScriptRecyclerViewAdapter getAdapter() {
        return (ScriptRecyclerViewAdapter) mRecyclerView.getAdapter();
    }

    final class ScriptRecyclerViewAdapter extends RecyclerView.Adapter<ScriptRecyclerViewAdapter.ViewHolder> {
        private final BehaviorSubject<Scripts> mScriptsSubject = BehaviorSubject.create(Scripts.EMPTY);
        private final PublishSubject<Integer> mDirChangedSubject = PublishSubject.create();
        private final FileObserver mFileObserver = new FileObserver(Scripts.AUTORUN_DIR.getAbsolutePath()) {
            @Override
            public void onEvent(int event, String path) {
                switch (event) {
                    case ATTRIB:
                    case CREATE:
                    case DELETE:
                    case MOVED_FROM:
                    case MOVED_TO:
                        mDirChangedSubject.onNext(event);
                        break;
                }
            }
        };

        ScriptRecyclerViewAdapter() {
            mScriptsSubject
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(scripts -> notifyDataSetChanged())
                ;
            mScriptsSubject
                .first()
                .subscribe(scripts -> mFileObserver.startWatching())
                ;
            mDirChangedSubject
                .throttleWithTimeout(300, TimeUnit.MILLISECONDS)
                .onBackpressureLatest()
                .observeOn(AndroidSchedulers.mainThread(), 1)
                .concatMap(event -> refresh().toObservable())
                .subscribe()
                ;

            refresh();
        }

        @Override
        public int getItemCount() {
            return scripts().list().size();
        }

        private Completable refresh() {
            if (mStopped) {
                mRefreshOnStart = true;
                return Completable.complete();
            }

            mRefreshOnStart = false;

            Single<Scripts> scripts = Scripts.forAutoRun().cache();
            scripts.subscribe(mScriptsSubject::onNext);
            return scripts.toCompletable();
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
            final View mView;

            @BindView(R.id.id) TextView mIdView;
            @BindView(R.id.start) ImageView mStartButton;
            @BindView(R.id.delete) ImageView mDeleteButton;
            @BindView(R.id.edit) ImageView mEditButton;
            @BindView(R.id.enabled) ImageView mEnabledToggle;

            ViewHolder(View view) {
                super(view);
                mView = view;

                ButterKnife.bind(this, view);
            }

            void bind(int position) {
                Script script = scripts().list().get(position);

                mIdView.setText(script.getName());

                mStartButton.setOnClickListener(v -> {
                    script.run().subscribe();
                    Toast.makeText(CommandListActivity.this, getString(R.string.script_running, script.getName()), Toast.LENGTH_SHORT).show();
                });

                //mEditButton.setOnClickListener(mPaneMode.onEditItem(script.getFile().getAbsolutePath()));
                mView.setOnClickListener(mPaneMode.onEditItem(script.getFile().getAbsolutePath()));

                mDeleteButton.setOnClickListener(v ->
                    new AlertDialog.Builder(CommandListActivity.this)
                        .setMessage(getString(R.string.confirm_delete, script.getName()))
                        .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                            if (!script.getFile().delete())
                                Log.w(TAG, "Delete file failed.");
                        })
                        .setNegativeButton(android.R.string.no, null)
                        .show()
                );

                setIconTint(mEnabledToggle, script.isEnabled());
                mEnabledToggle.setOnClickListener(v -> {
                    script.toggleEnabled();
                    notifyItemChanged(position);
                });
            }
        }
    }

    private interface PaneMode {
        View.OnClickListener onEditItem(String id);
    }

    private class OnePane implements PaneMode {
        @Override
        public View.OnClickListener onEditItem(String id) {
            return v -> {
                Context context = v.getContext();
                Intent intent = new Intent(context, CommandDetailActivity.class);
                intent.putExtra(CommandDetailFragment.ARG_ITEM_ID, id);

                context.startActivity(intent);
            };
        }
    }

    private class TwoPane implements PaneMode {
        @Override
        public View.OnClickListener onEditItem(String id) {
            return v -> {
                FragmentManager fm = getFragmentManager();

                if (fm.findFragmentById(R.id.command_detail_container) != null)
                    fm.popBackStack();

                Bundle arguments = new Bundle();
                arguments.putString(CommandDetailFragment.ARG_ITEM_ID, id);
                CommandDetailFragment fragment = new CommandDetailFragment();
                fragment.setArguments(arguments);
                fm.beginTransaction()
                        .replace(R.id.command_detail_container, fragment)
                        .addToBackStack(null)
                        .commit();
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
