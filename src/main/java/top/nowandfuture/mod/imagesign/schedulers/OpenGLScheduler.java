package top.nowandfuture.mod.imagesign.schedulers;

import com.mojang.blaze3d.pipeline.RenderCall;
import com.mojang.blaze3d.systems.RenderSystem;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.disposables.Disposable;


import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Executes work on the OpenGL main render thread.
 * This scheduler should only be used with actions that execute quickly.
 */
public class OpenGLScheduler extends Scheduler {
    private static final OpenGLScheduler INSTANCE = new OpenGLScheduler();

    /* package for unit test */OpenGLScheduler() {
    }

    public static OpenGLScheduler renderThread() {
        return INSTANCE;
    }

    private static void assertThatTheDelayIsValidForTheTimer(long delay) {
        if (delay < 0 || delay > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(String.format("The timer only accepts non-negative delays up to %d milliseconds.", Integer.MAX_VALUE));
        }
    }

    @Override
    public Worker createWorker() {
        return new OpenGLWorker();
    }

    /**
     * A Worker implementation which manages a queue of QueuedRunnable for execution on the Java FX Application thread
     * For a simpler implementation the queue always contains at least one element.
     * {@link #head} is the element, which is in execution or was last executed
     * {@link #tail} is an atomic reference to the last element in the queue, or null when the worker was disposed
     * Recursive actions are not preferred and inserted at the tail of the queue as any other action would be
     * The Worker will only schedule a single job with {@link RenderSystem#recordRenderCall(RenderCall)} for when the queue was previously empty
     */
    private static class OpenGLWorker extends Worker implements Runnable {
        private volatile QueuedRunnable                  head = new QueuedRunnable(null); /// only advanced in run(), initialised with a starter element
        private final    AtomicReference<QueuedRunnable> tail = new AtomicReference<>(head); /// points to the last element, null when disposed

        private static class QueuedRunnable extends AtomicReference<QueuedRunnable> implements Disposable, Runnable {
            private volatile Runnable action;

            private QueuedRunnable(Runnable action) {
                this.action = action;
            }

            @Override
            public void dispose() {
                action = null;
            }

            @Override
            public boolean isDisposed() {
                return action == null;
            }

            @Override
            public void run() {
                Runnable action = this.action;
                if (action != null) {
                    action.run();
                }
                this.action = null;
            }
        }

        @Override
        public void dispose() {
            tail.set(null);
            QueuedRunnable qr = this.head;
            while (qr != null) {
                qr.dispose();
                qr = qr.getAndSet(null);
            }
        }

        @Override
        public boolean isDisposed() {
            return tail.get() == null;
        }

        @Override
        public Disposable schedule(final Runnable action, long delayTime, TimeUnit unit) {
            long delay = Math.max(0, unit.toMillis(delayTime));
            assertThatTheDelayIsValidForTheTimer(delay);

            final QueuedRunnable queuedRunnable = new QueuedRunnable(action);
            if (delay == 0) { // delay is too small for the java timer, schedule it without delay
                return schedule(queuedRunnable);
            }

            final Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    schedule(queuedRunnable);
                }
            }, delay);

            return Disposable.fromRunnable(() -> {
                queuedRunnable.dispose();
                timer.cancel();
                timer.purge();
            });
        }

        @Override
        public Disposable schedule(final Runnable action) {
            if (isDisposed()) {
                return Disposable.disposed();
            }

            final QueuedRunnable queuedRunnable = action instanceof QueuedRunnable ? (QueuedRunnable) action : new QueuedRunnable(action);

            QueuedRunnable tailPivot;
            do {
                tailPivot = tail.get();
            } while (tailPivot != null && !tailPivot.compareAndSet(null, queuedRunnable));

            if (tailPivot == null) {
                queuedRunnable.dispose();
            } else {
                tail.compareAndSet(tailPivot, queuedRunnable); // can only fail with a concurrent dispose and we don't want to override the disposed value
                if (tailPivot == head) {

                    //Run the task in render thread in OpenGL. (OpenGL's main thread)
                    if (RenderSystem.isOnRenderThread()) {
                        action.run();
                    } else {
                        RenderSystem.recordRenderCall(this::run);
                    }
                }
            }
            return queuedRunnable;
        }

        @Override
        public void run() {
            for (QueuedRunnable qr = head.get(); qr != null; qr = qr.get()) {
                qr.run();
                head = qr;
            }
        }
    }
}
