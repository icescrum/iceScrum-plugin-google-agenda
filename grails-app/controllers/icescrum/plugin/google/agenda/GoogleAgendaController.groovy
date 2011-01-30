package icescrum.plugin.google.agenda

import grails.plugins.springsecurity.Secured

import com.google.gdata.client.calendar.CalendarService
import com.google.gdata.data.DateTime
import com.google.gdata.data.PlainTextConstruct
import com.google.gdata.data.calendar.CalendarEventEntry
import com.google.gdata.data.extensions.When
import com.google.gdata.util.AuthenticationException
import grails.converters.JSON
import org.icescrum.core.domain.Product
import org.icescrum.web.support.MenuBarSupport

@Secured('scrumMaster()')
class GoogleAgendaController {
    static final pluginName = 'ice-scrum-plugin-google-agenda'
    static final id = 'googleAgenda'
    static ui = true
    static menuBar = MenuBarSupport.productDynamicBar('is.googleAgenda.ui',id , false, 3)
    static window =  [title:'is.googleAgenda.ui',help:'is.googleAgenda.ui.help',toolbar:false]

    def index = {
        GoogleCalendarSettings projectAccount = GoogleCalendarSettings.findByProduct(Product.get(params.product))
        if (projectAccount) {
            render template:'displayAccount',
                  plugin:'iceScrum-plugin-google-agenda',
                  model:[id:id,login:projectAccount.login, displayDailyMeetings:projectAccount.displayDailyMeetings]
        }
        else {
            render template:'setAccount',
                plugin:'iceScrum-plugin-google-agenda',
                model:[id:id]
        }
    }

    def saveAccount = {
        GoogleCalendarSettings projectAccount = new GoogleCalendarSettings(login:params.googleLogin, password:params.googlePassword, product:Product.get(params.product))
        if(getConnection(params.googleLogin, params.googlePassword)) {
            projectAccount.save()
            redirect(action:'index',params:[product:params.product])
        }
        else
            render(status:400,contentType:'application/json', text: [notice: [text: message(code: 'is.googleAgenda.error.wrongCredentials')]] as JSON)

        println "setGoogleCalendarSettings has state > " + params.displaySettingsState
    }

    def setSettings = {
        GoogleCalendarSettings projectAccount = new GoogleCalendarSettings(login:params.googleLogin, password:params.googlePassword, product:Product.get(params.product))
        println "state onLoad > " + params.displayDailyMeetings
        projectAccount.displayDailyMeetings = (params.displaySettingsState) ? true : false
        if(projectAccount.save())
          render(status:200,contentType:'application/json', text: [notice: [text: message(code: 'is.googleAgenda.success.setGoogleCalendarSettings')]] as JSON)
        else
          render(status:400,contentType:'application/json', text: [notice: [text: message(code: 'is.googleAgenda.error.setGoogleCalendarSettings')]] as JSON)
    }

    def getConnection(login, password) {
        CalendarService googleService = new CalendarService("test")
        try {
          googleService.setUserCredentials(login, password);
        }
        catch (AuthenticationException e) {
          return false
        }
        return googleService
    }

    // Vider l'agenda !!
    // Gestion des erreurs !!!
    // Ajouter les scrum meetings si desirés !!!
    def updateCalendar = {
        GoogleCalendarSettings projectAccount = GoogleCalendarSettings.findByProduct(Product.get(params.product))
        CalendarService googleService = getConnection(projectAccount.login, projectAccount.password)

        addSprints (googleService)

        render(status:200,contentType:'application/json', text: [notice: [text: message(code: 'is.googleAgenda.success.updateCalendar')]] as JSON)
    }

    def addSprints (googleService) {
        int sprint = 1
        def product = Product.get(params.product);
        product.releases?.each { r->
            r.sprints.asList().each { s->
                createSingleEvent(googleService,
                              r.name + "-Sprint#" + sprint++,
                              "no comment",
                              iSDateToGoogleDate(s.startDate,true,false),
                              iSDateToGoogleDate(s.endDate,true,true))
            }
            sprint = 1
        }
    }

    def iSDateToGoogleDate (Date date, isAllDay, isEndDate) {
        // Google inclut la première exclut la deuxième date d'un évènement
        // on incrémente donc d'un jour la date de fin si c'est un évènement d'une journée
        // il faudrait de même incrémenter d'une minute la date de fin si c'est un évènement en heures
        if(isAllDay && isEndDate)
            date++
        DateTime googleDate = DateTime.parseDateTime(date.format("yyyy-MM-dd'T'HH:mm:ss"))
        if(isAllDay)
            googleDate.setDateOnly(true)
        return googleDate
    }

   def createSingleEvent(googleService, eventName, comment, startDate, endDate) {
        CalendarEventEntry newEvent = new CalendarEventEntry()
        newEvent.setTitle(new PlainTextConstruct(eventName))
        newEvent.setContent(new PlainTextConstruct(comment))
        When eventTimes = new When()
        eventTimes.setStartTime(startDate)
        eventTimes.setEndTime(endDate)
        newEvent.addTime(eventTimes)

        GoogleCalendarSettings projectAccount = GoogleCalendarSettings.findByProduct(Product.get(params.product))
        URL postUrl = new URL("https://www.google.com/calendar/feeds/"+projectAccount.login+"/private/full")

        return googleService.insert(postUrl, newEvent)
    }
}
