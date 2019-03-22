/*
 * Copyright (c) 2017 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.dmsexplorer.view.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;

import net.mm2d.android.util.AribUtils;
import net.mm2d.android.util.Toaster;
import net.mm2d.dmsexplorer.R;
import net.mm2d.dmsexplorer.Repository;
import net.mm2d.dmsexplorer.domain.entity.ContentEntity;
import net.mm2d.dmsexplorer.domain.model.MediaServerModel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;

/**
 * @author <a href="mailto:ryo@mm2d.net">大前良介 (OHMAE Ryosuke)</a>
 */
public class DeleteDialog extends DialogFragment {

    @NonNull
    public static DeleteDialog newInstance() {
        return new DeleteDialog();
    }

    public static void show(@NonNull final FragmentActivity activity) {
        if (activity.getLifecycle().getCurrentState() != Lifecycle.State.RESUMED) {
            return;
        }
        newInstance().show(activity.getSupportFragmentManager(), "");
    }

    public interface OnDeleteListener {
        void onDelete();
    }

    @Nullable
    private OnDeleteListener mOnDeleteListener;

    @Override
    public void onAttach(final Context context) {
        super.onAttach(context);
        if (context instanceof OnDeleteListener) {
            mOnDeleteListener = (OnDeleteListener) context;
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        final Context context = getActivity();
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        final MediaServerModel model = Repository.get().getMediaServerModel();
        if (model == null) {
            dismiss();
            return builder.create();
        }
        final ContentEntity entity = model.getSelectedEntity();
        if (entity == null) {
            dismiss();
            return builder.create();
        }
        final Context applicationContext = context.getApplicationContext();
        return builder
                .setIcon(R.drawable.ic_warning)
                .setTitle(R.string.dialog_title_delete)
                .setMessage(getString(R.string.dialog_message_delete,
                        AribUtils.toDisplayableString(entity.getName())))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.ok, (d, i) ->
                        model.delete(entity,
                                () -> {
                                    Toaster.show(applicationContext, R.string.toast_delete_succeed);
                                    if (mOnDeleteListener != null) {
                                        mOnDeleteListener.onDelete();
                                    }
                                },
                                () -> Toaster.show(applicationContext, R.string.toast_delete_error)))
                .create();
    }
}
