package com.captcha.toolkit.generator;

import com.captcha.toolkit.CaptchaType;
import com.captcha.toolkit.SliderConfig;
import com.captcha.toolkit.model.CaptchaAnswer;
import com.captcha.toolkit.model.CaptchaSession;
import com.captcha.toolkit.model.GeneratedCaptcha;
import com.captcha.toolkit.model.PointVo;
import com.captcha.toolkit.model.VerifyResult;
import com.captcha.toolkit.render.BackgroundProvider;
import com.captcha.toolkit.render.SliderRenderer;
import com.captcha.toolkit.shape.PuzzleShapeRegistry;

import java.util.List;

/**
 * 滑块拼图验证码生成器。
 */
public class SliderCaptchaGenerator extends AbstractCaptchaGenerator {

    private final SliderConfig options;
    private final BackgroundProvider backgroundProvider;
    private final PuzzleShapeRegistry shapeRegistry;

    public SliderCaptchaGenerator(SliderConfig options,
                                  BackgroundProvider backgroundProvider,
                                  PuzzleShapeRegistry shapeRegistry) {
        this.options = options;
        this.backgroundProvider = backgroundProvider;
        this.shapeRegistry = shapeRegistry;
    }

    @Override
    public CaptchaType type() {
        return CaptchaType.SLIDER;
    }

    @Override
    protected GeneratedCaptcha doGenerate(GenerateRequest request) {
        String shape = resolveShape(request.getParams().get("shape"));
        SliderRenderer renderer = new SliderRenderer(options, backgroundProvider, shapeRegistry);
        renderer.setShape(shape);
        renderer.run();

        CaptchaSession session = CaptchaSession.slider(
                request.getId(), shape, renderer.getX(), renderer.getY(),
                renderer.getWidth(), renderer.getHeight(), options.getExpireSeconds() * 1000);

        GeneratedCaptcha result = new GeneratedCaptcha();
        result.setSession(session);
        result.setImage1(renderer.getArtwork());
        result.setImage2(renderer.getVacancy());
        result.setWidth(renderer.getWidth());
        result.setHeight(renderer.getHeight());
        result.setShape(shape);
        result.setPieceOffsetX(renderer.getPieceOffsetX());
        if (request.isDebug()) {
            result.setDebugX(renderer.getX());
            result.setDebugFakeTargets(renderer.getFakeTargets().stream()
                    .map(p -> new PointVo(p.x, p.y))
                    .toList());
        }
        return result;
    }

    @Override
    protected VerifyResult doVerify(CaptchaSession session, CaptchaAnswer answer) {
        if (answer == null || answer.getX() == null) {
            return VerifyResult.badRequest("缺少滑块位移 x");
        }
        int clientWidth = answer.getClientWidth() == null
                ? session.getWidth()
                : answer.getClientWidth();
        if (clientWidth <= 0) {
            return VerifyResult.badRequest("clientWidth 不合法");
        }
        // 前端图片可能被 CSS 缩放，按宽度比例换算回服务端坐标
        double ratio = (double) session.getWidth() / clientWidth;
        if (Math.abs(session.getX() - answer.getX() * ratio) <= options.getTolerance() * ratio) {
            return VerifyResult.ok("验证通过");
        }
        return VerifyResult.fail("验证失败，请重试", "WRONG");
    }

    @Override
    protected long minElapsedMs() {
        return options.getMinElapsedMs();
    }

    private String resolveShape(String requested) {
        if (requested != null && options.getEnabledShapes().contains(requested)
                && shapeRegistry.contains(requested)) {
            return requested;
        }
        String fallback = options.getDefaultShape();
        if (shapeRegistry.contains(fallback) && options.getEnabledShapes().contains(fallback)) {
            return fallback;
        }
        return "classic";
    }

    public List<String> getShapeNames() {
        return options.getEnabledShapes().stream()
                .filter(shapeRegistry::contains)
                .toList();
    }
}
