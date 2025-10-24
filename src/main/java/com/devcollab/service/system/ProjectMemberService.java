package com.devcollab.service.system;

import com.devcollab.dto.MemberDTO;
import java.util.List;

import org.springframework.data.domain.Page;

public interface ProjectMemberService {
    List<MemberDTO> getMembersByProject(Long projectId, int limit);

    List<MemberDTO> getAllMembersByPmEmail(String email);
    
    Page<MemberDTO> getAllMembers(int page, int size, String keyword);
    
    boolean removeMember(Long userId); 

    boolean removeMemberFromProject(Long projectId, Long userId);
    boolean addMemberToProject(Long projectId, String pmEmail, String email, String role);

    
    boolean updateMemberRole(Long projectId, Long userId, String role);
    boolean removeUserFromAllProjectsOfPm(String pmEmail, Long userId);
    
    void updateMemberRole(Long projectId, Long userId, String role, String pmEmail);

}

