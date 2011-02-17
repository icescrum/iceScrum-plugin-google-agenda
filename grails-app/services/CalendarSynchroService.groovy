import org.springframework.context.ApplicationListener
import org.icescrum.core.event.IceScrumEvent
import org.icescrum.core.event.IceScrumSprintEvent
import org.icescrum.core.event.IceScrumReleaseEvent
import org.icescrum.core.domain.Release
import org.icescrum.core.domain.Product
import org.icescrum.core.domain.Sprint
import org.icescrum.core.domain.User
import org.apache.commons.lang.ObjectUtils.Null
import icescrum.plugin.google.agenda.GoogleCalendarSettings

class CalendarSynchroService implements ApplicationListener<IceScrumEvent> {

    def calendarEventService

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
            if (log.debugEnabled) exception.printStackTrace()
        }
    }

    private void manageReleaseEvent (Release release, User author, String type) {
        println "Received release event: "+ release.id + "type:" + type
        if (type != IceScrumEvent.EVENT_DELETED) {
            Product product = release.parentProduct
            GoogleCalendarSettings googleSettings = GoogleCalendarSettings.findByProduct(product)
            if (googleSettings) {
                if (type == IceScrumEvent.EVENT_UPDATED) {
                    //Fake hibernate session
                    product = Product.get(product.id)
                    author = User.get(author.id)
                    calendarEventService.updateWholeCalendar(product, author.preferences.language)
                    println "Update calendar after release update"
                }
            }
        }
    }

    private void manageSprintEvent (Sprint sprint, User author, String type) {
        println "Received sprint event: " + sprint.id + "type:" + type
        if (type != IceScrumEvent.EVENT_DELETED) {
            //Fake hibernate session
            author = User.get(author.id)
            sprint = Sprint.get(sprint.id)
            def product = Product.get(sprint.parentRelease.parentProduct.id)

            GoogleCalendarSettings googleSettings = GoogleCalendarSettings.findByProduct(product)
            if (googleSettings) {
                if (type == IceScrumEvent.EVENT_UPDATED) {
                    calendarEventService.updateWholeCalendar(product, author.preferences.language)
                    println "Update calendar after sprint update"
                }
                else if (type == IceScrumEvent.EVENT_CREATED) {
                    // Chercher dynamiquement le bon numero de sprint
                    calendarEventService.addSprint(product, sprint, 28, sprint.parentRelease.name)
                    println "Add sprint after sprint creation"
                }
                else if(type == IceScrumSprintEvent.EVENT_ACTIVATED) {
                    calendarEventService.addSprintMeetings(product, sprint, author.preferences.language)
                    println "Add meetings after sprint activation"
                }
            }
        }
    }
}