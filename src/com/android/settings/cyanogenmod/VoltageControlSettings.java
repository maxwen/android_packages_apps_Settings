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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.CheckBox;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.android.settings.R;
import com.android.settings.Utils;

public class VoltageControlSettings extends Fragment {

    public static class Voltage {
        private String freq;
        private String currentMv;
        private String savedMv;

        public void setFreq(final String freq) {
            this.freq = freq;
        }

        public String getFreq() {
            return freq;
        }

        public void setCurrentMV(final String currentMv) {
            this.currentMv = currentMv;
        }

        public String getCurrentMv() {
            return currentMv;
        }

        public void setSavedMV(final String savedMv) {
            this.savedMv = savedMv;
        }

        public String getSavedMV() {
            return savedMv;
        }

        public String getSysFSString() {
            return freq + " " +savedMv;
        }
    }

    private static final String TAG = "VoltageControlSettings";

    public static final String KEY_APPLY_BOOT = "pref_voltage_apply_at_boot";
    public static final String VDD_FILE = "/sys/devices/system/cpu/cpu0/cpufreq/vdd_levels";

    public static final int DIALOG_EDIT_VOLT = 0;
    private List<Voltage> mVoltages;
    private ListAdapter mAdapter;
    private static SharedPreferences preferences;
    private Voltage mVoltage;
    private Activity mActivity;
    private static final int MENU_RESET = Menu.FIRST;
    private static final int MENU_APPLY = Menu.FIRST + 1;
    private static Menu mOptionsMenu;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup root, Bundle savedInstanceState) {
        mActivity = getActivity();
        View view = inflater.inflate(R.xml.voltage_settings, root, false);
        preferences = PreferenceManager.getDefaultSharedPreferences(mActivity);
        mVoltages = getVolts(preferences);
        
        if (mVoltages.isEmpty()) {
            ((TextView) view.findViewById(R.id.emptyList))
                    .setVisibility(View.VISIBLE);
            ((LinearLayout) view.findViewById(R.id.BottomBar))
                    .setVisibility(View.GONE);
            ((ListView) view.findViewById(R.id.ListView))
                    .setVisibility(View.GONE);

            final SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(KEY_APPLY_BOOT, false);
            editor.commit();

        } else {
            final ListView listView = (ListView) view.findViewById(R.id.ListView);
            final CheckBox setOnBoot = (CheckBox) view.findViewById(R.id.applyAtBoot);

            setHasOptionsMenu(true);

            mAdapter = new ListAdapter(mActivity);
            mAdapter.setListItems(mVoltages);
            listView.setAdapter(mAdapter);

            setOnBoot.setChecked(preferences.getBoolean(KEY_APPLY_BOOT, false));
            setOnBoot.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView,
                        boolean isChecked) {
                    final SharedPreferences.Editor editor = preferences.edit();
                    editor.putBoolean(KEY_APPLY_BOOT, isChecked);
                    editor.commit();
                }
            });

            listView.setOnItemClickListener(new OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position,
                        long id) {
                    mVoltage = mVoltages.get(position);
                    showDialog(DIALOG_EDIT_VOLT);
                }
            });
        }

        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        mOptionsMenu = menu;

        menu.add(0, MENU_RESET, 0, R.string.voltage_reset_values)
                .setIcon(R.drawable.ic_settings_backup) // use the backup icon
                .setAlphabeticShortcut('r')
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM |
                MenuItem.SHOW_AS_ACTION_WITH_TEXT);

        menu.add(0, MENU_APPLY, 0, R.string.voltage_apply_values)
                .setIcon(R.drawable.ic_menu_save)
                .setAlphabeticShortcut('a')
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM |
                MenuItem.SHOW_AS_ACTION_WITH_TEXT);


        updateOptionsMenu();
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        updateOptionsMenu();
    }

    @Override
    public void onDestroyOptionsMenu() {
        mOptionsMenu = null;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_RESET:
                fillWithDefaultVolts(mVoltages);
                mAdapter.notifyDataSetChanged();
                return true;

            case MENU_APPLY:
                applyVoltages(mVoltages);

                final List<Voltage> volts = getVolts(preferences);
                mVoltages.clear();
                mVoltages.addAll(volts);
                mAdapter.notifyDataSetChanged();
                return true;

            default:
                return false;
        }
    }

    static void updateOptionsMenu() {
        if (mOptionsMenu == null) {
            return;
        }

        mOptionsMenu.findItem(MENU_RESET).setVisible(true);
        mOptionsMenu.findItem(MENU_APPLY).setVisible(true);
    }

    public static void applyVoltages(final List<Voltage> volts){
        for (final Voltage volt : volts) {
            Log.d(TAG, "set voltage:"+volt.getSysFSString());
            Utils.fileWriteOneLine(VoltageControlSettings.VDD_FILE, volt.getSysFSString());
        }
    }

    private static String getDefaultMV(String freq){	

    /*
    { 0, 24576,  LPXO, 0, 0,  30720000,  1000, VDD_RAW(1000) },
	{ 0, 61440,  PLL_3,    5, 11, 61440000,  1000, VDD_RAW(1000) },
	{ 0, 122880, PLL_3,    5, 5,  61440000,  1000, VDD_RAW(1000) },
	{ 0, 184320, PLL_3,    5, 4,  61440000,  1000, VDD_RAW(1000) },
	{ 0, 192000, PLL_3,    5, 4,  61440000,  1000, VDD_RAW(1000) },
	{ 1, 245760, PLL_3,    5, 2,  61440000,  1000, VDD_RAW(1000) },
	{ 1, 368640, PLL_3,    5, 1,  122800000, 1050, VDD_RAW(1050) },
	{ 1, 768000, PLL_1,    2, 0,  153600000, 1100, VDD_RAW(1100) },
	{ 1, 806400,  PLL_2, 3, 0, UINT_MAX, 1100, VDD_RAW(1100), &pll2_tbl[0]},
	{ 1, 1024000, PLL_2, 3, 0, UINT_MAX, 1200, VDD_RAW(1200), &pll2_tbl[1]},
	{ 1, 1200000, PLL_2, 3, 0, UINT_MAX, 1200, VDD_RAW(1200), &pll2_tbl[2]},
	{ 1, 1401600, PLL_2, 3, 0, UINT_MAX, 1250, VDD_RAW(1250), &pll2_tbl[3]},
	{ 1, 1497600, PLL_2, 3, 0, UINT_MAX, 1250, VDD_RAW(1250), &pll2_tbl[4]},
    { 1, 1708800, PLL_2, 3, 0, UINT_MAX, 1350, VDD_RAW(1350), &pll2_tbl[5]},
    { 1, 1804800, PLL_2, 3, 0, UINT_MAX, 1350, VDD_RAW(1350), &pll2_tbl[6]},
    { 1, 1900800, PLL_2, 3, 0, UINT_MAX, 1450, VDD_RAW(1450), &pll2_tbl[7]},
    { 1, 2016000, PLL_2, 3, 0, UINT_MAX, 1475, VDD_RAW(1475), &pll2_tbl[8]},
    */

        if(freq.equals("24576"))
            return "1000";
        if(freq.equals("61440"))
            return "1000";
        if(freq.equals("122880"))
            return "1000";
        if(freq.equals("184320"))
            return "1000";
        if(freq.equals("192000"))
            return "1000";
        if(freq.equals("245760"))
            return "1000";
        if(freq.equals("368640"))
            return "1050";
        if(freq.equals("768000"))
            return "1100";
        if(freq.equals("806400"))
            return "1100";
        if(freq.equals("1024000"))
            return "1200";
        if(freq.equals("1200000"))
            return "1200";
        if(freq.equals("1401600"))
            return "1250";
        if(freq.equals("1497600"))
            return "1250";
        if(freq.equals("1708800"))
            return "1350";
        if(freq.equals("1804800"))
            return "1350";
        if(freq.equals("1900800"))
            return "1450";
        if(freq.equals("2016000"))
            return "1475";

        return null;
    }

    private static void fillWithDefaultVolts(final List<Voltage> volts){
        for (final Voltage volt : volts) {
            String freq=volt.getFreq();
            String defaultMv=getDefaultMV(freq);
            if(defaultMv!=null){
                final SharedPreferences.Editor editor = preferences.edit();
                editor.putString(freq, defaultMv);
                editor.commit();
                volt.setSavedMV(defaultMv);
            }
        }
    }

    public static List<Voltage> getVolts(final SharedPreferences preferences) {
        final List<Voltage> volts = new ArrayList<Voltage>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(VDD_FILE), 256);
            String line = "";
            while ((line = br.readLine()) != null) {
                final String[] values = line.split(":");
                if (values != null) {
                    if (values.length == 2) {
                        final String freq = values[0].trim();
                        final String currentMv = values[1].trim();
                        final String savedMv = preferences.getString(freq, currentMv);
                        final Voltage voltage = new Voltage();
                        voltage.setFreq(freq);
                        voltage.setCurrentMV(currentMv);
                        voltage.setSavedMV(savedMv);
                        volts.add(voltage);
                    }
                }
            }
            br.close();
        } catch (FileNotFoundException e) {
            Log.d(TAG, VDD_FILE + " does not exist");
        } catch (IOException e) {
            Log.d(TAG, "Error reading " + VDD_FILE);
        }
        return volts;
    }

    private static final int[] STEPS = new int[] {
            700, 725, 750, 775, 800, 825, 850,
            875, 900, 925, 950, 975, 1000, 1025, 1050, 1075, 1100,
            1125, 1150, 1175, 1200, 1225, 1250, 1275, 1300, 1325,
            1350, 1375, 1400, 1425, 1450, 1475, 1500
    };

    private static int getNearestStepIndex(final int value) {
        int index = 0;
        for (int i = 0; i < STEPS.length; i++) {
            if (value > STEPS[i]) {
                index++;
            } else {
                break;
            }
        }
        return index;
    }

    protected void showDialog(final int id) {
        AlertDialog dialog = null;
        switch (id) {
            case DIALOG_EDIT_VOLT:
                final LayoutInflater factory = LayoutInflater.from(mActivity);
                final View voltageDialog = factory.inflate(R.layout.voltage_dialog, null);

                final EditText voltageEdit = (EditText) voltageDialog
                        .findViewById(R.id.voltageEdit);
                final SeekBar voltageSeek = (SeekBar) voltageDialog.findViewById(R.id.voltageSeek);
                final TextView voltageMeter = (TextView) voltageDialog
                        .findViewById(R.id.voltageMeter);

                final String savedMv = mVoltage.getSavedMV();
                final int savedVolt = Integer.parseInt(savedMv);
                voltageEdit.setText(savedMv);
                voltageEdit.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void afterTextChanged(Editable arg0) {
                        // TODO Auto-generated method stub
                    }

                    @Override
                    public void beforeTextChanged(CharSequence arg0, int arg1,
                            int arg2, int arg3) {
                        // TODO Auto-generated method stub
                    }

                    @Override
                    public void onTextChanged(CharSequence arg0, int arg1,
                            int arg2, int arg3) {
                        final String text = voltageEdit.getText().toString();
                        int value = 0;
                        try {
                            value = Integer.parseInt(text);
                        } catch (NumberFormatException nfe) {
                            return;
                        }
                        voltageMeter.setText(text + " mV");
                        final int index = getNearestStepIndex(value);
                        voltageSeek.setProgress(index);
                    }

                });

                voltageMeter.setText(savedMv + " mV");
                voltageSeek.setMax(STEPS.length);
                voltageSeek.setProgress(getNearestStepIndex(savedVolt));
                voltageSeek.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar sb, int progress,
                            boolean fromUser) {
                        if (fromUser) {
                            final String volt = Integer.toString(STEPS[progress]);
                            voltageMeter.setText(volt + " mV");
                            voltageEdit.setText(volt);
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                        // TODO Auto-generated method stub

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        // TODO Auto-generated method stub

                    }

                });

                dialog = new AlertDialog.Builder(mActivity)
                        .setTitle(mVoltage.getFreq() + getResources().getString(R.string.voltage_ps_volt_mhz_voltage))
                        .setView(voltageDialog)
                        .setPositiveButton(getResources().getString(R.string.voltage_ps_volt_save), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        removeDialog(id);
                        final String value = voltageEdit.getText().toString();
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putString(mVoltage.getFreq(), value);
                        editor.commit();
                        mVoltage.setSavedMV(value);
                        mAdapter.notifyDataSetChanged();
                    }
                })
                .setNegativeButton(null,
                        new  DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,
                            int whichButton) {
                        removeDialog(id);
                    }
                }).create();
                break;
            default:
                break;
        }

        if (dialog != null) {
            FragmentManager fm = getActivity().getFragmentManager();
            FragmentTransaction ftr = fm.beginTransaction();
            CustomDialogFragment newFragment = CustomDialogFragment.newInstance(dialog);
            DialogFragment fragmentDialog = (DialogFragment) fm.findFragmentByTag("" + id);
            if (fragmentDialog != null) {
                ftr.remove(fragmentDialog);
                ftr.commit();
            }
            newFragment.show(fm, "" + id);
        }
    }

    protected void removeDialog(int pDialogId) {
        FragmentManager fm = mActivity.getFragmentManager();
        FragmentTransaction ftr = fm.beginTransaction();
        DialogFragment fragmentDialog = null;
        fragmentDialog = (DialogFragment) fm.findFragmentByTag("" + pDialogId);
        if (fragmentDialog != null) {
            FragmentTransaction f = ftr.remove(fragmentDialog);
            f.commit();
        }
    }

    protected static class CustomDialogFragment extends DialogFragment {
        private Dialog mDialog;

        public static CustomDialogFragment newInstance(Dialog dialog) {
            CustomDialogFragment frag = new CustomDialogFragment();
            frag.mDialog = dialog;
            return frag;
        }

        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return mDialog;
        }
    }

    public class ListAdapter extends BaseAdapter {
        private LayoutInflater mInflater;
        private List<Voltage> results;

        public ListAdapter(Context context) {
            mInflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return results.size();
        }

        @Override
        public Object getItem(int position) {
            return results.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {

            final ViewHolder holder;
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.list_volt, null);
                holder = new ViewHolder();
                holder.mFreq = (TextView) convertView.findViewById(R.id.Freq);
                holder.mCurrentMV = (TextView) convertView.findViewById(R.id.mVCurrent);
                holder.mSavedMV = (TextView) convertView.findViewById(R.id.mVSaved);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            final Voltage voltage = mVoltages.get(position);
            holder.setFreq(voltage.getFreq());
            holder.setCurrentMV(voltage.getCurrentMv());
            holder.setSavedMV(voltage.getSavedMV());
            return convertView;
        }

        public void setListItems(List<Voltage> mVoltages) {
            results = mVoltages;
        }

        public class ViewHolder {
            private TextView mFreq;
            private TextView mCurrentMV;
            private TextView mSavedMV;

            public void setFreq(final String freq) {
                mFreq.setText(freq + " MHz");
            }

            public void setCurrentMV(final String currentMv) {
               mCurrentMV.setText(getResources().getString(R.string.voltage_ps_volt_current_voltage) + currentMv + " mV");
            }

            public void setSavedMV(final String savedMv) {
               mSavedMV.setText(getResources().getString(R.string.voltage_ps_volt_setting_to_apply) + savedMv + " mV");
            }
        }
    }
}

