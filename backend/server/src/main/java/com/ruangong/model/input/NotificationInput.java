package com.ruangong.model.input;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class NotificationInput {

    private Long id;
    @NotBlank
    private String title;

    @NotBlank
    private String type;

    @NotBlank
    private String content;

    @NotBlank
    private String channel;

    private String scheduledAt;

    @NotEmpty
    @Valid
    private List<NotificationTargetInput> targets;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(String scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public List<NotificationTargetInput> getTargets() {
        return targets;
    }

    public void setTargets(List<NotificationTargetInput> targets) {
        this.targets = targets;
    }
}
