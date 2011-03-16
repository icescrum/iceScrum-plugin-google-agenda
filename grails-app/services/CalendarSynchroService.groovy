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

import org.springframework.context.ApplicationListener
import org.icescrum.core.event.IceScrumEvent
import org.icescrum.core.event.IceScrumSprintEvent
import org.icescrum.core.domain.Product
import org.icescrum.core.domain.Sprint
import org.icescrum.core.domain.User
import icescrum.plugin.google.agenda.GoogleCalendarSettings

class CalendarSynchroService implements ApplicationListener<IceScrumEvent> {

    def calendarEventService

    void onApplicationEvent(IceScrumEvent e) {
        try {
            if(e.source instanceof Product){
                manageProductEvent((Product)e.source, (User)e.doneBy, e.type)
            }
            else if(e.source instanceof Sprint){
                manageSprintEvent((Sprint)e.source, (User)e.doneBy, e.type)
            }
        }
        catch(Exception exception){
            if (log.debugEnabled) exception.printStackTrace()
        }
    }

    private void manageProductEvent (Product product, User author, String type) {
        if (type != IceScrumEvent.EVENT_DELETED) {
            GoogleCalendarSettings googleSettings = GoogleCalendarSettings.findByProduct(product)
            if (googleSettings) {
                if (type == IceScrumEvent.EVENT_UPDATED) {
                    //Fake hibernate session
                    product = Product.get(product.id)
                    author = User.get(author.id)
                    calendarEventService.updateWholeCalendar(product, author.preferences.language)
                    println "Updated calendar after product update"
                }
            }
        }
    }

    private void manageSprintEvent (Sprint sprint, User author, String type) {
        if (type != IceScrumEvent.EVENT_DELETED) {
            //Fake hibernate session
            author = User.get(author.id)
            sprint = Sprint.get(sprint.id)
            def product = Product.get(sprint.parentRelease.parentProduct.id)

            GoogleCalendarSettings googleSettings = GoogleCalendarSettings.findByProduct(product)
            if (googleSettings) {
                if (type == IceScrumEvent.EVENT_UPDATED) {
                    calendarEventService.updateWholeCalendar(product, author.preferences.language)
                    println "Updated calendar after sprint update"
                }
                else if (type == IceScrumEvent.EVENT_CREATED) {
                    // Chercher dynamiquement le bon numero de sprint
                    calendarEventService.addSprint(product, sprint, sprint.parentRelease.name)
                    println "Added sprint after sprint creation"
                }
                else if(type == IceScrumSprintEvent.EVENT_ACTIVATED) {
                    calendarEventService.addSprintMeetings(product, sprint, author.preferences.language)
                    println "Added meetings after sprint activation"
                }
            }
        }
    }
}