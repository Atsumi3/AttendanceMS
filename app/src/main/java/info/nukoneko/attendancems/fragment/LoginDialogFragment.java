package info.nukoneko.attendancems.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import info.nukoneko.attendancems.R;
import info.nukoneko.attendancems.activity.MainActivity;
import info.nukoneko.attendancems.auth.Auth;
import info.nukoneko.attendancems.common.Globals;

/**
 * Created by Telneko on 2014/12/10.
 */
public class LoginDialogFragment extends DialogFragment {
    Auth.AuthCallback callback;
    public LoginDialogFragment(Auth.AuthCallback callback){
        this.callback = callback;
    }
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View content = inflater.inflate(R.layout.fragment_login, null);
        final EditText userID = (EditText)content.findViewById(R.id.login_id);
        final EditText passWD = (EditText)content.findViewById(R.id.login_pass);
        builder.setView(content);
        builder.setMessage("ログイン")
            .setNegativeButton("閉じる", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                }
            })
            .setPositiveButton("認証", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    final MainActivity activity = (MainActivity)getActivity();
                    final TextView authStatusText = (TextView) activity.findViewById(R.id.auth_status);
                    Auth.LoginObject object = new Auth.LoginObject(
                            userID.getText().toString(),
                            passWD.getText().toString());
                    new Auth(activity, object, callback);
                }
            });
        return builder.create();
    }
}
