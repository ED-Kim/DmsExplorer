/*
 * Copyright (c) 2017 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.dmsexplorer.settings;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import net.mm2d.dmsexplorer.BuildConfig;
import net.mm2d.dmsexplorer.domain.entity.ContentType;
import net.mm2d.dmsexplorer.settings.theme.Theme;
import net.mm2d.dmsexplorer.settings.theme.ThemeParams;
import net.mm2d.log.Log;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import io.reactivex.Completable;
import io.reactivex.schedulers.Schedulers;

/**
 * SharedPreferencesに覚えさせる設定値を集中管理するクラス。
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介 (OHMAE Ryosuke)</a>
 */
public class Settings {
    @Nullable
    private static Settings sSettings;
    @NonNull
    private static final Lock sLock = new ReentrantLock();
    @NonNull
    private static final Condition sCondition = sLock.newCondition();
    @NonNull
    private static final Thread sMainThread = Looper.getMainLooper().getThread();

    public static Settings get() {
        sLock.lock();
        try {
            while (sSettings == null) {
                if (BuildConfig.DEBUG && Thread.currentThread() == sMainThread) {
                    Log.e(null, "!!!!!!!!!! BLOCK !!!!!!!!!!", new Throwable());
                }
                try {
                    if (!sCondition.await(1, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Settings initialization timeout");
                    }
                } catch (InterruptedException e) {
                    Log.w(e);
                }
            }
            return sSettings;
        } finally {
            sLock.unlock();
        }
    }

    /**
     * アプリ起動時に一度だけコールされ、初期化を行う。
     *
     * @param context コンテキスト
     */
    @SuppressLint("CheckResult")
    public static void initialize(@NonNull final Context context) {
        Completable.fromAction(() -> initializeInner(context))
                .subscribeOn(Schedulers.io())
                .subscribe();
    }

    private static void initializeInner(@NonNull final Context context) {
        final SettingsStorage storage = new SettingsStorage(context);
        Maintainer.maintain(storage);
        sLock.lock();
        try {
            sSettings = new Settings(storage);
            sCondition.signalAll();
        } finally {
            sLock.unlock();
        }
    }

    @NonNull
    private final SettingsStorage mStorage;

    /**
     * インスタンス作成。
     *
     * @param storage SettingsStorage
     */
    private Settings(@NonNull final SettingsStorage storage) {
        mStorage = storage;
    }

    /**
     * 指定コンテンツ種別をアプリで再生するか否かを返す。
     *
     * @param type コンテンツ種別
     * @return アプリで再生する場合true
     */
    public boolean isPlayMyself(@NonNull final ContentType type) {
        switch (type) {
            case MOVIE:
                return isPlayMovieMyself();
            case MUSIC:
                return isPlayMusicMyself();
            case PHOTO:
                return isPlayPhotoMyself();
        }
        return false;
    }

    /**
     * 動画再生をアプリで行うか否かを返す。
     *
     * @return アプリで行う場合true
     */
    private boolean isPlayMovieMyself() {
        return mStorage.readBoolean(Key.PLAY_MOVIE_MYSELF);
    }

    /**
     * 音楽再生をアプリで行うか否かを返す。
     *
     * @return アプリで行う場合true
     */
    private boolean isPlayMusicMyself() {
        return mStorage.readBoolean(Key.PLAY_MUSIC_MYSELF);
    }

    /**
     * 静止画再生をアプリで行うか否かを返す。
     *
     * @return アプリで行う場合true
     */
    private boolean isPlayPhotoMyself() {
        return mStorage.readBoolean(Key.PLAY_PHOTO_MYSELF);
    }

    /**
     * 動画再生のリピートモードを設定する。
     *
     * @return 動画再生のリピートモード
     */
    @NonNull
    public RepeatMode getRepeatModeMovie() {
        return RepeatMode.of(mStorage.readString(Key.REPEAT_MODE_MOVIE));
    }

    /**
     * 動画再生のリピートモードを返す。
     *
     * @param mode 動画再生のリピートモード
     */
    public void setRepeatModeMovie(@NonNull RepeatMode mode) {
        mStorage.writeString(Key.REPEAT_MODE_MOVIE, mode.name());
    }

    /**
     * 音楽再生のリピートモードを返す。
     *
     * @return 音楽再生のリピートモード
     */
    @NonNull
    public RepeatMode getRepeatModeMusic() {
        return RepeatMode.of(mStorage.readString(Key.REPEAT_MODE_MUSIC));
    }

    /**
     * 音楽再生のリピートモードを設定する。
     *
     * @param mode 音楽再生のリピートモード
     */
    public void setRepeatModeMusic(@NonNull RepeatMode mode) {
        mStorage.writeString(Key.REPEAT_MODE_MUSIC, mode.name());
    }

    /**
     * リピートモードの操作案内を表示したか否か。
     *
     * @return 表示した場合true
     */
    public boolean isRepeatIntroduced() {
        return mStorage.readBoolean(Key.REPEAT_INTRODUCED);
    }

    /**
     * リピートモードの操作案内を表示した。
     */
    public void notifyRepeatIntroduced() {
        mStorage.writeBoolean(Key.REPEAT_INTRODUCED, true);
    }

    /**
     * Chrome Custom Tabsを使用するか否か。
     *
     * @return 使用する場合true
     */
    public boolean useCustomTabs() {
        return mStorage.readBoolean(Key.USE_CUSTOM_TABS);
    }

    /**
     * デバイスリストのシングルタップで詳細を表示するか否か。
     *
     * @return シングルタップで詳細を表示する場合true
     */
    public boolean shouldShowDeviceDetailOnTap() {
        return mStorage.readBoolean(Key.SHOULD_SHOW_DEVICE_DETAIL_ON_TAP);
    }

    /**
     * コンテンツリストのシングルタップで詳細を表示するか否か。
     *
     * @return シングルタップで詳細を表示する場合true
     */
    public boolean shouldShowContentDetailOnTap() {
        return mStorage.readBoolean(Key.SHOULD_SHOW_CONTENT_DETAIL_ON_TAP);
    }

    /**
     * 削除削除機能が有効か否か。
     *
     * @return 削除機能が有効なときtrue
     */
    public boolean isDeleteFunctionEnabled() {
        return mStorage.readBoolean(Key.DELETE_FUNCTION_ENABLED);
    }

    /**
     * アップデートファイルを取得した時刻を返す。
     *
     * @return アップデートファイルを取得した時刻
     */
    public long getUpdateFetchTime() {
        return mStorage.readLong(Key.UPDATE_FETCH_TIME);
    }

    /**
     * アップデートファイルを取得した時刻を更新する。
     */
    public void setUpdateFetchTime() {
        mStorage.writeLong(Key.UPDATE_FETCH_TIME, System.currentTimeMillis());
    }

    /**
     * アップデートが利用できるか否かを返す。
     *
     * @return アップデートが利用できるときtrue
     */
    public boolean isUpdateAvailable() {
        return mStorage.readBoolean(Key.UPDATE_AVAILABLE);
    }

    /**
     * アップデートが利用できるか否かを設定する。
     *
     * @param available アップデートが利用できるときtrue
     */
    public void setUpdateAvailable(final boolean available) {
        mStorage.writeBoolean(Key.UPDATE_AVAILABLE, available);
    }

    /**
     * update.jsonの文字列を返す。
     *
     * @return update.jsonの文字列
     */
    @NonNull
    public String getUpdateJson() {
        return mStorage.readString(Key.UPDATE_JSON);
    }

    /**
     * update.jsonの文字列を設定する。
     *
     * @param json update.jsonの文字列
     */
    public void setUpdateJson(@Nullable final String json) {
        mStorage.writeString(Key.UPDATE_JSON, TextUtils.isEmpty(json) ? "" : json);
    }

    /**
     * ブラウズ画面の画面の向き設定を返す。
     *
     * @return ブラウズ画面の画面の向き設定
     */
    @NonNull
    public Orientation getBrowseOrientation() {
        return Orientation.of(mStorage.readString(Key.ORIENTATION_BROWSE));
    }

    /**
     * 動画画面の画面の向き設定を返す。
     *
     * @return 動画画面の画面の向き設定
     */
    @NonNull
    public Orientation getMovieOrientation() {
        return Orientation.of(mStorage.readString(Key.ORIENTATION_MOVIE));
    }

    /**
     * 音楽画面の画面の向き設定を返す。
     *
     * @return 音楽画面の画面の向き設定
     */
    @NonNull
    public Orientation getMusicOrientation() {
        return Orientation.of(mStorage.readString(Key.ORIENTATION_MUSIC));
    }

    /**
     * 画面の画面の向き設定を返す。
     *
     * @return ブラウズ画面の画面の向き設定
     */
    @NonNull
    public Orientation getPhotoOrientation() {
        return Orientation.of(mStorage.readString(Key.ORIENTATION_PHOTO));
    }

    /**
     * DMC画面の画面の向き設定を返す。
     *
     * @return DMC画面の画面の向き設定
     */
    @NonNull
    public Orientation getDmcOrientation() {
        return Orientation.of(mStorage.readString(Key.ORIENTATION_DMC));
    }

    /**
     * 動画再生の最初にUIを表示するか
     *
     * @return 動画再生の最初にUIを表示するときtrue
     */
    public boolean shouldShowMovieUiOnStart() {
        return !mStorage.readBoolean(Key.DO_NOT_SHOW_MOVIE_UI_ON_START);
    }

    /**
     * タッチしたときに動画UIを表示するか
     *
     * @return タッチしたときに動画UIを表示するときtrue
     */
    public boolean shouldShowMovieUiOnTouch() {
        return !mStorage.readBoolean(Key.DO_NOT_SHOW_MOVIE_UI_ON_TOUCH);
    }

    /**
     * 動画UIでコンテンツタイトルを表示するか
     *
     * @return 動画UIでコンテンツタイトルを表示するときtrue
     */
    public boolean shouldShowTitleInMovieUi() {
        return !mStorage.readBoolean(Key.DO_NOT_SHOW_TITLE_IN_MOVIE_UI);
    }

    /**
     * 動画UIの背景を透明にするか
     *
     * @return 動画UIの背景を透明にするときtrue
     */
    public boolean isMovieUiBackgroundTransparent() {
        return mStorage.readBoolean(Key.IS_MOVIE_UI_BACKGROUND_TRANSPARENT);
    }

    /**
     * 静止画再生の最初にUIを表示するか
     *
     * @return 静止画再生の最初にUIを表示するときtrue
     */
    public boolean shouldShowPhotoUiOnStart() {
        return !mStorage.readBoolean(Key.DO_NOT_SHOW_PHOTO_UI_ON_START);
    }

    /**
     * タッチしたときに静止画UIを表示するか
     *
     * @return タッチしたときに静止画UIを表示するときtrue
     */
    public boolean shouldShowPhotoUiOnTouch() {
        return !mStorage.readBoolean(Key.DO_NOT_SHOW_PHOTO_UI_ON_TOUCH);
    }

    /**
     * 静止画UIでコンテンツタイトルを表示するか
     *
     * @return 静止画UIでコンテンツタイトルを表示するときtrue
     */
    public boolean shouldShowTitleInPhotoUi() {
        return !mStorage.readBoolean(Key.DO_NOT_SHOW_TITLE_IN_PHOTO_UI);
    }

    /**
     * 静止画UIの背景を透明にするか
     *
     * @return 静止画UIの背景を透明にするときtrue
     */
    public boolean isPhotoUiBackgroundTransparent() {
        return mStorage.readBoolean(Key.IS_PHOTO_UI_BACKGROUND_TRANSPARENT);
    }

    @NonNull
    public ThemeParams getThemeParams() {
        final Theme theme = mStorage.readBoolean(Key.DARK_THEME)
                ? Theme.DARK : Theme.DEFAULT;
        return theme.getParams();
    }

    public long getLogSendTime() {
        return mStorage.readLong(Key.LOG_SEND_TIME);
    }

    public void setLogSendTime(final long time) {
        mStorage.writeLong(Key.LOG_SEND_TIME, time);
    }

    @NonNull
    public Bundle getDump() {
        final List<Key> keys = Arrays.asList(
                Key.PLAY_MOVIE_MYSELF,
                Key.PLAY_MUSIC_MYSELF,
                Key.PLAY_PHOTO_MYSELF,
                Key.USE_CUSTOM_TABS,
                Key.SHOULD_SHOW_DEVICE_DETAIL_ON_TAP,
                Key.SHOULD_SHOW_CONTENT_DETAIL_ON_TAP,
                Key.DELETE_FUNCTION_ENABLED,
                Key.DARK_THEME,
                Key.DO_NOT_SHOW_MOVIE_UI_ON_START,
                Key.DO_NOT_SHOW_MOVIE_UI_ON_TOUCH,
                Key.DO_NOT_SHOW_TITLE_IN_MOVIE_UI,
                Key.IS_MOVIE_UI_BACKGROUND_TRANSPARENT,
                Key.DO_NOT_SHOW_PHOTO_UI_ON_START,
                Key.DO_NOT_SHOW_PHOTO_UI_ON_TOUCH,
                Key.DO_NOT_SHOW_TITLE_IN_PHOTO_UI,
                Key.IS_PHOTO_UI_BACKGROUND_TRANSPARENT,
                Key.ORIENTATION_BROWSE,
                Key.ORIENTATION_MOVIE,
                Key.ORIENTATION_MUSIC,
                Key.ORIENTATION_PHOTO,
                Key.ORIENTATION_DMC,
                Key.REPEAT_MODE_MOVIE,
                Key.REPEAT_MODE_MUSIC
        );
        final Bundle bundle = new Bundle();
        for (final Key key : keys) {
            if (key.isBooleanKey()) {
                bundle.putString(key.name(), mStorage.readBoolean(key) ? "on" : "off");
            } else if (key.isIntKey()) {
                bundle.putString(key.name(), String.valueOf(mStorage.readInt(key)));
            } else if (key.isLongKey()) {
                bundle.putString(key.name(), String.valueOf(mStorage.readLong(key)));
            } else if (key.isStringKey()) {
                bundle.putString(key.name(), mStorage.readString(key));
            }
        }
        return bundle;
    }
}
