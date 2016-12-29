package jackpal.androidterm.autorun;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.app.Activity;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import jackpal.androidterm.R;

import rx.android.schedulers.AndroidSchedulers;
import rx.subjects.BehaviorSubject;

public class CommandListActivity extends Activity {
    private boolean mTwoPane;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_command_list);

        View recyclerView = findViewById(R.id.command_list);
        assert recyclerView != null;
        setupRecyclerView((RecyclerView) recyclerView);

        // The detail container view will be present only in the
        // large-screen layouts (res/values-w900dp).
        // If this view is present, then the
        // activity should be in two-pane mode.
        mTwoPane = findViewById(R.id.command_detail_container) != null;
    }

    private void setupRecyclerView(@NonNull RecyclerView recyclerView) {
        recyclerView.setAdapter(new ScriptRecyclerViewAdapter());
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
