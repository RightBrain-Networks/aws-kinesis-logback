package com.proquest.intota.v2.aws_kinesis_logback;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Policy that implements producer thread blocking indefinitely whenever a new
 * task cannot be scheduled in the threadpool for AWS SDK's async Kinesis
 * client. Using this policy, the produce thread will unblock only after there
 * is space in the threadpool's processing queue.
 */
public final class BlockFastProducerPolicy implements RejectedExecutionHandler {
  @Override
  public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
    if (executor.isShutdown()) {
      throw new RejectedExecutionException("Threadpoolexecutor already shutdown");
    } else {
      try {
        executor.getQueue().put(r);
      } catch (InterruptedException e) {
        throw new RejectedExecutionException(
            "Thread was interrupted while waiting for space to be available in the threadpool", e);
      }
    }
  }
}