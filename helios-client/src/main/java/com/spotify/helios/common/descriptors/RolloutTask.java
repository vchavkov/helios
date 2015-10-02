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

package com.spotify.helios.common.descriptors;

import com.google.common.base.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.jetbrains.annotations.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RolloutTask extends Descriptor {

  public enum Action {
    UNDEPLOY_OLD_JOBS,
    DEPLOY_NEW_JOB,
    AWAIT_RUNNING,
  }

  public enum Status {
    OK,
    FAILED
  }

  private final Action action;
  private final String target;
  private final JobId job;

  /*public static RolloutTask of(final Action action, final String target) {
    return new RolloutTask(action, target, null);
  }*/

  public static RolloutTask of(final Action action, final String target, final JobId job) {
    return new RolloutTask(action, target, job);
  }

  private RolloutTask(@JsonProperty("action") final Action action,
                      @JsonProperty("target") final String target,
                      @JsonProperty("job") @Nullable final JobId job) {
    this.action = checkNotNull(action, "action");
    this.target = checkNotNull(target, "target");
    this.job = job;
  }

  public Action getAction() {
    return action;
  }

  public String getTarget() {
    return target;
  }

  public JobId getJob() {
    return job;
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("action", action)
        .add("target", target)
        .add("job", job)
        .toString();
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    final RolloutTask task = (RolloutTask) o;

    if (action != task.action) {
      return false;
    }
    if (job != null ? !job.equals(task.job) : task.job != null) {
      return false;
    }
    if (target != null ? !target.equals(task.target) : task.target != null) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = action != null ? action.hashCode() : 0;
    result = 31 * result + (target != null ? target.hashCode() : 0);
    result = 31 * result + (job != null ? job.hashCode() : 0);
    return result;
  }

  public static class Builder {
    private Action action;
    private String target;
    private JobId job;

    public Builder setAction(Action action) {
      this.action = action;
      return this;
    }

    public Builder setTarget(String target) {
      this.target = target;
      return this;
    }

    public Builder setJob(final JobId job) {
      this.job = job;
      return this;
    }

    public RolloutTask build() {
      return new RolloutTask(action, target, job);
    }
  }
}
