package info.nukoneko.attendancems.common.network;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

/**
 * Created by TEJNEK on 2014/11/05.
 */
public class Async<T> {
    private Context context;
    private boolean progressShow = false;
    private String progressString = "";
    private ProgressDialog dialog;
    private boolean cancelable;
    private AsyncTask<Void, Void, T> myTask;

    public Async(final AsyncCallback<T> asyncAPICallback, final Object... params) {
        if (this.progressShow) {
            dialog = new ProgressDialog(context);
            dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            dialog.setMessage(this.progressString);
            dialog.setCancelable(this.cancelable);
            dialog.show();
        }
        this.myTask = new AsyncTask<Void, Void, T>() {
            @Override
            protected T doInBackground(Void... voids) {
                return asyncAPICallback.doFunc(params);
            }

            @Override
            protected void onPostExecute(T ret) {
                super.onPostExecute(ret);
                asyncAPICallback.onResult(this.isCancelled() ? null : ret);
                if (dialog != null) {
                    dialog.dismiss();
                    dialog = null;
                }
            }
        };
    }

    public void run() {
        if (this.myTask == null) return;
        this.myTask.execute();
    }

    public enum method{
        GET,
        POST,
        DELETE,
        PUT
    }
    public enum Protocol{
        HTTP,
        HTTPS
    }
}
