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

package com.spotify.helios.master.resources;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;

import com.spotify.helios.common.descriptors.HostStatus;
import com.spotify.helios.common.descriptors.Job;
import com.spotify.helios.common.descriptors.JobId;
import com.spotify.helios.common.descriptors.TaskStatus;
import com.spotify.helios.master.MasterModel;
import com.yammer.dropwizard.views.View;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static com.google.common.base.CharMatcher.WHITESPACE;

public class DashboardView extends View {

  private MasterModel model;

  public DashboardView(final MasterModel model) {
    super("dashboard.ftl");
    this.model = model;
  }

  public List<JobTableEntry> getJobs() {
    final List<JobTableEntry> entries = Lists.newArrayList();
    for (final Map.Entry<JobId, Job> e : sorted(model.getJobs()).entrySet()) {
      entries.add(new JobTableEntry() {{
        id = e.getKey().toShortString();
        hosts = model.getJobStatus(e.getKey()).getDeployments().size();
        command = Joiner.on(' ').join(escape(e.getValue().getCommand()));
        env = Joiner.on(", ").withKeyValueSeparator("=").join(e.getValue().getEnv());
      }});
    }
    return entries;
  }

  public List<HostTableEntry> getHosts() {
    final List<String> hosts = Ordering.natural().sortedCopy(model.listHosts());
    final List<HostTableEntry> entries = Lists.newArrayList();
    for (final String host : hosts) {
      final HostStatus hostStatus = model.getHostStatus(host);
      if (hostStatus == null) {
        continue;
      }
      entries.add(new HostTableEntry(){{
        name = host;
        status = hostStatus.getStatus().toString();
        jobsDeployed = hostStatus.getJobs().size();
        jobsRunning = countRunning(hostStatus.getStatuses());
        cpus = "";
        mem = "";
        loadAvg = "";
        memUsage = "";
        osName = "";
        osVersion = "";
      }});
    }
    return entries;
  }

  private int countRunning(final Map<JobId, TaskStatus> statuses) {
    int n = 0;
    for (TaskStatus status : statuses.values()) {
      if (status.getState() == TaskStatus.State.RUNNING) {
        n++;
      }
    }
    return n;
  }

  private List<String> escape(final List<String> args) {
    return Lists.transform(args, new Function<String, String>() {
      @Override
      public String apply(final String input) {
        if (WHITESPACE.matchesAnyOf(input)) {
          return '"' + input + '"';
        } else {
          return input;
        }
      }
    });
  }

  private <K extends Comparable<K>, V> Map<K, V> sorted(final Map<K, V> map) {
    final TreeMap<K, V> sorted = Maps.newTreeMap();
    sorted.putAll(map);
    return sorted;
  }

  public static class JobTableEntry {

    public String id;
    public int hosts;
    public String command;
    public String env;

    public String getId() {
      return id;
    }

    public int getHosts() {
      return hosts;
    }

    public String getCommand() {
      return command;
    }

    public String getEnv() {
      return env;
    }
  }

  public class HostTableEntry {
    public String name;
    public String status;
    public int jobsDeployed;
    public int jobsRunning;
    public String cpus;
    public String mem;
    public String loadAvg;
    public String memUsage;
    public String osName;
    public String osVersion;

    public String getName() {
      return name;
    }

    public String getStatus() {
      return status;
    }

    public String getJobsDeployed() {
      return String.valueOf(jobsDeployed);
    }

    public String getJobsRunning() {
      return String.valueOf(jobsRunning);
    }

    public String getCpus() {
      return cpus;
    }

    public String getMem() {
      return mem;
    }

    public String getLoadAvg() {
      return loadAvg;
    }

    public String getMemUsage() {
      return memUsage;
    }

    public String getOsName() {
      return osName;
    }

    public String getOsVersion() {
      return osVersion;
    }
  }
}
