package kin.utils;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Represents method invocation, each request will run sequentially on background thread,
 * and will notify {@link ResultCallback} witch success or error on main thread.
 *
 * @param <T> request result type
 */
public class FutureRequest<T> extends Request<T> {

    private static final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Callable<T> callable;
    private Future<?> future;

    public FutureRequest(Callable<T> callable) {
        super();
        checkNotNull(callable, "callable");
        this.callable = callable;
    }

    @Override
    public synchronized void run(ResultCallback<T> callback) {
        super.run(callback);
        submitFuture(callable);
    }

    private void submitFuture(final Callable<T> callable) {
        future = executorService.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    final T result = callable.call();
                    handleResult(result);
                } catch (final Exception e) {
                    handleException(e);
                }
            }
        });
    }

    /**
     * Cancel {@code Request} and detach its callback,
     * an attempt will be made to cancel ongoing request, if request has not run yet it will
     * never run.
     *
     * @param mayInterruptIfRunning true if the request should be interrupted; otherwise,
     *                              in-progress requests are
     *                              allowed to complete
     */
    @Override
    public synchronized void cancel(boolean mayInterruptIfRunning) {
        if (!getIsCancelled()) {
            if (future != null) {
                future.cancel(mayInterruptIfRunning);
            }
            future = null;
        }
        super.cancel(mayInterruptIfRunning);
    }

}
