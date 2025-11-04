package com.devcollab.service.core;

import com.devcollab.dto.ProjectDTO;
import com.devcollab.dto.UserDTO;

import java.util.List;

public interface UserProjectService {

    /** 🔹 Get all projects that a user has joined */
    List<ProjectDTO> getProjectsByUser(Long userId);

    /** 🔹 Get all users that have collaborated with this user */
    List<UserDTO> getWorkedWithUsers(Long userId);
}
