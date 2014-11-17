/*
 * Copyright (c) 2014 Spotify AB.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
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

package com.spotify.helios.system;

import com.google.common.collect.ImmutableMap;
import com.google.common.net.HostAndPort;

import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.LogStream;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerInfo;
import com.spotify.docker.client.messages.HostConfig;
import com.spotify.docker.client.messages.PortBinding;
import com.spotify.helios.Polling;
import com.spotify.helios.common.descriptors.Job;
import com.spotify.helios.common.descriptors.JobId;
import com.spotify.helios.common.descriptors.TaskStatus;

import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeoutException;

import static com.spotify.docker.client.DockerClient.LogsParameter.STDERR;
import static com.spotify.docker.client.DockerClient.LogsParameter.STDOUT;
import static com.spotify.helios.common.descriptors.HostStatus.Status.UP;
import static com.spotify.helios.common.descriptors.TaskStatus.State.EXITED;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;

public class SyslogRedirectionTest extends SystemTestBase {

  public static final String REDIRECTOR = "spotify/syslog-redirector:0.0.5";

  private HostAndPort sinkHostAndPort;
  private String sink;

  @Before
  public void setup() throws Exception {
    final DockerClient docker = getNewDockerClient();
//    docker.pull(REDIRECTOR);
    final ContainerConfig redirectorConfig = ContainerConfig.builder()
        .image(REDIRECTOR)
        .volumes("/helios")
        .cmd("cp", "/syslog-redirector", "/helios")
        .build();
    final String redirector = randomName("redirector");
    docker.createContainer(redirectorConfig, redirector);
    docker.startContainer(redirector);
    docker.waitContainer(redirector);

    sink = randomName("sink");
    final ContainerConfig sinkConfig = ContainerConfig.builder()
        .image(BUSYBOX)
        .cmd("nc", "-p", "4711", "-l")
        .build();
    final int syslog = temporaryPorts.localPort("syslog");
    final HostConfig sinkHostConfig = HostConfig.builder()
        .portBindings(ImmutableMap.of(syslog + "/tcp", asList(PortBinding.of("0.0.0.0", 4711))))
        .build();
    docker.createContainer(sinkConfig, sink);
    docker.startContainer(sink, sinkHostConfig);
    awaitContainerRunning(docker, sink);

    sinkHostAndPort = HostAndPort.fromParts(DOCKER_HOST.address(), 4711);
  }

  private void awaitContainerRunning(final DockerClient docker, final String container)
      throws TimeoutException {
    Polling.awaitUnchecked(LONG_WAIT_SECONDS, SECONDS, new Callable<Object>() {
      @Override
      public Object call() throws Exception {
        ContainerInfo info = docker.inspectContainer(container);
        return info != null && info.state().running() ? true : null;
      }
    });
  }

  private String randomName(final String name) {
    return testTag + "_" + name + "_" + Integer.toHexString(ThreadLocalRandom.current().nextInt());
  }

  @Test
  public void test() throws Exception {
    // While this test doesn't specifically test that the output actually goes to syslog, it tests
    // just about every other part of it, and specifically, that the output doesn't get to
    // docker, and that the redirector executable exists and doesn't do anything terribly stupid.
    startDefaultMaster();
    startDefaultAgent(testHost(), "--syslog-redirect", "10.0.3.1:6514");
    awaitHostStatus(testHost(), UP, LONG_WAIT_SECONDS, SECONDS);

    try (final DockerClient dockerClient = getNewDockerClient()) {
      final List<String> command = asList("sh", "-c", "echo should-be-redirected");

      // Create job
      final Job job = Job.newBuilder()
          .setName(testJobName)
          .setVersion(testJobVersion)
          .setImage(BUSYBOX)
          .setCommand(command)
          .setVolumesFrom(sink)
          .build();

      final JobId jobId = createJob(job);

      // deploy
      deployJob(jobId, testHost());

      final TaskStatus taskStatus = awaitTaskState(jobId, testHost(), EXITED);

      final String log;
      try (LogStream logs = dockerClient.logs(taskStatus.getContainerId(), STDOUT, STDERR)) {
        log = logs.readFully();
      }

      // should be nothing in the docker output log, either error text or our message
      assertEquals("", log);
    }
  }

}
