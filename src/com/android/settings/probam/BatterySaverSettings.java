/*
 * Copyright (C) 2014 The OmniROM Project
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

package com.android.settings.probam;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.telephony.TelephonyManager;
import com.android.internal.telephony.Phone;

import com.android.settings.cyanogenmod.SeekBarPreference;
import com.android.settings.probam.BatterySaverHelper;
import com.android.settings.cyanogenmod.TimeRangePreference;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.R;

public class BatterySaverSettings extends SettingsPreferenceFragment implements
        OnPreferenceChangeListener {

    private static final String PREF_KEY_BATTERY_SAVER_ENABLE = "pref_battery_saver_enable";
    private static final String PREF_KEY_BATTERY_SAVER_NORMAL_GSM_MODE = "pref_battery_saver_normal_gsm_mode";
    private static final String PREF_KEY_BATTERY_SAVER_POWER_SAVING_GSM_MODE = "pref_battery_saver_power_saving_gsm_mode";
    private static final String PREF_KEY_BATTERY_SAVER_NORMAL_CDMA_MODE = "pref_battery_saver_normal_cdma_mode";
    private static final String PREF_KEY_BATTERY_SAVER_POWER_SAVING_CDMA_MODE = "pref_battery_saver_power_saving_cdma_mode";
    private static final String PREF_KEY_BATTERY_SAVER_SCREEN_OFF = "pref_battery_saver_screen_off";
    private static final String PREF_KEY_BATTERY_SAVER_IGNORE_LOCKED = "pref_battery_saver_ignore_locked";
    private static final String PREF_KEY_BATTERY_SAVER_MODE_CHANGE_DELAY = "pref_battery_saver_mode_change_delay";
    private static final String PREF_KEY_BATTERY_SAVER_MODE_BATTERY = "pref_battery_saver_mode_battery";
    private static final String PREF_KEY_BATTERY_SAVER_MODE_BATTERY_LEVEL = "pref_battery_saver_mode_battery_level";
    private static final String PREF_KEY_BATTERY_SAVER_MODE_DATA = "pref_battery_saver_mode_data";
    private static final String PREF_KEY_BATTERY_SAVER_MODE_WIFI = "pref_battery_saver_mode_wifi";
    private static final String PREF_KEY_BATTERY_SAVER_TIMERANGE = "pref_battery_saver_timerange";

    private ContentResolver mResolver;
    private Context mContext;
    private boolean mShow4GForLTE;
    private boolean mIsShowCdma;
    private boolean mIs2gSupport;
    private boolean mIsEnabledLte;

    private ListPreference mNormalGsmPreferredNetworkMode;
    private ListPreference mPowerSavingGsmPreferredNetworkMode;
    private ListPreference mNormalCdmaPreferredNetworkMode;
    private ListPreference mPowerSavingCdmaPreferredNetworkMode;
    private SwitchPreference mBatterySaverEnabled;
    private SeekBarPreference mBatterySaverDelay;
    private CheckBoxPreference mBatterySaverScreenOff;
    private CheckBoxPreference mBatterySaverIgnoreLocked;
    private CheckBoxPreference mSmartBatteryEnabled;
    private SeekBarPreference mLowBatteryLevel;
    private CheckBoxPreference mSmartDataEnabled;
    private CheckBoxPreference mSmartWifiEnabled;
    private TimeRangePreference mBatterySaverTimeRange;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.battery_saver_settings);

        mContext = getActivity().getApplicationContext();
        mResolver = mContext.getContentResolver();

        mShow4GForLTE = getItemFromApplications("com.android.systemui", "config_show4GForLTE", "bool", false);
        mIsShowCdma = getItemFromApplications("com.android.phone", "config_show_cdma", "bool", false);
        mIs2gSupport = getItemFromApplications("com.android.phone", "config_prefer_2g", "bool", true);
        mIsEnabledLte = getItemFromApplications("com.android.phone", "config_enabled_lte", "bool", true);

        PreferenceScreen prefSet = getPreferenceScreen();


        mBatterySaverEnabled = (SwitchPreference) prefSet.findPreference(PREF_KEY_BATTERY_SAVER_ENABLE);
        mBatterySaverEnabled.setChecked(Settings.Global.getInt(mResolver,
                Settings.Global.BATTERY_SAVER_OPTION, 0) != 0);
        mBatterySaverEnabled.setOnPreferenceChangeListener(this);

        mBatterySaverTimeRange = (TimeRangePreference) prefSet.findPreference(PREF_KEY_BATTERY_SAVER_TIMERANGE);
        mBatterySaverTimeRange.setTimeRange(
                    Settings.Global.getInt(mResolver, Settings.Global.BATTERY_SAVER_START, 0),
                    Settings.Global.getInt(mResolver, Settings.Global.BATTERY_SAVER_END, 0));
        mBatterySaverTimeRange.setOnPreferenceChangeListener(this);

        mSmartDataEnabled = (CheckBoxPreference) prefSet.findPreference(PREF_KEY_BATTERY_SAVER_MODE_DATA);
        mNormalGsmPreferredNetworkMode = (ListPreference) prefSet.findPreference(PREF_KEY_BATTERY_SAVER_NORMAL_GSM_MODE);
        mPowerSavingGsmPreferredNetworkMode = (ListPreference) prefSet.findPreference(PREF_KEY_BATTERY_SAVER_POWER_SAVING_GSM_MODE);
        mNormalCdmaPreferredNetworkMode = (ListPreference) prefSet.findPreference(PREF_KEY_BATTERY_SAVER_NORMAL_CDMA_MODE);
        mPowerSavingCdmaPreferredNetworkMode = (ListPreference) prefSet.findPreference(PREF_KEY_BATTERY_SAVER_POWER_SAVING_CDMA_MODE);

        if (BatterySaverHelper.deviceSupportsMobileData(mContext)) {
            TelephonyManager telephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
            int phoneType = telephonyManager.getPhoneType();
            int defaultNetwork = Settings.Global.getInt(mResolver,
                    Settings.Global.PREFERRED_NETWORK_MODE, Phone.PREFERRED_NT_MODE);

            mSmartDataEnabled.setChecked(Settings.Global.getInt(mResolver,
                     Settings.Global.BATTERY_SAVER_DATA_MODE, 1) == 1);
            mSmartDataEnabled.setOnPreferenceChangeListener(this);

            if (phoneType == TelephonyManager.PHONE_TYPE_CDMA) {
                prefSet.removePreference(mNormalGsmPreferredNetworkMode);
                prefSet.removePreference(mPowerSavingGsmPreferredNetworkMode);
                if (BatterySaverHelper.deviceSupportsLteCdma(mContext)) {
                    mNormalCdmaPreferredNetworkMode.setEntries(
                            R.array.enabled_networks_cdma_lte_choices);
                    mNormalCdmaPreferredNetworkMode.setEntryValues(
                            R.array.enabled_networks_cdma_lte_values);
                    mPowerSavingCdmaPreferredNetworkMode.setEntries(
                            R.array.enabled_networks_cdma_lte_choices);
                    mPowerSavingCdmaPreferredNetworkMode.setEntryValues(
                            R.array.enabled_networks_cdma_lte_values);
                }
                int normalNetwork = Settings.Global.getInt(mResolver,
                         Settings.Global.BATTERY_SAVER_NORMAL_MODE, defaultNetwork);
                mNormalCdmaPreferredNetworkMode.setValue(String.valueOf(normalNetwork));
                mNormalCdmaPreferredNetworkMode.setSummary(mNormalCdmaPreferredNetworkMode.getEntry());
                mNormalCdmaPreferredNetworkMode.setOnPreferenceChangeListener(this);
                int savingNetwork = Settings.Global.getInt(mResolver,
                         Settings.Global.BATTERY_SAVER_POWER_SAVING_MODE, defaultNetwork);
                mPowerSavingCdmaPreferredNetworkMode.setValue(String.valueOf(savingNetwork));
                mPowerSavingCdmaPreferredNetworkMode.setSummary(mPowerSavingCdmaPreferredNetworkMode.getEntry());
                mPowerSavingCdmaPreferredNetworkMode.setOnPreferenceChangeListener(this);
            } else if (phoneType == TelephonyManager.PHONE_TYPE_GSM) {
                if (!mIs2gSupport && !mIsEnabledLte) {
                    mNormalGsmPreferredNetworkMode.setEntries(
                            R.array.enabled_networks_except_gsm_lte_choices);
                    mNormalGsmPreferredNetworkMode.setEntryValues(
                            R.array.enabled_networks_except_gsm_lte_values);
                    mPowerSavingGsmPreferredNetworkMode.setEntries(
                            R.array.enabled_networks_except_gsm_lte_choices);
                    mPowerSavingGsmPreferredNetworkMode.setEntryValues(
                            R.array.enabled_networks_except_gsm_lte_values);
                } else if (!mIs2gSupport) {
                    int select = (mShow4GForLTE == true) ?
                        R.array.enabled_networks_except_gsm_4g_choices
                        : R.array.enabled_networks_except_gsm_choices;
                    mNormalGsmPreferredNetworkMode.setEntries(select);
                    mNormalGsmPreferredNetworkMode.setEntryValues(
                            R.array.enabled_networks_except_gsm_values);
                    mPowerSavingGsmPreferredNetworkMode.setEntries(select);
                    mPowerSavingGsmPreferredNetworkMode.setEntryValues(
                            R.array.enabled_networks_except_gsm_values);
                } else if (mIsShowCdma && BatterySaverHelper.deviceSupportsLteCdma(mContext)) {
                    mNormalGsmPreferredNetworkMode.setEntries(
                            R.array.enabled_networks_cdma_lte_choices);
                    mNormalGsmPreferredNetworkMode.setEntryValues(
                            R.array.enabled_networks_cdma_lte_values);
                    mPowerSavingGsmPreferredNetworkMode.setEntries(
                            R.array.enabled_networks_cdma_lte_choices);
                    mPowerSavingGsmPreferredNetworkMode.setEntryValues(
                            R.array.enabled_networks_cdma_lte_values);
                } else if (!mIsShowCdma && BatterySaverHelper.deviceSupportsLteGsm(mContext)) {
                    int select = (mShow4GForLTE == true) ?
                        R.array.enabled_networks_4g_choices
                        : R.array.enabled_networks_lte_choices;
                    mNormalGsmPreferredNetworkMode.setEntries(select);
                    mNormalGsmPreferredNetworkMode.setEntryValues(
                            R.array.enabled_networks_4g_lte_values);
                    mPowerSavingGsmPreferredNetworkMode.setEntries(select);
                    mPowerSavingGsmPreferredNetworkMode.setEntryValues(
                            R.array.enabled_networks_4g_lte_values);
                }
                int normalNetwork = Settings.Global.getInt(mResolver,
                         Settings.Global.BATTERY_SAVER_NORMAL_MODE, defaultNetwork);
                mNormalGsmPreferredNetworkMode.setValue(String.valueOf(normalNetwork));
                mNormalGsmPreferredNetworkMode.setSummary(mNormalGsmPreferredNetworkMode.getEntry());
                mNormalGsmPreferredNetworkMode.setOnPreferenceChangeListener(this);
                int savingNetwork = Settings.Global.getInt(mResolver,
                         Settings.Global.BATTERY_SAVER_POWER_SAVING_MODE, defaultNetwork);
                mPowerSavingGsmPreferredNetworkMode.setValue(String.valueOf(savingNetwork));
                mPowerSavingGsmPreferredNetworkMode.setSummary(mPowerSavingGsmPreferredNetworkMode.getEntry());
                mPowerSavingGsmPreferredNetworkMode.setOnPreferenceChangeListener(this);
                prefSet.removePreference(mNormalCdmaPreferredNetworkMode);
                prefSet.removePreference(mPowerSavingCdmaPreferredNetworkMode);
            }
        } else {
            mBatterySaverEnabled.setSummary(R.string.pref_battery_saver_enable_no_mobiledata_summary);
            prefSet.removePreference(mSmartDataEnabled);
            prefSet.removePreference(mNormalGsmPreferredNetworkMode);
            prefSet.removePreference(mPowerSavingGsmPreferredNetworkMode);
            prefSet.removePreference(mNormalCdmaPreferredNetworkMode);
            prefSet.removePreference(mPowerSavingCdmaPreferredNetworkMode);
        }

        mBatterySaverDelay = (SeekBarPreference) prefSet.findPreference(PREF_KEY_BATTERY_SAVER_MODE_CHANGE_DELAY);
        mBatterySaverDelay.setValue(Settings.Global.getInt(mResolver,
                     Settings.Global.BATTERY_SAVER_MODE_CHANGE_DELAY, 5));
        mBatterySaverDelay.setOnPreferenceChangeListener(this);

        mBatterySaverScreenOff = (CheckBoxPreference) prefSet.findPreference(PREF_KEY_BATTERY_SAVER_SCREEN_OFF);
        mBatterySaverScreenOff.setChecked(Settings.Global.getInt(mResolver,
                     Settings.Global.BATTERY_SAVER_SCREEN_OFF, 1) == 1);
        mBatterySaverScreenOff.setOnPreferenceChangeListener(this);

        mBatterySaverIgnoreLocked = (CheckBoxPreference) prefSet.findPreference(PREF_KEY_BATTERY_SAVER_IGNORE_LOCKED);
        mBatterySaverIgnoreLocked.setChecked(Settings.Global.getInt(mResolver,
                     Settings.Global.BATTERY_SAVER_IGNORE_LOCKED, 1) == 1);
        mBatterySaverIgnoreLocked.setOnPreferenceChangeListener(this);
        mSmartWifiEnabled = (CheckBoxPreference) prefSet.findPreference(PREF_KEY_BATTERY_SAVER_MODE_WIFI);
        mSmartWifiEnabled.setChecked(Settings.Global.getInt(mResolver,
                     Settings.Global.BATTERY_SAVER_WIFI_MODE, 0) == 1);
        mSmartWifiEnabled.setOnPreferenceChangeListener(this);

        mSmartBatteryEnabled = (CheckBoxPreference) prefSet.findPreference(PREF_KEY_BATTERY_SAVER_MODE_BATTERY);
        mSmartBatteryEnabled.setChecked(Settings.Global.getInt(mResolver,
                     Settings.Global.BATTERY_SAVER_BATTERY_MODE, 0) == 1);
        mSmartBatteryEnabled.setOnPreferenceChangeListener(this);

        mLowBatteryLevel = (SeekBarPreference) prefSet.findPreference(PREF_KEY_BATTERY_SAVER_MODE_BATTERY_LEVEL);
        int lowBatteryLevels = mContext.getResources().getInteger(
                        com.android.internal.R.integer.config_lowBatteryWarningLevel);
        mLowBatteryLevel.setValue(Settings.Global.getInt(mResolver,
                     Settings.Global.BATTERY_SAVER_BATTERY_LEVEL, lowBatteryLevels));
        mLowBatteryLevel.setOnPreferenceChangeListener(this);
    }

    private boolean getItemFromApplications(String packagename, String name, String type, boolean val) {
        PackageManager pm = mContext.getPackageManager();
        Resources appResources = null;
        if (pm != null) {
            try {
                appResources = pm.getResourcesForApplication(packagename);
            } catch (Exception e) {
                appResources = null;
            }
        }
        if (appResources != null) {
            int resId = (int) appResources.getIdentifier(name, type, packagename);
            if (resId > 0) {
                try {
                    return appResources.getBoolean(resId);
                } catch (NotFoundException e) {
                }
            }
        }
        return val;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
       if (preference == mBatterySaverEnabled) {
            boolean value = (Boolean) newValue;
            BatterySaverHelper.setBatterySaverActive(mContext, value ? 1 : 0);
            BatterySaverHelper.scheduleService(mContext);
        } else if (preference == mBatterySaverTimeRange) {
            Settings.Global.putInt(mResolver, Settings.Global.BATTERY_SAVER_START,
                    mBatterySaverTimeRange.getStartTime());
            Settings.Global.putInt(mResolver, Settings.Global.BATTERY_SAVER_END,
                    mBatterySaverTimeRange.getEndTime());
            BatterySaverHelper.scheduleService(mContext);
        } else if (preference == mBatterySaverScreenOff) {
            boolean value = (Boolean) newValue;
            Settings.Global.putInt(mResolver,
                     Settings.Global.BATTERY_SAVER_SCREEN_OFF, value ? 1 : 0);
        } else if (preference == mBatterySaverIgnoreLocked) {
            boolean value = (Boolean) newValue;
            Settings.Global.putInt(mResolver,
                     Settings.Global.BATTERY_SAVER_IGNORE_LOCKED, value ? 1 : 0);
        } else if (preference == mSmartDataEnabled) {
            boolean value = (Boolean) newValue;
            Settings.Global.putInt(mResolver,
                     Settings.Global.BATTERY_SAVER_DATA_MODE, value ? 1 : 0);
        } else if (preference == mBatterySaverDelay) {
            int val = ((Integer)newValue).intValue();
            Settings.Global.putInt(mResolver,
                     Settings.Global.BATTERY_SAVER_MODE_CHANGE_DELAY, val);
        } else if (preference == mNormalGsmPreferredNetworkMode) {
            int val = Integer.parseInt((String) newValue);
            int index = mNormalGsmPreferredNetworkMode.findIndexOfValue((String) newValue);
            Settings.Global.putInt(mResolver,
                Settings.Global.BATTERY_SAVER_NORMAL_MODE, val);
            mNormalGsmPreferredNetworkMode.setSummary(mNormalGsmPreferredNetworkMode.getEntries()[index]);
        } else if (preference == mPowerSavingGsmPreferredNetworkMode) {
            int val = Integer.parseInt((String) newValue);
            int index = mPowerSavingGsmPreferredNetworkMode.findIndexOfValue((String) newValue);
            Settings.Global.putInt(mResolver,
                Settings.Global.BATTERY_SAVER_POWER_SAVING_MODE, val);
            mPowerSavingGsmPreferredNetworkMode.setSummary(mPowerSavingGsmPreferredNetworkMode.getEntries()[index]);
        } else if (preference == mNormalCdmaPreferredNetworkMode) {
            int val = Integer.parseInt((String) newValue);
            int index = mNormalCdmaPreferredNetworkMode.findIndexOfValue((String) newValue);
            Settings.Global.putInt(mResolver,
                Settings.Global.BATTERY_SAVER_NORMAL_MODE, val);
            mNormalCdmaPreferredNetworkMode.setSummary(mNormalCdmaPreferredNetworkMode.getEntries()[index]);
        } else if (preference == mPowerSavingCdmaPreferredNetworkMode) {
            int val = Integer.parseInt((String) newValue);
            int index = mPowerSavingCdmaPreferredNetworkMode.findIndexOfValue((String) newValue);
            Settings.Global.putInt(mResolver,
                Settings.Global.BATTERY_SAVER_POWER_SAVING_MODE, val);
            mPowerSavingCdmaPreferredNetworkMode.setSummary(mPowerSavingCdmaPreferredNetworkMode.getEntries()[index]);
        } else if (preference == mSmartBatteryEnabled) {
            boolean value = (Boolean) newValue;
            Settings.Global.putInt(mResolver,
                     Settings.Global.BATTERY_SAVER_BATTERY_MODE, value ? 1 : 0);
        } else if (preference == mLowBatteryLevel) {
            int val = ((Integer)newValue).intValue();
            Settings.Global.putInt(mResolver,
                     Settings.Global.BATTERY_SAVER_BATTERY_LEVEL, val);
        } else if (preference == mSmartWifiEnabled) {
            boolean value = (Boolean) newValue;
            Settings.Global.putInt(mResolver,
                     Settings.Global.BATTERY_SAVER_WIFI_MODE, value ? 1 : 0);
        } else {
            return false;
        }

        return true;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        // If we didn't handle it, let preferences handle it.
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }
}
