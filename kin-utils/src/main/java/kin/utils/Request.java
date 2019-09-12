package kin.utils;

import android.os.Handler;
import android.os.Looper;

/**
 * Represents method invocation, each request will run sequentially on background thread,
 * and will notify {@link ResultCallback} witch success or error on main thread.
 *
 * @param <T> request result type
 */
class Request<T> {

    private final Handler mainHandler;
    private boolean cancelled;
    private boolean executed;
    private ResultCallback<T> resultCallback;

    Request() {
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * Run request asynchronously, notify {@code callback} with successful result or error
     */
    public synchronized void run(ResultCallback<T> callback) {
        this.resultCallback = callback;
        checkBeforeRun(callback);
        executed = true;
    }

    protected synchronized void executeOnMainThreadIfNotCancelled(Runnable runnable) {
        if (!cancelled) {
            mainHandler.post(runnable);
        }
    }

    protected synchronized void handleResult(final T result) {
        executeOnMainThreadIfNotCancelled(new Runnable() {

            @Override
            public void run() {
                resultCallback.onResult(result);
            }
        });
    }

    protected synchronized void handleException(final Exception e) {
        executeOnMainThreadIfNotCancelled(new Runnable() {

            @Override
            public void run() {
                resultCallback.onError(e);
            }
        });
    }

    protected synchronized boolean getIsCancelled() {
        return cancelled;
    }

    /**
     * Cancel {@code Request} and detach its callback,
     * an attempt will be made to cancel ongoing request, if request has not run yet it will never run.
     *
     * allowed to complete
     */
    public synchronized void cancel(boolean mayInterruptIfRunning) { // TODO: 2019-09-12 Dont
        // really need here the 'mayInterruptIfRunning' it is just for backward compatibility
        if (!cancelled) {
            cancelled = true;
            mainHandler.removeCallbacksAndMessages(null);
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    resultCallback = null;
                }
            });
        }
    }

    protected void checkNotNull(Object param, String name) {
        if (param == null) {
            throw new IllegalArgumentException(name + " cannot be null.");
        }
    }

    private void checkBeforeRun(ResultCallback<T> callback) {
        checkNotNull(callback, "callback");
        if (executed) {
            throw new IllegalStateException("Request already running.");
        }
        if (cancelled) {
            throw new IllegalStateException("Request already cancelled.");
        }
    }

}
