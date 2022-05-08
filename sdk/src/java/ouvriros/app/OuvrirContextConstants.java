/**
 * Copyright (C) 2015, The CyanogenMod Project
 *               2017-2022 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ouvriros.app;

import android.annotation.SdkConstant;

/**
 * @hide
 * TODO: We need to somehow make these managers accessible via getSystemService
 */
public final class OuvrirContextConstants {

    /**
     * @hide
     */
    private OuvrirContextConstants() {
        // Empty constructor
    }

    /**
     * Use with {@link android.content.Context#getSystemService} to retrieve a
     * {@link ouvriros.app.ProfileManager} for informing the user of
     * background events.
     *
     * @see android.content.Context#getSystemService
     * @see ouvriros.app.ProfileManager
     *
     * @hide
     */
    public static final String OUVRIR_PROFILE_SERVICE = "profile";

    /**
     * Use with {@link android.content.Context#getSystemService} to retrieve a
     * {@link ouvriros.hardware.OuvrirHardwareManager} to manage the extended
     * hardware features of the device.
     *
     * @see android.content.Context#getSystemService
     * @see ouvriros.hardware.OuvrirHardwareManager
     *
     * @hide
     */
    public static final String OUVRIR_HARDWARE_SERVICE = "ouvrirhardware";

    /**
     * Control device power profile and characteristics.
     *
     * @hide
     */
    public static final String OUVRIR_PERFORMANCE_SERVICE = "ouvrirperformance";

    /**
     * Manages display color adjustments
     *
     * @hide
     */
    public static final String OUVRIR_LIVEDISPLAY_SERVICE = "ouvrirlivedisplay";

    /**
     * Use with {@link android.content.Context#getSystemService} to retrieve a
     * {@link ouvriros.trust.TrustInterface} to access the Trust interface.
     *
     * @see android.content.Context#getSystemService
     * @see ouvriros.trust.TrustInterface
     *
     * @hide
     */
    public static final String OUVRIR_TRUST_INTERFACE = "ouvrirtrust";

    /**
     * Update power menu (GlobalActions)
     *
     * @hide
     */
    public static final String OUVRIR_GLOBAL_ACTIONS_SERVICE = "ouvrirglobalactions";

    /**
     * Features supported by the Ouvrir SDK.
     */
    public static class Features {
        /**
         * Feature for {@link PackageManager#getSystemAvailableFeatures} and
         * {@link PackageManager#hasSystemFeature}: The device includes the hardware abstraction
         * framework service utilized by the ouvrir sdk.
         */
        @SdkConstant(SdkConstant.SdkConstantType.FEATURE)
        public static final String HARDWARE_ABSTRACTION = "org.ouvriros.hardware";

        /**
         * Feature for {@link PackageManager#getSystemAvailableFeatures} and
         * {@link PackageManager#hasSystemFeature}: The device includes the ouvrir profiles service
         * utilized by the ouvrir sdk.
         */
        @SdkConstant(SdkConstant.SdkConstantType.FEATURE)
        public static final String PROFILES = "org.ouvriros.profiles";

        /**
         * Feature for {@link PackageManager#getSystemAvailableFeatures} and
         * {@link PackageManager#hasSystemFeature}: The device includes the ouvrir performance service
         * utilized by the ouvrir sdk.
         */
        @SdkConstant(SdkConstant.SdkConstantType.FEATURE)
        public static final String PERFORMANCE = "org.ouvriros.performance";

        /**
         * Feature for {@link PackageManager#getSystemAvailableFeatures} and
         * {@link PackageManager#hasSystemFeature}: The device includes the LiveDisplay service
         * utilized by the ouvrir sdk.
         */
        @SdkConstant(SdkConstant.SdkConstantType.FEATURE)
        public static final String LIVEDISPLAY = "org.ouvriros.livedisplay";

        /**
         * Feature for {@link PackageManager#getSystemAvailableFeatures} and
         * {@link PackageManager#hasSystemFeature}: The device includes the Ouvrir audio extensions
         * utilized by the ouvrir sdk.
         */
        @SdkConstant(SdkConstant.SdkConstantType.FEATURE)
        public static final String AUDIO = "org.ouvriros.audio";

        /**
         * Feature for {@link PackageManager#getSystemAvailableFeatures} and
         * {@link PackageManager#hasSystemFeature}: The device includes the ouvrir trust service
         * utilized by the ouvrir sdk.
         */
        @SdkConstant(SdkConstant.SdkConstantType.FEATURE)
        public static final String TRUST = "org.ouvriros.trust";

        /**
         * Feature for {@link PackageManager#getSystemAvailableFeatures} and
         * {@link PackageManager#hasSystemFeature}: The device includes the ouvrir settings service
         * utilized by the ouvrir sdk.
         */
        @SdkConstant(SdkConstant.SdkConstantType.FEATURE)
        public static final String SETTINGS = "org.ouvriros.settings";

        /**
         * Feature for {@link PackageManager#getSystemAvailableFeatures} and
         * {@link PackageManager#hasSystemFeature}: The device includes the ouvrir globalactions
         * service utilized by the ouvrir sdk and OuvrirParts.
         */
        @SdkConstant(SdkConstant.SdkConstantType.FEATURE)
        public static final String GLOBAL_ACTIONS = "org.ouvriros.globalactions";
    }
}
