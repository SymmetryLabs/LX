package heronarts.lx.utils;

import java.util.concurrent.ConcurrentLinkedQueue;

public class LXDispatchQueue {

  private volatile long threadId;
  private volatile boolean forceAsync;
  private final ConcurrentLinkedQueue<Runnable> queuedRunnables = new ConcurrentLinkedQueue<Runnable>();

  public LXDispatchQueue() {
    this(true);
  }

  public LXDispatchQueue(boolean forceAsync) {
    setThreadIdToCurrentThread();
    this.forceAsync = forceAsync;
  }

  public LXDispatchQueue(long threadId) {
    this.threadId = threadId;
  }

  public void setThreadIdToCurrentThread() {
    setThreadId(Thread.currentThread().getId());
  }

  public void setThreadId(long threadId) {
    this.threadId = threadId;
  }

  public long getThreadId() {
    return threadId;
  }

  public void setForceAsync(boolean forceAsync) {
    this.forceAsync = forceAsync;
  }

  public boolean getForceAsync() {
    return forceAsync;
  }

  public void executeAll() {
    Runnable runnable;
    while ((runnable = queuedRunnables.poll()) != null) {
      runnable.run();
    }
  }

  public void queue(Runnable runnable) {
    if (!this.forceAsync && Thread.currentThread().getId() == this.threadId) {
      runnable.run();
    } else {
      queuedRunnables.add(runnable);
    }
  }

}
