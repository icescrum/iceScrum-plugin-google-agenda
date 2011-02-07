package icescrum.plugin.google.agenda

import com.google.gdata.client.calendar.CalendarService
import com.google.gdata.data.DateTime
import com.google.gdata.data.calendar.CalendarEventEntry
import com.google.gdata.data.extensions.When
import com.google.gdata.data.extensions.Recurrence

import grails.converters.JSON
import grails.plugins.springsecurity.Secured

import org.icescrum.core.domain.Product
import org.icescrum.core.domain.Sprint
import org.icescrum.core.domain.preferences.ProductPreferences

import org.icescrum.web.support.MenuBarSupport

@Secured('scrumMaster()')
class GoogleAgendaController {

    static final pluginName = 'icescrum-plugin-google-agenda'
    static final id = 'googleAgenda'
    static ui = true
    static menuBar = MenuBarSupport.productDynamicBar('is.googleAgenda.ui',id , false, 3)
    static window =  [title:'is.googleAgenda.ui',help:'is.googleAgenda.ui.help',toolbar:false]

    def SMALL_SPRINT_DURATION = 7
    def CALENDAR_NAME = "iceScrum"

    def googleCalendarService

    def index = {
        GoogleCalendarSettings googleSettings = GoogleCalendarSettings.findByProduct(Product.get(params.product))
        if (googleSettings) {
            render template:'settings',
                  plugin:pluginName,
                  model:[id:id,
                         login:googleSettings.login,
                         displayDailyMeetings:googleSettings.displayDailyMeetings,
                         displaySprintReview:googleSettings.displaySprintReview,
                         displaySprintRetrospective:googleSettings.displaySprintRetrospective,
                         displayReleasePlanning:googleSettings.displayReleasePlanning,
                         displaySprintPlanning:googleSettings.displaySprintPlanning]
        }
        else {
            render template:'setAccount',
                plugin:pluginName,
                model:[id:id]
        }
    }

    def saveAccount = {
        GoogleCalendarSettings googleSettings = new GoogleCalendarSettings(login:params.googleLogin, password:params.googlePassword, product:Product.get(params.product))
        CalendarService googleService = googleCalendarService.getConnection(params.googleLogin, params.googlePassword);
        if(googleService) {
            googleSettings.save()
            googleCalendarService.createCalendar(googleService, googleSettings.login, googleSettings.password, CALENDAR_NAME)
            redirect(action:'index',params:[product:params.product])
        }
        else
            render(status:400,contentType:'application/json', text: [notice: [text: message(code: 'is.googleAgenda.error.wrongCredentials')]] as JSON)
    }

    def saveSettings = {
        GoogleCalendarSettings googleSettings = GoogleCalendarSettings.findByProduct(Product.get(params.product))
        googleSettings.displayDailyMeetings = (params.displayDailyMeetings) ? true : false
        googleSettings.displaySprintReview = (params.displaySprintReview) ? true : false
        googleSettings.displaySprintRetrospective = (params.displaySprintRetrospective) ? true : false
        googleSettings.displaySprintPlanning = (params.displaySprintPlanning) ? true : false
        googleSettings.displayReleasePlanning = (params.displayReleasePlanning) ? true : false
        if(googleSettings.save())
            render(status:200,contentType:'application/json', text: [notice: [text: message(code: 'is.googleAgenda.success.saveSettings')]] as JSON)
        else
            render(status:400,contentType:'application/json', text: [notice: [text: message(code: 'is.googleAgenda.error.saveSettings')]] as JSON)
    }

    // Vider l'agenda !!
    // Gestion des erreurs !!!
    def updateCalendar = {
        GoogleCalendarSettings googleSettings = GoogleCalendarSettings.findByProduct(Product.get(params.product))
        CalendarService googleService = googleCalendarService.getConnection(googleSettings.login, googleSettings.password)
        addScrumEvents(googleService, googleSettings)
        render(status:200,contentType:'application/json', text: [notice: [text: message(code: 'is.googleAgenda.success.updateCalendar')]] as JSON)
    }

    def addScrumEvents (googleService, googleSettings) {
        int sprint = 1
        def product = Product.get(params.product);
        product.releases?.each { r->
            r.sprints.asList().each { s->
                createSingleEvent(googleService,
                                  r.name + "-Sprint#" + sprint++,
                                  "no comment",
                                  iSDateToGoogleDate(s.startDate,true,false),
                                  iSDateToGoogleDate(s.endDate,true,true),
                                  googleSettings.login)
                if(s.state == Sprint.STATE_INPROGRESS)
                    addSprintMeetings(googleService, s.startDate, s.endDate, googleSettings)
            }
            sprint = 1
        }
    }

    def addSprintMeetings(googleService, startDate, endDate, googleSettings) {
        boolean longSprint = (endDate - startDate > SMALL_SPRINT_DURATION)
        Product currentProduct = Product.get(params.product)
        ProductPreferences preferences = currentProduct.preferences
        if(googleSettings.displayDailyMeetings){
            def hour = preferences.dailyMeetingHour.split(':')
            Date startHour = new Date();
            startHour.hours = Integer.parseInt(hour[0]);
            startHour.minutes = Integer.parseInt(hour[1]);
            createScrumMeetingEvent(googleService,
                                    getDailyScrumMeetingStartDate(startDate, longSprint),
                                    getDailyScrumMeetingEndDate(endDate, longSprint),
                                    startHour,
                                    googleSettings.login)
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

    def createSingleEvent(googleService, eventName, comment, startDate, endDate, login) {
        CalendarEventEntry newEvent = googleCalendarService.getNewEvent(eventName, comment)
        When eventTimes = new When()
        eventTimes.setStartTime(startDate)
        eventTimes.setEndTime(endDate)
        newEvent.addTime(eventTimes)
        return googleCalendarService.sendEvent(googleService, login, newEvent)
    }

    def createScrumMeetingEvent(googleService, startDate, endDate, startHour, login){
        def recurData = "DTSTART;VALUE=PERIOD:" +
                        startDate.format("yyyyMMdd'T'") +
                        startHour.format("HHmmss") +
                        "/PT15M\r\nRRULE:FREQ=WEEKLY;UNTIL=" +
                        endDate.format("yyyyMMdd") +
                        ";BYDAY=MO,TU,WE,TH,FR\r\n"
        Recurrence recur = new Recurrence()
        recur.setValue(recurData)
        CalendarEventEntry newEvent = googleCalendarService.getNewEvent("Scrum Meeting", null)
        newEvent.setRecurrence(recur)
        return googleCalendarService.sendEvent(googleService, login, newEvent)
    }

    def getDailyScrumMeetingStartDate(startDate, longSprint) {
        def computedDate = startDate
        if(longSprint)
            computedDate ++
        // Week-end exclusion because Google doesn't take the "byday" into account for the start date
        return getFirstWorkingDay(computedDate)
    }

    def getDailyScrumMeetingEndDate(endDate, longSprint) {
        def computedDate = endDate;
        if(!longSprint)
            computedDate ++
        return computedDate
    }

    def getFirstWorkingDay(date) {
        def computedDate = date;
        switch(date.getAt(Calendar.DAY_OF_WEEK)){
        case 1 : // SU
            computedDate += 1
            break
        case 7 : // SA
            computedDate += 2
            break
        }
        return computedDate
    }
}
