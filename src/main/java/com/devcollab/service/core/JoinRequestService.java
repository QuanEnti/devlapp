package com.devcollab.service.core;

import com.devcollab.domain.JoinRequest;
import com.devcollab.domain.Project;
import com.devcollab.domain.User;
import java.util.List;

public interface JoinRequestService {

    JoinRequest createJoinRequest(Project project, User user);

    JoinRequest approveRequest(Long requestId, String reviewerEmail);

    JoinRequest rejectRequest(Long requestId, String reviewerEmail);

    List<JoinRequest> getPendingRequests(Long projectId);
}
