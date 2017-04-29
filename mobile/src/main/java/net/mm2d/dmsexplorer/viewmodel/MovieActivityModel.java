/*
 * Copyright (c) 2017 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.dmsexplorer.viewmodel;

import android.app.Activity;
import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.graphics.Point;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;
import android.widget.VideoView;

import com.android.databinding.library.baseAdapters.BR;

import net.mm2d.android.util.AribUtils;
import net.mm2d.android.util.DisplaySizeUtils;
import net.mm2d.dmsexplorer.R;
import net.mm2d.dmsexplorer.Repository;
import net.mm2d.dmsexplorer.domain.model.MediaServerModel;
import net.mm2d.dmsexplorer.domain.model.MoviePlayerModel;
import net.mm2d.dmsexplorer.domain.model.PlaybackTargetModel;
import net.mm2d.dmsexplorer.domain.model.PlayerModel;
import net.mm2d.dmsexplorer.settings.RepeatMode;
import net.mm2d.dmsexplorer.settings.Settings;
import net.mm2d.dmsexplorer.viewmodel.ControlPanelModel.OnCompletionListener;
import net.mm2d.dmsexplorer.viewmodel.ControlPanelModel.SkipControlListener;

/**
 * @author <a href="mailto:ryo@mm2d.net">大前良介 (OHMAE Ryosuke)</a>
 */
public class MovieActivityModel extends BaseObservable
        implements OnCompletionListener, SkipControlListener {
    public interface OnSwitchListener {
        void onSwitch();
    }

    private static final OnSwitchListener ON_SWITCH_LISTENER = () -> {
    };

    @NonNull
    public final ControlPanelParam controlPanelParam;

    @NonNull
    private String mTitle;
    @NonNull
    private ControlPanelModel mControlPanelModel;
    private int mRightNavigationSize;

    @NonNull
    private OnSwitchListener mOnSwitchListener = ON_SWITCH_LISTENER;
    @NonNull
    private RepeatMode mRepeatMode;
    @DrawableRes
    private int mRepeatIconId;
    @Nullable
    private Toast mToast;

    @NonNull
    private final Activity mActivity;
    @NonNull
    private final VideoView mVideoView;
    @NonNull
    private final Repository mRepository;
    @NonNull
    private final MediaServerModel mServerModel;
    @NonNull
    private final Settings mSettings;

    public MovieActivityModel(@NonNull final Activity activity,
                              @NonNull final VideoView videoView,
                              @NonNull final Repository repository) {
        mActivity = activity;
        mVideoView = videoView;
        mRepository = repository;
        mServerModel = repository.getMediaServerModel();
        mSettings = new Settings(activity);
        mRepeatMode = mSettings.getRepeatModeMovie();
        mRepeatIconId = mRepeatMode.getIconId();

        final int color = ContextCompat.getColor(activity, R.color.translucent_control);
        controlPanelParam = new ControlPanelParam();
        controlPanelParam.setBackgroundColor(color);
        updateTargetModel();
    }

    private void updateTargetModel() {
        final PlaybackTargetModel targetModel = mRepository.getPlaybackTargetModel();
        if (targetModel == null) {
            throw new IllegalStateException();
        }
        final PlayerModel playerModel = new MoviePlayerModel(mVideoView);
        mControlPanelModel = new ControlPanelModel(mActivity, playerModel);
        mControlPanelModel.setRepeatMode(mRepeatMode);
        mControlPanelModel.setOnCompletionListener(this);
        mControlPanelModel.setSkipControlListener(this);
        playerModel.setUri(targetModel.getUri(), null);
        mTitle = AribUtils.toDisplayableString(targetModel.getTitle());

        notifyPropertyChanged(BR.title);
        notifyPropertyChanged(BR.controlPanelModel);
    }

    public void adjustPanel(@NonNull final Activity activity) {
        final Point size = DisplaySizeUtils.getNavigationBarArea(activity);
        setRightNavigationSize(size.x);
        controlPanelParam.setBottomPadding(size.y);
    }

    public void terminate() {
        mControlPanelModel.terminate();
    }

    public void restoreSaveProgress(final int position) {
        mControlPanelModel.restoreSaveProgress(position);
    }

    public int getCurrentProgress() {
        return mControlPanelModel.getProgress();
    }

    public void setOnSwitchListener(@NonNull final OnSwitchListener listener) {
        mOnSwitchListener = listener != null ? listener : ON_SWITCH_LISTENER;
    }

    public void onClickBack() {
        mActivity.onBackPressed();
    }

    public void onClickRepeat() {
        mRepeatMode = mRepeatMode.next();
        mControlPanelModel.setRepeatMode(mRepeatMode);
        setRepeatIconId(mRepeatMode.getIconId());
        mSettings.setRepeatModeMovie(mRepeatMode);

        showRepeatToast();
    }

    private void showRepeatToast() {
        if (mToast != null) {
            mToast.cancel();
        }
        mToast = Toast.makeText(mActivity, mRepeatMode.getMessageId(), Toast.LENGTH_LONG);
        mToast.show();
    }

    @Bindable
    public int getRepeatIconId() {
        return mRepeatIconId;
    }

    public void setRepeatIconId(@DrawableRes final int id) {
        mRepeatIconId = id;
        notifyPropertyChanged(net.mm2d.dmsexplorer.BR.repeatIconId);
    }

    @NonNull
    @Bindable
    public String getTitle() {
        return mTitle;
    }

    @NonNull
    @Bindable
    public ControlPanelModel getControlPanelModel() {
        return mControlPanelModel;
    }

    @Bindable
    public int getRightNavigationSize() {
        return mRightNavigationSize;
    }

    private void setRightNavigationSize(final int rightNavigationSize) {
        controlPanelParam.setMarginRight(rightNavigationSize);
        mRightNavigationSize = rightNavigationSize;
        notifyPropertyChanged(BR.rightNavigationSize);
    }

    @Override
    public void onCompletion() {
        mControlPanelModel.terminate();
        if (!selectNext()) {
            mActivity.onBackPressed();
            return;
        }
        updateTargetModel();
        mOnSwitchListener.onSwitch();
    }

    @Override
    public void next() {
        mControlPanelModel.terminate();
        if (!selectNext()) {
            mActivity.onBackPressed();
            return;
        }
        updateTargetModel();
        mOnSwitchListener.onSwitch();
    }

    @Override
    public void previous() {
        mControlPanelModel.terminate();
        if (!selectPrevious()) {
            mActivity.onBackPressed();
            return;
        }
        updateTargetModel();
        mOnSwitchListener.onSwitch();
    }

    private boolean selectNext() {
        switch (mRepeatMode) {
            case PLAY_ONCE:
                return false;
            case SEQUENTIAL:
                return mServerModel.selectNextObject(MediaServerModel.SCAN_MODE_SEQUENTIAL);
            case REPEAT_ALL:
                return mServerModel.selectNextObject(MediaServerModel.SCAN_MODE_LOOP);
            case REPEAT_ONE:
                return false;
        }
        return false;
    }

    private boolean selectPrevious() {
        switch (mRepeatMode) {
            case PLAY_ONCE:
                return false;
            case SEQUENTIAL:
                return mServerModel.selectPreviousObject(MediaServerModel.SCAN_MODE_SEQUENTIAL);
            case REPEAT_ALL:
                return mServerModel.selectPreviousObject(MediaServerModel.SCAN_MODE_LOOP);
            case REPEAT_ONE:
                return false;
        }
        return false;
    }
}
