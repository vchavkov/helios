/*
 * Copyright (c) 2014 Spotify AB.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.spotify.helios.testing;

import com.google.common.base.Throwables;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.MultipleFailureException;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * A class that wraps {@link TemporaryJobs} for use as a JUnit {@link org.junit.Rule} or
 * {@link org.junit.ClassRule}.
 */
public class TemporaryJobsResource implements TestRule {

  private static final Logger log = LoggerFactory.getLogger(TemporaryJobsResource.class);

  private static final long JOB_HEALTH_CHECK_INTERVAL_MILLIS = SECONDS.toMillis(5);

  private final TemporaryJobs temporaryJobs;

  private TemporaryJobsResource(final TemporaryJobs temporaryJobs) {
    this.temporaryJobs = temporaryJobs;
  }

  public static TemporaryJobsResource newInstance() {
    return new TemporaryJobsResource(TemporaryJobs.create());
  }

  // TODO (dxia) Is this a descriptive enough method name or should we use "newTemporaryJobBuilder"?
  public TemporaryJobBuilder job() {
    return temporaryJobs.job();
  }

  private final ExecutorService executor = MoreExecutors.getExitingExecutorService(
      (ThreadPoolExecutor) Executors.newFixedThreadPool(
          1, new ThreadFactoryBuilder()
              .setNameFormat("helios-test-runner-%d")
              .setDaemon(true)
              .build()),
      0, SECONDS);

  @Override
  public Statement apply(final Statement base, final Description description) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        try {
          before(temporaryJobs);
          perform(base, temporaryJobs);
        } finally {
          after();
        }
      }
    };
  }

  /**
   * Perform setup. This is normally called by JUnit when TemporaryJobs is used with @Rule.
   * If @Rule cannot be used, call this method before calling {@link TemporaryJobs#job()}.
   *
   * Note: When not being used as a @Rule, jobs will not be monitored during test runs.
   * @param temporaryJobs {@link TemporaryJobs}
   */
  public void before(final TemporaryJobs temporaryJobs) {
    temporaryJobs.deployer().readyToDeploy();
  }

  private void perform(final Statement base, final TemporaryJobs temporaryJobs)
      throws InterruptedException {
    // Run the actual test on a thread
    final Future<Object> future = executor.submit(new Callable<Object>() {
      @Override
      public Object call() throws Exception {
        try {
          base.evaluate();
        } catch (MultipleFailureException e) {
          // Log the stack trace for each exception in the MultipleFailureException, because
          // stack traces won't be logged when this is caught and logged higher in the call stack.
          final List<Throwable> failures = e.getFailures();
          log.error(format("MultipleFailureException contains %d failures:", failures.size()));
          for (int i = 0; i < failures.size(); i++) {
            log.error(format("MultipleFailureException %d:", i), failures.get(i));
          }
          throw Throwables.propagate(e);
        } catch (Throwable throwable) {
          Throwables.propagateIfPossible(throwable, Exception.class);
          throw Throwables.propagate(throwable);
        }
        return null;
      }
    });

    // Monitor jobs while test is running
    while (!future.isDone()) {
      Thread.sleep(JOB_HEALTH_CHECK_INTERVAL_MILLIS);
      verifyJobsHealthy(temporaryJobs);
    }

    // Rethrow test failure, if any
    try {
      future.get();
    } catch (ExecutionException e) {
      final Throwable cause = (e.getCause() == null) ? e : e.getCause();
      throw Throwables.propagate(cause);
    }
  }

  /**
   * Perform teardown. This is normally called by JUnit when TemporaryJobsResource is used
   * with @Rule. If @Rule cannot be used, call this method after running tests.
   */
  public void after() {
    // Stop the test runner thread
    executor.shutdownNow();
    try {
      final boolean terminated = executor.awaitTermination(30, SECONDS);
      if (!terminated) {
        log.warn("Failed to stop test runner thread");
      }
    } catch (InterruptedException ignore) {
    }

    temporaryJobs.close();
  }

  private static void verifyJobsHealthy(final TemporaryJobs temporaryJobs) throws AssertionError {
    for (final TemporaryJob job : temporaryJobs.jobs()) {
      job.verifyHealthy();
    }
  }
}
