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

import com.google.gdata.data.DateTime
import com.google.gdata.data.calendar.CalendarEventEntry
import com.google.gdata.data.extensions.When
import com.google.gdata.data.extensions.Recurrence
import com.google.gdata.client.calendar.CalendarService

import org.icescrum.core.domain.Product
import org.icescrum.core.domain.Sprint
import org.icescrum.core.domain.preferences.ProductPreferences
import icescrum.plugin.google.agenda.GoogleCalendarSettings
import com.google.gdata.data.calendar.CalendarEntry

import java.sql.Timestamp

class CalendarEventService {

    def googleCalendarService
    def messageSource
    def SMALL_SPRINT_DURATION = 7
    def CALENDAR_NAME = "iceScrum"

    def updateWholeCalendar (Product product, language) {
        GoogleCalendarSettings googleSettings = GoogleCalendarSettings.findByProduct(product)
        CalendarService googleService = googleCalendarService.getConnection(googleSettings.login, googleSettings.password)
        CalendarEntry c = googleCalendarService.getCalendar(googleService, googleSettings.login, CALENDAR_NAME)
        while(c == null) {
            initCalendar(product)
            //googleCalendarService.createCalendar(googleService, googleSettings.login, CALENDAR_NAME)
            c = googleCalendarService.getCalendar(googleService, googleSettings.login, CALENDAR_NAME)
        }
        Sprint currentSprint = null
        // possibilité d'utiliser les namedQueries de Sprint
        // pour récupérer la date de début du sprint courant ?
        product.releases?.each { release->
            release.sprints?.each { sprint->
                boolean sprintActivated = false
                String sprintStartDate = null
                if(sprint.state == Sprint.STATE_INPROGRESS) {
                    sprintActivated = true
                    sprintStartDate = iSDateToGoogleDate(sprint.startDate, false, false)
                    googleCalendarService.emptyCalendar(googleService, c, googleSettings.login, sprintStartDate)
                    addSprint(product, sprint, release.name)
                    addSprintMeetings(product, sprint, language)
                } else if((sprint.state == Sprint.STATE_WAIT) && sprintActivated) {
                    addSprint(product, sprint, release.name)
                } else if((sprint.state == Sprint.STATE_WAIT) && !sprintActivated) {
                    sprintActivated = true
                    sprintStartDate = iSDateToGoogleDate(sprint.startDate, false, false)
                    googleCalendarService.emptyCalendar(googleService, c, googleSettings.login, sprintStartDate)
                    addSprint(product, sprint, release.name)
                }
            }
        }
    }

    def addAllSprints(Product product) {
        product.releases?.each { release->
            release.sprints?.each { sprint->
                addSprint(product, sprint, release.name)
            }
        }
    }

    def initCalendar(Product product) {
        GoogleCalendarSettings googleSettings = GoogleCalendarSettings.findByProduct(product)
        CalendarService googleService = googleCalendarService.getConnection(googleSettings.login, googleSettings.password)
        CalendarEntry c = googleCalendarService.getCalendar(googleService, googleSettings.login, CALENDAR_NAME)
        if(c == null) {
            googleCalendarService.createCalendar(googleService, googleSettings.login, CALENDAR_NAME)
        } else {
            googleCalendarService.emptyCalendar(googleService, c, googleSettings.login, null)
        }
        addAllSprints(product)
    }

    def addSprint(Product product, Sprint sprint, releaseName) {
        GoogleCalendarSettings googleSettings = GoogleCalendarSettings.findByProduct(product)
        CalendarService googleService = googleCalendarService.getConnection(googleSettings.login, googleSettings.password)
        createSingleEvent(googleService,
                          releaseName + "-Sprint#" + sprint.orderNumber,
                          null,
                          iSDateToGoogleDate(sprint.startDate,true,false),
                          iSDateToGoogleDate(sprint.endDate,true,true),
                          googleSettings.login)
    }

    def addSprintMeetings(Product product, sprint, language) {
        GoogleCalendarSettings googleSettings = GoogleCalendarSettings.findByProduct(product)
        CalendarService googleService = googleCalendarService.getConnection(googleSettings.login, googleSettings.password)
        ProductPreferences preferences = product.preferences
        boolean longSprint = (sprint.endDate - sprint.startDate > SMALL_SPRINT_DURATION)
        Locale locale = new Locale(language)
        if(googleSettings.displayDailyMeetings){
            def hour = preferences.dailyMeetingHour.split(':')
            Date startHour = new Date();
            startHour.hours = Integer.parseInt(hour[0]);
            startHour.minutes = Integer.parseInt(hour[1]);
            createScrumMeetingEvent(googleService,
                                    messageSource.resolveCode('is.googleAgenda.ui.dailyMeeting',locale).format({} as Object[]),
                                    null,
                                    getDailyScrumMeetingStartDate(sprint.startDate, longSprint),
                                    getDailyScrumMeetingEndDate(sprint.endDate, longSprint),
                                    startHour,
                                    googleSettings.login)
        }
        if(googleSettings.displaySprintPlanning){
            def duration = getSprintWeekDuration(sprint.startDate, sprint.endDate)
            def hour = preferences.sprintPlanningHour.split(':')
            def sprintPlanning = getMeetingTimeInterval(sprint.startDate, hour, duration, false)
            createSingleEvent(googleService,
                                  messageSource.resolveCode('is.googleAgenda.ui.sprintPlanning',locale).format({} as Object[]),
                                  null,
                                  iSDateToGoogleDate(sprintPlanning.get(0),false,false),
                                  iSDateToGoogleDate(sprintPlanning.get(1),false,false),
                                  googleSettings.login)
        }
        if(googleSettings.displaySprintReview){
            def hour = preferences.sprintReviewHour.split(':')
            def sprintReview = getMeetingTimeInterval(sprint.endDate, hour, 2, true)
            createSingleEvent(googleService,
                                  messageSource.resolveCode('is.googleAgenda.ui.sprintReview',locale).format({} as Object[]),
                                  null,
                                  iSDateToGoogleDate(sprintReview.get(0),false,false),
                                  iSDateToGoogleDate(sprintReview.get(1),false,false),
                                  googleSettings.login)
        }
        if(googleSettings.displaySprintRetrospective){
            def hour = preferences.sprintRetrospectiveHour.split(':')
            def sprintRetrospective = getMeetingTimeInterval(sprint.endDate, hour, 1, true)
            createSingleEvent(googleService,
                                  messageSource.resolveCode('is.googleAgenda.ui.sprintRetrospective',locale).format({} as Object[]),
                                  null,
                                  iSDateToGoogleDate(sprintRetrospective.get(0),false,false),
                                  iSDateToGoogleDate(sprintRetrospective.get(1),false,false),
                                  googleSettings.login)
        }
        if(googleSettings.displayReleasePlanning){
          def duration = getSprintWeekDuration(sprint.startDate, sprint.endDate) - 1
          Date end = new Date()
          end.setTime(sprint.endDate.getTime())
          if(duration >= 2){
            end = end - 2
          }else {
            end = end - duration
          }
          Timestamp endDate = new Timestamp(end.getTime())
          def hour = preferences.releasePlanningHour.split(':')
          def releasePlanning = getMeetingTimeInterval(endDate, hour, 1, true)
          createSingleEvent(googleService,
                              messageSource.resolveCode('is.googleAgenda.ui.releasePlanning',locale).format({} as Object[]),
                              null,
                              iSDateToGoogleDate(releasePlanning.get(0), false, false),
                              iSDateToGoogleDate(releasePlanning.get(1), false, false),
                              googleSettings.login)
        }
    }

    def getMeetingTimeInterval(startDate, hour, duration, endOfSprint){
        // Define start date and end date of meeting
        Date start = new Date()
        start.setTime(startDate.getTime())
        Date end = new Date()
        end.setTime(start.getTime())
        start.setHours(Integer.parseInt(hour[0]))
        start.setMinutes(Integer.parseInt(hour[1]))
        end.setHours(Integer.parseInt(hour[0]) + duration)
        end.setMinutes(Integer.parseInt(hour[1]))

        if(endOfSprint){
            switch(start.getAt(Calendar.DAY_OF_WEEK)){
                case 1 : // SU
                    start -= 2
                    end -= 2
                    break
                case 7 : // SA
                    start -= 1
                    end -= 1
                    break
            }
        }
        else {
            switch(start.getAt(Calendar.DAY_OF_WEEK)){
                case 1 : // SU
                    start += 1
                    end += 1
                    break
                case 7 : // SA
                    start += 2
                    end += 2
                    break
            }
        }

        List<Timestamp> meetingTimes = new ArrayList<Timestamp>();
        meetingTimes.add(new Timestamp(start.getTime()))
        meetingTimes.add(new Timestamp(end.getTime()))
        return meetingTimes;
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

    def createScrumMeetingEvent(googleService, eventName, comment, startDate, endDate, startHour, login){
        def recurData = "DTSTART;VALUE=PERIOD:" +
                        startDate.format("yyyyMMdd'T'") +
                        startHour.format("HHmmss") +
                        "/PT15M\r\nRRULE:FREQ=WEEKLY;UNTIL=" +
                        endDate.format("yyyyMMdd") +
                        ";BYDAY=MO,TU,WE,TH,FR\r\n"
        Recurrence recur = new Recurrence()
        recur.setValue(recurData)
        CalendarEventEntry newEvent = googleCalendarService.getNewEvent(eventName, comment)
        newEvent.setRecurrence(recur)
        return googleCalendarService.sendEvent(googleService, login, newEvent)
    }

    def getDailyScrumMeetingStartDate(startDate, longSprint) {
        def computedDate = startDate
        // Week-end exclusion because Google doesn't take the "byday" into account for the start date
        return getFirstWorkingDay(computedDate, longSprint)
    }

    def getDailyScrumMeetingEndDate(endDate, longSprint) {
        def computedDate = endDate;
        if(!longSprint)
            computedDate ++
        return computedDate
    }

    def getSprintWeekDuration(startDate, endDate){
      Date start = new Date()
      start.setTime(startDate.getTime())
      Date end = new Date()
      end.setTime(endDate.getTime())
      def nbWeek = Math.floor((end - start) / 7)
      if((end - start) % 7)
        nbWeek++
      return (int)nbWeek
    }

    def getFirstWorkingDay(date, longSprint) {
        def computedDate = date;
        switch(date.getAt(Calendar.DAY_OF_WEEK)){
        case 1 : // SU
            if(longSprint)
              computedDate += 2
            else
              computedDate++
            break
        case 6 : // FR
            if(longSprint)
              computedDate += 3
            break
        case 7 : // SA
            if(longSprint)
              computedDate += 3
            else
              computedDate += 2
            break
         default :
            if(longSprint)
              computedDate++
        }
        return computedDate
    }


}