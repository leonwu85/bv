/*****************************************************************************
 * LibVLC.java
 *****************************************************************************
 * Copyright © 2010-2013 VLC authors and VideoLAN
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301, USA.
 *****************************************************************************/

package org.videolan.libvlc;

import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;

import org.videolan.libvlc.interfaces.AbstractVLCEvent;
import org.videolan.libvlc.interfaces.ILibVLC;
import org.videolan.libvlc.util.HWDecoderUtil;

import java.util.ArrayList;
import java.util.List;

/*
 * BV fork of libvlcjni (master @ a8d53a91, libvlc-all 4.0.0-eap29) that also drives the
 * libvlcjni-3.x native libraries (libvlc-all 3.7.x). Everything the JNI side of either
 * version looks up by name is present, and behaviour that differs between the two cores is
 * switched on {@link #majorVersion()} at runtime. See player/libvlcjni/README.md.
 */
@SuppressWarnings("unused, JniMissingFunction")
public class LibVLC extends VLCObject<ILibVLC.Event> implements ILibVLC {
    private static final String TAG = "VLC/LibVLC";

    final Context mAppContext;

    public static class Event extends AbstractVLCEvent {
        protected Event(int type) {
            super(type);
        }
    }

    /**
     * Create a LibVLC withs options
     *
     * @param options
     */
    public LibVLC(Context context, List<String> options) {
        mAppContext = context.getApplicationContext();
        loadLibraries();

        if (isVlc3()) {
            /* libvlcjni-3.x: pick the audio output and the display chroma for the device unless the
             * caller already did (VLC 4 selects both by itself). */
            if (options == null)
                options = new ArrayList<>();
            boolean setAout = true, setChroma = true;
            for (String option : options) {
                if (option.startsWith("--aout="))
                    setAout = false;
                if (option.startsWith("--android-display-chroma"))
                    setChroma = false;
                if (!setAout && !setChroma)
                    break;
            }
            if (setAout) {
                final HWDecoderUtil.AudioOutput hwAout = HWDecoderUtil.getAudioOutputFromDevice();
                options.add(hwAout == HWDecoderUtil.AudioOutput.OPENSLES ? "--aout=opensles" : "--aout=android_audiotrack");
            }
            if (setChroma) {
                options.add("--android-display-chroma");
                options.add("RV16");
            }
        }

        nativeNew(options != null ? options.toArray(new String[options.size()]) : null,
                  context.getDir("vlc", Context.MODE_PRIVATE).getAbsolutePath());
    }

    /**
     * Whether the loaded native libraries are the VLC 3 line (libvlcjni-3.x). Requires the
     * libraries to be loaded; used to switch behaviours that differ between the two cores.
     */
    public static boolean isVlc3() {
        return majorVersion() < 4;
    }

    /**
     * Create a LibVLC
     */
    public LibVLC(Context context) {
        this(context, null);
    }

    /**
     * Get the libVLC version
     *
     * @return the libVLC version string
     */
    public static native String version();

    /**
     * Get the libVLC major version
     *
     * @return the libVLC major version, always >= 3
     */
    public static native int majorVersion();

    /**
     * Get the libVLC compiler
     *
     * @return the libVLC compiler string
     */
    public static native String compiler();

    /**
     * Get the libVLC changeset
     *
     * @return the libVLC changeset string
     */
    public static native String changeset();

    @Override
    protected ILibVLC.Event onEventNative(int eventType, long arg1, long arg2, float argf1, @Nullable String args1) {
        return null;
    }

    @Override
    public Context getAppContext() {
        return mAppContext;
    }

    @Override
    protected void onReleaseNative() {
        nativeRelease();
    }

    /**
     * Sets the application name. LibVLC passes this as the user agent string
     * when a protocol requires it.
     *
     * @param name human-readable application name, e.g. "FooBar player 1.2.3"
     * @param http HTTP User Agent, e.g. "FooBar/1.2.3 Python/2.6.0"
     */
    public void setUserAgent(String name, String http) {
        nativeSetUserAgent(name, http);
    }

    /* JNI */
    private native void nativeNew(String[] options, String homePath);

    private native void nativeRelease();

    private native void nativeSetUserAgent(String name, String http);

    private static boolean sLoaded = false;

    /**
     * Tell LibVLC that libvlc.so / libvlcjni.so were already loaded by the application (e.g. with
     * {@code System.load} from a download directory), so {@link #loadLibraries()} becomes a no-op.
     */
    public static synchronized void markLibrariesLoaded() {
        sLoaded = true;
    }

    public static synchronized boolean areLibrariesLoaded() {
        return sLoaded;
    }

    /**
     * Loads the native libraries bundled in the APK.
     *
     * Unlike upstream this never calls {@code System.exit(1)}: a missing library surfaces as an
     * {@link UnsatisfiedLinkError} so the application can fall back to another player.
     */
    public static synchronized void loadLibraries() {
        if (sLoaded)
            return;

        try {
            System.loadLibrary("c++_shared");
        } catch (UnsatisfiedLinkError ule) {
            Log.e(TAG, "Can't load c++_shared library");
        } catch (SecurityException se) {
            Log.e(TAG, "Encountered a security issue when loading c++_shared library");
        }

        try {
            System.loadLibrary("vlc");
            System.loadLibrary("vlcjni");
        } catch (SecurityException se) {
            Log.e(TAG, "Encountered a security issue when loading vlcjni library: " + se);
            throw new UnsatisfiedLinkError("Security issue when loading vlcjni: " + se.getMessage());
        }
        sLoaded = true;
    }
}
