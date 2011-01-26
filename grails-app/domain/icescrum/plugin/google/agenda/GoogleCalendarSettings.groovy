package icescrum.plugin.google.agenda

import org.icescrum.core.domain.Product

class GoogleCalendarSettings {

    String login
    String password
    boolean displayMeetings = false

    static constraints = {
      login(nullable:false)
      password(nullable:false)
    }

    static belongsTo = [product:Product]
}