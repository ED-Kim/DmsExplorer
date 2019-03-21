/*
 * Copyright (c) 2017 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.dmsexplorer.viewmodel;

import android.app.Activity;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import net.mm2d.android.util.AribUtils;
import net.mm2d.android.util.Toaster;
import net.mm2d.dmsexplorer.BR;
import net.mm2d.dmsexplorer.R;
import net.mm2d.dmsexplorer.Repository;
import net.mm2d.dmsexplorer.domain.entity.ContentEntity;
import net.mm2d.dmsexplorer.domain.model.MediaServerModel;
import net.mm2d.dmsexplorer.domain.model.PlaybackTargetModel;
import net.mm2d.dmsexplorer.domain.model.PlayerModel;
import net.mm2d.dmsexplorer.domain.model.PlayerModel.StatusListener;
import net.mm2d.dmsexplorer.view.adapter.PropertyAdapter;
import net.mm2d.dmsexplorer.view.view.ScrubBar;
import net.mm2d.dmsexplorer.view.view.ScrubBar.Accuracy;
import net.mm2d.dmsexplorer.view.view.ScrubBar.ScrubBarListener;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;

/**
 * @author <a href="mailto:ryo@mm2d.net">大前良介 (OHMAE Ryosuke)</a>
 */
public class DmcActivityModel extends BaseObservable implements StatusListener {
    private static final long TRACKING_DELAY = 1000L;
    private static final char EN_SPACE = 0x2002; // &ensp;
    @NonNull
    public final String title;
    @NonNull
    public final String subtitle;
    @NonNull
    public final PropertyAdapter propertyAdapter;
    @DrawableRes
    public final int imageResource;
    public final boolean isPlayControlEnabled;
    public final boolean hasDuration;
    @NonNull
    public final ScrubBarListener seekBarListener;

    @NonNull
    private String mProgressText = makeTimeText(0);
    @NonNull
    private String mDurationText = makeTimeText(0);
    private boolean mPlaying;
    private boolean mPrepared;
    private int mDuration;
    private int mProgress;
    private boolean mSeekable;
    private int mPlayButtonResId;
    @NonNull
    private String mScrubText = "";
    @NonNull
    private List<Integer> mChapterList = Collections.emptyList();
    private boolean mChapterInfoEnabled;

    @NonNull
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    @NonNull
    private final Activity mActivity;
    @NonNull
    private final PlaybackTargetModel mTargetModel;
    @NonNull
    private final PlayerModel mRendererModel;
    private boolean mTracking;

    @NonNull
    private final Runnable mTrackingCancel;

    public DmcActivityModel(
            @NonNull final Activity activity,
            @NonNull final Repository repository) {
        mActivity = activity;
        final PlaybackTargetModel targetModel = repository.getPlaybackTargetModel();
        final PlayerModel playerModel = repository.getMediaRendererModel();
        if (playerModel == null || targetModel == null || targetModel.getUri() == Uri.EMPTY) {
            throw new IllegalStateException();
        }
        mTargetModel = targetModel;
        mRendererModel = playerModel;
        mRendererModel.setStatusListener(this);
        mPlayButtonResId = R.drawable.ic_play;

        final ContentEntity entity = mTargetModel.getContentEntity();
        title = AribUtils.toDisplayableString(entity.getName());
        hasDuration = entity.getType().hasDuration();
        isPlayControlEnabled = hasDuration && mRendererModel.canPause();
        final MediaServerModel serverModel = repository.getMediaServerModel();
        subtitle = mRendererModel.getName()
                + "  ←  "
                + serverModel.getMediaServer().getFriendlyName();
        propertyAdapter = PropertyAdapter.ofContent(mActivity, entity);
        imageResource = getImageResource(entity);

        mTrackingCancel = () -> mTracking = false;
        seekBarListener = new ScrubBarListener() {
            @Override
            public void onProgressChanged(
                    @NonNull final ScrubBar seekBar,
                    final int progress,
                    final boolean fromUser) {
                if (fromUser) {
                    setProgressText(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(@NonNull final ScrubBar seekBar) {
                mHandler.removeCallbacks(mTrackingCancel);
                mTracking = true;
            }

            @Override
            public void onStopTrackingTouch(@NonNull final ScrubBar seekBar) {
                mRendererModel.seekTo(seekBar.getProgress());
                mHandler.postDelayed(mTrackingCancel, TRACKING_DELAY);
                setScrubText("");
            }

            @Override
            public void onAccuracyChanged(
                    @NonNull final ScrubBar seekBar,
                    @Accuracy final int accuracy) {
                setScrubText(getAccuracyText(accuracy));
            }
        };
    }

    public void initialize() {
        final Uri uri = mTargetModel.getUri();
        mRendererModel.setUri(uri, mTargetModel.getContentEntity());
    }

    public void terminate() {
        mRendererModel.terminate();
    }

    @Bindable
    public int getProgress() {
        return mProgress;
    }

    public void setProgress(final int progress) {
        setProgressText(progress);
        mProgress = progress;
        notifyPropertyChanged(BR.progress);
    }

    @Bindable
    public int getDuration() {
        return mDuration;
    }

    public void setDuration(final int duration) {
        mDuration = duration;
        notifyPropertyChanged(BR.duration);
        if (duration > 0) {
            setSeekable(true);
        }
        setDurationText(duration);
        setPrepared(true);
        setChapterInfoEnabled();
    }

    @NonNull
    @Bindable
    public String getProgressText() {
        return mProgressText;
    }

    private void setProgressText(final int progress) {
        mProgressText = makeTimeText(progress);
        notifyPropertyChanged(BR.progressText);
    }

    @NonNull
    @Bindable
    public String getDurationText() {
        return mDurationText;
    }

    private void setDurationText(final int duration) {
        mDurationText = makeTimeText(duration);
        notifyPropertyChanged(BR.durationText);
    }

    private void setPlaying(final boolean playing) {
        if (mPlaying == playing) {
            return;
        }
        mPlaying = playing;
        setPlayButtonResId(playing ? R.drawable.ic_pause : R.drawable.ic_play);
    }

    @NonNull
    @Bindable
    public String getScrubText() {
        return mScrubText;
    }

    private String getAccuracyText(final int accuracy) {
        switch (accuracy) {
            case ScrubBar.ACCURACY_NORMAL:
                return mActivity.getString(R.string.seek_bar_scrub_normal);
            case ScrubBar.ACCURACY_HALF:
                return mActivity.getString(R.string.seek_bar_scrub_half);
            case ScrubBar.ACCURACY_QUARTER:
                return mActivity.getString(R.string.seek_bar_scrub_quarter);
            default:
                return "";
        }
    }

    private void setScrubText(@NonNull final String scrubText) {
        mScrubText = scrubText;
        notifyPropertyChanged(BR.scrubText);
    }

    @Bindable
    public int getPlayButtonResId() {
        return mPlayButtonResId;
    }

    private void setPlayButtonResId(final int playButtonResId) {
        mPlayButtonResId = playButtonResId;
        notifyPropertyChanged(BR.playButtonResId);
    }

    @Bindable
    public boolean isPrepared() {
        return mPrepared;
    }

    private void setPrepared(final boolean prepared) {
        mPrepared = prepared;
        notifyPropertyChanged(BR.prepared);
    }

    @Bindable
    public boolean isSeekable() {
        return mSeekable;
    }

    private void setSeekable(final boolean seekable) {
        mSeekable = seekable;
        notifyPropertyChanged(BR.seekable);
    }

    @Bindable
    @NonNull
    public List<Integer> getChapterList() {
        return mChapterList;
    }

    private void setChapterList(@NonNull final List<Integer> chapterList) {
        mChapterList = chapterList;
        notifyPropertyChanged(BR.chapterList);
        setChapterInfoEnabled();
        if (chapterList.isEmpty()) {
            return;
        }
        mHandler.post(() -> {
            final int count = propertyAdapter.getItemCount();
            propertyAdapter.addEntry(mActivity.getString(R.string.prop_chapter_info),
                    makeChapterString(chapterList));
            propertyAdapter.notifyItemInserted(count);
        });
    }

    @Bindable
    public boolean isChapterInfoEnabled() {
        return mChapterInfoEnabled;
    }

    private void setChapterInfoEnabled() {
        mChapterInfoEnabled = (mDuration != 0 && !mChapterList.isEmpty());
        notifyPropertyChanged(BR.chapterInfoEnabled);
    }

    @DrawableRes
    private static int getImageResource(@NonNull final ContentEntity entity) {
        switch (entity.getType()) {
            case MOVIE:
                return R.drawable.ic_movie;
            case MUSIC:
                return R.drawable.ic_music;
            case PHOTO:
                return R.drawable.ic_image;
            default:
                break;
        }
        return 0;
    }

    private static String makeTimeText(final int millisecond) {
        final long second = (millisecond / 1000) % 60;
        final long minute = (millisecond / 60000) % 60;
        final long hour = millisecond / 3600000;
        return String.format(Locale.US, "%01d:%02d:%02d", hour, minute, second);
    }

    @NonNull
    private static String makeChapterString(@NonNull final List<Integer> chapterList) {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < chapterList.size(); i++) {
            if (sb.length() != 0) {
                sb.append("\n");
            }
            if (i < 9) {
                sb.append(EN_SPACE);
            }
            sb.append(String.valueOf(i + 1));
            sb.append(" : ");
            final int chapter = chapterList.get(i);
            sb.append(makeTimeText(chapter));
        }
        return sb.toString();
    }

    public void onClickPlay() {
        if (mPlaying) {
            mRendererModel.pause();
        } else {
            mRendererModel.play();
        }
    }

    public void onClickNext() {
        mRendererModel.next();
    }

    public void onClickPrevious() {
        mRendererModel.previous();
    }

    @Override
    public void notifyDuration(final int duration) {
        setDuration(duration);
    }

    @Override
    public void notifyProgress(final int progress) {
        if (!mTracking) {
            setProgress(progress);
        }
    }

    @Override
    public void notifyPlayingState(final boolean playing) {
        setPlaying(playing);
    }

    @Override
    public void notifyChapterList(@NonNull final List<Integer> chapterList) {
        setChapterList(chapterList);
    }

    @Override
    public boolean onError(
            final int what,
            final int extra) {
        Toaster.show(mActivity, R.string.toast_command_error);
        return false;
    }

    @Override
    public boolean onInfo(
            final int what,
            final int extra) {
        return false;
    }

    @Override
    public void onCompletion() {
        ActivityCompat.finishAfterTransition(mActivity);
    }
}
