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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.Futures;

import com.spotify.helios.client.HeliosClient;
import com.spotify.helios.common.descriptors.Job;
import com.spotify.helios.common.descriptors.JobId;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ExistingHeliosDeploymentTest {

  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  private HeliosClient client;

  @Before
  public void before() {
    client = mock(HeliosClient.class);
  }

  @Test
  public void testRemoveOldJobs() throws Exception {
    final File file1 = folder.newFile("foo");
    final File file2 = folder.newFile("bar.tmp");
    final File dir = folder.newFolder("qux");
    final List<File> files = ImmutableList.of(file1, file2, dir);

    final Map<JobId, Job> jobs = ImmutableMap.of(
        new JobId("foo", "1-aaa"), Job.newBuilder().build(),
        new JobId("foobar", "1-aaa"), Job.newBuilder().build(),
        new JobId("bar", "2-bbb"), Job.newBuilder().build(),
        new JobId("qux", "3-ccc"), Job.newBuilder().build()
    );
    when(client.jobs()).thenReturn(Futures.immediateFuture(jobs));
    ExistingHeliosDeployment.removeOldJobs(files, client);


  }
}
