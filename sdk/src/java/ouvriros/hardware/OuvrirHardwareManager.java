/*
 * Copyright (C) 2015-2016 The CyanogenMod Project
 *               2017-2022 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ouvriros.hardware;

import android.content.Context;
import android.hidl.base.V1_0.IBase;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Range;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.ArrayUtils;

import ouvriros.app.OuvrirContextConstants;

import vendor.ouvrir.livedisplay.V2_0.IAdaptiveBacklight;
import vendor.ouvrir.livedisplay.V2_0.IAutoContrast;
import vendor.ouvrir.livedisplay.V2_0.IColorBalance;
import vendor.ouvrir.livedisplay.V2_0.IColorEnhancement;
import vendor.ouvrir.livedisplay.V2_0.IDisplayColorCalibration;
import vendor.ouvrir.livedisplay.V2_0.IDisplayModes;
import vendor.ouvrir.livedisplay.V2_0.IPictureAdjustment;
import vendor.ouvrir.livedisplay.V2_0.IReadingEnhancement;
import vendor.ouvrir.livedisplay.V2_0.ISunlightEnhancement;
import vendor.ouvrir.livedisplay.V2_1.IAntiFlicker;
import vendor.ouvrir.touch.V1_0.IGloveMode;
import vendor.ouvrir.touch.V1_0.IHighTouchPollingRate;
import vendor.ouvrir.touch.V1_0.IKeyDisabler;
import vendor.ouvrir.touch.V1_0.IKeySwapper;
import vendor.ouvrir.touch.V1_0.IStylusMode;
import vendor.ouvrir.touch.V1_0.ITouchscreenGesture;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Manages access to OuvrirOS hardware extensions
 *
 *  <p>
 *  This manager requires the HARDWARE_ABSTRACTION_ACCESS permission.
 *  <p>
 *  To get the instance of this class, utilize OuvrirHardwareManager#getInstance(Context context)
 */
public final class OuvrirHardwareManager {
    private static final String TAG = "OuvrirHardwareManager";

    // The VisibleForTesting annotation is to ensure Proguard doesn't remove these
    // fields, as they might be used via reflection. When the @Keep annotation in
    // the support library is properly handled in the platform, we should change this.

    /**
     * Adaptive backlight support (this refers to technologies like NVIDIA SmartDimmer,
     * QCOM CABL or Samsung CABC)
     */
    @VisibleForTesting
    public static final int FEATURE_ADAPTIVE_BACKLIGHT = 0x1;

    /**
     * Color enhancement support
     */
    @VisibleForTesting
    public static final int FEATURE_COLOR_ENHANCEMENT = 0x2;

    /**
     * Display RGB color calibration
     */
    @VisibleForTesting
    public static final int FEATURE_DISPLAY_COLOR_CALIBRATION = 0x4;

    /**
     * High Touch Polling Rate
     */
    @VisibleForTesting
    public static final int FEATURE_HIGH_TOUCH_POLLING_RATE = 0x8;

    /**
     * High touch sensitivity for touch panels
     */
    @VisibleForTesting
    public static final int FEATURE_HIGH_TOUCH_SENSITIVITY = 0x10;

    /**
     * Hardware navigation key disablement
     */
    @VisibleForTesting
    public static final int FEATURE_KEY_DISABLE = 0x20;

    /**
     * Hardware navigation key swapping
     */
    @VisibleForTesting
    public static final int FEATURE_KEY_SWAP = 0x40;

    /**
     * Increased display readability in bright light
     */
    @VisibleForTesting
    public static final int FEATURE_SUNLIGHT_ENHANCEMENT = 0x100;

    /**
     * Variable vibrator intensity
     */
    @VisibleForTesting
    public static final int FEATURE_VIBRATOR = 0x400;

    /**
     * Touchscreen hovering
     */
    @VisibleForTesting
    public static final int FEATURE_TOUCH_HOVERING = 0x800;

    /**
     * Auto contrast
     */
    @VisibleForTesting
    public static final int FEATURE_AUTO_CONTRAST = 0x1000;

    /**
     * Display modes
     */
    @VisibleForTesting
    public static final int FEATURE_DISPLAY_MODES = 0x2000;

    /**
     * Reading mode
     */
    @VisibleForTesting
    public static final int FEATURE_READING_ENHANCEMENT = 0x4000;

    /**
     * Color balance
     */
    @VisibleForTesting
    public static final int FEATURE_COLOR_BALANCE = 0x20000;

    /**
     * HSIC picture adjustment
     */
    @VisibleForTesting
    public static final int FEATURE_PICTURE_ADJUSTMENT = 0x40000;

    /**
     * Touchscreen gesture
     */
    @VisibleForTesting
    public static final int FEATURE_TOUCHSCREEN_GESTURES = 0x80000;

    /**
     * Anti flicker mode
     */
    @VisibleForTesting
    public static final int FEATURE_ANTI_FLICKER = 0x200000;

    private static final List<Integer> BOOLEAN_FEATURES = Arrays.asList(
        FEATURE_ADAPTIVE_BACKLIGHT,
        FEATURE_ANTI_FLICKER,
        FEATURE_AUTO_CONTRAST,
        FEATURE_COLOR_ENHANCEMENT,
        FEATURE_HIGH_TOUCH_POLLING_RATE,
        FEATURE_HIGH_TOUCH_SENSITIVITY,
        FEATURE_KEY_DISABLE,
        FEATURE_KEY_SWAP,
        FEATURE_SUNLIGHT_ENHANCEMENT,
        FEATURE_TOUCH_HOVERING,
        FEATURE_READING_ENHANCEMENT
    );

    private static IOuvrirHardwareService sService;
    private static OuvrirHardwareManager sOuvrirHardwareManagerInstance;

    private Context mContext;

    private final ArrayMap<String, String> mDisplayModeMappings = new ArrayMap<String, String>();
    private final boolean mFilterDisplayModes;

    // HIDL hals
    private HashMap<Integer, IBase> mHIDLMap = new HashMap<Integer, IBase>();

    /**
     * @hide to prevent subclassing from outside of the framework
     */
    private OuvrirHardwareManager(Context context) {
        Context appContext = context.getApplicationContext();
        if (appContext != null) {
            mContext = appContext;
        } else {
            mContext = context;
        }
        sService = getService();

        if (context.getPackageManager().hasSystemFeature(
                OuvrirContextConstants.Features.HARDWARE_ABSTRACTION) && !checkService()) {
            Log.wtf(TAG, "Unable to get OuvrirHardwareService. The service either" +
                    " crashed, was not started, or the interface has been called to early in" +
                    " SystemServer init");
        }

        final String[] mappings = mContext.getResources().getStringArray(
                org.ouvriros.platform.internal.R.array.config_displayModeMappings);
        if (mappings != null && mappings.length > 0) {
            for (String mapping : mappings) {
                String[] split = mapping.split(":");
                if (split.length == 2) {
                    mDisplayModeMappings.put(split[0], split[1]);
                }
            }
        }
        mFilterDisplayModes = mContext.getResources().getBoolean(
                org.ouvriros.platform.internal.R.bool.config_filterDisplayModes);
    }

    /**
     * Get or create an instance of the {@link ouvriros.hardware.OuvrirHardwareManager}
     * @param context
     * @return {@link OuvrirHardwareManager}
     */
    public static OuvrirHardwareManager getInstance(Context context) {
        if (sOuvrirHardwareManagerInstance == null) {
            sOuvrirHardwareManagerInstance = new OuvrirHardwareManager(context);
        }
        return sOuvrirHardwareManagerInstance;
    }

    /** @hide */
    public static IOuvrirHardwareService getService() {
        if (sService != null) {
            return sService;
        }
        IBinder b = ServiceManager.getService(OuvrirContextConstants.OUVRIR_HARDWARE_SERVICE);
        if (b != null) {
            sService = IOuvrirHardwareService.Stub.asInterface(b);
            return sService;
        }
        return null;
    }

    /**
     * Determine if a Ouvrir Hardware feature is supported on this device
     *
     * @param feature The Ouvrir Hardware feature to query
     *
     * @return true if the feature is supported, false otherwise.
     */
    public boolean isSupported(int feature) {
        return isSupportedHIDL(feature) || isSupportedHWC2(feature);
    }

    private boolean isSupportedHIDL(int feature) {
        if (!mHIDLMap.containsKey(feature)) {
            mHIDLMap.put(feature, getHIDLService(feature));
        }
        return mHIDLMap.get(feature) != null;
    }

    private boolean isSupportedHWC2(int feature) {
        try {
            if (checkService()) {
                return feature == (sService.getSupportedFeatures() & feature);
            }
        } catch (RemoteException e) {
        }
        return false;
    }

    private IBase getHIDLService(int feature) {
        try {
            switch (feature) {
                case FEATURE_ADAPTIVE_BACKLIGHT:
                    return IAdaptiveBacklight.getService(true);
                case FEATURE_ANTI_FLICKER:
                    return IAntiFlicker.getService(true);
                case FEATURE_AUTO_CONTRAST:
                    return IAutoContrast.getService(true);
                case FEATURE_COLOR_BALANCE:
                    return IColorBalance.getService(true);
                case FEATURE_COLOR_ENHANCEMENT:
                    return IColorEnhancement.getService(true);
                case FEATURE_DISPLAY_COLOR_CALIBRATION:
                    return IDisplayColorCalibration.getService(true);
                case FEATURE_DISPLAY_MODES:
                    return IDisplayModes.getService(true);
                case FEATURE_PICTURE_ADJUSTMENT:
                    return IPictureAdjustment.getService(true);
                case FEATURE_READING_ENHANCEMENT:
                    return IReadingEnhancement.getService(true);
                case FEATURE_SUNLIGHT_ENHANCEMENT:
                    return ISunlightEnhancement.getService(true);
                case FEATURE_HIGH_TOUCH_POLLING_RATE:
                    return IHighTouchPollingRate.getService(true);
                case FEATURE_HIGH_TOUCH_SENSITIVITY:
                    return IGloveMode.getService(true);
                case FEATURE_KEY_DISABLE:
                    return IKeyDisabler.getService(true);
                case FEATURE_KEY_SWAP:
                    return IKeySwapper.getService(true);
                case FEATURE_TOUCH_HOVERING:
                    return IStylusMode.getService(true);
                case FEATURE_TOUCHSCREEN_GESTURES:
                    return ITouchscreenGesture.getService(true);
            }
        } catch (NoSuchElementException | RemoteException e) {
        }
        return null;
    }

    /**
     * String version for preference constraints
     *
     * @hide
     */
    public boolean isSupported(String feature) {
        if (!feature.startsWith("FEATURE_")) {
            return false;
        }
        try {
            Field f = getClass().getField(feature);
            if (f != null) {
                return isSupported((int) f.get(null));
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Log.d(TAG, e.getMessage(), e);
        }

        return false;
    }
    /**
     * Determine if the given feature is enabled or disabled.
     *
     * Only used for features which have simple enable/disable controls.
     *
     * @param feature the Ouvrir Hardware feature to query
     *
     * @return true if the feature is enabled, false otherwise.
     */
    public boolean get(int feature) {
        if (!BOOLEAN_FEATURES.contains(feature)) {
            throw new IllegalArgumentException(feature + " is not a boolean");
        }

        try {
            if (isSupportedHIDL(feature)) {
                IBase obj = mHIDLMap.get(feature);
                switch (feature) {
                    case FEATURE_ADAPTIVE_BACKLIGHT:
                        IAdaptiveBacklight adaptiveBacklight = (IAdaptiveBacklight) obj;
                        return adaptiveBacklight.isEnabled();
                    case FEATURE_ANTI_FLICKER:
                        IAntiFlicker antiFlicker = (IAntiFlicker) obj;
                        return antiFlicker.isEnabled();
                    case FEATURE_AUTO_CONTRAST:
                        IAutoContrast autoContrast = (IAutoContrast) obj;
                        return autoContrast.isEnabled();
                    case FEATURE_COLOR_ENHANCEMENT:
                        IColorEnhancement colorEnhancement = (IColorEnhancement) obj;
                        return colorEnhancement.isEnabled();
                    case FEATURE_HIGH_TOUCH_POLLING_RATE:
                        IHighTouchPollingRate highTouchPollingRate = (IHighTouchPollingRate) obj;
                        return highTouchPollingRate.isEnabled();
                    case FEATURE_HIGH_TOUCH_SENSITIVITY:
                        IGloveMode gloveMode = (IGloveMode) obj;
                        return gloveMode.isEnabled();
                    case FEATURE_KEY_DISABLE:
                        IKeyDisabler keyDisabler = (IKeyDisabler) obj;
                        return keyDisabler.isEnabled();
                    case FEATURE_KEY_SWAP:
                        IKeySwapper keySwapper = (IKeySwapper) obj;
                        return keySwapper.isEnabled();
                    case FEATURE_SUNLIGHT_ENHANCEMENT:
                        ISunlightEnhancement sunlightEnhancement = (ISunlightEnhancement) obj;
                        return sunlightEnhancement.isEnabled();
                    case FEATURE_TOUCH_HOVERING:
                        IStylusMode stylusMode = (IStylusMode) obj;
                        return stylusMode.isEnabled();
                    case FEATURE_READING_ENHANCEMENT:
                        IReadingEnhancement readingEnhancement = (IReadingEnhancement) obj;
                        return readingEnhancement.isEnabled();
                }
            } else if (checkService()) {
                return sService.get(feature);
            }
        } catch (RemoteException e) {
        }
        return false;
    }

    /**
     * Enable or disable the given feature
     *
     * Only used for features which have simple enable/disable controls.
     *
     * @param feature the Ouvrir Hardware feature to set
     * @param enable true to enable, false to disale
     *
     * @return true if the feature is enabled, false otherwise.
     */
    public boolean set(int feature, boolean enable) {
        if (!BOOLEAN_FEATURES.contains(feature)) {
            throw new IllegalArgumentException(feature + " is not a boolean");
        }

        try {
            if (isSupportedHIDL(feature)) {
                IBase obj = mHIDLMap.get(feature);
                switch (feature) {
                    case FEATURE_ADAPTIVE_BACKLIGHT:
                        IAdaptiveBacklight adaptiveBacklight = (IAdaptiveBacklight) obj;
                        return adaptiveBacklight.setEnabled(enable);
                    case FEATURE_ANTI_FLICKER:
                        IAntiFlicker antiFlicker = (IAntiFlicker) obj;
                        return antiFlicker.setEnabled(enable);
                    case FEATURE_AUTO_CONTRAST:
                        IAutoContrast autoContrast = (IAutoContrast) obj;
                        return autoContrast.setEnabled(enable);
                    case FEATURE_COLOR_ENHANCEMENT:
                        IColorEnhancement colorEnhancement = (IColorEnhancement) obj;
                        return colorEnhancement.setEnabled(enable);
                    case FEATURE_HIGH_TOUCH_POLLING_RATE:
                        IHighTouchPollingRate highTouchPollingRate = (IHighTouchPollingRate) obj;
                        return highTouchPollingRate.setEnabled(enable);
                    case FEATURE_HIGH_TOUCH_SENSITIVITY:
                        IGloveMode gloveMode = (IGloveMode) obj;
                        return gloveMode.setEnabled(enable);
                    case FEATURE_KEY_DISABLE:
                        IKeyDisabler keyDisabler = (IKeyDisabler) obj;
                        return keyDisabler.setEnabled(enable);
                    case FEATURE_KEY_SWAP:
                        IKeySwapper keySwapper = (IKeySwapper) obj;
                        return keySwapper.setEnabled(enable);
                    case FEATURE_SUNLIGHT_ENHANCEMENT:
                        ISunlightEnhancement sunlightEnhancement = (ISunlightEnhancement) obj;
                        return sunlightEnhancement.setEnabled(enable);
                    case FEATURE_TOUCH_HOVERING:
                        IStylusMode stylusMode = (IStylusMode) obj;
                        return stylusMode.setEnabled(enable);
                    case FEATURE_READING_ENHANCEMENT:
                        IReadingEnhancement readingEnhancement = (IReadingEnhancement) obj;
                        return readingEnhancement.setEnabled(enable);
                }
            } else if (checkService()) {
                return sService.set(feature, enable);
            }
        } catch (RemoteException e) {
        }
        return false;
    }

    private int getArrayValue(int[] arr, int idx, int defaultValue) {
        if (arr == null || arr.length <= idx) {
            return defaultValue;
        }

        return arr[idx];
    }

    /**
     * {@hide}
     */
    public static final int COLOR_CALIBRATION_RED_INDEX = 0;
    /**
     * {@hide}
     */
    public static final int COLOR_CALIBRATION_GREEN_INDEX = 1;
    /**
     * {@hide}
     */
    public static final int COLOR_CALIBRATION_BLUE_INDEX = 2;
    /**
     * {@hide}
     */
    public static final int COLOR_CALIBRATION_MIN_INDEX = 3;
    /**
     * {@hide}
     */
    public static final int COLOR_CALIBRATION_MAX_INDEX = 4;

    private int[] getDisplayColorCalibrationArray() {
        try {
            if (isSupportedHIDL(FEATURE_DISPLAY_COLOR_CALIBRATION)) {
                IDisplayColorCalibration displayColorCalibration = (IDisplayColorCalibration)
                        mHIDLMap.get(FEATURE_DISPLAY_COLOR_CALIBRATION);
                return ArrayUtils.convertToIntArray(displayColorCalibration.getCalibration());
            } else if (checkService()) {
                return sService.getDisplayColorCalibration();
            }
        } catch (RemoteException e) {
        }
        return null;
    }

    /**
     * @return the current RGB calibration, where int[0] = R, int[1] = G, int[2] = B.
     */
    public int[] getDisplayColorCalibration() {
        int[] arr = getDisplayColorCalibrationArray();
        if (arr == null || arr.length < 3) {
            return null;
        }
        return Arrays.copyOf(arr, 3);
    }

    /**
     * @return The minimum value for all colors
     */
    public int getDisplayColorCalibrationMin() {
        if (isSupportedHIDL(FEATURE_DISPLAY_COLOR_CALIBRATION)) {
            IDisplayColorCalibration displayColorCalibration = (IDisplayColorCalibration)
                    mHIDLMap.get(FEATURE_DISPLAY_COLOR_CALIBRATION);
            try {
                return displayColorCalibration.getMinValue();
            } catch (RemoteException e) {
                return 0;
            }
        }

        return getArrayValue(getDisplayColorCalibrationArray(), COLOR_CALIBRATION_MIN_INDEX, 0);
    }

    /**
     * @return The maximum value for all colors
     */
    public int getDisplayColorCalibrationMax() {
        if (isSupportedHIDL(FEATURE_DISPLAY_COLOR_CALIBRATION)) {
            IDisplayColorCalibration displayColorCalibration = (IDisplayColorCalibration)
                    mHIDLMap.get(FEATURE_DISPLAY_COLOR_CALIBRATION);
            try {
                return displayColorCalibration.getMaxValue();
            } catch (RemoteException e) {
                return 0;
            }
        }

        return getArrayValue(getDisplayColorCalibrationArray(), COLOR_CALIBRATION_MAX_INDEX, 0);
    }

    /**
     * Set the display color calibration to the given rgb triplet
     *
     * @param rgb RGB color calibration.  Each value must be between
     * {@link #getDisplayColorCalibrationMin()} and {@link #getDisplayColorCalibrationMax()},
     * inclusive.
     *
     * @return true on success, false otherwise.
     */
    public boolean setDisplayColorCalibration(int[] rgb) {
        try {
            if (isSupportedHIDL(FEATURE_DISPLAY_COLOR_CALIBRATION)) {
                IDisplayColorCalibration displayColorCalibration = (IDisplayColorCalibration)
                        mHIDLMap.get(FEATURE_DISPLAY_COLOR_CALIBRATION);
                return displayColorCalibration.setCalibration(
                       new ArrayList<Integer>(Arrays.asList(rgb[0], rgb[1], rgb[2])));
            } else if (checkService()) {
                return sService.setDisplayColorCalibration(rgb);
            }
        } catch (RemoteException e) {
        }
        return false;
    }

    /**
     * @return a list of available display modes on the devices
     */
    public DisplayMode[] getDisplayModes() {
        DisplayMode[] modes = null;
        try {
            if (isSupportedHIDL(FEATURE_DISPLAY_MODES)) {
                IDisplayModes displayModes = (IDisplayModes) mHIDLMap.get(FEATURE_DISPLAY_MODES);
                modes = HIDLHelper.fromHIDLModes(displayModes.getDisplayModes());
            }
        } catch (RemoteException e) {
        } finally {
            if (modes == null) {
                return null;
            }
            final ArrayList<DisplayMode> remapped = new ArrayList<DisplayMode>();
            for (DisplayMode mode : modes) {
                DisplayMode r = remapDisplayMode(mode);
                if (r != null) {
                    remapped.add(r);
                }
            }
            return remapped.toArray(new DisplayMode[0]);
        }
    }

    /**
     * @return the currently active display mode
     */
    public DisplayMode getCurrentDisplayMode() {
        DisplayMode mode = null;
        try {
            if (isSupportedHIDL(FEATURE_DISPLAY_MODES)) {
                IDisplayModes displayModes = (IDisplayModes) mHIDLMap.get(FEATURE_DISPLAY_MODES);
                mode = HIDLHelper.fromHIDLMode(displayModes.getCurrentDisplayMode());
            }
        } catch (RemoteException e) {
        } finally {
            return mode != null ? remapDisplayMode(mode) : null;
        }
    }

    /**
     * @return the default display mode to be set on boot
     */
    public DisplayMode getDefaultDisplayMode() {
        DisplayMode mode = null;
        try {
            if (isSupportedHIDL(FEATURE_DISPLAY_MODES)) {
                IDisplayModes displayModes = (IDisplayModes) mHIDLMap.get(FEATURE_DISPLAY_MODES);
                mode = HIDLHelper.fromHIDLMode(displayModes.getDefaultDisplayMode());
            }
        } catch (RemoteException e) {
        } finally {
            return mode != null ? remapDisplayMode(mode) : null;
        }
    }

    /**
     * @return true if setting the mode was successful
     */
    public boolean setDisplayMode(DisplayMode mode, boolean makeDefault) {
        try {
            if (isSupportedHIDL(FEATURE_DISPLAY_MODES)) {
                IDisplayModes displayModes = (IDisplayModes) mHIDLMap.get(FEATURE_DISPLAY_MODES);
                return displayModes.setDisplayMode(mode.id, makeDefault);
            }
        } catch (RemoteException e) {
        }
        return false;
    }

    private DisplayMode remapDisplayMode(DisplayMode in) {
        if (in == null) {
            return null;
        }
        if (mDisplayModeMappings.containsKey(in.name)) {
            return new DisplayMode(in.id, mDisplayModeMappings.get(in.name));
        }
        if (!mFilterDisplayModes) {
            return in;
        }
        return null;
    }

    /**
     * @return the available range for color temperature adjustments
     */
    public Range<Integer> getColorBalanceRange() {
        try {
            if (isSupportedHIDL(FEATURE_COLOR_BALANCE)) {
                IColorBalance colorBalance = (IColorBalance) mHIDLMap.get(FEATURE_COLOR_BALANCE);
                return HIDLHelper.fromHIDLRange(colorBalance.getColorBalanceRange());
            }
        } catch (RemoteException e) {
        }
        return new Range<Integer>(0, 0);
    }

    /**
     * @return the current color balance value
     */
    public int getColorBalance() {
        try {
            if (isSupportedHIDL(FEATURE_COLOR_BALANCE)) {
                IColorBalance colorBalance = (IColorBalance) mHIDLMap.get(FEATURE_COLOR_BALANCE);
                return colorBalance.getColorBalance();
            }
        } catch (RemoteException e) {
        }
        return 0;
    }

    /**
     * Sets the desired color balance. Must fall within the range obtained from
     * getColorBalanceRange()
     *
     * @param value
     * @return true if success
     */
    public boolean setColorBalance(int value) {
        try {
            if (isSupportedHIDL(FEATURE_COLOR_BALANCE)) {
                IColorBalance colorBalance = (IColorBalance) mHIDLMap.get(FEATURE_COLOR_BALANCE);
                return colorBalance.setColorBalance(value);
            }
        } catch (RemoteException e) {
        }
        return false;
    }

    /**
     * Gets the current picture adjustment values
     *
     * @return HSIC object with current settings
     */
    public HSIC getPictureAdjustment() {
        try {
            if (isSupportedHIDL(FEATURE_PICTURE_ADJUSTMENT)) {
                IPictureAdjustment pictureAdjustment = (IPictureAdjustment)
                        mHIDLMap.get(FEATURE_PICTURE_ADJUSTMENT);
                return HIDLHelper.fromHIDLHSIC(pictureAdjustment.getPictureAdjustment());
            }
        } catch (RemoteException e) {
        }
        return null;
    }

    /**
     * Gets the default picture adjustment for the current mode
     *
     * @return HSIC object with default settings
     */
    public HSIC getDefaultPictureAdjustment() {
        try {
            if (isSupportedHIDL(FEATURE_PICTURE_ADJUSTMENT)) {
                IPictureAdjustment pictureAdjustment = (IPictureAdjustment)
                        mHIDLMap.get(FEATURE_PICTURE_ADJUSTMENT);
                return HIDLHelper.fromHIDLHSIC(pictureAdjustment.getDefaultPictureAdjustment());
            }
        } catch (RemoteException e) {
        }
        return null;
    }

    /**
     * Sets the desired hue/saturation/intensity/contrast
     *
     * @param hsic
     * @return true if success
     */
    public boolean setPictureAdjustment(final HSIC hsic) {
        try {
            if (isSupportedHIDL(FEATURE_PICTURE_ADJUSTMENT)) {
                IPictureAdjustment pictureAdjustment = (IPictureAdjustment)
                        mHIDLMap.get(FEATURE_PICTURE_ADJUSTMENT);
                return pictureAdjustment.setPictureAdjustment(HIDLHelper.toHIDLHSIC(hsic));
            }
        } catch (RemoteException e) {
        }
        return false;
    }

    /**
     * Get a list of ranges valid for picture adjustment.
     *
     * @return range list
     */
    public List<Range<Float>> getPictureAdjustmentRanges() {
        try {
            if (isSupportedHIDL(FEATURE_PICTURE_ADJUSTMENT)) {
                IPictureAdjustment pictureAdjustment = (IPictureAdjustment)
                        mHIDLMap.get(FEATURE_PICTURE_ADJUSTMENT);
                return Arrays.asList(
                        HIDLHelper.fromHIDLRange(pictureAdjustment.getHueRange()),
                        HIDLHelper.fromHIDLRange(pictureAdjustment.getSaturationRange()),
                        HIDLHelper.fromHIDLRange(pictureAdjustment.getIntensityRange()),
                        HIDLHelper.fromHIDLRange(pictureAdjustment.getContrastRange()),
                        HIDLHelper.fromHIDLRange(pictureAdjustment.getSaturationThresholdRange()));
            }
        } catch (RemoteException e) {
        }
        return null;
    }

    /**
     * @return a list of available touchscreen gestures on the devices
     */
    public TouchscreenGesture[] getTouchscreenGestures() {
        try {
            if (isSupportedHIDL(FEATURE_TOUCHSCREEN_GESTURES)) {
                ITouchscreenGesture touchscreenGesture = (ITouchscreenGesture)
                        mHIDLMap.get(FEATURE_TOUCHSCREEN_GESTURES);
                return HIDLHelper.fromHIDLGestures(touchscreenGesture.getSupportedGestures());
            }
        } catch (RemoteException e) {
        }
        return null;
    }

    /**
     * @return true if setting the activation status was successful
     */
    public boolean setTouchscreenGestureEnabled(
            TouchscreenGesture gesture, boolean state) {
        try {
            if (isSupportedHIDL(FEATURE_TOUCHSCREEN_GESTURES)) {
                ITouchscreenGesture touchscreenGesture = (ITouchscreenGesture)
                        mHIDLMap.get(FEATURE_TOUCHSCREEN_GESTURES);
                return touchscreenGesture.setGestureEnabled(
                        HIDLHelper.toHIDLGesture(gesture), state);
            }
        } catch (RemoteException e) {
        }
        return false;
    }

    /**
     * @return true if service is valid
     */
    private boolean checkService() {
        if (sService == null) {
            Log.w(TAG, "not connected to OuvrirHardwareManagerService");
            return false;
        }
        return true;
    }

}
