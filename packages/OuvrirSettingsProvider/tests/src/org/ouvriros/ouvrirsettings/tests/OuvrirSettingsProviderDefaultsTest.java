/**
 * Copyright (C) 2016 The CyanogenMod Project
 *               2021 The LineageOS Project
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

package org.ouvriros.ouvrirsettings.tests;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.UserHandle;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;
import android.text.TextUtils;
import android.util.TypedValue;

import ouvriros.providers.OuvrirSettings;
import org.ouvriros.ouvrirsettings.OuvrirDatabaseHelper;
import org.ouvriros.ouvrirsettings.OuvrirSettingsProvider;

import java.util.ArrayList;

/**
 * Created by adnan on 1/25/16.
 */
public class OuvrirSettingsProviderDefaultsTest extends AndroidTestCase {
    private ContentResolver mContentResolver;
    private boolean mHasMigratedSettings;
    private Resources mRemoteResources;

    // These data structures are set up in a way that is easier for manual input of new defaults
    private static ArrayList<Setting> SYSTEM_SETTINGS_DEFAULTS = new ArrayList<Setting>();
    private static ArrayList<Setting> SECURE_SETTINGS_DEFAULTS = new ArrayList<Setting>();
    private static ArrayList<Setting> GLOBAL_SETTINGS_DEFAULTS = new ArrayList<Setting>();

    //SYSTEM
    static {
        SYSTEM_SETTINGS_DEFAULTS.add(new Setting(
                OuvrirSettings.System.STATUS_BAR_QUICK_QS_PULLDOWN,
                "R.integer.def_qs_quick_pulldown"));
        SYSTEM_SETTINGS_DEFAULTS.add(new Setting(
                OuvrirSettings.System.NOTIFICATION_LIGHT_BRIGHTNESS_LEVEL,
                "R.integer.def_notification_brightness_level"));
        SYSTEM_SETTINGS_DEFAULTS.add(new Setting(
                OuvrirSettings.System.SYSTEM_PROFILES_ENABLED,
                "R.bool.def_profiles_enabled"));
        SYSTEM_SETTINGS_DEFAULTS.add(new Setting(
                OuvrirSettings.System.NOTIFICATION_LIGHT_PULSE_CUSTOM_ENABLE,
                "R.bool.def_notification_pulse_custom_enable"));
        SYSTEM_SETTINGS_DEFAULTS.add(new Setting(
                OuvrirSettings.System.SWAP_VOLUME_KEYS_ON_ROTATION,
                "R.bool.def_swap_volume_keys_on_rotation"));
        SYSTEM_SETTINGS_DEFAULTS.add(new Setting(
                OuvrirSettings.System.NOTIFICATION_LIGHT_PULSE_CUSTOM_VALUES,
                "R.string.def_notification_pulse_custom_value"));
        SYSTEM_SETTINGS_DEFAULTS.add(new Setting(
                OuvrirSettings.System.STATUS_BAR_BATTERY_STYLE,
                "R.integer.def_battery_style"));
        SYSTEM_SETTINGS_DEFAULTS.add(new Setting(
                OuvrirSettings.System.FORCE_SHOW_NAVBAR,
                "R.integer.def_force_show_navbar"));
    }

    //SECURE
    static {
        SECURE_SETTINGS_DEFAULTS.add(new Setting(
                OuvrirSettings.Secure.STATS_COLLECTION,
                "R.bool.def_stats_collection"));
        SECURE_SETTINGS_DEFAULTS.add(new Setting(
                OuvrirSettings.Secure.LOCKSCREEN_VISUALIZER_ENABLED,
                "R.bool.def_lockscreen_visualizer"));
    }

    //GLOBAL
    /*
    static {
        GLOBAL_SETTINGS_DEFAULTS.add(new Setting(
                OuvrirSettings.Global.WAKE_WHEN_PLUGGED_OR_UNPLUGGED,
                false));
    }
    */

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mContentResolver = getContext().getContentResolver();
        mHasMigratedSettings = getContext().getSharedPreferences(OuvrirSettingsProvider.TAG,
                Context.MODE_PRIVATE).getBoolean(OuvrirSettingsProvider.PREF_HAS_MIGRATED_OUVRIR_SETTINGS,
                false);
        mRemoteResources = getRemoteResources("org.ouvriros.ouvrirsettings");
    }

    @SmallTest
    public void testVerifySystemSettingsDefault() {
        if (verifyNotMigratedSettings()) {
            for (Setting setting : SYSTEM_SETTINGS_DEFAULTS) {
                verifyDefaultSettingForTable(setting, OuvrirDatabaseHelper.OuvrirTableNames.TABLE_SYSTEM);
            }
        }
    }

    @SmallTest
    public void testVerifySecureSettingsDefaults() {
        if (verifyNotMigratedSettings()) {
            for (Setting setting : SECURE_SETTINGS_DEFAULTS) {
                verifyDefaultSettingForTable(setting, OuvrirDatabaseHelper.OuvrirTableNames.TABLE_SECURE);
            }
        }
    }

    @SmallTest
    public void testVerifyGlobalSettingsDefaults() {
        if (verifyNotMigratedSettings()) {
            for (Setting setting : GLOBAL_SETTINGS_DEFAULTS) {
                verifyDefaultSettingForTable(setting, OuvrirDatabaseHelper.OuvrirTableNames.TABLE_GLOBAL);
            }
        }
    }

    private boolean verifyNotMigratedSettings() {
        return !mHasMigratedSettings;
    }

    private void verifyDefaultSettingForTable(Setting setting, String table) {
        TypedValue value = new TypedValue();
        try {
            int identifier = mRemoteResources.getIdentifier(
                    setting.mDefResName, setting.mType, "org.ouvriros.ouvrirsettings");
            mRemoteResources.getValue(identifier, value, true);
        } catch (Resources.NotFoundException e) {
            // Resource not found, can't verify because it probably wasn't loaded in
            throw new AssertionError("Unable to find resource for " + setting.mKey);
        }

        try {
            switch (value.type) {
                case TypedValue.TYPE_INT_DEC:
                    int actualValue = getIntForTable(setting, table);
                    try {
                        assertEquals(value.data, actualValue);
                    } catch (AssertionError e) {
                        throw new AssertionError("Compared value of " + setting.mKey + " expected "
                                + value.data + " got " + actualValue);
                    }
                    break;
                case TypedValue.TYPE_INT_BOOLEAN:
                    int actualBooleanValue = getIntForTable(setting, table);
                    try {
                        //This is gross
                        //Boolean can be "true" as long as it isn't 0
                        if (value.data != 0) {
                            value.data = 1;
                        }
                        assertEquals(value.data, actualBooleanValue);
                    } catch (AssertionError e) {
                        throw new AssertionError("Compared value of " + setting.mKey + " expected "
                                + value.data + " got " + actualBooleanValue);
                    }
                    break;
                case TypedValue.TYPE_STRING:
                    if (!TextUtils.isEmpty(value.string)) {
                        //This should really be done as a parameterized test
                        String actualStringValue = getStringForTable(setting, table);
                        try {
                            assertEquals(value.string, actualStringValue);
                        } catch (AssertionError e) {
                            throw new AssertionError("Compared value of " + setting.mKey
                                    + " expected " + value.string + " got " + actualStringValue);
                        }
                    }
                    break;
                case TypedValue.TYPE_NULL:
                    break;
            }
        } catch (OuvrirSettings.OuvrirSettingNotFoundException e) {
            e.printStackTrace();
            throw new AssertionError("Setting " + setting.mKey + " not found!");
        }
    }

    private int getIntForTable(Setting setting, String table)
            throws OuvrirSettings.OuvrirSettingNotFoundException {
        switch (table) {
            case OuvrirDatabaseHelper.OuvrirTableNames.TABLE_SYSTEM:
                return OuvrirSettings.System.getIntForUser(mContentResolver, setting.mKey,
                        UserHandle.USER_SYSTEM);
            case OuvrirDatabaseHelper.OuvrirTableNames.TABLE_SECURE:
                return OuvrirSettings.Secure.getIntForUser(mContentResolver, setting.mKey,
                        UserHandle.USER_SYSTEM);
            case OuvrirDatabaseHelper.OuvrirTableNames.TABLE_GLOBAL:
                return OuvrirSettings.Global.getIntForUser(mContentResolver, setting.mKey,
                        UserHandle.USER_SYSTEM);
            default:
                throw new AssertionError("Invalid or empty table!");
        }
    }

    private String getStringForTable(Setting setting, String table)
            throws OuvrirSettings.OuvrirSettingNotFoundException {
        switch (table) {
            case OuvrirDatabaseHelper.OuvrirTableNames.TABLE_SYSTEM:
                return OuvrirSettings.System.getStringForUser(mContentResolver, setting.mKey,
                        UserHandle.USER_SYSTEM);
            case OuvrirDatabaseHelper.OuvrirTableNames.TABLE_SECURE:
                return OuvrirSettings.Secure.getStringForUser(mContentResolver, setting.mKey,
                        UserHandle.USER_SYSTEM);
            case OuvrirDatabaseHelper.OuvrirTableNames.TABLE_GLOBAL:
                return OuvrirSettings.Global.getStringForUser(mContentResolver, setting.mKey,
                        UserHandle.USER_SYSTEM);
            default:
                throw new AssertionError("Invalid or empty table!");
        }
    }

    private static class Setting {
        public String mKey;
        public String mDefResName;
        public String mType;

        public Setting(String key, String defResId) {
            mKey = key;
            String[] parts = defResId.split("\\.");
            mType = parts[1];
            mDefResName = parts[2];
        }
    }

    private Resources getRemoteResources(String packageName)
            throws PackageManager.NameNotFoundException {
        PackageManager packageManager = mContext.getPackageManager();
        return packageManager.getResourcesForApplication(packageName);
    }
}
