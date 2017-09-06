package org.secuso.privacyfriendlybreakreminder.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.ColorDrawable;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.secuso.privacyfriendlybreakreminder.ExerciseLocale;
import org.secuso.privacyfriendlybreakreminder.R;
import org.secuso.privacyfriendlybreakreminder.activities.adapter.ExerciseSetListAdapter;
import org.secuso.privacyfriendlybreakreminder.activities.helper.BaseActivity;
import org.secuso.privacyfriendlybreakreminder.database.SQLiteHelper;
import org.secuso.privacyfriendlybreakreminder.database.data.ExerciseSet;

import java.util.List;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class ManageExerciseSetsActivity extends BaseActivity implements android.support.v4.app.LoaderManager.LoaderCallbacks<List<ExerciseSet>> {

    private RecyclerView exerciseSetList;
    private ProgressBar loadingSpinner;
    private TextView noExerciseSetsText;
    private FloatingActionButton fabButton;
    private MenuItem toolbarDeleteIcon;

    private ExerciseSetListAdapter exerciseSetAdapter;

    private boolean deleteMode = false;
    private ColorStateList fabDefaultBackgroundTint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_exercise_set);

        initResources();
        getSupportLoaderManager().initLoader(0, null, this);
    }

    private void initResources() {
        exerciseSetAdapter = new ExerciseSetListAdapter(this, null);
        exerciseSetList = (RecyclerView) findViewById(R.id.exercise_set_list);
        loadingSpinner = (ProgressBar) findViewById(R.id.loading_spinner);
        loadingSpinner.setAlpha(1.0f);
        noExerciseSetsText = (TextView) findViewById(R.id.no_exercise_sets_text);
        fabButton = (FloatingActionButton) findViewById(R.id.add_button);

        fabDefaultBackgroundTint = fabButton.getBackgroundTintList();

        exerciseSetList.setLayoutManager(new LinearLayoutManager(this));
        exerciseSetList.setAdapter(exerciseSetAdapter);
    }

    @Override
    public Loader<List<ExerciseSet>> onCreateLoader(int id, final Bundle args) {
        return new AsyncTaskLoader<List<ExerciseSet>>(this) {
            @Override
            public List<ExerciseSet> loadInBackground() {
                SQLiteHelper helper = new SQLiteHelper(getContext());
                return helper.getExerciseSetsWithExercises(ExerciseLocale.getLocale());
            }

            @Override
            protected void onStartLoading() {
                setLoading(true, false);
                forceLoad();
            }

            @Override
            protected void onReset() {}
        };
    }

    @Override
    public void onLoadFinished(Loader<List<ExerciseSet>> loader, List<ExerciseSet> data) {

        boolean hasElements = data.size() > 0;

        setLoading(false, hasElements);

        exerciseSetAdapter.setData(data);
    }

    @Override
    public void onLoaderReset(Loader<List<ExerciseSet>> loader) {}

    @Override
    protected void onResume() {
        super.onResume();

        getSupportLoaderManager().restartLoader(0, null, this);
    }

    private void setLoading(boolean isLoading, boolean hasElements) {
        if(isLoading) {
            loadingSpinner.setVisibility(VISIBLE);
            loadingSpinner.animate().alpha(1.0f).setDuration(1000).start();

            noExerciseSetsText.setVisibility(GONE);
            exerciseSetList.setVisibility(GONE);
        } else {
            loadingSpinner.animate().alpha(0.0f).setDuration(500).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    loadingSpinner.setVisibility(GONE);
                }
            });

            if(hasElements) {
                noExerciseSetsText.setVisibility(GONE);
                exerciseSetList.setVisibility(VISIBLE);
            } else {
                noExerciseSetsText.setVisibility(VISIBLE);
                exerciseSetList.setVisibility(GONE);
            }
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        mDrawerToggle.setToolbarNavigationClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(deleteMode)
                    disableDeleteMode();
            }
        });
    }

    public void setDrawerEnabled(final boolean enabled) {

        int lockMode = enabled ?
                DrawerLayout.LOCK_MODE_UNLOCKED :
                DrawerLayout.LOCK_MODE_LOCKED_CLOSED;

        mDrawerLayout.setDrawerLockMode(lockMode);

        mDrawerToggle.setDrawerIndicatorEnabled(enabled);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(!enabled);
            actionBar.setDefaultDisplayHomeAsUpEnabled(!enabled);
            actionBar.setDisplayShowHomeEnabled(enabled);
            actionBar.setHomeButtonEnabled(enabled);
        }

        mDrawerToggle.syncState();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if(deleteMode)
                    disableDeleteMode();
                else
                    finish();
                return true;
            case R.id.action_delete:
                enableDeleteMode();
                return true;
            default:
                Toast.makeText(this, "option selected", Toast.LENGTH_SHORT).show();
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public void onBackPressed() {
        if(deleteMode)
            disableDeleteMode();
        else
            super.onBackPressed();
    }

    @Override
    protected int getNavigationDrawerID() {
        return R.id.nav_manage_exercise_sets;
    }

    public void onClick(View view) {
        switch(view.getId()) {
            case R.id.add_button:
                if(deleteMode) {
                    SQLiteHelper helper = new SQLiteHelper(this);

                    List<Long> deleteIds = exerciseSetAdapter.getDeleteIdList();

                    if(deleteIds.size() == 0) {
                        Toast.makeText(this, "Please select an item to delete.", Toast.LENGTH_SHORT).show();
                    } else {
                        for (Long l : deleteIds) {
                            helper.deleteExerciseSet(l);
                        }
                        disableDeleteMode();
                        getSupportLoaderManager().restartLoader(0, null, this);
                    }

                } else {
                    AddExerciseSetDialogFragment dialog = new AddExerciseSetDialogFragment();
                    dialog.show(this.getSupportFragmentManager(), AddExerciseSetDialogFragment.TAG);
                }
                break;
        }
    }

    public void enableDeleteMode() {
        deleteMode = true;

        setDrawerEnabled(false);

        exerciseSetAdapter.enableDeleteMode();

        if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.middlegrey));
        }
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(ActivityCompat.getColor(this, R.color.middlegrey)));
        getSupportActionBar().setTitle(R.string.activity_title_manage_exercise_sets);

        if(toolbarDeleteIcon != null) {
            toolbarDeleteIcon.setVisible(false);
        }

        fabButton.setBackgroundTintList(ColorStateList.valueOf(ActivityCompat.getColor(this, R.color.red)));
        fabButton.setImageResource(R.drawable.ic_delete_white);
    }

    public void disableDeleteMode() {
        deleteMode = false;

        setDrawerEnabled(true);

        exerciseSetAdapter.disableDeleteMode();

        if(toolbarDeleteIcon != null) {
            toolbarDeleteIcon.setVisible(true);
        }
        if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimary));
        }
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(ActivityCompat.getColor(this, R.color.colorPrimary)));
        getSupportActionBar().setTitle(R.string.activity_title_manage_exercise_sets);

        fabButton.setBackgroundTintList(fabDefaultBackgroundTint);
        fabButton.setImageResource(R.drawable.ic_add_white_24dp);
    }

    public static class AddExerciseSetDialogFragment extends DialogFragment {

        static final String TAG = AddExerciseSetDialogFragment.class.getSimpleName();

        TextInputEditText exerciseSetName;
        ManageExerciseSetsActivity activity;

        @Override
        public void onAttach(Context context) {
            super.onAttach(context);

            activity = (ManageExerciseSetsActivity)context;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), 0);

            LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(FragmentActivity.LAYOUT_INFLATER_SERVICE);
            View v = inflater.inflate(R.layout.dialog_add_exercise_set, null);

            exerciseSetName = (TextInputEditText) v.findViewById(R.id.dialog_add_exercise_set_name);

            builder.setView(v);
            builder.setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    String text = exerciseSetName.getText().toString();

                    if(TextUtils.isEmpty(text)) {
                        Toast.makeText(getActivity(), "Please specify a name.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    SQLiteHelper sqLiteHelper = new SQLiteHelper(getActivity());
                    long id = sqLiteHelper.addExerciseSet(text);

                    Intent intent = new Intent(getActivity(), EditExerciseSetActivity.class);
                    intent.putExtra(EditExerciseSetActivity.EXTRA_EXERCISE_SET_ID, id);
                    intent.putExtra(EditExerciseSetActivity.EXTRA_EXERCISE_SET_NAME, text);
                    startActivity(intent);

                    dismiss();
                }
            });
            builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dismiss();
                }
            });
            builder.setTitle(R.string.dialog_add_exercise_set_title);

            return builder.create();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_manage_exercise_sets, menu);
        toolbarDeleteIcon = menu.findItem(R.id.action_delete);
        return true;
    }

}
