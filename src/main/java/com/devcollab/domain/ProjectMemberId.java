package com.devcollab.domain;

import java.io.Serializable;
import java.util.Objects;

public class ProjectMemberId implements Serializable {
    private Long project;
    private Long user;

    public ProjectMemberId() {
    }

    public ProjectMemberId(Long project, Long user) {
        this.project = project;
        this.user = user;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof ProjectMemberId that))
            return false;
        return Objects.equals(project, that.project) &&
                Objects.equals(user, that.user);
    }

    @Override
    public int hashCode() {
        return Objects.hash(project, user);
    }
}
