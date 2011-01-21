package icescrum.plugin.google.agenda

import grails.plugins.springsecurity.Secured
import org.icescrum.web.support.MenuBarSupport
import org.icescrum.core.domain.Product
import org.icescrum.core.domain.Sprint

import grails.converters.JSON

import com.google.gdata.client.calendar.CalendarService
import com.google.gdata.util.AuthenticationException
import java.net.URL
import com.google.gdata.data.DateTime
import com.google.gdata.data.PlainTextConstruct
import com.google.gdata.data.calendar.CalendarEventEntry
import com.google.gdata.data.extensions.When

@Secured('scrumMaster()')
class GoogleAgendaController {
    static final pluginName = 'ice-scrum-plugin-google-agenda'
    static final id = 'googleAgenda'
    static ui = true
    static menuBar = MenuBarSupport.productDynamicBar('is.ui.googleAgenda',id , false, 3)
    static window =  [title:'is.ui.googleAgenda',help:'is.ui.googleAgenda.help',toolbar:false]

    def index = {
        GoogleAccount projectAccount = GoogleAccount.findByProduct(Product.get(params.product))
        if (projectAccount) {
            render template:'displayAccount',
                  plugin:'iceScrum-plugin-google-agenda',
                  model:[id:id,login:projectAccount.login]
        }
        else {
            render template:'setAccount',
                plugin:'iceScrum-plugin-google-agenda',
                model:[id:id]
        }

        getSprints()
    }

    def saveAccount = {
        GoogleAccount projectAccount = new GoogleAccount(login:params.googleLogin, password:params.googlePassword, product:Product.get(params.product))
        if(getConnection(params.googleLogin, params.googlePassword)) {
            projectAccount.save()
            render(status:200,contentType:'application/json', text: [notice: [text: message(code:'Compte ok')]] as JSON)
        }
        else
            render(status:400,contentType:'application/json', text: [notice: [text: message(code: 'is.googleAgenda.error.wrongCredentials')]] as JSON)
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

    def updateCalendar = {
        GoogleAccount googleAccount = GoogleAccount.findByProduct(Product.get(params.product))
        CalendarService googleService = getConnection(googleAccount.login, googleAccount.password)
        int i = 0
        getSprints().each {
            createEvent(googleService, "sprint-" + i++, "no comment", convertDate(it.startDate), convertDate(it.endDate))
        }
        redirect(action:'index')
    }

    def getSprints = {
        def sprints = []
        def product = Product.get(params.product);
        product.releases?.each { r->
            r.sprints.asList().each { s->
                 sprints.add(s)
            }
        }
        return sprints.asList()
    }

    def convertDate (Date date) {
        return date.toString().replace(" ", "T").substring(0,date.toString().indexOf("."));
    }

   def createEvent(googleService, nom, commentaire, dateDep, dateFin) {
        println dateDep + "-> " + dateFin

        CalendarEventEntry newEvent = new CalendarEventEntry()
        newEvent.setTitle(new PlainTextConstruct(nom))
        newEvent.setContent(new PlainTextConstruct(commentaire))
        // Time : "2010-12-31T23:59:59"
        DateTime startTime = DateTime.parseDateTime(dateDep)
        DateTime endTime = DateTime.parseDateTime(dateFin)
        When eventTimes = new When()
        eventTimes.setStartTime(startTime)
        eventTimes.setEndTime(endTime)
        newEvent.addTime(eventTimes)

        GoogleAccount projectAccount = GoogleAccount.findByProduct(Product.get(params.product))
        URL postUrl = new URL("https://www.google.com/calendar/feeds/"+projectAccount.login+"/private/full")

        // Send the request and receive the response:
        CalendarEventEntry insertedEntry = googleService.insert(postUrl, newEvent)
    }
}
