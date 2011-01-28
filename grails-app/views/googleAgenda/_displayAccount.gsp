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
                  type="link"
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
                  <p> </p>
                  <form name="googleForm" method="post" onsubmit="$('input[name=googleButton]').click();return false;">
                          <is:checkbox>
                          </is:checkbox>
                          <p> </p>
                          <is:button
                           url="[controller:id, action:'saveDisplaySettings', params:[product:params.product]]"
                           type="link"
                           value="${message(code: 'is.googleAgenda.ui.saveDisplaySettings')}" />
                      <p> </p>
                    <p> </p>
                  </form>
              </div>
          </div>
      </div>
  </div>
</div>