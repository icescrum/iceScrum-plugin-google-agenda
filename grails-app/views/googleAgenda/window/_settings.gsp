<%--*
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
 *--%>

<div class="dashboard">
  <div class="colset-2 clearfix">
      <div class="col1">
        <div class="panel-box">
          <h3 class="panel-box-title">${message(code:'is.googleAgenda.ui.accountTitle')}</h3>
            <div class="panel-box-content">
              <p>${message(code:'is.googleAgenda.ui.account')} <b>${login}</b>
                (<is:link
                  class="scrum-link"
                  remote="true"
                  history="false"
                  update="window-content-${id}"
                  url="[controller:id, action:'changeAccount', params:[product:params.product]]">${message(code:'is.googleAgenda.ui.modify')}</is:link>)</p>
              <p>${message(code: 'is.googleAgenda.ui.linkToCalendar')}
                  <is:link id="googleLink" name="googleLink" class="scrum-link" target="_blank" url="${googleLink}">Google Link</is:link>
              </p>
              <p>
                 <is:button
                  url="[controller:id, action:'updateCalendar', params:[product:params.product]]"
                  type="submit"
                  remote="true"
                  history="false"
                  onSuccess="jQuery.icescrum.renderNotice(data.notice.text,data.notice.type)"
                  value="${message(code: 'is.googleAgenda.ui.updateCalendar')}" />
              </p>
              <br/>
            </div>
        </div>
      </div>
      <div class="col2">
          <div class="panel-box">
              <h3 class="panel-box-title">${message(code:'is.googleAgenda.ui.settingsTitle')}</h3>
              <div class="panel-box-content">
                  <p>${message(code:'is.googleAgenda.ui.settings')}</p>
                  <form name="googleForm" method="post" onsubmit="$('input[name=googleButton]').click();return false;">
                          <is:checkbox  name="displayReleasePlanning"
                                        value="${displayReleasePlanning}"
                                        label="${message(code: 'is.googleAgenda.ui.releasePlanning')+' ('+releasePlanningHour+')'}" /><br/>
                          <is:checkbox  name="displaySprintPlanning"
                                        value="${displaySprintPlanning}"
                                        label="${message(code: 'is.googleAgenda.ui.sprintPlanning')+' ('+sprintPlanningHour+')'}" /><br/>
                          <is:checkbox  name="displayDailyMeetings"
                                        value="${displayDailyMeetings}"
                                        label="${message(code: 'is.googleAgenda.ui.dailyMeeting')+' ('+dailyMeetingHour+')'}" /><br/>
                          <is:checkbox  name="displaySprintReview"
                                        value="${displaySprintReview}"
                                        label="${message(code: 'is.googleAgenda.ui.sprintReview')+' ('+sprintReviewHour+')'}" /><br/>
                          <is:checkbox  name="displaySprintRetrospective"
                                        value="${displaySprintRetrospective}"
                                        label="${message(code: 'is.googleAgenda.ui.sprintRetrospective')+' ('+sprintRetrospectiveHour+')'}" /><br/><br/>
                          <is:button
                           remote="true"
                           history="false"
                           type="submit"
                           url="[controller:id, action:'saveSettings', params:[product:params.product]]"
                           onSuccess="jQuery.icescrum.renderNotice(data.notice.text,data.notice.type)"
                           value="${message(code: 'is.googleAgenda.ui.saveSettings')}" />
                  </form>
                  <br/>
              </div>
          </div>
      </div>
  </div>
</div>