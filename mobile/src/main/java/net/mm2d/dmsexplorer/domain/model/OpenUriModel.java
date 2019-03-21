/*
 * Copyright (c) 2017 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.dmsexplorer.domain.model;

import android.content.Context;

import java.util.List;

import androidx.annotation.NonNull;

/**
 * @author <a href="mailto:ryo@mm2d.net">大前良介 (OHMAE Ryosuke)</a>
 */
public interface OpenUriModel {
    void openUri(
            @NonNull Context context,
            @NonNull String uri);

    void setUseCustomTabs(boolean use);

    void mayLaunchUrl(@NonNull String url);

    void mayLaunchUrl(@NonNull List<String> urls);
}
