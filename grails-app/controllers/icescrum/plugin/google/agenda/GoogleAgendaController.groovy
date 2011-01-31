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

    def index = {
        GoogleCalendarSettings googleSettings = GoogleCalendarSettings.findByProduct(Product.get(params.product))
        if (googleSettings) {
            render template:'settings',
                  plugin:pluginName,
                  model:[id:id,login:googleSettings.login, displayDailyMeetings:googleSettings.displayDailyMeetings]
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
        }
        else
            render(status:400,contentType:'application/json', text: [notice: [text: message(code: 'is.googleAgenda.error.wrongCredentials')]] as JSON)
    }

    def saveSettings = {
        GoogleCalendarSettings googleSettings = GoogleCalendarSettings.findByProduct(Product.get(params.product))
        googleSettings.displayDailyMeetings = (params.displayDailyMeetings) ? true : false
        if(googleSettings.save())
            render(status:200,contentType:'application/json', text: [notice: [text: message(code: 'is.googleAgenda.success.saveSettings')]] as JSON)
        else
            render(status:400,contentType:'application/json', text: [notice: [text: message(code: 'is.googleAgenda.error.saveSettings')]] as JSON)
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
                GoogleCalendarSettings googleSettings = GoogleCalendarSettings.findByProduct(Product.get(params.product))
                if(googleSettings.displayDailyMeetings){
                  Date startHour = new Date();
                  startHour.hours = 9;
                  startHour.minutes = 0;
                  startHour.seconds = 0;
                  createScrumMeetingEvent(googleService, s.startDate, s.endDate, startHour)
                }

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
        CalendarEventEntry newEvent = getNewEvent(eventName, comment)
        When eventTimes = new When()
        eventTimes.setStartTime(startDate)
        eventTimes.setEndTime(endDate)
        newEvent.addTime(eventTimes)

        return sendEvent(googleService, newEvent)

    }

   def createScrumMeetingEvent(googleService,startDate, endDate, startHour){
     // weekDay
     // SU:1, MO:2, TH:3, WE:4, TU:5, FR:6, SA:7
     int weekDay = startDate.getAt(Calendar.DAY_OF_WEEK);
     switch(weekDay){
      case 1 :
        startDate = startDate + 2
      case 6 :
        startDate = startDate + 3
        break
      case 7 :
        startDate = startDate + 3
        break
      default :
        startDate = startDate + 1
        break;
     }
     CalendarEventEntry newEvent = getNewEvent("Scrum Meeting", null)

     DateFormat startFormatter = new SimpleDateFormat("yyyyMMdd'T'")
     DateFormat hourFormatter = new SimpleDateFormat("HHmmss")
     DateFormat endFormatter = new SimpleDateFormat("yyyyMMdd")

     def recurData = "DTSTART;VALUE=PERIOD:"
     recurData += startFormatter.format(startDate)
     recurData += hourFormatter.format(startHour)
     recurData += "/PT15M\r\nRRULE:FREQ=WEEKLY;UNTIL="
     recurData += endFormatter.format(endDate)
     recurData += ";BYDAY=MO,TU,WE,TH,FR\r\n"

     Recurrence recur = new Recurrence()
     recur.setValue(recurData)
     newEvent.setRecurrence(recur)

     return sendEvent(googleService, newEvent)
   }

  def getNewEvent(eventName, comment) {
    CalendarEventEntry newEvent = new CalendarEventEntry()
    newEvent.setTitle(new PlainTextConstruct(eventName))
    if(comment){
      newEvent.setContent(new PlainTextConstruct(comment))
    }
    return newEvent
  }

  def sendEvent(googleService, event){
    GoogleCalendarSettings googleSettings = GoogleCalendarSettings.findByProduct(Product.get(params.product))
    URL postUrl = new URL("https://www.google.com/calendar/feeds/"+googleSettings.login+"/private/full")

    return googleService.insert(postUrl, event)
  }
}
