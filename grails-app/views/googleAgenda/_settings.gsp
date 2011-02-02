<div class="dashboard">
  <div class="colset-2 clearfix">
      <div class="col1">
        <div class="panel-box">
          <h3 class="panel-box-title">${message(code:'is.googleAgenda.ui.accountTitle')}</h3>
            <div class="panel-box-content">
              <p>${message(code:'is.googleAgenda.ui.account')} <b>${login}</b></p>
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
                          <is:checkbox  name="displayDailyMeetings"
                                        value="${displayDailyMeetings}"
                                        label="${message(code: 'is.googleAgenda.ui.displayDailyMeetings')}" /><br />
                          <is:checkbox  name="displaySprintReview"
                                        value="${displaySprintReview}"
                                        label="${message(code: 'is.googleAgenda.ui.displaySprintReview')}" /><br />
                          <is:checkbox  name="displaySprintRetrospective"
                                        value="${displaySprintRetrospective}"
                                        label="${message(code: 'is.googleAgenda.ui.displaySprintRetrospective')}" /><br />
                          <is:checkbox  name="displayReleasePlanning"
                                        value="${displayReleasePlanning}"
                                        label="${message(code: 'is.googleAgenda.ui.displayReleasePlanning')}" /><br />
                          <is:checkbox  name="displaySprintPlanning"
                                        value="${displaySprintPlanning}"
                                        label="${message(code: 'is.googleAgenda.ui.displaySprintPlanning')}" /><br /><br />
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