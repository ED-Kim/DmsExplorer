/*
 * Copyright (c) 2017 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.dmsexplorer.viewmodel.adapter;

import android.graphics.Color;
import android.graphics.Paint;
import android.widget.TextView;

import net.mm2d.dmsexplorer.util.AttrUtils;

import androidx.annotation.NonNull;
import androidx.databinding.BindingAdapter;

/**
 * @author <a href="mailto:ryo@mm2d.net">大前良介 (OHMAE Ryosuke)</a>
 */
public class TextViewBindingAdapter {
    @BindingAdapter("underline")
    public static void setUnderlineFlag(
            @NonNull final TextView view,
            final boolean underline) {
        if (underline) {
            view.setPaintFlags(view.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        } else {
            view.setPaintFlags(view.getPaintFlags() & ~Paint.UNDERLINE_TEXT_FLAG);
        }
    }

    @BindingAdapter("textColorAttr")
    public static void setTextColorAttr(
            @NonNull final TextView view,
            final int attr) {
        view.setTextColor(AttrUtils.resolveColor(view.getContext(), attr, Color.BLACK));
    }
}
