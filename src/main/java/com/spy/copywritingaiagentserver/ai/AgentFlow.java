package com.spy.copywritingaiagentserver.ai;

import com.spy.copywritingaiagentserver.ai.agent.*;
import com.spy.copywritingaiagentserver.ai.model.ContentPlanResult;
import com.spy.copywritingaiagentserver.ai.model.CopywritingResult;
import com.spy.copywritingaiagentserver.ai.model.FinalPostResult;
import com.spy.copywritingaiagentserver.ai.model.RequirementParseResult;
import com.spy.copywritingaiagentserver.ai.model.ReviewResult;
import com.spy.copywritingaiagentserver.ai.model.UserRequirement;
import com.spy.copywritingaiagentserver.ai.model.VisualPromptResult;
import com.spy.copywritingaiagentserver.ai.service.ImageGenerationService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 总控编排器：
 * UserRequirement
 * -> RequirementAgent
 * -> PlannerAgent
 * -> CopywriterAgent
 * -> VisualPromptAgent
 * -> ImageGenerationService
 * -> ReviewerAgent
 * -> 如果不通过则局部修正
 * -> FinalPostResult
 */
@Slf4j
@Component
public class AgentFlow {

    @Resource
    private RequirementAgent requirementAgent;

    @Resource
    private PlannerAgent plannerAgent;

    @Resource
    private CopywriterAgent copywriterAgent;

    @Resource
    private VisualPromptAgent visualPromptAgent;

    @Resource
    private ReviewerAgent reviewerAgent;

    @Resource
    private ImageGenerationService imageGenerationService;

    /**
     * 最大局部修正次数
     */
    private static final int MAX_RETRY = 2;

    public FinalPostResult generate(UserRequirement userRequirement) {
        try {
            log.info("========== Agent 工作流开始 ==========");

            // 1. 需求解析
            RequirementParseResult requirementParseResult = requirementAgent.execute(userRequirement);

            // 2. 内容策划
            ContentPlanResult contentPlanResult = plannerAgent.execute(requirementParseResult);

            // 3. 文案生成
            CopywritingResult copywritingResult = copywriterAgent.execute(requirementParseResult, contentPlanResult);

            // 4. 视觉提示词生成
            VisualPromptResult visualPromptResult = visualPromptAgent.execute(
                    requirementParseResult,
                    contentPlanResult,
                    copywritingResult
            );

            // 5. 首次图片生成
            String imageUrl = imageGenerationService.generate(visualPromptResult);

            // 6. 首次评审
            ReviewResult reviewResult = reviewerAgent.execute(
                    requirementParseResult,
                    contentPlanResult,
                    copywritingResult,
                    visualPromptResult,
                    imageUrl
            );

            log.info("首次评审结果: pass={}, overallScore={}, feedback={}",
                    reviewResult.getPass(),
                    reviewResult.getOverallScore(),
                    reviewResult.getFeedback());

            // 7. 局部修正闭环
            int retry = 0;
            while (!reviewResult.getPass() && retry < MAX_RETRY) {
                log.info("========== 开始第 {} 次局部修正 ==========", retry + 1);

                boolean changed = false;

                // 7.1 重写标题
                if (reviewResult.isRewriteTitle()) {
                    log.info("触发标题重写, feedback={}", reviewResult.getTitleFeedback());

                    String newTitle = copywriterAgent.rewriteTitle(
                            requirementParseResult,
                            contentPlanResult,
                            copywritingResult,
                            reviewResult.getTitleFeedback()
                    );

                    copywritingResult.setTitle(newTitle);
                    changed = true;

                    log.info("标题重写完成: {}", newTitle);
                }

                // 7.2 重写 CTA
                if (reviewResult.isRewriteCta()) {
                    log.info("触发 CTA 重写, feedback={}", reviewResult.getCtaFeedback());

                    String newCta = copywriterAgent.rewriteCta(
                            requirementParseResult,
                            contentPlanResult,
                            copywritingResult,
                            reviewResult.getCtaFeedback()
                    );

                    copywritingResult.setCta(newCta);
                    changed = true;

                    log.info("CTA 重写完成: {}", newCta);
                }

                // 7.3 重写图片提示词
                if (reviewResult.isRewriteImagePrompt()) {
                    log.info("触发图片提示词重写, feedback={}", reviewResult.getImagePromptFeedback());

                    String newImagePrompt = visualPromptAgent.rewriteImagePrompt(
                            requirementParseResult,
                            contentPlanResult,
                            copywritingResult,
                            visualPromptResult,
                            reviewResult.getImagePromptFeedback()
                    );

                    visualPromptResult.setImagePrompt(newImagePrompt);
                    changed = true;

                    log.info("图片提示词重写完成: {}", newImagePrompt);
                }

                // 如果这一轮没有任何字段需要改，就直接退出，避免空循环
                if (!changed) {
                    log.info("本轮没有可修正项，提前结束闭环。");
                    break;
                }

                // 7.4 重生成图片
                imageUrl = imageGenerationService.generate(visualPromptResult);

                // 7.5 重新评审
                reviewResult = reviewerAgent.execute(
                        requirementParseResult,
                        contentPlanResult,
                        copywritingResult,
                        visualPromptResult,
                        imageUrl
                );

                log.info("第 {} 次修正后评审结果: pass={}, overallScore={}, feedback={}",
                        retry + 1,
                        reviewResult.getPass(),
                        reviewResult.getOverallScore(),
                        reviewResult.getFeedback());

                retry++;
            }

            // 8. 聚合最终结果
            FinalPostResult finalPostResult = new FinalPostResult();
            finalPostResult.setRequirementParseResult(requirementParseResult);
            finalPostResult.setContentPlanResult(contentPlanResult);
            finalPostResult.setCopywritingResult(copywritingResult);
            finalPostResult.setVisualPromptResult(visualPromptResult);
            finalPostResult.setImageUrl(imageUrl);
            finalPostResult.setReviewResult(reviewResult);

            log.info("========== Agent 工作流结束, 最终 pass={}, overallScore={} ==========",
                    reviewResult.getPass(),
                    reviewResult.getOverallScore());

            return finalPostResult;

        } catch (Exception e) {
            log.error("Agent 工作流执行失败", e);
            throw new RuntimeException("Agent 工作流执行失败", e);
        }
    }
}