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

<form id="googleForm" name="googleForm" method="post" class='box-form box-form-small-legend box-content box-form-180' onsubmit="$('input[name=googleButton]').click();return false;">
    <is:fieldset title="is.googleAgenda.ui.googleAgendaAdmin">
        <is:fieldInformation nobordertop="false">
          <g:message code="${login?'is.googleAgenda.ui.changeAccountDescription':'is.googleAgenda.ui.setAccountDescription'}"/>
        </is:fieldInformation>
        <is:fieldInput for="googleLogin" label="is.googleAgenda.ui.googleLogin">
          <is:input id="googleLogin" name="googleLogin" />
        </is:fieldInput>
        <is:fieldInput for="googlePassword" label="is.googleAgenda.ui.googlePassword">
          <is:password id="googlePassword" name="googlePassword" />
        </is:fieldInput>
        <is:buttonBar id="googleButtonBar">
          <is:button
                    id="buttonSubmit"
                    remote="true"
                    history="false"
                    update="window-content-${id}"
                    type="submitToRemote"
                    url="[controller:id, action:'saveAccount', params:[product:params.product]]"
                    value="${login?message(code:'is.googleAgenda.ui.changeAccount'):message(code:'is.googleAgenda.ui.connectAccount')}" />
        <g:if test="${login}">
          <is:button
                    id="buttonCancel"
                    remote="true"
                    history="false"
                    update="window-content-${id}"
                    type="submitToRemote"
                    url="[controller:id, action:'index', params:[product:params.product]]"
                    value="${message(code:'is.googleAgenda.ui.cancel')}" />
        </g:if>


        </is:buttonBar>

    </is:fieldset>

</form>
<is:shortcut key="return" callback="\$('#buttonSubmit').click();"/>