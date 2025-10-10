package com.devcollab.domain;

import java.io.Serializable;
import java.util.Objects;

public class TaskFollowerId implements Serializable {
    private Long task;
    private Long user;

    public TaskFollowerId() {
    }

    public TaskFollowerId(Long task, Long user) {
        this.task = task;
        this.user = user;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof TaskFollowerId that))
            return false;
        return Objects.equals(task, that.task) &&
                Objects.equals(user, that.user);
    }

    @Override
    public int hashCode() {
        return Objects.hash(task, user);
    }
}
