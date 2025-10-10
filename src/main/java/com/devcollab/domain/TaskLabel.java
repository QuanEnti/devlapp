package com.devcollab.domain;

import jakarta.persistence.*;
@Entity
@Table(name = "TaskLabel")
@IdClass(TaskLabelId.class)
public class TaskLabel {

    @Id
    @ManyToOne
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @Id
    @ManyToOne
    @JoinColumn(name = "label_id", nullable = false)
    private Label label;

    public TaskLabel() {
    }

    public Task getTask() {
        return task;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public Label getLabel() {
        return label;
    }

    public void setLabel(Label label) {
        this.label = label;
    }
}
