import org.springframework.context.ApplicationListener
import org.icescrum.core.event.IceScrumEvent
import org.icescrum.core.event.IceScrumSprintEvent
import org.icescrum.core.event.IceScrumReleaseEvent
import org.icescrum.core.domain.Release
import org.icescrum.core.domain.Sprint
import org.icescrum.core.domain.User

class CalendarSynchroService implements ApplicationListener<IceScrumEvent> {

    void onApplicationEvent(IceScrumEvent e) {
        try {
            if(e.source instanceof Release){
                manageReleaseEvent((Release)e.source, (User)e.doneBy, e.type)
            }
            else if(e.source instanceof Sprint){
                manageSprintEvent((Sprint)e.source, (User)e.doneBy, e.type)
            }
        }
        catch(Exception exception){
            exception.printStackTrace()
        }
    }

    private void manageReleaseEvent (Release release, User author, String type) {
        println "Received release event: "+ release.id + "type:" + type
    }

    private void manageSprintEvent (Sprint sprint, User author, String type) {
        println "Received sprint event: " + sprint.id + "type:" + type
    }
}