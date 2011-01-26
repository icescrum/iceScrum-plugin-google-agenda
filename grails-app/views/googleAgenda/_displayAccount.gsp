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
                value="${message(code: 'is.googleAgenda.ui.updateCalendar')}" />
            </p>
          </div>

      </div>
    </div>

    <div class="col2">
      <div class="panel-box">
        <h3 class="panel-box-title">
        </h3>
          <div class="panel-box-content">

              No description currently defined

          </div>

      </div>
    </div>
  </div>