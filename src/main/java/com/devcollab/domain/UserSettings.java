package com.devcollab.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "[UserSettings]")
public class UserSettings {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "settings_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    @Column(nullable = false)
    private boolean emailEnabled = true; // (3) Bật/tắt toàn bộ email

    @Column(nullable = false)
    private boolean emailHighImmediate = true; // (1) Gửi ngay noti HIGH

    @Column(nullable = false)
    private boolean emailDigestEnabled = true; // (2) Bật digest

    @Column(nullable = false)
    private int emailDigestEveryHours = 2; // (2) Tần suất: 2 / 4 / 6

    @Column(name = "last_digest_at")
    private LocalDateTime lastDigestAt; // Lần gửi digest gần nhất

    public UserSettings() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public boolean isEmailEnabled() {
        return emailEnabled;
    }

    public void setEmailEnabled(boolean emailEnabled) {
        this.emailEnabled = emailEnabled;
    }

    public boolean isEmailHighImmediate() {
        return emailHighImmediate;
    }

    public void setEmailHighImmediate(boolean emailHighImmediate) {
        this.emailHighImmediate = emailHighImmediate;
    }

    public boolean isEmailDigestEnabled() {
        return emailDigestEnabled;
    }

    public void setEmailDigestEnabled(boolean emailDigestEnabled) {
        this.emailDigestEnabled = emailDigestEnabled;
    }

    public int getEmailDigestEveryHours() {
        return emailDigestEveryHours;
    }

    public void setEmailDigestEveryHours(int emailDigestEveryHours) {
        this.emailDigestEveryHours = emailDigestEveryHours;
    }

    public LocalDateTime getLastDigestAt() {
        return lastDigestAt;
    }

    public void setLastDigestAt(LocalDateTime lastDigestAt) {
        this.lastDigestAt = lastDigestAt;
    }

    
}
