package com.pnkx.domain.po;

import com.pnkx.common.annotation.Excel;
import com.pnkx.common.core.domain.BaseEntity;

/**
 * @author by PHY
 * @Classname PxToDo
 * @date 2021-04-13 11:46
 */
public class PxToDo extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private Long id;

    /**
     * 内容
     */
    @Excel(name = "内容")
    private String content;

    /**
     * 执行者
     */
    @Excel(name = "执行者")
    private String performer;

    /**
     * 计划开始时间
     */
    @Excel(name = "计划开始时间")
    private String planStartTime;

    /**
     * 计划结束时间
     */
    @Excel(name = "计划结束时间")
    private String planEndTime;

    /**
     * 状态(0未完成；1已完成)
     */
    @Excel(name = "状态(0未完成；1已完成)")
    private Boolean status;

    /**
     * 标签
     */
    @Excel(name = "标签")
    private String label;

    /**
     * 版本号
     */
    @Excel(name = "版本号")
    private String version;

    /**
     * 客户端唯一标识（离线幂等去重）
     */
    private String clientUuid;

    /**
     * 完成人
     */
    @Excel(name = "完成人")
    private String finishBy;

    /**
     * 完成时间
     */
    @Excel(name = "完成时间")
    private String finishTime;

    /**
     * 优先级（0无 1低 2中 3高 4紧急）
     */
    @Excel(name = "优先级")
    private Integer priority;

    /**
     * 看板状态（0待办 1进行中 2已完成）
     */
    @Excel(name = "看板状态")
    private Integer kanbanStatus;

    /**
     * 父任务ID（0=顶级任务）
     */
    private Long parentId;

    /**
     * 看板列内排序
     */
    private Integer sortOrder;

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    public void setPerformer(String performer) {
        this.performer = performer;
    }

    public String getPerformer() {
        return performer;
    }

    public void setPlanStartTime(String planStartTime) {
        this.planStartTime = planStartTime;
    }

    public String getPlanStartTime() {
        return planStartTime;
    }

    public void setPlanEndTime(String planEndTime) {
        this.planEndTime = planEndTime;
    }

    public String getPlanEndTime() {
        return planEndTime;
    }

    public Boolean getStatus() {
        return status;
    }

    public void setStatus(Boolean status) {
        this.status = status;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }

    public String getClientUuid() {
        return clientUuid;
    }

    public void setClientUuid(String clientUuid) {
        this.clientUuid = clientUuid;
    }

    public void setFinishBy(String finishBy) {
        this.finishBy = finishBy;
    }

    public String getFinishBy() {
        return finishBy;
    }

    public void setFinishTime(String finishTime) {
        this.finishTime = finishTime;
    }

    public String getFinishTime() {
        return finishTime;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public Integer getKanbanStatus() {
        return kanbanStatus;
    }

    public void setKanbanStatus(Integer kanbanStatus) {
        this.kanbanStatus = kanbanStatus;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    @Override
    public String toString() {
        return "PxToDo{" +
                "id=" + id +
                ", content='" + content + '\'' +
                ", performer='" + performer + '\'' +
                ", planStartTime='" + planStartTime + '\'' +
                ", planEndTime='" + planEndTime + '\'' +
                ", status='" + status + '\'' +
                ", label='" + label + '\'' +
                ", version='" + version + '\'' +
                ", finishBy='" + finishBy + '\'' +
                ", finishTime='" + finishTime + '\'' +
                '}';
    }
}
