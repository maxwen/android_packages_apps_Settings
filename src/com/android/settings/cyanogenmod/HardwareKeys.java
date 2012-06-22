/*
 * Copyright (C) 2012 The CyanogenMod Project
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

package com.android.settings.cyanogenmod;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.widget.Toast;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class HardwareKeys extends SettingsPreferenceFragment implements OnPreferenceChangeListener {

    private static final String HARDWARE_KEYS_CATEGORY_BINDINGS = "hardware_keys_bindings";
    private static final String HARDWARE_KEYS_HOME_LONG_PRESS = "hardware_keys_home_long_press";
    private static final String HARDWARE_KEYS_MENU_PRESS = "hardware_keys_menu_press";
    private static final String HARDWARE_KEYS_MENU_LONG_PRESS = "hardware_keys_menu_long_press";
    private static final String HARDWARE_KEYS_SEARCH_PRESS = "hardware_keys_search_press";
    private static final String HARDWARE_KEYS_SEARCH_LONG_PRESS = "hardware_keys_search_long_press";
    private static final String HARDWARE_KEYS_APP_SWITCH_PRESS = "hardware_keys_app_switch_press";
    private static final String HARDWARE_KEYS_APP_SWITCH_LONG_PRESS = "hardware_keys_app_switch_long_press";
    private static final String HARDWARE_KEYS_SHOW_OVERFLOW = "hardware_keys_show_overflow";

    // Available custom actions to perform on a key press.
    // Must match values for KEY_HOME_LONG_PRESS_ACTION in:
    // frameworks/base/core/java/android/provider/Settings.java
    private static final int ACTION_NOTHING = 0;
    private static final int ACTION_MENU = 1;
    private static final int ACTION_APP_SWITCH = 2;
    private static final int ACTION_SEARCH = 3;
    private static final int ACTION_VOICE_SEARCH = 4;

    // Masks for checking presence of hardware keys.
    // Must match values in frameworks/base/core/res/res/values/config.xml
    private static final int KEY_MASK_HOME = 0x01;
    private static final int KEY_MASK_BACK = 0x02;
    private static final int KEY_MASK_MENU = 0x04;
    private static final int KEY_MASK_SEARCH = 0x08;
    private static final int KEY_MASK_APP_SWITCH = 0x10;

    private ListPreference mHomeLongPressAction;
    private ListPreference mMenuPressAction;
    private ListPreference mMenuLongPressAction;
    private ListPreference mSearchPressAction;
    private ListPreference mSearchLongPressAction;
    private ListPreference mAppSwitchPressAction;
    private ListPreference mAppSwitchLongPressAction;
    private CheckBoxPreference mShowActionOverflow;

    private boolean mDisableToast;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final int deviceKeys = getResources().getInteger(
                com.android.internal.R.integer.config_deviceHardwareKeys);
        final boolean hasHomeKey = (deviceKeys & KEY_MASK_HOME) != 0;
        final boolean hasMenuKey = (deviceKeys & KEY_MASK_MENU) != 0;
        final boolean hasSearchKey = (deviceKeys & KEY_MASK_SEARCH) != 0;
        final boolean hasAppSwitchKey = (deviceKeys & KEY_MASK_APP_SWITCH) != 0;

        addPreferencesFromResource(R.xml.hardware_keys);

        mHomeLongPressAction = (ListPreference) getPreferenceScreen().findPreference(
                HARDWARE_KEYS_HOME_LONG_PRESS);
        mMenuPressAction = (ListPreference) getPreferenceScreen().findPreference(
                HARDWARE_KEYS_MENU_PRESS);
        mMenuLongPressAction = (ListPreference) getPreferenceScreen().findPreference(
                HARDWARE_KEYS_MENU_LONG_PRESS);
        mSearchPressAction = (ListPreference) getPreferenceScreen().findPreference(
                HARDWARE_KEYS_SEARCH_PRESS);
        mSearchLongPressAction = (ListPreference) getPreferenceScreen().findPreference(
                HARDWARE_KEYS_SEARCH_LONG_PRESS);
        mAppSwitchPressAction = (ListPreference) getPreferenceScreen().findPreference(
                HARDWARE_KEYS_APP_SWITCH_PRESS);
        mAppSwitchLongPressAction = (ListPreference) getPreferenceScreen().findPreference(
                HARDWARE_KEYS_APP_SWITCH_LONG_PRESS);
        mShowActionOverflow = (CheckBoxPreference) getPreferenceScreen().findPreference(
                HARDWARE_KEYS_SHOW_OVERFLOW);
        PreferenceCategory bindingsCategory = (PreferenceCategory) findPreference(
                HARDWARE_KEYS_CATEGORY_BINDINGS);

        if (hasHomeKey) {
            int homeLongPressAction = Settings.System.getInt(getContentResolver(),
                    Settings.System.KEY_HOME_LONG_PRESS_ACTION, ACTION_APP_SWITCH);
            mHomeLongPressAction.setValue(Integer.toString(homeLongPressAction));
            mHomeLongPressAction.setSummary(mHomeLongPressAction.getEntry());
            mHomeLongPressAction.setOnPreferenceChangeListener(this);
        } else {
            bindingsCategory.removePreference(mHomeLongPressAction);
        }

        if (hasMenuKey) {
            int menuPressAction = Settings.System.getInt(getContentResolver(),
                    Settings.System.KEY_MENU_ACTION, ACTION_MENU);
            mMenuPressAction.setValue(Integer.toString(menuPressAction));
            mMenuPressAction.setSummary(mMenuPressAction.getEntry());
            mMenuPressAction.setOnPreferenceChangeListener(this);

            int menuLongPressAction;
            if (hasSearchKey) {
                menuLongPressAction = Settings.System.getInt(getContentResolver(),
                        Settings.System.KEY_MENU_LONG_PRESS_ACTION, ACTION_NOTHING);
            } else {
                menuLongPressAction = Settings.System.getInt(getContentResolver(),
                        Settings.System.KEY_MENU_LONG_PRESS_ACTION, ACTION_SEARCH);
            }
            mMenuLongPressAction.setValue(Integer.toString(menuLongPressAction));
            mMenuLongPressAction.setSummary(mMenuLongPressAction.getEntry());
            mMenuLongPressAction.setOnPreferenceChangeListener(this);
        } else {
            bindingsCategory.removePreference(mMenuPressAction);
            bindingsCategory.removePreference(mMenuLongPressAction);
        }

        if (hasSearchKey) {
            int searchPressAction = Settings.System.getInt(getContentResolver(),
                    Settings.System.KEY_SEARCH_ACTION, ACTION_SEARCH);
            mSearchPressAction.setValue(Integer.toString(searchPressAction));
            mSearchPressAction.setSummary(mSearchPressAction.getEntry());
            mSearchPressAction.setOnPreferenceChangeListener(this);

            int searchLongPressAction = Settings.System.getInt(getContentResolver(),
                    Settings.System.KEY_SEARCH_LONG_PRESS_ACTION, ACTION_VOICE_SEARCH);
            mSearchLongPressAction.setValue(Integer.toString(searchLongPressAction));
            mSearchLongPressAction.setSummary(mSearchLongPressAction.getEntry());
            mSearchLongPressAction.setOnPreferenceChangeListener(this);
        } else {
            bindingsCategory.removePreference(mSearchPressAction);
            bindingsCategory.removePreference(mSearchLongPressAction);
        }

        if (hasAppSwitchKey) {
            int appSwitchPressAction = Settings.System.getInt(getContentResolver(),
                    Settings.System.KEY_APP_SWITCH_ACTION, ACTION_APP_SWITCH);
            mAppSwitchPressAction.setValue(Integer.toString(appSwitchPressAction));
            mAppSwitchPressAction.setSummary(mAppSwitchPressAction.getEntry());
            mAppSwitchPressAction.setOnPreferenceChangeListener(this);

            int appSwitchLongPressAction = Settings.System.getInt(getContentResolver(),
                    Settings.System.KEY_APP_SWITCH_LONG_PRESS_ACTION, ACTION_NOTHING);
            mAppSwitchLongPressAction.setValue(Integer.toString(appSwitchLongPressAction));
            mAppSwitchLongPressAction.setSummary(mAppSwitchLongPressAction.getEntry());
            mAppSwitchLongPressAction.setOnPreferenceChangeListener(this);
        } else {
            bindingsCategory.removePreference(mAppSwitchPressAction);
            bindingsCategory.removePreference(mAppSwitchLongPressAction);
        }

        mShowActionOverflow.setChecked((Settings.System.getInt(getActivity().
                getApplicationContext().getContentResolver(),
                Settings.System.UI_FORCE_OVERFLOW_BUTTON, 0) == 1));
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mHomeLongPressAction) {
            int homeLongPressAction = Integer.valueOf((String) newValue);
            mHomeLongPressAction.setSummary(getResources().getStringArray(
                    R.array.hardware_keys_action_entries)[homeLongPressAction]);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.KEY_HOME_LONG_PRESS_ACTION, homeLongPressAction);
            return true;
        } else if (preference == mMenuPressAction) {
            int menuPressAction = Integer.valueOf((String) newValue);
            mMenuPressAction.setSummary(getResources().getStringArray(
                    R.array.hardware_keys_action_entries)[menuPressAction]);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.KEY_MENU_ACTION, menuPressAction);
            return true;
        } else if (preference == mMenuLongPressAction) {
            int menuLongPressAction = Integer.valueOf((String) newValue);
            mMenuLongPressAction.setSummary(getResources().getStringArray(
                    R.array.hardware_keys_action_entries)[menuLongPressAction]);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.KEY_MENU_LONG_PRESS_ACTION, menuLongPressAction);
            return true;
        } else if (preference == mSearchPressAction) {
            int searchPressAction = Integer.valueOf((String) newValue);
            mSearchPressAction.setSummary(getResources().getStringArray(
                    R.array.hardware_keys_action_entries)[searchPressAction]);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.KEY_SEARCH_ACTION, searchPressAction);
            return true;
        } else if (preference == mSearchLongPressAction) {
            int searchLongPressAction = Integer.valueOf((String) newValue);
            mSearchLongPressAction.setSummary(getResources().getStringArray(
                    R.array.hardware_keys_action_entries)[searchLongPressAction]);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.KEY_SEARCH_LONG_PRESS_ACTION, searchLongPressAction);
            return true;
        } else if (preference == mAppSwitchPressAction) {
            int appSwitchPressAction = Integer.valueOf((String) newValue);
            mAppSwitchPressAction.setSummary(getResources().getStringArray(
                    R.array.hardware_keys_action_entries)[appSwitchPressAction]);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.KEY_APP_SWITCH_ACTION, appSwitchPressAction);
            return true;
        } else if (preference == mAppSwitchLongPressAction) {
            int appSwitchLongPressAction = Integer.valueOf((String) newValue);
            mAppSwitchLongPressAction.setSummary(getResources().getStringArray(
                    R.array.hardware_keys_action_entries)[appSwitchLongPressAction]);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.KEY_APP_SWITCH_LONG_PRESS_ACTION, appSwitchLongPressAction);
            return true;
        }
        return false;
    }

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mShowActionOverflow) {
            Settings.System.putInt(getContentResolver(), Settings.System.UI_FORCE_OVERFLOW_BUTTON,
                    mShowActionOverflow.isChecked() ? 1 : 0);
            // Only show toast every other click
            if (!mDisableToast) {
                Toast.makeText(getActivity(), R.string.hardware_keys_show_overflow_toast,
                        Toast.LENGTH_LONG).show();
            }
            mDisableToast = !mDisableToast;
            return true;
        }
        return false;
    }
}
