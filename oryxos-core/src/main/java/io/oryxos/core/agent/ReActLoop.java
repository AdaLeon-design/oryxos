package io.oryxos.core.agent;

import io.oryxos.core.ToolResult;
import io.oryxos.core.profile.Profile;
import io.oryxos.core.provider.ProviderRequest;
import io.oryxos.core.provider.ProviderResponse;
import io.oryxos.core.provider.ProviderService;
import io.oryxos.core.provider.ToolCallRequest;
import io.oryxos.core.session.Session;

/**
 * ReAct 主循环——Agent 的大脑（宪法 I：自实现，不用框架 Agent 封装）。
 *
 * <p>循环只做调度：转圈、判停、把每轮结果攒起来。拼上下文归 {@link PromptBuilder}、 调模型归 {@link ProviderService}、执行工具归 {@link
 * ToolExecutor}——循环里塞的东西越少越不容易出 bug。
 */
public class ReActLoop {

  /** 转满最大轮数的强制收尾答复（课件字面量，harness 断言点）。 */
  static final String MAX_ITERATIONS_REPLY = "达到最大轮数，已停止";

  static final String CONVERGENCE_HINT = "预算即将耗尽：停止扩大调查范围，优先基于已有工具结果形成最终回答，不要再发起非必要的工具调用。";

  private static final int CONVERGENCE_REMAINING_THRESHOLD = 2;

  private final PromptBuilder promptBuilder;
  private final ProviderService providerService;
  private final ToolExecutor toolExecutor;
  private final AgentRunEventPublisher events;

  public ReActLoop(
      PromptBuilder promptBuilder, ProviderService providerService, ToolExecutor toolExecutor) {
    this(promptBuilder, providerService, toolExecutor, null);
  }

  public ReActLoop(
      PromptBuilder promptBuilder,
      ProviderService providerService,
      ToolExecutor toolExecutor,
      AgentRunEventPublisher events) {
    this.promptBuilder = promptBuilder;
    this.providerService = providerService;
    this.toolExecutor = toolExecutor;
    this.events = events;
  }

  public String run(Session session, String userMessage, Profile profile) {
    session.appendUser(userMessage);
    // 最大轮数兜底（坑一）：模型可能反复要调工具永不收敛，转够强制退出
    for (int i = 0; i < profile.settings().maxIterations(); i++) {
      checkCancel();
      long stepStarted = System.currentTimeMillis();
      publish(
          AgentRunEventTypes.STEP_STARTED,
          java.util.Map.of("step", i + 1, "iteration", i + 1, "kind", "model"));
      ProviderRequest prompt = promptBuilder.build(session, profile);
      int remaining = profile.settings().maxIterations() - i;
      if (remaining <= CONVERGENCE_REMAINING_THRESHOLD) {
        String system =
            (prompt.systemPrompt() == null ? "" : prompt.systemPrompt()) + "\n" + CONVERGENCE_HINT;
        prompt = new ProviderRequest(system, prompt.messages(), prompt.availableTools());
      }
      // sessionId 随调用传递：llm_calls 审计按 session 关联
      ProviderResponse response = providerService.chat(session.sessionId(), profile, prompt);
      // 先累积再判停（坑三）：每一轮都留痕，事后可审计、下一轮接得上
      session.appendAssistant(response);
      if (!response.hasToolCalls()) {
        String text = response.text() == null ? "" : response.text();
        if (!text.isEmpty()) {
          publish(
              AgentRunEventTypes.MESSAGE_CONTENT,
              java.util.Map.of("messageId", "run-answer", "delta", text));
        }
        publish(
            AgentRunEventTypes.STEP_FINISHED,
            java.util.Map.of(
                "step",
                i + 1,
                "iteration",
                i + 1,
                "durationMs",
                System.currentTimeMillis() - stepStarted));
        return text;
      }
      for (ToolCallRequest call : response.toolCalls()) {
        checkCancel();
        // 执行权只在 ToolExecutor（宪法 I/II）；失败结果同样回填，模型下一轮自行决定
        // 传 profile.name() 作为 Agent 名：记忆类工具据此落到本 Agent 专属 MEMORY.md（30 节）
        ToolResult result = toolExecutor.execute(session.sessionId(), profile.name(), call);
        session.appendToolResult(call, result);
      }
      publish(
          AgentRunEventTypes.STEP_FINISHED,
          java.util.Map.of(
              "step",
              i + 1,
              "iteration",
              i + 1,
              "durationMs",
              System.currentTimeMillis() - stepStarted));
    }
    return MAX_ITERATIONS_REPLY;
  }

  private static void checkCancel() {
    if (AgentRunExecutionContext.isCancelRequested()) {
      throw new RunCancelledException();
    }
  }

  private void publish(String type, java.util.Map<String, Object> payload) {
    if (events == null) {
      return;
    }
    events.publishCurrent(type, payload);
  }
}
