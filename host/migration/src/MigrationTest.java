/*
 * Copyright (C) 2015 The CyanogenMod Project
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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

/**
 * A verbose settings migration test
 */
class MigrationTest {
    private static final String ARGUMENT_SETTINGS = "--settings";
    private static final String ARGUMENT_BOOT_IMG = "--bootimg";
    private static final String ARGUMENT_SYSTEM_IMG = "--systemimg";
    private static final String ARGUMENT_PREFIX = "--";

    public static final boolean DEBUG = true;

    private static ArrayList<Setting> ouvrirSystemSettingList = new ArrayList<Setting>();
    private static ArrayList<Setting> ouvrirSecureSettingList = new ArrayList<Setting>();
    private static ArrayList<Setting> ouvrirGlobalSettingList = new ArrayList<Setting>();

    private static ArrayList<Setting> legacySystemSettings = new ArrayList<Setting>();
    private static ArrayList<Setting> legacySecureSettings = new ArrayList<Setting>();
    private static ArrayList<Setting> legacyGlobalSettings = new ArrayList<Setting>();

    private static Tokenizer tokenizer;

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            showUsage();
            System.exit(-1);
        }
        tokenizer = new Tokenizer(args);

        String settingFileName = null;
        String bootImage = null;
        String systemImage = null;
        for (String argument; (argument = tokenizer.nextArg())!= null;) {
            if (ARGUMENT_SETTINGS.equals(argument)) {
                settingFileName = argumentValueRequired(argument);
            } else if (ARGUMENT_BOOT_IMG.equals(argument)) {
                bootImage = argumentValueRequired(argument);
            } else if (ARGUMENT_SYSTEM_IMG.equals(argument)) {
                systemImage = argumentValueRequired(argument);
            }
        }

        if (!new File(settingFileName).exists()) {
            System.err.print("Invalid file provided " + settingFileName);
            System.exit(-1);
        }

        SettingImageCommands legacySettings =
                new SettingImageCommands(SettingsConstants.SETTINGS_AUTHORITY);
        legacySettings.addRead(settingFileName, SettingsConstants.SYSTEM, legacySystemSettings);
        legacySettings.addRead(settingFileName, SettingsConstants.SECURE, legacySecureSettings);
        legacySettings.addRead(settingFileName, SettingsConstants.GLOBAL, legacyGlobalSettings);

        //Read settings
        legacySettings.execute();

        SettingImageCommands legacyToOuvrirSettings =
                new SettingImageCommands(SettingsConstants.SETTINGS_AUTHORITY);
        //For each example setting in the table, add inserts
        for (Setting setting : legacySystemSettings) {
            legacyToOuvrirSettings.addInsert(SettingsConstants.SYSTEM, setting);
        }
        for (Setting setting : legacySecureSettings) {
            legacyToOuvrirSettings.addInsert(SettingsConstants.SECURE, setting);
        }
        for (Setting setting : legacyGlobalSettings) {
            legacyToOuvrirSettings.addInsert(SettingsConstants.GLOBAL, setting);
        }
        //Write them to the database for verification later
        legacyToOuvrirSettings.execute();

        //Force update
        DebuggingCommands updateRom = new DebuggingCommands();
        updateRom.addAdb(AdbCommand.Types.REBOOT_BOOTLOADER);
        updateRom.addFastboot(FastbootCommand.Types.FASTBOOT_DEVICES, null);
        updateRom.addFastboot(FastbootCommand.Types.FASTBOOT_FLASH,
                new String[]{"boot", bootImage});
        updateRom.addFastboot(FastbootCommand.Types.FASTBOOT_FLASH,
                new String[]{"system", systemImage});
        updateRom.addFastboot(FastbootCommand.Types.FASTBOOT_REBOOT, null);
        updateRom.addAdb(AdbCommand.Types.CHECK_BOOT_COMPLETE);
        updateRom.execute();

        //Requery
        SettingImageCommands ouvrirSettingImage =
                new SettingImageCommands(SettingsConstants.OUVRIRSETTINGS_AUTHORITY);
        ouvrirSettingImage.addQuery(SettingsConstants.SYSTEM, ouvrirSystemSettingList);
        ouvrirSettingImage.addQuery(SettingsConstants.SECURE, ouvrirSecureSettingList);
        ouvrirSettingImage.addQuery(SettingsConstants.GLOBAL, ouvrirGlobalSettingList);
        ouvrirSettingImage.execute();

        //Validate
        System.out.println("\n\nValidating " + SettingsConstants.SYSTEM + "...");
        validate(legacySystemSettings, ouvrirSystemSettingList);
        System.out.println("\n\nValidating " + SettingsConstants.SECURE + "...");
        validate(legacySecureSettings, ouvrirSecureSettingList);
        System.out.println("\n\nValidating " + SettingsConstants.GLOBAL + "...");
        validate(legacyGlobalSettings, ouvrirGlobalSettingList);
        System.exit(0);
    }

    private static void showUsage() {
        System.err.println("Usage: MigrationTest --settings [example setting file] "
                + "--bootimg [image]"
                + "--systemimg [image]");
    }

    private static class Tokenizer {
        private final String[] mArgs;
        private int mNextArg;

        public Tokenizer(String[] args) {
            mArgs = args;
        }

        private String nextArg() {
            if (mNextArg < mArgs.length) {
                return mArgs[mNextArg++];
            } else {
                return null;
            }
        }
    }

    private static String argumentValueRequired(String argument) {
        String value = tokenizer.nextArg();
        if (value == null || value.length() == 0 || value.startsWith(ARGUMENT_PREFIX)) {
            throw new IllegalArgumentException("No value for argument: " + argument);
        }
        return value;
    }

    private static void validate(ArrayList<Setting> legacySettings, ArrayList<Setting> ouvrirSettings) {
        Collections.sort(legacySettings);
        Collections.sort(ouvrirSettings);

        if (legacySettings.size() != ouvrirSettings.size()) {
            System.err.println("Warning: Size mismatch: " + " legacy "
                    + legacySettings.size() + " ouvrir " + ouvrirSettings.size());
        }

        for (int i = 0; i < legacySettings.size(); i++) {
            Setting legacySetting = legacySettings.get(i);
            Setting ouvrirSetting = ouvrirSettings.get(i);
            int error = 0;

            System.out.println("Comparing: legacy " + legacySetting.getKey() + " and ouvrirsetting "
                    + ouvrirSetting.getKey());

            if (!legacySetting.getKey().equals(ouvrirSetting.getKey())) {
                System.err.println("    Key mismatch: " + legacySetting.getKey() + " and "
                        + ouvrirSetting.getKey());
                error = 1;
            }
            if (!legacySetting.getKeyType().equals(ouvrirSetting.getKeyType())) {
                System.err.println("    Key type mismatch: " + legacySetting.getKeyType() + " and "
                        + ouvrirSetting.getKeyType());
                error = 1;
            }
            if (legacySetting.getValue().length() > 0) {
                if (!legacySetting.getValue().equals(ouvrirSetting.getValue())) {
                    System.err.println("    Value mismatch: " + legacySetting.getValue() + " and "
                            + ouvrirSetting.getValue());
                    error = 1;
                }
            }
            if (!legacySetting.getValueType().equals(ouvrirSetting.getValueType())) {
                System.err.println("    Value type mismatch: " + legacySetting.getValueType()
                        + " and " + ouvrirSetting.getValueType());
                error = 1;
            }

            if (error > 0) {
                System.exit(-1);
            } else {
                System.out.println("...OK");
            }
        }
    }
}