package com.devcollab.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "[NotificationPreference]")
public class NotificationPreference {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "email_on", nullable = false)
    private Boolean emailOn = true;

    @Column(name = "inapp_on", nullable = false)
    private Boolean inappOn = true;

    @Column(name = "push_on", nullable = false)
    private Boolean pushOn = false;

    @Column(name = "digest_daily_on", nullable = false)
    private Boolean digestDailyOn = false;

    public NotificationPreference() {
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Boolean getEmailOn() {
        return emailOn;
    }

    public void setEmailOn(Boolean emailOn) {
        this.emailOn = emailOn;
    }

    public Boolean getInappOn() {
        return inappOn;
    }

    public void setInappOn(Boolean inappOn) {
        this.inappOn = inappOn;
    }

    public Boolean getPushOn() {
        return pushOn;
    }

    public void setPushOn(Boolean pushOn) {
        this.pushOn = pushOn;
    }

    public Boolean getDigestDailyOn() {
        return digestDailyOn;
    }

    public void setDigestDailyOn(Boolean digestDailyOn) {
        this.digestDailyOn = digestDailyOn;
    }
}
