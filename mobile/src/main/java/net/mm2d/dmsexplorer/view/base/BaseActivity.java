/*
 * Copyright (c) 2017 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.dmsexplorer.view.base;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import net.mm2d.dmsexplorer.view.eventrouter.EventObserver;
import net.mm2d.dmsexplorer.view.eventrouter.EventRouter;

import java.util.concurrent.atomic.AtomicBoolean;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NavUtils;
import androidx.core.app.TaskStackBuilder;

/**
 * @author <a href="mailto:ryo@mm2d.net">大前良介 (OHMAE Ryosuke)</a>
 */
public abstract class BaseActivity extends AppCompatActivity {
    @NonNull
    private final AtomicBoolean mFinishAfterTransitionLatch = new AtomicBoolean();
    @NonNull
    private final AtomicBoolean mFinishLatch = new AtomicBoolean();
    private final boolean mMainMenu;
    private OptionsMenuDelegate mDelegate;
    private EventObserver mFinishObserver;
    private EventObserver mOrientationSettingsObserver;

    public BaseActivity() {
        this(false);
    }

    public BaseActivity(final boolean mainMenu) {
        mMainMenu = mainMenu;
    }

    @CallSuper
    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDelegate = mMainMenu
                ? new MainOptionsMenuDelegate(this)
                : new BaseOptionsMenuDelegate(this);
        mDelegate.onCreate(savedInstanceState);
        mFinishObserver = EventRouter.createFinishObserver();
        mFinishObserver.register(this::finish);
        mOrientationSettingsObserver = EventRouter.createOrientationSettingsObserver();
        mOrientationSettingsObserver.register(this::updateOrientationSettings);
    }

    @CallSuper
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mDelegate.onDestroy();
        mFinishObserver.unregister();
        mOrientationSettingsObserver.unregister();
    }

    public void navigateUpTo() {
        final Intent upIntent = NavUtils.getParentActivityIntent(this);
        if (upIntent == null) {
            onBackPressed();
            return;
        }
        if (NavUtils.shouldUpRecreateTask(this, upIntent)) {
            TaskStackBuilder.create(this)
                    .addNextIntentWithParentStack(upIntent)
                    .startActivities();
        } else {
            NavUtils.navigateUpTo(this, upIntent);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull final Menu menu) {
        return mDelegate.onCreateOptionsMenu(menu)
                || super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull final MenuItem item) {
        return mDelegate.onOptionsItemSelected(item)
                || super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPrepareOptionsMenu(@NonNull final Menu menu) {
        return mDelegate.onPrepareOptionsMenu(menu)
                || super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onBackPressed() {
        try {
            super.onBackPressed();
        } catch (final IllegalStateException ignored) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                finishAfterTransition();
            } else {
                finish();
            }
        }
    }

    protected void updateOrientationSettings() {
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void finishAfterTransition() {
        if (mFinishAfterTransitionLatch.getAndSet(true)) {
            return;
        }
        super.finishAfterTransition();
    }

    @Override
    public void finish() {
        if (mFinishLatch.getAndSet(true)) {
            return;
        }
        super.finish();
    }
}
