/*
 * Copyright (c) 2016 大前良介 (OHMAE Ryosuke)
 *
 * This software is released under the MIT License.
 * http://opensource.org/licenses/MIT
 */

package net.mm2d.android.upnp.cds;

import android.os.Parcel;
import android.os.Parcelable;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * シンプルなXMLのタグ情報を表現するクラス
 *
 * <p>Elementのままでは情報の参照コストが高いため、
 * よりシンプルな構造に格納するためのクラス。
 * CdsObjectのXMLのようにElementが入れ子になることのない
 * タグ＋値、属性＋値の情報を表現できれば十分なものを表現するのに使用する。
 * 入れ子関係を持つXMLは表現できない。
 *
 * @author <a href="mailto:ryo@mm2d.net">大前良介 (OHMAE Ryosuke)</a>
 */
public class Tag implements Parcelable {
    @NonNull
    private final String mName;
    @NonNull
    private final String mValue;
    @NonNull
    private final Map<String, String> mAttribute;

    /**
     * インスタンス作成。
     *
     * パッケージ外でのインスタンス化禁止
     *
     * @param element タグ情報
     */
    Tag(@NonNull final Element element) {
        this(element, false);
    }

    /**
     * インスタンス作成。
     *
     * パッケージ外でのインスタンス化禁止
     *
     * @param element タグ情報
     * @param root    タグがitem/containerのときtrue
     */
    Tag(
            @NonNull final Element element,
            final boolean root) {
        this(element, root ? "" : element.getTextContent());
    }

    /**
     * インスタンス作成。
     *
     * @param element タグ情報
     * @param value   タグの値
     */
    private Tag(
            @NonNull final Element element,
            @NonNull final String value) {
        mName = element.getTagName();
        mValue = value;
        final NamedNodeMap attributes = element.getAttributes();
        final int size = attributes.getLength();
        if (size == 0) {
            mAttribute = Collections.emptyMap();
            return;
        }
        mAttribute = new LinkedHashMap<>(size);
        for (int i = 0; i < size; i++) {
            final Node attr = attributes.item(i);
            mAttribute.put(attr.getNodeName(), attr.getNodeValue());
        }
    }

    /**
     * タグ名を返す。
     *
     * @return タグ名
     */
    @NonNull
    public String getName() {
        return mName;
    }

    /**
     * タグの値を返す。
     *
     * @return タグの値
     */
    @NonNull
    public String getValue() {
        return mValue;
    }

    /**
     * 属性値を返す。
     *
     * @param name 属性名
     * @return 属性値、見つからない場合null
     */
    @Nullable
    public String getAttribute(@Nullable final String name) {
        return mAttribute.get(name);
    }

    /**
     * 属性値を格納したMapを返す。
     *
     * @return 属性値を格納したUnmodifiable Map
     */
    @NonNull
    public Map<String, String> getAttributes() {
        if (mAttribute.size() == 0) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(mAttribute);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(mValue);
        for (final Entry<String, String> entry : mAttribute.entrySet()) {
            sb.append("\n");
            sb.append("@");
            sb.append(entry.getKey());
            sb.append(" => ");
            sb.append(entry.getValue());
        }
        return sb.toString();
    }

    /**
     * Parcelable用のコンストラクタ。
     *
     * @param in Parcel
     */
    private Tag(@NonNull final Parcel in) {
        mName = in.readString();
        mValue = in.readString();
        final int size = in.readInt();
        if (size == 0) {
            mAttribute = Collections.emptyMap();
        } else {
            mAttribute = new LinkedHashMap<>(size);
            for (int i = 0; i < size; i++) {
                final String name = in.readString();
                final String value = in.readString();
                mAttribute.put(name, value);
            }
        }
    }

    @Override
    public void writeToParcel(
            @NonNull final Parcel dest,
            final int flags) {
        dest.writeString(mName);
        dest.writeString(mValue);
        dest.writeInt(mAttribute.size());
        for (final Entry<String, String> entry : mAttribute.entrySet()) {
            dest.writeString(entry.getKey());
            dest.writeString(entry.getValue());
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Parcelableのためのフィールド
     */
    public static final Creator<Tag> CREATOR = new Creator<Tag>() {
        @Override
        public Tag createFromParcel(final Parcel in) {
            return new Tag(in);
        }

        @Override
        public Tag[] newArray(final int size) {
            return new Tag[size];
        }
    };
}
