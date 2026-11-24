package com.pnkx.web.controller.blog;
import com.pnkx.blog.domain.po.PxAnalyticsEvent;
import com.pnkx.web.controller.blog.client.PxClientAnalyticsController;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
class AnalyticsValidationTest {
    private PxAnalyticsEvent event() {
        PxAnalyticsEvent e=new PxAnalyticsEvent();
        e.setEventId(UUID.randomUUID().toString()); e.setVisitorId(UUID.randomUUID().toString());
        e.setSessionId(UUID.randomUUID().toString()); e.setPageViewId(UUID.randomUUID().toString());
        e.setEventName("page_view"); e.setPageKey("home"); e.setDurationMs(0L); e.setOccurredAt(System.currentTimeMillis());
        return e;
    }
    @Test void validEvent() { assertDoesNotThrow(() -> PxClientAnalyticsController.validate(event(),System.currentTimeMillis())); }
    @Test void rejectsUnknownEvents() { var e=event();e.setEventName("private_message");assertThrows(ResponseStatusException.class,()->PxClientAnalyticsController.validate(e,System.currentTimeMillis())); }
    @Test void rejectsUrlInContentId() { var e=event();e.setContentId("token=secret");assertThrows(ResponseStatusException.class,()->PxClientAnalyticsController.validate(e,System.currentTimeMillis())); }
    @Test void rejectsDurationOnClick() { var e=event();e.setDurationMs(1000L);assertThrows(ResponseStatusException.class,()->PxClientAnalyticsController.validate(e,System.currentTimeMillis())); }
    @Test void rejectsStaleEvent() { var e=event();e.setOccurredAt(0L);assertThrows(ResponseStatusException.class,()->PxClientAnalyticsController.validate(e,System.currentTimeMillis())); }
    @Test void rejectsMissingIdentity() { var e=event();e.setVisitorId(null);assertThrows(ResponseStatusException.class,()->PxClientAnalyticsController.validate(e,System.currentTimeMillis())); }
    @Test void acceptsVideoTarget() { var e=event();e.setEventName("video_start");e.setTargetId("123");assertDoesNotThrow(()->PxClientAnalyticsController.validate(e,System.currentTimeMillis())); }
    @Test void rejectsSensitiveTarget() { var e=event();e.setTargetId("email@example.com");assertThrows(ResponseStatusException.class,()->PxClientAnalyticsController.validate(e,System.currentTimeMillis())); }
    @Test void acceptsReadingMilestone() { var e=event();e.setEventName("read_75");e.setPageKey("post_detail");assertDoesNotThrow(()->PxClientAnalyticsController.validate(e,System.currentTimeMillis())); }
    @Test void acceptsLoginAndReplyOutcomes() {
        for (String name : new String[]{"register_submit","register_success","register_fail","activation_submit","activation_success","activation_fail","login_submit","login_success","login_fail","reply_submit","reply_success","reply_fail"}) {
            var e=event();e.setEventName(name);
            assertDoesNotThrow(()->PxClientAnalyticsController.validate(e,System.currentTimeMillis()));
        }
    }
}
