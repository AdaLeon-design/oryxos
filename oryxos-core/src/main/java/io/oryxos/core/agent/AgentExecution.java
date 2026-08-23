package io.oryxos.core.agent;

import java.time.Instant;

/**
 * Agent 维度的一次执行记录（第 32 节）：手动触发 / 定时触发都记，含起止时间与状态。
 *
 * <p>流式工作台复用同一主键作为 Run ID。存量记录没有显式 status 时，仍按 {@code endedAt}/{@code success} 推导。
 */
public record AgentExecution(
    long id,
    String agentName,
    String source,
    String sessionId,
    Instant startedAt,
    Instant endedAt,
    Boolean success,
    Long durationMs,
    String errorMessage,
    Instant updatedAt,
    String inputPreview,
    Instant cancelRequestedAt,
    String persistedStatus,
    String stopReason) {

  public AgentExecution(
      long id,
      String agentName,
      String source,
      String sessionId,
      Instant startedAt,
      Instant endedAt,
      Boolean success,
      Long durationMs,
      String errorMessage) {
    this(
        id,
        agentName,
        source,
        sessionId,
        startedAt,
        endedAt,
        success,
        durationMs,
        errorMessage,
        endedAt != null ? endedAt : startedAt,
        null,
        null,
        null,
        null);
  }

  public AgentExecution(
      long id,
      String agentName,
      String source,
      String sessionId,
      Instant startedAt,
      Instant endedAt,
      Boolean success,
      Long durationMs,
      String errorMessage,
      Instant updatedAt,
      String inputPreview,
      Instant cancelRequestedAt,
      String persistedStatus) {
    this(
        id,
        agentName,
        source,
        sessionId,
        startedAt,
        endedAt,
        success,
        durationMs,
        errorMessage,
        updatedAt,
        inputPreview,
        cancelRequestedAt,
        persistedStatus,
        null);
  }

  /** 运行中 / 成功 / 失败 / 取消——供前端直接展示。 */
  public String status() {
    if (persistedStatus != null && !persistedStatus.isBlank()) {
      return persistedStatus;
    }
    if (cancelRequestedAt != null && endedAt == null) {
      return "CANCELLING";
    }
    if (endedAt == null) {
      return "RUNNING";
    }
    return Boolean.TRUE.equals(success) ? "SUCCESS" : "FAILED";
  }

  public Instant lastUpdatedAt() {
    if (updatedAt != null) {
      return updatedAt;
    }
    return endedAt != null ? endedAt : startedAt;
  }

  public boolean terminal() {
    String status = status();
    return "SUCCESS".equals(status) || "FAILED".equals(status) || "CANCELLED".equals(status);
  }

  public boolean cancellable() {
    String status = status();
    return "QUEUED".equals(status) || "RUNNING".equals(status);
  }
}
