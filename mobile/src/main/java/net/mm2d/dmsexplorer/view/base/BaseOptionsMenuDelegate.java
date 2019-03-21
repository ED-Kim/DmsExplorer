/*
 * Copyright (c) 2017 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.dmsexplorer.view.base;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * @author <a href="mailto:ryo@mm2d.net">大前良介 (OHMAE Ryosuke)</a>
 */
class BaseOptionsMenuDelegate implements OptionsMenuDelegate {
    @NonNull
    private final BaseActivity mActivity;

    BaseOptionsMenuDelegate(@NonNull final BaseActivity activity) {
        mActivity = activity;
    }

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
    }

    @Override
    public void onDestroy() {
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull final Menu menu) {
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull final MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                mActivity.navigateUpTo();
                return true;
        }
        return false;
    }

    @Override
    public boolean onPrepareOptionsMenu(@NonNull final Menu menu) {
        return false;
    }
}
