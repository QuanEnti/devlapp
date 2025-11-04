package com.devcollab.repository;

import com.devcollab.domain.Project;
import com.devcollab.domain.ProjectMember;
import com.devcollab.domain.ProjectMemberId;
import com.devcollab.domain.User;
import com.devcollab.dto.MemberDTO;
import com.devcollab.dto.UserDTO;
import org.springframework.data.repository.query.Param;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, ProjectMemberId> {

        List<ProjectMember> findByProject_ProjectId(Long projectId);

        List<ProjectMember> findByUser_UserId(Long userId);

        List<ProjectMember> findByProject_ProjectIdAndUser_UserId(Long projectId, Long userId);

        boolean existsByProject_ProjectIdAndUser_UserIdAndRoleInProjectIn(
                        Long projectId, Long userId, List<String> roles);

        // ✅ NEW: Kiểm tra quyền PM/ADMIN theo email (cho removeMemberFromProject)
        boolean existsByProject_ProjectIdAndUser_EmailAndRoleInProjectIn(
                        Long projectId,
                        String email,
                        List<String> roles);

        @Query("""
                            SELECT new com.devcollab.dto.UserDTO(u)
                            FROM ProjectMember pm
                            JOIN pm.user u
                            WHERE pm.project.projectId = :projectId
                        """)
        List<UserDTO> findMembersByProjectId(@Param("projectId") Long projectId);

        @Query("""
                            SELECT DISTINCT new com.devcollab.dto.MemberDTO(
                                u.userId,
                                u.name,
                                u.avatarUrl
                            )
                            FROM ProjectMember pm
                                JOIN pm.user u
                                JOIN pm.project p
                            WHERE p.createdBy.email = :pmEmail
                            ORDER BY u.name ASC
                        """)
        List<MemberDTO> findAllMembersByPmEmail(String pmEmail);

        @Query(value = """
                        SELECT DISTINCT new com.devcollab.dto.MemberDTO(
                            u.userId,
                            u.name,
                            u.email,
                            u.avatarUrl
                        )
                        FROM ProjectMember pm
                        JOIN pm.user u
                        WHERE (:keyword IS NULL
                            OR LOWER(u.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                            OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')))
                        ORDER BY u.name ASC
                        """, countQuery = """
                        SELECT COUNT(DISTINCT u.userId)
                        FROM ProjectMember pm
                        JOIN pm.user u
                        WHERE (:keyword IS NULL
                            OR LOWER(u.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                            OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')))
                        """)
        Page<MemberDTO> findAllMembers(
                        @Param("keyword") String keyword,
                        Pageable pageable);

        @Query("""
                            SELECT pm.project
                            FROM ProjectMember pm
                            WHERE pm.user.userId = :userId
                        """)
        List<Project> findProjectsByUserId(@Param("userId") Long userId);

        boolean existsByProject_ProjectIdAndUser_UserId(Long projectId, Long userId);

        @Query("""
                            select new com.devcollab.dto.MemberDTO(
                                u.userId,
                                u.name,
                                u.email,
                                u.avatarUrl,
                                pm.roleInProject
                            )
                            from ProjectMember pm
                            join pm.user u
                            where pm.project.projectId = :projectId
                            order by pm.joinedAt asc
                        """)
        List<MemberDTO> findMembersByProject(@Param("projectId") Long projectId);

        @Query("""
                            SELECT p FROM Project p
                            WHERE p.createdBy.email = :pmEmail
                        """)
        List<Project> findProjectsCreatedByPm(@Param("pmEmail") String pmEmail);

        @Query("""
                            SELECT u FROM User u
                            WHERE u.email = :email
                        """)
        List<User> findUserByEmail(@Param("email") String email);

        @Transactional
        @Modifying
        @Query("""
                            DELETE FROM ProjectMember pm
                            WHERE pm.project.projectId = :projectId
                            AND pm.user.userId = :userId
                        """)
        void deleteByProject_ProjectIdAndUser_UserId(
                        @Param("projectId") Long projectId,
                        @Param("userId") Long userId);

        @Transactional
        @Modifying
        @Query("""
                            UPDATE ProjectMember pm
                            SET pm.roleInProject = :role
                            WHERE pm.project.projectId = :projectId AND pm.user.userId = :userId
                        """)
        void updateMemberRole(
                        @Param("projectId") Long projectId,
                        @Param("userId") Long userId,
                        @Param("role") String role);

        @Transactional
        @Modifying
        @Query("""
                            DELETE FROM ProjectMember pm
                            WHERE pm.user.userId = :userId
                            AND pm.project.createdBy.email = :pmEmail
                        """)
        void deleteAllByUserIdAndPmEmail(
                        @Param("userId") Long userId,
                        @Param("pmEmail") String pmEmail);

        @Modifying
        @Query(value = """
                            INSERT INTO dbo.ProjectMember (project_id, user_id, role_in_project, joined_at)
                            VALUES (:projectId, :userId, :role, GETDATE())
                        """, nativeQuery = true)
        void addMember(
                        @Param("projectId") Long projectId,
                        @Param("userId") Long userId,
                        @Param("role") String role);

        @Query("""
                            SELECT COUNT(pm) > 0
                            FROM ProjectMember pm
                            WHERE pm.project.projectId = :projectId
                              AND pm.user.email = :email
                              AND pm.roleInProject IN :roles
                        """)
        boolean hasManagerPermission(
                        @Param("projectId") Long projectId,
                        @Param("email") String email,
                        @Param("roles") List<String> roles);

        // ✅ NEW: Gom logic Owner + PM/ADMIN (tiện dùng ở service)
        @Query("""
                            SELECT COUNT(pm) > 0
                            FROM ProjectMember pm
                            WHERE pm.project.projectId = :projectId
                              AND (
                                    pm.project.createdBy.email = :email
                                    OR (pm.user.email = :email AND pm.roleInProject IN :roles)
                              )
                        """)
        boolean hasProjectManagementPermission(
                        @Param("projectId") Long projectId,
                        @Param("email") String email,
                        @Param("roles") List<String> roles);

        @Query("""
                            SELECT new com.devcollab.dto.MemberDTO(
                                u.userId,
                                u.name,
                                u.email,
                                u.avatarUrl,
                                pm.roleInProject
                            )
                            FROM ProjectMember pm
                            JOIN pm.user u
                            WHERE pm.project.projectId = :projectId
                              AND (
                                    :keyword IS NULL
                                    OR :keyword = ''
                                    OR LOWER(u.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                                    OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
                              )
                            ORDER BY pm.joinedAt ASC
                        """)
        List<MemberDTO> searchMembersByProject(
                        @Param("projectId") Long projectId,
                        @Param("keyword") String keyword);

        @Query("""
                            SELECT pm.roleInProject
                            FROM ProjectMember pm
                            WHERE pm.project.projectId = :projectId
                              AND pm.user.userId = :userId
                        """)
        Optional<String> findRoleInProject(
                        @Param("projectId") Long projectId,
                        @Param("userId") Long userId);

        // ✅ NEW: Xóa thành viên khỏi project theo email (hỗ trợ "rời project")
        @Transactional
        @Modifying
        @Query("""
                            DELETE FROM ProjectMember pm
                            WHERE pm.project.projectId = :projectId
                              AND pm.user.email = :email
                        """)
        void deleteByProjectIdAndEmail(
                        @Param("projectId") Long projectId,
                        @Param("email") String email);

        @Query("""
                            SELECT DISTINCT new com.devcollab.dto.MemberDTO(
                                u.userId,
                                u.name,
                                u.email,
                                u.avatarUrl
                            )
                            FROM ProjectMember pm
                            JOIN pm.user u
                            JOIN pm.project p
                            WHERE p.createdBy.email = :pmEmail
                              AND (:keyword IS NULL OR LOWER(u.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                                                   OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')))
                            ORDER BY u.name ASC
                        """)
        Page<MemberDTO> findAllMembersByPmEmailPaged(
                        @Param("pmEmail") String pmEmail,
                        @Param("keyword") String keyword,
                        Pageable pageable);

        @Query("SELECT DISTINCT pm.project.projectId FROM ProjectMember pm WHERE pm.user.userId = :userId")
        List<Long> findProjectIdsByUserId(@Param("userId") Long userId);

        @Query("""
                    SELECT DISTINCT u FROM User u
                    WHERE u.userId <> :userId
                    AND u.userId IN (
                        SELECT pm2.user.userId
                        FROM ProjectMember pm2
                        WHERE pm2.project.projectId IN :projectIds
                    )
                """)
        List<User> findUsersByProjectIds(@Param("projectIds") List<Long> projectIds, @Param("userId") Long userId);

}
