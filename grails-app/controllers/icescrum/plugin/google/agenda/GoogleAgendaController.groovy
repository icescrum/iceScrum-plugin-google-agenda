/*
 * Copyright (c) 2011 BE ISI iSPlugins Université Paul Sabatier.
 *
 * This file is part of iceScrum.
 *
 * Google Agenda plugin is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
 *
 * Google Agenda plugin is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Google Agenda plugin.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Authors: 	Claude AUBRY (claude.aubry@gmail.com)
 * 		Vincent BARRIER (vbarrier@kagilum.com)
 *		Marc-Antoine BEAUVAIS (marcantoine.beauvais@gmail.com)
 *		Vincent CARASSUS (vincentcarassus@gmail.com)
 *		Gabriel GIL (contact.gabrielgil@gmail.com)
 *		Julien GOUDEAUX (julien.goudeaux@orange.fr)
 *		Guillaume JANDIN (guillaume.baz@gmail.com)
 *		Jihane KHALIL (khaliljihane@gmail.com)
 *		Paul LABONNE (paul.labonne@gmail.com)
 *		Nicolas NOULLET (nicolas.noullet@gmail.com)
 *		Bertrand PAGES (pages.bertrand@gmail.com)
 *		Jérémy SIMONKLEIN (jeremy.simonklein@gmail.com)
 *		Steven STREHL (steven.strehl@googlemail.com)
 *
 *
 */

package icescrum.plugin.google.agenda

import com.google.gdata.client.calendar.CalendarService

import grails.converters.JSON
import grails.plugins.springsecurity.Secured

import org.icescrum.core.domain.Product
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
                             enableSynchro:googleSettings.enableSynchro,
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
        def currentProduct = Product.get(params.product)
        GoogleCalendarSettings googleSettings = GoogleCalendarSettings.findByProduct(currentProduct)
        if(!googleSettings)
            googleSettings = new GoogleCalendarSettings(product:currentProduct)
        try {
            CalendarService googleService = googleCalendarService.getConnection(params.googleLogin, params.googlePassword);
            if(googleService) {
                googleSettings.login = params.googleLogin
                googleSettings.password = params.googlePassword
                googleSettings.save()
                calendarEventService.initCalendar(currentProduct)
                redirect(action:'index',params:[product:params.product])
            }
        }catch(RuntimeException e){
            render(status:400,contentType:'application/json', text: [notice: [text: message(code: e.getMessage())]] as JSON)
        }
    }

    def changeAccount = {
        GoogleCalendarSettings googleSettings = GoogleCalendarSettings.findByProduct(Product.get(params.product))

        render template:'window/setAccount',
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
        googleSettings.enableSynchro = (params.enableSynchro) ? true : false
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

    def dashboardLink = {
        GoogleCalendarSettings googleSettings = GoogleCalendarSettings.findByProduct(params.product)
        def googleLink = null
        if (googleSettings) {
            try {
                CalendarService googleService = googleCalendarService.getConnection(googleSettings.login, googleSettings.password)
                googleLink = googleCalendarService.getCalendarPublicURL(googleService, googleSettings.login, CALENDAR_NAME)
            }catch(RuntimeException e){}
        }
        params.product = params.product.id
        render template:'window/dashboardLink',
                plugin:pluginName,
                model:[id:id, googleLink:googleLink]
    }
}