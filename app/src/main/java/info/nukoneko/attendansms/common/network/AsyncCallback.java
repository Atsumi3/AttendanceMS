package info.nukoneko.attendansms.common.network;

/**
 * Created by TEJNEK on 2014/11/05.
 */
public interface AsyncCallback<T> {
    T doFunc(Object... params);
    void onResult(T result);
}
