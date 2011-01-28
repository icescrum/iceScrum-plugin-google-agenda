<div class="dashboard">
  <div class="colset-2 clearfix">

      <div class="col1">
        <div class="panel-box">
          <h3 class="panel-box-title">${message(code:'is.googleAgenda.ui.registeredAccountInformation')}</h3>
            <div class="panel-box-content">
              <p>${message(code:'is.googleAgenda.ui.registeredAccountIs')} <b>${login}</b></p>
              <p>
                 <is:button
                  url="[controller:id, action:'updateCalendar', params:[product:params.product]]"
                  type="submit"
                  remote="true"
                  history="false"
                  onSuccess="jQuery.icescrum.renderNotice(data.notice.text,data.notice.type)"
                  value="${message(code: 'is.googleAgenda.ui.updateCalendar')}" />
              </p>
              <p> </p>
            </div>
        </div>
      </div>
      <div class="col2">
          <div class="panel-box">
              <h3 class="panel-box-title">${message(code:'is.googleAgenda.ui.displaySettingsInformationTitle')}</h3>
              <div class="panel-box-content">
                  <p>${message(code:'is.googleAgenda.ui.displaySettingsInformation')}</p>
                  <form name="googleForm" method="post" onsubmit="$('input[name=googleButton]').click();return false;">
                          <is:checkbox  name="displaySettingsState"
                                        value="${displayDailyMeetings}"
                                        label="${message(code: 'is.googleAgenda.ui.allowRegularMeetings')}" />
                          <p> </p>
                          <is:button
                           remote="true"
                           history="false"
                           type="submit"
                           url="[controller:id, action:'setSettings', params:[product:params.product]]"
                           onSuccess="jQuery.icescrum.renderNotice(data.notice.text,data.notice.type)"
                           value="${message(code: 'is.googleAgenda.ui.saveDisplaySettings')}" />
                      <p> </p>
                    <p> </p>
                  </form>
              </div>
          </div>
      </div>
  </div>
</div>