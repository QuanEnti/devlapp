package com.devcollab.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "[Project]", schema = "dbo")
public class Project {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "project_id")
    private Long projectId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String description;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(length = 16, nullable = false)
    private String status = "Active"; // Active | Archived

    @Column(length = 16)
    private String priority = "Normal"; // High | Normal | Low

    @Column(length = 16)
    private String visibility = "private"; // private | public

    @Column(name = "cover_image", columnDefinition = "NVARCHAR(500)")
    private String coverImage;

    @ManyToOne
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProjectMember> members = new ArrayList<>();

    @Column(name = "invite_link", length = 200, unique = true)
    private String inviteLink;

    @Column(name = "allow_link_join", nullable = false)
    private boolean allowLinkJoin = false;

    @Column(name = "invite_auto_regen", nullable = false)
    private boolean inviteAutoRegen = true;

    @Column(name = "invite_created_at")
    private LocalDateTime inviteCreatedAt;

    @Column(name = "invite_expired_at")
    private LocalDateTime inviteExpiredAt;

    @Column(name = "invite_usage_count")
    private Integer inviteUsageCount = 0; // số lượng người đã dùng link

    @Column(name = "invite_max_uses")
    private Integer inviteMaxUses = 10; // giới hạn số lần sử dụng link
    // ai là người bật / tạo link mời gần nhất
    @Column(name = "invite_created_by", length = 150)
    private String inviteCreatedBy; // lưu email hoặc userId người tạo link

    public Project() {}

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public String getCoverImage() {
        return coverImage;
    }

    public void setCoverImage(String coverImage) {
        this.coverImage = coverImage;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getArchivedAt() {
        return archivedAt;
    }

    public void setArchivedAt(LocalDateTime archivedAt) {
        this.archivedAt = archivedAt;
    }

    public List<ProjectMember> getMembers() {
        return members;
    }

    public void setMembers(List<ProjectMember> members) {
        this.members = members;
    }

    public String getInviteLink() {
        return inviteLink;
    }

    public void setInviteLink(String inviteLink) {
        this.inviteLink = inviteLink;
    }

    public boolean isAllowLinkJoin() {
        return allowLinkJoin;
    }

    public void setAllowLinkJoin(boolean allowLinkJoin) {
        this.allowLinkJoin = allowLinkJoin;
    }

    public boolean isInviteAutoRegen() {
        return inviteAutoRegen;
    }

    public void setInviteAutoRegen(boolean inviteAutoRegen) {
        this.inviteAutoRegen = inviteAutoRegen;
    }

    public LocalDateTime getInviteCreatedAt() {
        return inviteCreatedAt;
    }

    public void setInviteCreatedAt(LocalDateTime inviteCreatedAt) {
        this.inviteCreatedAt = inviteCreatedAt;
    }

    public LocalDateTime getInviteExpiredAt() {
        return inviteExpiredAt;
    }

    public void setInviteExpiredAt(LocalDateTime inviteExpiredAt) {
        this.inviteExpiredAt = inviteExpiredAt;
    }

    public Integer getInviteUsageCount() {
        return inviteUsageCount != null ? inviteUsageCount : 0;
    }

    public void setInviteUsageCount(Integer inviteUsageCount) {
        this.inviteUsageCount = inviteUsageCount;
    }

    public Integer getInviteMaxUses() {
        return inviteMaxUses != null ? inviteMaxUses : 10;
    }

    public void setInviteMaxUses(Integer inviteMaxUses) {
        this.inviteMaxUses = inviteMaxUses;
    }

    public String getInviteCreatedBy() {
        return inviteCreatedBy;
    }

    public void setInviteCreatedBy(String inviteCreatedBy) {
        this.inviteCreatedBy = inviteCreatedBy;
    }

}
