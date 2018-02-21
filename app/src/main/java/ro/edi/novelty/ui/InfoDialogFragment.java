/*
* Copyright 2015 Eduard Scarlat
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package ro.edi.novelty.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;

import ro.edi.novelty.R;
import ro.edi.util.Utils;

public class InfoDialogFragment extends DialogFragment {

    public InfoDialogFragment() {

    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity());
        dialog.setTitle(R.string.app_name);

        StringBuilder txtAbout = new StringBuilder(128);
        txtAbout.append(getText(R.string.about_app));
        txtAbout.append(Utils.getAppVersionName(getActivity()));
        dialog.setMessage(txtAbout);

        dialog.setPositiveButton(R.string.btn_ok, null);
        dialog.setNegativeButton(R.string.btn_rate, (dlg, which) -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("market://details?id=ro.edi.novelty"));
            startActivity(intent);
        });
        dialog.setNeutralButton(R.string.btn_other_apps, (dlg, which) -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("market://search?q=pub:Eduard%20Scarlat"));
            startActivity(intent);
        });

        return dialog.create();
    }
}
