package cc.intmain.nawyki.application;

import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/** Immutable invalidations, published inside the write transaction where one exists. */
@Component
public class JournalChanges {
    public enum Channel { HABITS, WORKOUTS }
    public record Change(UUID account, Channel channel) {}
    private final ApplicationEventPublisher publisher;
    public JournalChanges(ApplicationEventPublisher publisher) { this.publisher = publisher; }
    public void habits(UUID account) { publisher.publishEvent(new Change(account, Channel.HABITS)); }
    public void workouts(UUID account) { publisher.publishEvent(new Change(account, Channel.WORKOUTS)); }
}
