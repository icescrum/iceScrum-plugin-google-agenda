<div id='login_form' class="box">
  <is:boxTile><g:message code="is.googleAgenda.ui.googleAgendaAdmin" /></is:boxTile>
  <form id="loginForm" name="adminForm" method="post" class='box-form box-form-small-legend box-content box-form-180' onsubmit="$('input[name=adminButton]').click();return false;">
  <is:fieldInformation nobordertop="false">
    <g:message code="is.ui.googleAgenda.ui.setAccountDescription"/>
  </is:fieldInformation>
  <is:fieldInput for="googleLogin" label="is.googleAgenda.ui.googleLogin">
    <is:input id="googleLogin" name="googleLogin" />
  </is:fieldInput>
  <is:fieldInput for="googlePassword" label="is.googleAgenda.ui.googlePassword">
    <is:password id="googlePassword" name="googlePassword" />
  </is:fieldInput>
  <is:buttonBar id="admin-button-bar">
    <is:button
              targetLocation="adminGoogleAgenda"
              id="connect"
              type="submitToRemote"
              url="[controller:'adminGoogleAgenda', action:'connectAccount']"
              value="${message(code:'is.googleAgenda.ui.connectAccount')}"/>
    <is:button type="link" button="button-s button-s-black"
              value="${message(code: 'is.button.cancel')}"/>
   </is:buttonBar>
</form>
</div>