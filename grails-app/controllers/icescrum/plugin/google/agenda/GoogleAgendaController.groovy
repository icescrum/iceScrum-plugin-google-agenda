package icescrum.plugin.google.agenda

import grails.plugins.springsecurity.Secured

import com.google.gdata.client.calendar.CalendarService
import com.google.gdata.data.DateTime
import com.google.gdata.data.PlainTextConstruct
import com.google.gdata.data.calendar.CalendarEventEntry
import com.google.gdata.data.calendar.CalendarEntry;
import com.google.gdata.data.extensions.When
import com.google.gdata.util.AuthenticationException
import com.google.gdata.data.calendar.CalendarFeed;
import grails.converters.JSON
import org.icescrum.core.domain.Product
import org.icescrum.core.domain.Sprint
import org.icescrum.web.support.MenuBarSupport
import org.icescrum.core.domain.preferences.ProductPreferences
import com.google.gdata.data.extensions.Recurrence
import java.text.SimpleDateFormat
import java.text.DateFormat

@Secured('scrumMaster()')
class GoogleAgendaController {
    static final pluginName = 'icescrum-plugin-google-agenda'
    static final id = 'googleAgenda'
    static ui = true
    static menuBar = MenuBarSupport.productDynamicBar('is.googleAgenda.ui',id , false, 3)
    static window =  [title:'is.googleAgenda.ui',help:'is.googleAgenda.ui.help',toolbar:false]

    def CALENDAR_NAME = "iceScrum"
    def SMALL_SPRINT_DURATION = 7

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
        if(getConnection(params.googleLogin, params.googlePassword)) {
            googleSettings.save()
            redirect(action:'index',params:[product:params.product])
            createCalendar()
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

    def getConnection(login, password) {
        CalendarService googleService = new CalendarService("iceScrum")
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
    def updateCalendar = {
        GoogleCalendarSettings googleSettings = GoogleCalendarSettings.findByProduct(Product.get(params.product))
        CalendarService googleService = getConnection(googleSettings.login, googleSettings.password)
        addScrumEvents (googleService)
        render(status:200,contentType:'application/json', text: [notice: [text: message(code: 'is.googleAgenda.success.updateCalendar')]] as JSON)
    }

    def addScrumEvents (googleService) {
        int sprint = 1
        def product = Product.get(params.product);
        product.releases?.each { r->
            r.sprints.asList().each { s->
                createSingleEvent(googleService,
                                  r.name + "-Sprint#" + sprint++,
                                  "no comment",
                                  iSDateToGoogleDate(s.startDate,true,false),
                                  iSDateToGoogleDate(s.endDate,true,true))
                if(s.state == Sprint.STATE_INPROGRESS)
                    addSprintMeetings(googleService, s.startDate, s.endDate)
            }
            sprint = 1
        }
    }

    def addSprintMeetings(googleService, startDate, endDate) {
        Product currentProduct = Product.get(params.product)
        GoogleCalendarSettings googleSettings = GoogleCalendarSettings.findByProduct(currentProduct)
        ProductPreferences preferences = currentProduct.preferences
        if(googleSettings.displayDailyMeetings){
            def hour = preferences.dailyMeetingHour.split(':')
            Date startHour = new Date();
            startHour.hours = Integer.parseInt(hour[0]);
            startHour.minutes = Integer.parseInt(hour[1]);
            createScrumMeetingEvent(googleService, startDate, endDate, startHour)
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
        CalendarEventEntry newEvent = getNewEvent(eventName, comment)
        When eventTimes = new When()
        eventTimes.setStartTime(startDate)
        eventTimes.setEndTime(endDate)
        newEvent.addTime(eventTimes)
        return sendEvent(googleService, newEvent)
    }

    def createScrumMeetingEvent(googleService,startDate, endDate, startHour){

        boolean longsprint = (endDate - startDate > SMALL_SPRINT_DURATION)
        // WeekEnd exclusion
        switch(startDate.getAt(Calendar.DAY_OF_WEEK)){
            case 1 : // SU
                startDate += 1
                break
            case 6 : // FR
                if(longsprint)
                  startDate += 2
                break
            case 7 : // SA
                startDate += 2
                break
        }

        // Add "margins" to the sprint meetings if it lates more than 7 days
        if(longsprint)
            startDate ++
        else
            endDate ++

        def recurData = "DTSTART;VALUE=PERIOD:"
        recurData += startDate.format("yyyyMMdd'T'")
        recurData += startHour.format("HHmmss")
        recurData += "/PT15M\r\nRRULE:FREQ=WEEKLY;UNTIL="
        recurData += endDate.format("yyyyMMdd")
        recurData += ";BYDAY=MO,TU,WE,TH,FR\r\n"

        Recurrence recur = new Recurrence()
        recur.setValue(recurData)
        CalendarEventEntry newEvent = getNewEvent("Scrum Meeting", null)
        newEvent.setRecurrence(recur)

        return sendEvent(googleService, newEvent)
    }

    def getCalendar(CalendarService service, String calendarName) {
        GoogleCalendarSettings googleSettings = GoogleCalendarSettings.findByProduct(Product.get(params.product))
        URL feedUrl = null
        CalendarFeed resultFeed = null
        try {
            feedUrl = new URL("https://www.google.com/calendar/feeds/"+googleSettings.login+"/owncalendars/full")
            resultFeed = service.getFeed(feedUrl, CalendarFeed.class)
        } catch (Exception e) {
            e.printStackTrace()
        }

        for(int i = 0; i < resultFeed.getEntries().size(); i++)
            if(resultFeed.getEntries().get(i).getTitle().getPlainText().equals(calendarName))
                return resultFeed.getEntries().get(i)

        return null;
    }


    def createCalendar() {
        GoogleCalendarSettings googleSettings = GoogleCalendarSettings.findByProduct(Product.get(params.product))
        CalendarService service = getConnection(googleSettings.login, googleSettings.password)
        CalendarEntry calendar = getCalendar(service, CALENDAR_NAME)
        if((calendar == null)) {
            // Create the calendar
            calendar = new CalendarEntry()
            calendar.setTitle(new PlainTextConstruct(CALENDAR_NAME))
            calendar.setSummary(new PlainTextConstruct("Auto-generated by iceScrum"))
            // Insert the calendar
            return postCalendar(service, calendar)
        }
        return calendar;
    }

    def postCalendar(CalendarService service, CalendarEntry calendar) {
        GoogleCalendarSettings googleSettings = GoogleCalendarSettings.findByProduct(Product.get(params.product))
        try {
            URL postUrl = new URL("https://www.google.com/calendar/feeds/"+googleSettings.login+"/owncalendars/full")
            return service.insert(postUrl, calendar);
        } catch (Exception e) {
            e.printStackTrace()
        }
        return calendar;
    }


    def deleteCalendar(CalendarService service, String calendarName) {
        CalendarEntry calendar = getCalendar(service, calendarName)
        try {
            calendar.delete()
        } catch (Exception e) {
            System.out.println("Unable to delete calendar : " + calendar.getTitle().getPlainText())
        }
    }

    def getNewEvent(eventName, comment) {
        CalendarEventEntry newEvent = new CalendarEventEntry()
        newEvent.setTitle(new PlainTextConstruct(eventName))
        if(comment)
            newEvent.setContent(new PlainTextConstruct(comment))
        return newEvent
    }

    def sendEvent(googleService, event){
        GoogleCalendarSettings googleSettings = GoogleCalendarSettings.findByProduct(Product.get(params.product))
        URL postUrl = new URL("https://www.google.com/calendar/feeds/"+googleSettings.login+"/private/full")
        return googleService.insert(postUrl, event)
    }

}
