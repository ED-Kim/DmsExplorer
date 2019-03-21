/*
 * Copyright (c) 2017 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.dmsexplorer.viewmodel.adapter;

import android.view.View;

import net.mm2d.dmsexplorer.util.ViewLayoutUtils;

import androidx.annotation.NonNull;
import androidx.databinding.BindingAdapter;

/**
 * @author <a href="mailto:ryo@mm2d.net">大前良介 (OHMAE Ryosuke)</a>
 */
public class ViewBindingAdapter {
    @BindingAdapter("layout_marginRight")
    public static void setLayoutMarginRight(
            @NonNull final View view,
            final int margin) {
        ViewLayoutUtils.setLayoutMarginRight(view, margin);
    }
}
