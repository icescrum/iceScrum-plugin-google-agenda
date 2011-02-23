package icescrum.plugin.google.agenda

import com.google.gdata.client.calendar.CalendarService

import grails.converters.JSON
import grails.plugins.springsecurity.Secured

import org.icescrum.core.domain.Product
import org.icescrum.core.domain.Sprint
import org.icescrum.core.domain.User
import org.icescrum.core.domain.preferences.ProductPreferences

import org.icescrum.core.support.MenuBarSupport

@Secured('scrumMaster()')
class GoogleAgendaController {

    // Gestion des erreurs !!!!!!!!!!!!!!!!
    // Calendrier iceScrum non présent pour l'insertion des éléments,
    // pour l'affichage du lien, pb de connexion etc

    static final pluginName = 'icescrum-plugin-google-agenda'
    static final id = 'googleAgenda'
    static ui = true
    static menuBar = MenuBarSupport.productDynamicBar('is.googleAgenda.ui',id , false, 3)
    static window =  [title:'is.googleAgenda.ui',help:'is.googleAgenda.ui.help',toolbar:false]

    def googleCalendarService
    def calendarEventService
    def springSecurityService

    def CALENDAR_NAME = "iceScrum"

    def index = {
        Product currentProduct = Product.get(params.product)
        ProductPreferences preferences = currentProduct.preferences
        GoogleCalendarSettings googleSettings = GoogleCalendarSettings.findByProduct(currentProduct)
        if (googleSettings) {
            try {
                CalendarService googleService = googleCalendarService.getConnection(googleSettings.login, googleSettings.password)
                def googleLink = googleCalendarService.getCalendarPublicURL(googleService, googleSettings.login, CALENDAR_NAME)
                render template:'window/settings',
                      plugin:pluginName,
                      model:[id:id,
                             login:googleSettings.login,
                             displayDailyMeetings:googleSettings.displayDailyMeetings,
                             displaySprintReview:googleSettings.displaySprintReview,
                             displaySprintRetrospective:googleSettings.displaySprintRetrospective,
                             displayReleasePlanning:googleSettings.displayReleasePlanning,
                             displaySprintPlanning:googleSettings.displaySprintPlanning,
                             dailyMeetingHour:preferences.dailyMeetingHour,
                             sprintReviewHour:preferences.sprintReviewHour,
                             sprintRetrospectiveHour:preferences.sprintRetrospectiveHour,
                             releasePlanningHour:preferences.releasePlanningHour,
                             sprintPlanningHour:preferences.sprintPlanningHour,
                             googleLink:googleLink]
            }catch(RuntimeException e){
                render template:'window/blank',
                plugin:pluginName,
                model:[id:id]
            }
        }
        else {
            render template:'window/setAccount',
                plugin:pluginName,
                model:[id:id]
        }
    }

    def saveAccount = {
        GoogleCalendarSettings googleSettings = new GoogleCalendarSettings(login:params.googleLogin, password:params.googlePassword, product:Product.get(params.product))
        try {
            CalendarService googleService = googleCalendarService.getConnection(params.googleLogin, params.googlePassword);
            if(googleService) {
                googleSettings.save()
                googleCalendarService.createCalendar(googleService, googleSettings.login, CALENDAR_NAME)
                redirect(action:'index',params:[product:params.product])
            }
        }catch(RuntimeException e){
            render(status:400,contentType:'application/json', text: [notice: [text: message(code: e.getMessage())]] as JSON)
        }
    }

    def changeAccount = {
        Product currentProduct = Product.get(params.product)
        GoogleCalendarSettings googleSettings = GoogleCalendarSettings.findByProduct(currentProduct)

        render template:'window/changeAccount',
                plugin:pluginName,
                model:[id:id, login:googleSettings.login]
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

    def updateCalendar = {
        Product currentProduct = Product.get(params.product)
        def language = User.get(springSecurityService.principal.id).preferences.language
        try {
            calendarEventService.updateWholeCalendar(currentProduct, language)
            render(status:200,contentType:'application/json', text: [notice: [text: message(code: 'is.googleAgenda.success.updateCalendar')]] as JSON)
        }catch(RuntimeException e){
            render(status:400,contentType:'application/json', text: [notice: [text: message(code: e.getMessage())]] as JSON)
        }
    }
}